package org.freeone.javabean.tsinterface;

import com.intellij.lang.jvm.JvmClassKind;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import org.freeone.javabean.tsinterface.swing.SampleDialogWrapper;
import org.freeone.javabean.tsinterface.swing.TypescriptInterfaceShowerWrapper;
import org.freeone.javabean.tsinterface.util.CommonUtils;
import org.freeone.javabean.tsinterface.util.TypescriptContentGenerator;
import org.freeone.javabean.tsinterface.service.DtoTypescriptGeneratorService;

import javax.swing.*;
import javax.swing.SwingUtilities;
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
import java.util.Optional;

/**
 * 將 Java DTO 轉換為 TypeScript 接口
 */
public class JavaBeanToTypescriptInterfaceAction extends AnAction {

    // 使用新的 NotificationGroupManager API 獲取通知組
    private final com.intellij.notification.NotificationGroup notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("JavaDtoToTypescriptInterface");

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        boolean needSaveToFile = true;
        Presentation presentation = e.getPresentation();
        String description = Optional.ofNullable(presentation.getDescription()).orElse("");
        String menuText = Optional.ofNullable(presentation.getText()).orElse("");
        try {
            if (!menuText.toLowerCase().startsWith("save")) {
                needSaveToFile = false;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        VirtualFile[] virtualFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (virtualFiles != null && virtualFiles.length == 1) {
            VirtualFile target = virtualFiles[0];
            if (target.isDirectory()) {
                Messages.showMessageDialog("Please choose a Java file !", "", Messages.getInformationIcon());
                return;
            }
            PsiManager psiMgr = PsiManager.getInstance(project);
            PsiFile file = psiMgr.findFile(target);
            if (!(file instanceof PsiJavaFile)) {
                Messages.showMessageDialog("Unsupported source file!", "", Messages.getInformationIcon());
                return;
            }
            PsiElement psiElement = e.getData(PlatformDataKeys.PSI_ELEMENT);
            // 當在 editor 右鍵的時候 psiElement 可能是 null 的
            if ("EditorPopup".equalsIgnoreCase(e.getPlace())) {

                // psiElement may be null
                // 在 field 上右鍵選擇其所屬類
                if (psiElement instanceof PsiField || psiElement instanceof PsiMethod) {
                    if (psiElement.getParent() != null && psiElement.getParent() instanceof PsiClass) {
                        psiElement = psiElement.getParent();
                    }
                } else if (!(psiElement instanceof PsiClass)) {
                    // psiElement 可能是 null 的
                    PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                    PsiClass[] classes = psiJavaFile.getClasses();
                    if (classes.length != 0) {
                        psiElement = classes[0];
                    }
                } else if (psiElement instanceof PsiExtensibleClass) {
                    PsiExtensibleClass psiExtensibleClass = (PsiExtensibleClass) psiElement;
                    JvmClassKind classKind = psiExtensibleClass.getClassKind();
                    // 註解
                    if (classKind == JvmClassKind.ANNOTATION) {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                        PsiClass[] classes = psiJavaFile.getClasses();
                        if (classes.length != 0) {
                            psiElement = classes[0];
                        }
                    }
                }

                if (psiElement == null) {
                    Messages.showMessageDialog("Can not find a class!", "", Messages.getInformationIcon());
                    return;
                }
            } else {
                // ProjectViewPopup
                if (psiElement == null) {
                    Messages.showMessageDialog("Can not find a class", "", Messages.getInformationIcon());
                    return;
                }
            }

            if (menuText.contains("2") || description.contains("2.0")) {
                if (psiElement == null) {
                    Messages.showMessageDialog("Can not find a class", "", Messages.getInformationIcon());
                    return;
                }
                if (psiElement instanceof PsiClass) {
                    PsiClass psiClass = (PsiClass) psiElement;
                    TypescriptContentGenerator.processPsiClass(project, psiClass, needSaveToFile);
                    String content = TypescriptContentGenerator.mergeContent(psiClass, needSaveToFile);
                    TypescriptContentGenerator.clearCache();
                    generateTypescriptContent(e, project, needSaveToFile, psiClass.getName(), content);
                }
            } else {
                // 1.0
                if (file instanceof PsiJavaFile) {
                    PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                    if (psiElement instanceof PsiClass) {
                        PsiClass psiClass = (PsiClass) psiElement;
                        boolean innerPublicClass = CommonUtils.isInnerPublicClass(psiJavaFile, psiClass);
                        if (innerPublicClass) {
                            SampleDialogWrapper sampleDialogWrapper = new SampleDialogWrapper();
                            boolean b = sampleDialogWrapper.showAndGet();
                            if (b) {
                                // 只有內部 public static class 會執行這一步
                                TypescriptContentGenerator.processPsiClass(project, psiClass, needSaveToFile);
                                String content = TypescriptContentGenerator.mergeContent(psiClass, needSaveToFile);
                                TypescriptContentGenerator.clearCache();
                                generateTypescriptContent(e, project, needSaveToFile, psiClass.getName(),
                                        content);
                                return;
                            }
                        }
                    }

                    // 正常情況下
                    // 聲明文件的主要內容 || content of *.d.ts
                    PsiClass[] classes = psiJavaFile.getClasses();
                    if (classes.length > 0) {
                        PsiClass mainClass = classes[0];
                        TypescriptContentGenerator.processPsiClass(project, mainClass, needSaveToFile);
                        String content = TypescriptContentGenerator.mergeContent(mainClass, needSaveToFile);
                        TypescriptContentGenerator.clearCache();
                        generateTypescriptContent(e, project, needSaveToFile,
                                psiJavaFile.getVirtualFile().getNameWithoutExtension(), content);
                    } else {
                        Messages.showMessageDialog("No classes found in file", "", Messages.getInformationIcon());
                    }
                }
            }

        } else {
            Messages.showMessageDialog("Please choose a Java", "", Messages.getInformationIcon());
        }
    }

    /**
     * 處理 interfaceContent，生成內容
     *
     * @param e
     * @param project
     * @param saveToFile
     * @param fileNameToSave
     * @param interfaceContent
     */
    private void generateTypescriptContent(AnActionEvent e, Project project, boolean saveToFile, String fileNameToSave,
            String interfaceContent) {
        if (saveToFile) {
            FileChooserDescriptor chooserDescriptor = CommonUtils.createFileChooserDescriptor("Choose a folder",
                    "The declaration file end with '.d.ts' will be saved in this folder");
            VirtualFile savePathFile = FileChooser.chooseFile(chooserDescriptor, null, null);
            if (savePathFile != null && savePathFile.isDirectory()) {
                String savePath = savePathFile.getPath();
                String interfaceFileSavePath = savePath + "/" + fileNameToSave + ".d.ts";
                try {
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(interfaceFileSavePath, false), StandardCharsets.UTF_8));
                    bufferedWriter.write(interfaceContent);
                    bufferedWriter.close();
                    // 使用新的 Notification Builder API
                    notificationGroup.createNotification(
                            "The target file was saved to:  " + interfaceFileSavePath, NotificationType.INFORMATION)
                            .setImportant(true)
                            .notify(project);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        } else {
            try {
                // 獲取當前菜單的文本
                String text = e.getPresentation().getText();
                // 複製到剪貼板
                if (text != null && text.toLowerCase().startsWith("copy")) {
                    Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    Transferable tText = new StringSelection(interfaceContent);
                    systemClipboard.setContents(tText, null);

                    // 使用通知而不是彈窗
                    Notification notification = notificationGroup.createNotification(
                            "已複製 TypeScript 接口到剪貼板",
                            NotificationType.INFORMATION);
                    notification.setImportant(false).notify(project);

                    // 如果用戶選擇將內容複製到剪貼板而不是保存到檔案，也會觸發此代碼
                } else {
                    // 在文本區域顯示編輯
                    if (interfaceContent != null && !interfaceContent.trim().isEmpty()) {
                        // 保持原始內容格式，不添加標記
                        final String finalContent = interfaceContent;

                        // 使用獨立的線程安全方式顯示
                        SwingUtilities.invokeLater(() -> {
                            try {
                                TypescriptInterfaceShowerWrapper typescriptInterfaceShowerWrapper = new TypescriptInterfaceShowerWrapper();
                                typescriptInterfaceShowerWrapper.setContent(finalContent);
                                // 設置類名用於建議檔名
                                typescriptInterfaceShowerWrapper.setClassName(fileNameToSave);
                                typescriptInterfaceShowerWrapper.show();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Messages.showMessageDialog("顯示對話框時發生錯誤: " + ex.getMessage(), "錯誤",
                                        Messages.getErrorIcon());
                            }
                        });
                    } else {
                        // 如果內容為空，顯示錯誤消息
                        Messages.showMessageDialog("無法生成 TypeScript 接口內容", "錯誤", Messages.getErrorIcon());
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * 顯示選項對話框
     */
    private void showOptionsDialog(AnActionEvent event, List<PsiClass> psiClasses, String title) {
        Project project = event.getProject();

        // 使用正確的模態狀態顯示操作選項
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                String[] options = { "保存到文件", "複製到剪貼板", "在文本框中編輯" };

                int choice = Messages.showDialog(
                        project,
                        "請選擇操作",
                        title,
                        options,
                        0,
                        Messages.getQuestionIcon());

                if (choice == 0) { // 保存到文件
                    DtoTypescriptGeneratorService.generateTypescriptInterfaces(project, psiClasses, true);
                } else if (choice == 1) { // 複製到剪貼板
                    DtoTypescriptGeneratorService.generateTypescriptInterfaces(project, psiClasses, false);
                } else if (choice == 2) { // 在文本框中編輯
                    processSelectedClassesForEditor(project, psiClasses);
                }
            } catch (Exception e) {
                System.err.println("顯示選項對話框時發生錯誤: " + e.getMessage());
                e.printStackTrace();

                // 使用通知而不是彈窗
                Notification notification = notificationGroup.createNotification(
                        "顯示選項時發生錯誤: " + e.getMessage(),
                        NotificationType.ERROR);
                notification.notify(project);
            }
        }, ModalityState.defaultModalityState());
    }

    /**
     * 處理選中的類，生成 TypeScript 接口並顯示在編輯器中
     */
    private void processSelectedClassesForEditor(Project project, List<PsiClass> psiClasses) {
        try {
            // 生成 TypeScript 接口內容
            Map<String, String> contentMap = new HashMap<>();

            for (PsiClass psiClass : psiClasses) {
                try {
                    TypescriptContentGenerator.processPsiClass(project, psiClass, false);
                    String content = TypescriptContentGenerator.mergeContent(psiClass, false);
                    contentMap.put(psiClass.getName(), content);
                } catch (Exception e) {
                    System.err.println("處理類 " + psiClass.getName() + " 時發生錯誤: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // 清理緩存
                    TypescriptContentGenerator.clearCache();
                }
            }

            if (contentMap.isEmpty()) {
                // 使用通知而不是彈窗
                ApplicationManager.getApplication().invokeLater(() -> {
                    Notification notification = notificationGroup.createNotification(
                            "沒有生成任何 TypeScript 接口內容",
                            NotificationType.WARNING);
                    notification.notify(project);
                }, ModalityState.defaultModalityState());
                return;
            }

            // 顯示在編輯器中
            DtoTypescriptGeneratorService.showInTextEditor(project, contentMap);
        } catch (Exception e) {
            System.err.println("處理選中類時發生錯誤: " + e.getMessage());
            e.printStackTrace();

            // 使用通知而不是彈窗
            ApplicationManager.getApplication().invokeLater(() -> {
                Notification notification = notificationGroup.createNotification(
                        "處理選中類時發生錯誤: " + e.getMessage(),
                        NotificationType.ERROR);
                notification.notify(project);
            }, ModalityState.defaultModalityState());
        }
    }
}
