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
import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceProjectSettings;
import org.freeone.javabean.tsinterface.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;

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
                                    try {
                                        // 使用 invokeLater 確保 UI 操作在 EDT 上執行，並且在 popup 關閉後執行
                                        // 使用 defaultModalityState 而不是 ANY 以避免 TransactionGuard 錯誤
                                        ApplicationManager.getApplication().invokeLater(() -> {
                                            try {
                                                if (selectedValue.equals("保存到文件")) {
                                                    DtoTypescriptGeneratorService.saveToFiles(project, finalContentMap);
                                                } else if (selectedValue.equals("複製到剪貼板")) {
                                                    copyToClipboard(project, mergedContent);
                                                } else if (selectedValue.equals("在文本框中編輯")) {
                                                    // 使用帶有類名支持的顯示方法
                                                    DtoTypescriptGeneratorService.showInTextEditor(project,
                                                            finalContentMap);
                                                }
                                            } catch (Exception e) {
                                                // 捕獲並記錄任何執行操作時的錯誤
                                                System.err.println("執行所選操作時發生錯誤: " + e.getMessage());
                                                e.printStackTrace();
                                            }
                                        }, com.intellij.openapi.application.ModalityState.defaultModalityState());
                                    } catch (Exception e) {
                                        // 捕獲並記錄任何在設置 invokeLater 時的錯誤
                                        System.err.println("設置操作執行時發生錯誤: " + e.getMessage());
                                        e.printStackTrace();
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

        // 使用通知而不是彈窗
        Notification notification = com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("JavaDtoToTypescriptInterface")
                .createNotification("已將 TypeScript 接口複製到剪貼板",
                        com.intellij.notification.NotificationType.INFORMATION);
        notification.setImportant(false).notify(project);
    }

    private void showInTextEditor(String content) {
        // 確保有內容才顯示
        if (content != null && !content.trim().isEmpty()) {
            // 不添加額外標記，保持內容的原始格式
            final String finalContent = content;

            // 使用獨立的線程安全方式顯示
            ApplicationManager.getApplication().invokeLater(() -> {
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

            // 檢查參數類型
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (PsiParameter parameter : parameters) {
                PsiType parameterType = parameter.getType();
                addDtoClassesFromType(parameterType, dtoClasses);
            }

            // 檢查返回類型
            PsiType returnType = method.getReturnType();
            if (returnType != null) {
                addDtoClassesFromType(returnType, dtoClasses);
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
        // 確保不是接口、枚舉或註解
        if (psiClass.isInterface() || psiClass.isEnum() || psiClass.isAnnotationType()) {
            return false;
        }

        // 獲取類名
        String className = psiClass.getName();
        if (className == null) {
            return false;
        }

        System.out.println("Intention檢查類是否為DTO: " + className);

        // 大小寫不敏感的檢查
        String lowerClassName = className.toLowerCase();

        // 檢查是否包含Qrystatement、Query或Qry
        if (lowerClassName.contains("qrystatement") ||
                lowerClassName.contains("qry") ||
                lowerClassName.contains("query")) {
            System.out.println("  Intention匹配到查詢關鍵字: " + className);
            return true;
        }

        // 獲取項目設置
        Project project = psiClass.getProject();
        JavaBeanToTypescriptInterfaceProjectSettings settings = CommonUtils.getProjectSettings(project);

        // 輸出調試信息
        System.out.println("  Intention當前項目設置中請求DTO後綴: " + settings.getEffectiveRequestDtoSuffixes());
        System.out.println("  Intention當前項目設置中響應DTO後綴: " + settings.getEffectiveResponseDtoSuffixes());

        // 檢查是否包含常見的DTO相關詞
        if (lowerClassName.contains("dto") ||
                lowerClassName.contains("model") ||
                lowerClassName.contains("bean") ||
                lowerClassName.contains("vo") ||
                lowerClassName.contains("entity") ||
                lowerClassName.contains("request") ||
                lowerClassName.contains("response") ||
                lowerClassName.contains("result")) {
            System.out.println("  Intention匹配到常見DTO關鍵字: " + className);
            return true;
        }

        // 檢查請求DTO後綴
        List<String> requestSuffixes = settings.getEffectiveRequestDtoSuffixes();
        for (String suffix : requestSuffixes) {
            if (className.endsWith(suffix)) {
                System.out.println("  Intention匹配到請求DTO後綴 " + suffix + ": " + className);
                return true;
            }
        }

        // 檢查響應DTO後綴
        List<String> responseSuffixes = settings.getEffectiveResponseDtoSuffixes();
        for (String suffix : responseSuffixes) {
            if (className.endsWith(suffix)) {
                System.out.println("  Intention匹配到響應DTO後綴 " + suffix + ": " + className);
                return true;
            }
        }

        // 如果後綴檢查沒通過，則檢查類結構
        boolean isDto = checkClassStructure(psiClass);
        System.out.println("  Intention基於類結構判斷 " + className + " 是否為DTO: " + isDto);
        return isDto;
    }

    /**
     * 根據類結構判斷是否為DTO類
     */
    private boolean checkClassStructure(PsiClass psiClass) {
        // 檢查類中是否存在公開的字段或者 getter/setter 方法
        // 通常 DTO 類會包含多個公開字段或 getter/setter 方法
        PsiField[] fields = psiClass.getFields();
        if (fields.length > 0) {
            int publicFieldCount = 0;
            for (PsiField field : fields) {
                if (field.hasModifierProperty(PsiModifier.PUBLIC)) {
                    publicFieldCount++;
                }
            }
            // 如果有多個公開字段，可能是數據傳輸對象
            if (publicFieldCount > 2) {
                return true;
            }
        }

        // 檢查 getter/setter 方法
        PsiMethod[] methods = psiClass.getMethods();
        int getterSetterCount = 0;
        for (PsiMethod method : methods) {
            String methodName = method.getName();
            if ((methodName.startsWith("get") || methodName.startsWith("set")) &&
                    methodName.length() > 3 &&
                    Character.isUpperCase(methodName.charAt(3))) {
                getterSetterCount++;
            }
        }
        // 如果有多個 getter/setter 方法，可能是 DTO
        return getterSetterCount > 3;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // 檢查是否在方法聲明中
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method == null) {
            return false;
        }

        // 檢查是否在控制器類中
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return false;
        }

        // 檢查類是否有 Controller 相關註解
        PsiAnnotation[] classAnnotations = containingClass.getAnnotations();
        for (PsiAnnotation annotation : classAnnotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && (qualifiedName.endsWith("Controller") ||
                    qualifiedName.endsWith("RestController"))) {
                return true;
            }
        }

        // 檢查方法是否有 RequestMapping 相關註解
        PsiAnnotation[] methodAnnotations = method.getAnnotations();
        for (PsiAnnotation annotation : methodAnnotations) {
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