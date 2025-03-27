package org.freeone.javabean.tsinterface.service;

import com.intellij.notification.*;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import org.freeone.javabean.tsinterface.util.CommonUtils;
import org.freeone.javabean.tsinterface.util.TypescriptContentGenerator;

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

    private static final NotificationGroup notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("JavaDtoToTypescriptInterface");

    /**
     * 為多個 DTO 類生成 TypeScript 接口
     *
     * @param project        當前項目
     * @param dtoClasses     要處理的 DTO 類列表
     * @param needSaveToFile 是否需要保存到文件
     */
    public static void generateTypescriptInterfaces(Project project, List<PsiClass> dtoClasses,
            boolean needSaveToFile) {
        if (dtoClasses.isEmpty()) {
            Messages.showMessageDialog("沒有找到要處理的 DTO 類", "提示", Messages.getInformationIcon());
            return;
        }

        // 生成所有 DTO 類的 TypeScript 接口
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
            Messages.showMessageDialog("生成 TypeScript 接口失敗", "錯誤", Messages.getErrorIcon());
            return;
        }

        if (needSaveToFile) {
            saveToFiles(project, contentMap);
        } else {
            copyToClipboard(project, contentMap);
        }
    }

    /**
     * 將生成的 TypeScript 接口保存到文件
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

            Notification notification = notificationGroup.createNotification(
                    "已將 TypeScript 接口複製到剪貼板", NotificationType.INFORMATION);
            notification.setImportant(false).notify(project);
        } else {
            // 如果有多個 DTO 類，合併內容後複製
            StringBuilder mergedContent = new StringBuilder();
            for (Map.Entry<String, String> entry : contentMap.entrySet()) {
                mergedContent.append("// ").append(entry.getKey()).append(".d.ts\n");
                mergedContent.append(entry.getValue()).append("\n\n");
            }

            Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable tText = new StringSelection(mergedContent.toString());
            systemClipboard.setContents(tText, null);

            Notification notification = notificationGroup.createNotification(
                    "已將 " + contentMap.size() + " 個 TypeScript 接口複製到剪貼板", NotificationType.INFORMATION);
            notification.setImportant(false).notify(project);
        }
    }
}