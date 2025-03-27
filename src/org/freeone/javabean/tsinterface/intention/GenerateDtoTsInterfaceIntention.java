package org.freeone.javabean.tsinterface.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.freeone.javabean.tsinterface.service.DtoTypescriptGeneratorService;
import org.freeone.javabean.tsinterface.swing.TypescriptInterfaceShowerWrapper;
import org.freeone.javabean.tsinterface.util.TypescriptContentGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用於檢測方法簽名中的 DTO 類並提供生成 TypeScript 接口的選項
 */
public class GenerateDtoTsInterfaceIntention extends PsiElementBaseIntentionAction implements IntentionAction {

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        // 檢查是否在預覽模式下
        if (editor == null || isPreviewMode(project)) {
            return; // 預覽模式下不執行實際操作
        }

        // 確保模型讀取在讀取線程中執行
        ReadAction.run(() -> {
            // 尋找包含當前元素的方法聲明
            PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
            if (method == null) {
                return;
            }

            // 收集所有需要處理的 DTO 類
            List<PsiClass> dtoClasses = collectDtoClasses(method);
            if (dtoClasses.isEmpty()) {
                ApplicationManager.getApplication().invokeLater(
                        () -> Messages.showMessageDialog("沒有找到要處理的 DTO 類", "提示", Messages.getInformationIcon()));
                return;
            }

            // 生成 TypeScript 接口
            Map<String, String> contentMap = new HashMap<>();
            for (PsiClass psiClass : dtoClasses) {
                try {
                    TypescriptContentGenerator.processPsiClass(project, psiClass, false);
                    String content = TypescriptContentGenerator.mergeContent(psiClass, false);
                    contentMap.put(psiClass.getName(), content);
                    TypescriptContentGenerator.clearCache();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (contentMap.isEmpty()) {
                ApplicationManager.getApplication().invokeLater(
                        () -> Messages.showMessageDialog("生成 TypeScript 接口失敗", "錯誤", Messages.getErrorIcon()));
                return;
            }

            // 合併內容
            final String mergedContent = mergeContent(contentMap);
            final Map<String, String> finalContentMap = contentMap;

            // 所有 UI 操作必須在 EDT 中執行
            ApplicationManager.getApplication().invokeLater(() -> {
                // 顯示選項菜單
                List<String> options = List.of("保存到文件", "複製到剪貼板", "在文本框中編輯");
                ListPopup popup = JBPopupFactory.getInstance().createListPopup(
                        new BaseListPopupStep<>("選擇操作", options) {
                            @Override
                            public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
                                return doFinalStep(() -> {
                                    if (selectedValue.equals("保存到文件")) {
                                        DtoTypescriptGeneratorService.saveToFiles(project, finalContentMap);
                                    } else if (selectedValue.equals("複製到剪貼板")) {
                                        copyToClipboard(project, mergedContent);
                                    } else if (selectedValue.equals("在文本框中編輯")) {
                                        // 使用帶有類名支持的顯示方法
                                        DtoTypescriptGeneratorService.showInTextEditor(project, finalContentMap);
                                    }
                                });
                            }
                        });

                popup.showInBestPositionFor(editor);
            });
        });
    }

    private String mergeContent(Map<String, String> contentMap) {
        if (contentMap.size() == 1) {
            return contentMap.values().iterator().next();
        } else {
            StringBuilder mergedContent = new StringBuilder();
            for (Map.Entry<String, String> entry : contentMap.entrySet()) {
                mergedContent.append(entry.getValue()).append("\n\n");
            }
            return mergedContent.toString();
        }
    }

    private void copyToClipboard(Project project, String content) {
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable tText = new StringSelection(content);
        systemClipboard.setContents(tText, null);

        // 移除彈窗提示，改用更不擾人的方式
        System.out.println("已將 TypeScript 接口複製到剪貼板，長度: " + content.length());

        // 如果真的需要向用戶顯示提示，可以使用狀態欄通知，但不推薦彈窗
        // Messages.showMessageDialog("已將 TypeScript 接口複製到剪貼板", "提示",
        // Messages.getInformationIcon());
    }

    private void showInTextEditor(String content) {
        // 確保有內容才顯示
        if (content != null && !content.trim().isEmpty()) {
            // 不添加額外標記，保持內容的原始格式
            final String finalContent = content;

            // 使用獨立的線程安全方式顯示
            SwingUtilities.invokeLater(() -> {
                TypescriptInterfaceShowerWrapper wrapper = new TypescriptInterfaceShowerWrapper();
                wrapper.setContent(finalContent);

                // 從內容中提取類名
                String classNames = extractClassNames(content);
                if (!classNames.isEmpty()) {
                    wrapper.setClassName(classNames);
                }

                wrapper.show();
            });
        } else {
            // 如果內容為空，顯示錯誤消息
            Messages.showMessageDialog("生成的 TypeScript 接口內容為空", "錯誤", Messages.getErrorIcon());
        }
    }

    /**
     * 從 TypeScript 內容中提取類名
     */
    private String extractClassNames(String content) {
        StringBuilder classNames = new StringBuilder();

        // 嘗試匹配所有 interface 定義
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("interface\\s+([A-Za-z0-9_]+)");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            if (classNames.length() > 0) {
                classNames.append(", ");
            }
            classNames.append(matcher.group(1));
        }

        return classNames.toString();
    }

    /**
     * 收集方法簽名中的所有 DTO 類
     */
    private List<PsiClass> collectDtoClasses(PsiMethod method) {
        return ReadAction.compute(() -> {
            List<PsiClass> dtoClasses = new ArrayList<>();

            // 檢查返回類型
            PsiType returnType = method.getReturnType();
            if (returnType != null) {
                addDtoClassesFromType(returnType, dtoClasses);
            }

            // 檢查參數類型
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (PsiParameter parameter : parameters) {
                PsiType parameterType = parameter.getType();
                addDtoClassesFromType(parameterType, dtoClasses);
            }

            return dtoClasses;
        });
    }

    /**
     * 從類型中提取 DTO 類
     */
    private void addDtoClassesFromType(PsiType type, List<PsiClass> dtoClasses) {
        // 處理泛型類型
        if (type instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) type;
            PsiClass psiClass = ReadAction.compute(() -> classType.resolve());

            if (psiClass != null && isDtoClass(psiClass)) {
                dtoClasses.add(psiClass);
            }

            // 處理泛型參數
            PsiType[] parameters = classType.getParameters();
            for (PsiType parameter : parameters) {
                addDtoClassesFromType(parameter, dtoClasses);
            }
        }
    }

    /**
     * 判斷一個類是否為 DTO 類
     */
    private boolean isDtoClass(PsiClass psiClass) {
        // 通常 DTO 類是普通 Java 類，不是接口、枚舉或者註解
        if (psiClass.isInterface() || psiClass.isEnum() || psiClass.isAnnotationType()) {
            return false;
        }

        // 通常 DTO 類名稱包含一些特定的模式
        String className = psiClass.getName();
        if (className == null) {
            return false;
        }

        return className.endsWith("DTO")
                || className.endsWith("Dto")
                || className.endsWith("Request")
                || className.endsWith("Response")
                || className.endsWith("Rq")
                || className.endsWith("Rs")
                || className.endsWith("Tranrq")
                || className.endsWith("Tranrs");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // 檢查是否在方法聲明中
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method == null) {
            return false;
        }

        // 只對帶有 RequestMapping 相關註解的方法啟用
        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && (qualifiedName.endsWith("RequestMapping") ||
                    qualifiedName.endsWith("GetMapping") ||
                    qualifiedName.endsWith("PostMapping") ||
                    qualifiedName.endsWith("PutMapping") ||
                    qualifiedName.endsWith("DeleteMapping"))) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return "生成 DTO 的 TypeScript 接口";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Java DTO 到 TypeScript 接口";
    }

    /**
     * 檢查當前是否在預覽模式下
     */
    private boolean isPreviewMode(Project project) {
        return ApplicationManager.getApplication().isUnitTestMode() ||
                project == null ||
                project.isDisposed() ||
                !ApplicationManager.getApplication().isDispatchThread(); // 非 EDT 線程可能是預覽
    }
}