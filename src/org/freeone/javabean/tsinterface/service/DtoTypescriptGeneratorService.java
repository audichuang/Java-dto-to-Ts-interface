package org.freeone.javabean.tsinterface.service;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiClassType;
import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceProjectSettings;
import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceSettingsState;
import org.freeone.javabean.tsinterface.swing.TypescriptInterfaceShowerWrapper;
import org.freeone.javabean.tsinterface.util.CommonUtils;
import org.freeone.javabean.tsinterface.util.TypescriptContentGenerator;
import org.freeone.javabean.tsinterface.util.TypescriptUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多 DTO 類的 TypeScript 接口生成服務
 */
public class DtoTypescriptGeneratorService {

    // 使用 NotificationGroupManager 獲取通知組
    private static final NotificationGroup notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("JavaDtoToTypescriptInterface");

    /**
     * 為多個 DTO 類生成 TypeScript 接口
     *
     * @param project    當前項目
     * @param dtoClasses 要處理的 DTO 類列表
     * @param saveToFile 是否保存到文件
     */
    public static void generateTypescriptInterfaces(Project project, List<PsiClass> dtoClasses, boolean saveToFile) {
        if (dtoClasses == null || dtoClasses.isEmpty()) {
            return;
        }

        // 生成接口内容
        Map<String, String> contentMap = new HashMap<>();
        for (PsiClass psiClass : dtoClasses) {
            try {
                TypescriptContentGenerator.processPsiClass(project, psiClass, saveToFile);
                String content = TypescriptContentGenerator.mergeContent(psiClass, saveToFile);
                contentMap.put(psiClass.getName(), content);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 清理缓存
                TypescriptContentGenerator.clearCache();
            }
        }

        if (contentMap.isEmpty()) {
            Messages.showMessageDialog(project, "沒有生成任何內容", "警告", Messages.getWarningIcon());
            return;
        }

        // 根據選項處理生成的內容
        if (saveToFile) {
            saveToFiles(project, contentMap);
        } else {
            // 預設複製到剪貼板
            copyToClipboard(project, contentMap);
        }
    }

    /**
     * 顯示操作選項對話框
     */
    public static void showOptionsDialog(Project project, List<PsiClass> dtoClasses) {
        if (dtoClasses == null || dtoClasses.isEmpty()) {
            return;
        }

        // 生成接口内容
        Map<String, String> contentMap = new HashMap<>();
        for (PsiClass psiClass : dtoClasses) {
            try {
                TypescriptContentGenerator.processPsiClass(project, psiClass, false);
                String content = TypescriptContentGenerator.mergeContent(psiClass, false);
                contentMap.put(psiClass.getName(), content);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 清理缓存
                TypescriptContentGenerator.clearCache();
            }
        }

        if (contentMap.isEmpty()) {
            Messages.showMessageDialog(project, "沒有生成任何內容", "警告", Messages.getWarningIcon());
            return;
        }

        // 顯示選項對話框
        String[] options = { "保存到文件", "複製到剪貼板", "在文本框中編輯" };
        int choice = Messages.showDialog(project, "請選擇操作", "TypeScript 接口生成", options, 0, Messages.getQuestionIcon());

        if (choice == 0) {
            saveToFiles(project, contentMap);
        } else if (choice == 1) {
            copyToClipboard(project, contentMap);
        } else if (choice == 2) {
            showInTextEditor(project, contentMap);
        }
    }

    /**
     * 保存生成的 TypeScript 接口到文件
     */
    public static void saveToFiles(Project project, Map<String, String> contentMap) {
        FileChooserDescriptor chooserDescriptor = CommonUtils.createFileChooserDescriptor("選擇一個文件夾",
                "TypeScript 介面文件（.ts）將保存在此文件夾中");
        VirtualFile savePathFile = FileChooser.chooseFile(chooserDescriptor, null, null);

        if (savePathFile != null && savePathFile.isDirectory()) {
            String savePath = savePathFile.getPath();
            StringBuilder successFiles = new StringBuilder();

            for (Map.Entry<String, String> entry : contentMap.entrySet()) {
                String fileName = entry.getKey();
                String content = entry.getValue();
                // 確保檔案名有 .ts 擴展名（不含 .d）
                String interfaceFileSavePath = savePath + "/" + ensureTsExtension(fileName);

                try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(interfaceFileSavePath, false), StandardCharsets.UTF_8))) {
                    bufferedWriter.write(content);
                    successFiles.append(fileName).append(".ts, ");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (successFiles.length() > 0) {
                successFiles.delete(successFiles.length() - 2, successFiles.length());
                Notification notification = notificationGroup.createNotification(
                        "已成功保存以下文件: " + successFiles, NotificationType.INFORMATION);
                notification.setImportant(true).notify(project);
            }
        }
    }

    /**
     * 確保文件名有 .ts 擴展名（不含 .d）
     */
    private static String ensureTsExtension(String fileName) {
        if (fileName.endsWith(".d.ts")) {
            return fileName.substring(0, fileName.length() - 5) + ".ts";
        } else if (fileName.endsWith(".ts")) {
            return fileName;
        } else {
            return fileName + ".ts";
        }
    }

    /**
     * 將生成的 TypeScript 接口複製到剪貼板
     */
    private static void copyToClipboard(Project project, Map<String, String> contentMap) {
        if (contentMap.size() == 1) {
            // 如果只有一個 DTO 類，直接複製內容
            String content = contentMap.values().iterator().next();
            Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable tText = new StringSelection(content);
            systemClipboard.setContents(tText, null);

            // 使用通知而不是彈窗
            Notification notification = notificationGroup.createNotification(
                    "已將 TypeScript 接口複製到剪貼板", NotificationType.INFORMATION);
            notification.setImportant(false).notify(project);
        } else {
            // 如果有多個 DTO 類，合併內容後複製
            StringBuilder mergedContent = new StringBuilder();
            for (Map.Entry<String, String> entry : contentMap.entrySet()) {
                // 直接添加內容，不添加檔名註釋
                mergedContent.append(entry.getValue()).append("\n\n");
            }

            Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable tText = new StringSelection(mergedContent.toString());
            systemClipboard.setContents(tText, null);

            // 使用通知而不是彈窗
            Notification notification = notificationGroup.createNotification(
                    "已將 " + contentMap.size() + " 個 TypeScript 接口複製到剪貼板",
                    NotificationType.INFORMATION);
            notification.setImportant(false).notify(project);
        }
    }

    /**
     * 在文本編輯器中顯示生成的內容
     */
    public static void showInTextEditor(Project project, Map<String, String> contentMap) {
        if (contentMap.isEmpty()) {
            return;
        }

        // 使用 ApplicationManager 確保在 EDT 線程中執行 UI 操作
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // 合併內容
                StringBuilder combinedContent = new StringBuilder();
                StringBuilder classNamesBuilder = new StringBuilder();

                for (Map.Entry<String, String> entry : contentMap.entrySet()) {
                    combinedContent.append(entry.getValue()).append("\n\n");

                    if (classNamesBuilder.length() > 0) {
                        classNamesBuilder.append(", ");
                    }
                    classNamesBuilder.append(entry.getKey());
                }

                // 創建顯示包裝器
                TypescriptInterfaceShowerWrapper wrapper = new TypescriptInterfaceShowerWrapper();
                wrapper.setContent(combinedContent.toString());

                // 設置類名以生成建議檔名
                if (classNamesBuilder.length() > 0) {
                    wrapper.setClassName(classNamesBuilder.toString());
                }

                // 顯示對話框
                wrapper.show();
            } catch (Exception e) {
                System.err.println("在編輯器中顯示內容時發生錯誤: " + e.getMessage());
                e.printStackTrace();

                // 使用通知而不是彈窗報錯
                Notification notification = notificationGroup.createNotification(
                        "顯示 TypeScript 接口失敗: " + e.getMessage(),
                        NotificationType.ERROR);
                notification.notify(project);
            }
        }, com.intellij.openapi.application.ModalityState.defaultModalityState());
    }

    // 使用項目級設定
    private static List<PsiClass> collectDtoClasses(Project project, PsiMethod method) {
        return ReadAction.compute(() -> {
            List<PsiClass> dtoClasses = new ArrayList<>();

            // 檢查返回類型
            PsiType returnType = method.getReturnType();
            if (returnType != null) {
                addDtoClassesFromType(project, returnType, dtoClasses);
            }

            // 檢查參數類型
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (PsiParameter parameter : parameters) {
                PsiType parameterType = parameter.getType();
                addDtoClassesFromType(project, parameterType, dtoClasses);
            }

            return dtoClasses;
        });
    }

    /**
     * 從類型中提取 DTO 類
     */
    private static void addDtoClassesFromType(Project project, PsiType type, List<PsiClass> dtoClasses) {
        // 處理泛型類型
        if (type instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) type;
            PsiClass psiClass = ReadAction.compute(() -> classType.resolve());

            if (psiClass != null && isDtoClass(project, psiClass)) {
                dtoClasses.add(psiClass);
            }

            // 處理泛型參數
            PsiType[] parameters = classType.getParameters();
            for (PsiType parameter : parameters) {
                addDtoClassesFromType(project, parameter, dtoClasses);
            }
        }
    }

    /**
     * 檢查是否為DTO類
     */
    private static boolean isDtoClass(Project project, PsiClass psiClass) {
        // 確保不是接口、枚舉或註解
        if (psiClass.isInterface() || psiClass.isEnum() || psiClass.isAnnotationType()) {
            return false;
        }

        // 獲取類名
        String className = psiClass.getName();
        if (className == null) {
            return false;
        }

        System.out.println("Service檢查類是否為DTO: " + className);

        // 大小寫不敏感的檢查
        String lowerClassName = className.toLowerCase();

        // 檢查是否包含Qrystatement、Query或Qry
        if (lowerClassName.contains("qrystatement") ||
                lowerClassName.contains("qry") ||
                lowerClassName.contains("query")) {
            System.out.println("  Service匹配到查詢關鍵字: " + className);
            return true;
        }

        // 獲取項目設置
        JavaBeanToTypescriptInterfaceProjectSettings settings = CommonUtils.getProjectSettings(project);

        // 輸出調試信息
        System.out.println("  Service當前項目設置中請求DTO後綴: " + settings.getEffectiveRequestDtoSuffixes());
        System.out.println("  Service當前項目設置中響應DTO後綴: " + settings.getEffectiveResponseDtoSuffixes());

        // 檢查類名是否包含配置的請求DTO後綴
        for (String suffix : settings.getEffectiveRequestDtoSuffixes()) {
            if (className.endsWith(suffix)) {
                System.out.println("  Service匹配到請求DTO後綴 " + suffix + ": " + className);
                return true;
            }
        }

        // 檢查類名是否包含配置的響應DTO後綴
        for (String suffix : settings.getEffectiveResponseDtoSuffixes()) {
            if (className.endsWith(suffix)) {
                System.out.println("  Service匹配到響應DTO後綴 " + suffix + ": " + className);
                return true;
            }
        }

        // 檢查是否包含常見的DTO相關詞
        if (lowerClassName.contains("dto") ||
                lowerClassName.contains("model") ||
                lowerClassName.contains("bean") ||
                lowerClassName.contains("vo") ||
                lowerClassName.contains("entity") ||
                lowerClassName.contains("request") ||
                lowerClassName.contains("response") ||
                lowerClassName.contains("result")) {
            System.out.println("  Service匹配到常見DTO關鍵字: " + className);
            return true;
        }

        // 如果上述檢查都未通過，則檢查類結構
        return false; // 在服務中，不使用結構檢查以提高性能
    }
}