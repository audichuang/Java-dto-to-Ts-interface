package org.freeone.javabean.tsinterface.service;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import org.freeone.javabean.tsinterface.swing.TypescriptInterfaceShowerWrapper;
import org.freeone.javabean.tsinterface.util.CommonUtils;
import org.freeone.javabean.tsinterface.util.TypescriptContentGenerator;

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
}