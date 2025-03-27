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
                "TypeScript 聲明文件（.d.ts）將保存在此文件夾中");
        VirtualFile savePathFile = FileChooser.chooseFile(chooserDescriptor, null, null);

        if (savePathFile != null && savePathFile.isDirectory()) {
            String savePath = savePathFile.getPath();
            StringBuilder successFiles = new StringBuilder();

            for (Map.Entry<String, String> entry : contentMap.entrySet()) {
                String fileName = entry.getKey();
                String content = entry.getValue();
                String interfaceFileSavePath = savePath + "/" + fileName + ".d.ts";

                try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(interfaceFileSavePath, false), StandardCharsets.UTF_8))) {
                    bufferedWriter.write(content);
                    successFiles.append(fileName).append(".d.ts, ");
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
     * 將生成的 TypeScript 接口複製到剪貼板
     */
    private static void copyToClipboard(Project project, Map<String, String> contentMap) {
        if (contentMap.size() == 1) {
            // 如果只有一個 DTO 類，直接複製內容
            String content = contentMap.values().iterator().next();
            Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable tText = new StringSelection(content);
            systemClipboard.setContents(tText, null);

            // 不使用彈窗通知，只在控制台輸出
            System.out.println("已將 TypeScript 接口複製到剪貼板");
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

            // 不使用彈窗通知，只在控制台輸出
            System.out.println("已將 " + contentMap.size() + " 個 TypeScript 接口複製到剪貼板");
        }
    }

    /**
     * 在文本編輯器中顯示生成的內容
     */
    public static void showInTextEditor(Project project, Map<String, String> contentMap) {
        if (contentMap.isEmpty()) {
            return;
        }

        // 使用原子引用來追蹤對話框實例
        final java.util.concurrent.atomic.AtomicReference<TypescriptInterfaceShowerWrapper> wrapperRef = new java.util.concurrent.atomic.AtomicReference<>();

        // 使用 invokeLater 確保在 EDT 上執行 UI 相關操作
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // 創建新的對話框實例
                TypescriptInterfaceShowerWrapper wrapper = new TypescriptInterfaceShowerWrapper();
                wrapperRef.set(wrapper);

                // 如果只有一個 DTO 類，直接設置內容
                if (contentMap.size() == 1) {
                    Map.Entry<String, String> entry = contentMap.entrySet().iterator().next();
                    wrapper.setClassName(entry.getKey());
                    wrapper.setContent(entry.getValue());
                } else {
                    // 如果有多個 DTO 類，合併內容並設置類名
                    StringBuilder mergedContent = new StringBuilder();
                    for (Map.Entry<String, String> entry : contentMap.entrySet()) {
                        // 添加類名作為標題
                        wrapper.addClassName(entry.getKey());
                        // 添加內容
                        mergedContent.append(entry.getValue()).append("\n\n");
                    }
                    wrapper.setContent(mergedContent.toString());
                }

                // 使用更安全的方式顯示對話框
                if (!wrapper.isDisposed()) {
                    wrapper.show();
                } else {
                    System.err.println("對話框已被銷毀，無法顯示");
                }
            } catch (Exception e) {
                // 安全地處理所有異常
                System.err.println("顯示對話框時發生錯誤: " + e.getMessage());
                e.printStackTrace();

                // 嘗試關閉可能部分初始化的對話框
                TypescriptInterfaceShowerWrapper wrapper = wrapperRef.get();
                if (wrapper != null && !wrapper.isDisposed()) {
                    try {
                        wrapper.close(0);
                    } catch (Exception ex) {
                        // 忽略清理過程中的錯誤
                    }
                }
            }
        });
    }
}