//package org.freeone.javabean.tsinterface;
//
//import com.intellij.lang.jvm.JvmClassKind;
//import com.intellij.notification.Notification;
//import com.intellij.notification.NotificationGroupManager;
//import com.intellij.notification.NotificationType;
//import com.intellij.openapi.actionSystem.AnAction;
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.actionSystem.PlatformDataKeys;
//import com.intellij.openapi.actionSystem.Presentation;
//import com.intellij.openapi.application.ApplicationManager;
//import com.intellij.openapi.application.ModalityState;
//import com.intellij.openapi.fileChooser.FileChooser;
//import com.intellij.openapi.fileChooser.FileChooserDescriptor;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.ui.Messages;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.psi.*;
//import com.intellij.psi.impl.source.PsiExtensibleClass;
//import lombok.extern.slf4j.Slf4j;
//import org.freeone.javabean.tsinterface.service.DtoTypescriptGeneratorService;
//import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceProjectSettings;
//import org.freeone.javabean.tsinterface.swing.SampleDialogWrapper;
//import org.freeone.javabean.tsinterface.swing.TypescriptInterfaceShowerWrapper;
//import org.freeone.javabean.tsinterface.util.CommonUtils;
//import org.freeone.javabean.tsinterface.util.TypescriptContentGenerator;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.datatransfer.Clipboard;
//import java.awt.datatransfer.StringSelection;
//import java.awt.datatransfer.Transferable;
//import java.io.BufferedWriter;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
///**
// * 將 Java DTO 轉換為 TypeScript 接口的 Action 類
// * 此類負責處理從 Java Bean 到 TypeScript 接口的轉換操作
// * 包括文件選擇、生成接口內容、保存文件、複製到剪貼板等功能
// */
//@Slf4j
//public class JavaBeanToTypescriptInterfaceAction extends AnAction {
//
//    /**
//     * 使用 NotificationGroupManager API 獲取通知組
//     * 用於向用戶顯示各種操作的結果和錯誤信息
//     */
//    private final com.intellij.notification.NotificationGroup notificationGroup = NotificationGroupManager.getInstance()
//            .getNotificationGroup("JavaDtoToTypescriptInterface");
//
//    /**
//     * 執行 Action 的主要方法，當用戶點擊菜單項時調用
//     *
//     * @param e 動作事件，包含上下文信息
//     */
//    @Override
//    public void actionPerformed(AnActionEvent e) {
//        Project project = e.getProject();
//        if (project == null) {
//            return;
//        }
//        boolean needSaveToFile = true;
//        Presentation presentation = e.getPresentation();
//        String description = Optional.ofNullable(presentation.getDescription()).orElse("");
//        String menuText = Optional.ofNullable(presentation.getText()).orElse("");
//        try {
//            if (!menuText.toLowerCase().startsWith("save")) {
//                needSaveToFile = false;
//            }
//        } catch (Exception exception) {
//            log.error("解析菜單文本時出錯", exception);
//        }
//
//        VirtualFile[] virtualFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
//        if (virtualFiles != null && virtualFiles.length == 1) {
//            VirtualFile target = virtualFiles[0];
//            if (target.isDirectory()) {
//                Messages.showMessageDialog("Please choose a Java file !", "", Messages.getInformationIcon());
//                return;
//            }
//            PsiManager psiMgr = PsiManager.getInstance(project);
//            PsiFile file = psiMgr.findFile(target);
//            if (!(file instanceof PsiJavaFile)) {
//                Messages.showMessageDialog("Unsupported source file!", "", Messages.getInformationIcon());
//                return;
//            }
//            PsiElement psiElement = e.getData(PlatformDataKeys.PSI_ELEMENT);
//            // 當在 editor 右鍵的時候 psiElement 可能是 null 的
//            if ("EditorPopup".equalsIgnoreCase(e.getPlace())) {
//                psiElement = processPsiElementInEditor(psiElement, file);
//                if (psiElement == null) {
//                    Messages.showMessageDialog("Can not find a class!", "", Messages.getInformationIcon());
//                    return;
//                }
//            } else {
//                // ProjectViewPopup
//                if (psiElement == null) {
//                    Messages.showMessageDialog("Can not find a class", "", Messages.getInformationIcon());
//                    return;
//                }
//            }
//
//            if (menuText.contains("2") || description.contains("2.0")) {
//                processVersion2(e, project, needSaveToFile, psiElement);
//            } else {
//                // 1.0
//                processVersion1(e, project, needSaveToFile, file, psiElement);
//            }
//
//        } else {
//            Messages.showMessageDialog("Please choose a Java", "", Messages.getInformationIcon());
//        }
//    }
//
//    /**
//     * 處理編輯器中選中的 PSI 元素
//     *
//     * @param psiElement 當前選中的 PSI 元素
//     * @param file       當前文件
//     * @return 經過處理的 PSI 元素
//     */
//    private PsiElement processPsiElementInEditor(PsiElement psiElement, PsiFile file) {
//        // psiElement may be null
//        // 在 field 上右鍵選擇其所屬類
//        if (psiElement instanceof PsiField || psiElement instanceof PsiMethod) {
//            if (psiElement.getParent() != null && psiElement.getParent() instanceof PsiClass) {
//                psiElement = psiElement.getParent();
//            }
//        } else if (!(psiElement instanceof PsiClass)) {
//            // psiElement 可能是 null 的
//            PsiJavaFile psiJavaFile = (PsiJavaFile) file;
//            PsiClass[] classes = psiJavaFile.getClasses();
//            if (classes.length != 0) {
//                psiElement = classes[0];
//            }
//        } else if (psiElement instanceof PsiExtensibleClass) {
//            PsiExtensibleClass psiExtensibleClass = (PsiExtensibleClass) psiElement;
//            JvmClassKind classKind = psiExtensibleClass.getClassKind();
//            // 註解
//            if (classKind == JvmClassKind.ANNOTATION) {
//                PsiJavaFile psiJavaFile = (PsiJavaFile) file;
//                PsiClass[] classes = psiJavaFile.getClasses();
//                if (classes.length != 0) {
//                    psiElement = classes[0];
//                }
//            }
//        }
//        return psiElement;
//    }
//
//    /**
//     * 處理版本 2.0 的轉換邏輯
//     *
//     * @param e              動作事件
//     * @param project        當前項目
//     * @param needSaveToFile 是否需要保存文件
//     * @param psiElement     當前選中的 PSI 元素
//     */
//    private void processVersion2(AnActionEvent e, Project project, boolean needSaveToFile, PsiElement psiElement) {
//        if (psiElement == null) {
//            Messages.showMessageDialog("Can not find a class", "", Messages.getInformationIcon());
//            return;
//        }
//        if (psiElement instanceof PsiClass) {
//            PsiClass psiClass = (PsiClass) psiElement;
//            // 創建 TypescriptContentGenerator 實例
//            TypescriptContentGenerator generator = new TypescriptContentGenerator(project);
//            generator.processPsiClass(psiClass, needSaveToFile);
//            String content = generator.mergeContent(psiClass, needSaveToFile);
//            generateTypescriptContent(e, project, needSaveToFile, psiClass.getName(), content);
//        }
//    }
//
//    /**
//     * 處理版本 1.0 的轉換邏輯
//     *
//     * @param e              動作事件
//     * @param project        當前項目
//     * @param needSaveToFile 是否需要保存文件
//     * @param file           當前文件
//     * @param psiElement     當前選中的 PSI 元素
//     */
//    private void processVersion1(AnActionEvent e, Project project, boolean needSaveToFile, PsiFile file,
//                                 PsiElement psiElement) {
//        if (file instanceof PsiJavaFile) {
//            PsiJavaFile psiJavaFile = (PsiJavaFile) file;
//            if (psiElement instanceof PsiClass) {
//                PsiClass psiClass = (PsiClass) psiElement;
//                boolean innerPublicClass = CommonUtils.isInnerPublicClass(psiJavaFile, psiClass);
//                if (innerPublicClass) {
//                    SampleDialogWrapper sampleDialogWrapper = new SampleDialogWrapper();
//                    boolean b = sampleDialogWrapper.showAndGet();
//                    if (b) {
//                        // 只有內部 public static class 會執行這一步
//                        // 創建 TypescriptContentGenerator 實例
//                        TypescriptContentGenerator generator = new TypescriptContentGenerator(project);
//                        generator.processPsiClass(psiClass, needSaveToFile);
//                        String content = generator.mergeContent(psiClass, needSaveToFile);
//                        generateTypescriptContent(e, project, needSaveToFile, psiClass.getName(),
//                                content);
//                        return;
//                    }
//                }
//            }
//
//            // 正常情況下
//            // 聲明文件的主要內容 || content of *.d.ts
//            PsiClass[] classes = psiJavaFile.getClasses();
//            if (classes.length > 0) {
//                PsiClass mainClass = classes[0];
//                // 創建 TypescriptContentGenerator 實例
//                TypescriptContentGenerator generator = new TypescriptContentGenerator(project);
//                generator.processPsiClass(mainClass, needSaveToFile);
//                String content = generator.mergeContent(mainClass, needSaveToFile);
//                generateTypescriptContent(e, project, needSaveToFile,
//                        psiJavaFile.getVirtualFile().getNameWithoutExtension(), content);
//            } else {
//                Messages.showMessageDialog("No classes found in file", "", Messages.getInformationIcon());
//            }
//        }
//    }
//
//    /**
//     * 處理 interfaceContent，生成內容
//     * 根據用戶選擇保存到文件或複製到剪貼板或顯示在文本框中
//     *
//     * @param e                動作事件
//     * @param project          當前項目
//     * @param saveToFile       是否保存到文件
//     * @param fileNameToSave   要保存的文件名
//     * @param interfaceContent 接口內容
//     */
//    private void generateTypescriptContent(AnActionEvent e, Project project, boolean saveToFile, String fileNameToSave,
//                                           String interfaceContent) {
//        if (saveToFile) {
//            handleSaveToFile(project, fileNameToSave, interfaceContent);
//        } else {
//            handleCopyOrDisplay(e, project, fileNameToSave, interfaceContent);
//        }
//    }
//
//    /**
//     * 處理保存文件操作
//     *
//     * @param project          當前項目
//     * @param fileNameToSave   要保存的文件名
//     * @param interfaceContent 接口內容
//     */
//    private void handleSaveToFile(Project project, String fileNameToSave, String interfaceContent) {
//        FileChooserDescriptor chooserDescriptor = CommonUtils.createFileChooserDescriptor("Choose a folder",
//                "The declaration file end with '.d.ts' will be saved in this folder");
//        VirtualFile savePathFile = FileChooser.chooseFile(chooserDescriptor, null, null);
//        if (savePathFile != null && savePathFile.isDirectory()) {
//            String savePath = savePathFile.getPath();
//            String interfaceFileSavePath = savePath + "/" + fileNameToSave + ".d.ts";
//            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(
//                    new FileOutputStream(interfaceFileSavePath, false), StandardCharsets.UTF_8))) {
//                bufferedWriter.write(interfaceContent);
//                // 使用新的 Notification Builder API
//                notificationGroup.createNotification(
//                                "The target file was saved to:  " + interfaceFileSavePath, NotificationType.INFORMATION)
//                        .setImportant(true)
//                        .notify(project);
//            } catch (IOException ioException) {
//                log.error("保存文件時出錯", ioException);
//            }
//        }
//    }
//
//    /**
//     * 處理複製到剪貼板或顯示在文本框中的操作
//     *
//     * @param e                動作事件
//     * @param project          當前項目
//     * @param fileNameToSave   文件名
//     * @param interfaceContent 接口內容
//     */
//    private void handleCopyOrDisplay(AnActionEvent e, Project project, String fileNameToSave, String interfaceContent) {
//        try {
//            // 獲取當前菜單的文本
//            String text = e.getPresentation().getText();
//            // 複製到剪貼板
//            if (text != null && text.toLowerCase().startsWith("copy")) {
//                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//                Transferable tText = new StringSelection(interfaceContent);
//                systemClipboard.setContents(tText, null);
//
//                // 使用通知而不是彈窗
//                Notification notification = notificationGroup.createNotification(
//                        "已複製 TypeScript 接口到剪貼板",
//                        NotificationType.INFORMATION);
//                notification.setImportant(false).notify(project);
//
//                // 如果用戶選擇將內容複製到剪貼板而不是保存到檔案，也會觸發此代碼
//            } else {
//                // 在文本區域顯示編輯
//                if (interfaceContent != null && !interfaceContent.trim().isEmpty()) {
//                    // 保持原始內容格式，不添加標記
//                    final String finalContent = interfaceContent;
//
//                    // 使用獨立的線程安全方式顯示
//                    SwingUtilities.invokeLater(() -> {
//                        try {
//                            TypescriptInterfaceShowerWrapper typescriptInterfaceShowerWrapper = new TypescriptInterfaceShowerWrapper();
//                            typescriptInterfaceShowerWrapper.setContent(finalContent);
//                            // 設置類名用於建議檔名
//                            typescriptInterfaceShowerWrapper.setClassName(fileNameToSave);
//                            typescriptInterfaceShowerWrapper.show();
//                        } catch (Exception ex) {
//                            log.error("顯示對話框時發生錯誤", ex);
//                            Messages.showMessageDialog("顯示對話框時發生錯誤: " + ex.getMessage(), "錯誤",
//                                    Messages.getErrorIcon());
//                        }
//                    });
//                } else {
//                    // 如果內容為空，顯示錯誤消息
//                    Messages.showMessageDialog("無法生成 TypeScript 接口內容", "錯誤", Messages.getErrorIcon());
//                }
//            }
//        } catch (Exception exception) {
//            log.error("處理複製或顯示操作時出錯", exception);
//        }
//    }
//
//    /**
//     * 顯示選項對話框
//     *
//     * @param event      動作事件
//     * @param psiClasses PSI類列表
//     * @param title      對話框標題
//     */
//    private void showOptionsDialog(AnActionEvent event, List<PsiClass> psiClasses, String title) {
//        Project project = event.getProject();
//
//        // 使用正確的模態狀態顯示操作選項
//        ApplicationManager.getApplication().invokeLater(() -> {
//            try {
//                String[] options = {"保存到文件", "複製到剪貼板", "在文本框中編輯"};
//
//                int choice = Messages.showDialog(
//                        project,
//                        "請選擇操作",
//                        title,
//                        options,
//                        0,
//                        Messages.getQuestionIcon());
//
//                if (choice == 0) { // 保存到文件
//                    DtoTypescriptGeneratorService.generateTypescriptInterfaces(project, psiClasses, true);
//                } else if (choice == 1) { // 複製到剪貼板
//                    DtoTypescriptGeneratorService.generateTypescriptInterfaces(project, psiClasses, false);
//                } else if (choice == 2) { // 在文本框中編輯
//                    processSelectedClassesForEditor(project, psiClasses);
//                }
//            } catch (Exception e) {
//                log.error("顯示選項對話框時發生錯誤", e);
//
//                // 使用通知而不是彈窗
//                Notification notification = notificationGroup.createNotification(
//                        "顯示選項時發生錯誤: " + e.getMessage(),
//                        NotificationType.ERROR);
//                notification.notify(project);
//            }
//        }, ModalityState.defaultModalityState());
//    }
//
//    /**
//     * 處理選中的類，生成 TypeScript 接口並顯示在編輯器中
//     *
//     * @param project    當前項目
//     * @param psiClasses PSI類列表
//     */
//    private void processSelectedClassesForEditor(Project project, List<PsiClass> psiClasses) {
//        try {
//            // 生成 TypeScript 接口內容
//            Map<String, String> contentMap = new HashMap<>();
//
//            for (PsiClass psiClass : psiClasses) {
//                try {
//                    // 創建 TypescriptContentGenerator 實例
//                    TypescriptContentGenerator generator = new TypescriptContentGenerator(project);
//                    generator.processPsiClass(psiClass, false);
//                    String content = generator.mergeContent(psiClass, false);
//                    contentMap.put(psiClass.getName(), content);
//                } catch (Exception e) {
//                    log.error("處理類 {} 時發生錯誤", psiClass.getName(), e);
//                }
//            }
//
//            if (contentMap.isEmpty()) {
//                // 使用通知而不是彈窗
//                ApplicationManager.getApplication().invokeLater(() -> {
//                    Notification notification = notificationGroup.createNotification(
//                            "沒有生成任何 TypeScript 接口內容",
//                            NotificationType.WARNING);
//                    notification.notify(project);
//                }, ModalityState.defaultModalityState());
//                return;
//            }
//
//            // 顯示在編輯器中
//            DtoTypescriptGeneratorService.showInTextEditor(project, contentMap);
//        } catch (Exception e) {
//            log.error("處理選中類時發生錯誤", e);
//
//            // 使用通知而不是彈窗
//            ApplicationManager.getApplication().invokeLater(() -> {
//                Notification notification = notificationGroup.createNotification(
//                        "處理選中類時發生錯誤: " + e.getMessage(),
//                        NotificationType.ERROR);
//                notification.notify(project);
//            }, ModalityState.defaultModalityState());
//        }
//    }
//
//    /**
//     * 判斷是否為DTO類
//     *
//     * @param psiClass 需要判斷的PSI類
//     * @return 是否為DTO類
//     */
//    private boolean isDtoClass(PsiClass psiClass) {
//        // 通常 DTO 類是普通 Java 類，不是接口、枚舉或者註解
//        if (psiClass.isInterface() || psiClass.isEnum() || psiClass.isAnnotationType()) {
//            return false;
//        }
//
//        // 通常 DTO 類名稱包含一些特定的模式
//        String className = psiClass.getName();
//        if (className == null) {
//            return false;
//        }
//
//        // 獲取當前項目
//        Project project = psiClass.getProject();
//
//        // 獲取項目級別的設定
//        JavaBeanToTypescriptInterfaceProjectSettings settings = JavaBeanToTypescriptInterfaceProjectSettings
//                .getInstance(project);
//
//        // // 檢查QryStatement特殊類型和常見包含關鍵詞的類
//        // if (className.toLowerCase().contains("qrystatement") ||
//        // className.toLowerCase().contains("qry") ||
//        // className.toLowerCase().contains("query") ||
//        // className.toLowerCase().contains("dto") ||
//        // className.toLowerCase().contains("model") ||
//        // className.toLowerCase().contains("bean") ||
//        // className.toLowerCase().contains("vo") ||
//        // className.toLowerCase().contains("entity") ||
//        // className.toLowerCase().contains("request") ||
//        // className.toLowerCase().contains("response") ||
//        // className.toLowerCase().contains("result")) {
//        // log.debug("找到DTO類: {}", className);
//        // return true;
//        // }
//
//        // 檢查請求DTO後綴
//        List<String> requestSuffixes = settings.getEffectiveRequestDtoSuffixes();
//        for (String suffix : requestSuffixes) {
//            if (className.endsWith(suffix)) {
//                log.debug("找到請求DTO類: {}, 後綴: {}", className, suffix);
//                return true;
//            }
//        }
//
//        // 檢查響應DTO後綴
//        List<String> responseSuffixes = settings.getEffectiveResponseDtoSuffixes();
//        for (String suffix : responseSuffixes) {
//            if (className.endsWith(suffix)) {
//                log.debug("找到響應DTO類: {}, 後綴: {}", className, suffix);
//                return true;
//            }
//        }
//
//        // 如果後綴檢查沒通過，則檢查類結構
//        boolean isDto = checkClassStructure(psiClass);
//        if (isDto) {
//            log.debug("根據類結構識別出DTO類: {}", className);
//        }
//        return isDto;
//    }
//
//    /**
//     * 根據類結構判斷是否為DTO類
//     *
//     * @param psiClass 需要判斷的PSI類
//     * @return 是否為DTO類
//     */
//    private boolean checkClassStructure(PsiClass psiClass) {
//        // 檢查類中是否存在公開的字段或者 getter/setter 方法
//        // 通常 DTO 類會包含多個公開字段或 getter/setter 方法
//        PsiField[] fields = psiClass.getFields();
//        if (fields.length > 0) {
//            int publicFieldCount = 0;
//            for (PsiField field : fields) {
//                if (field.hasModifierProperty(PsiModifier.PUBLIC)) {
//                    publicFieldCount++;
//                }
//            }
//            // 如果有多個公開字段，可能是數據傳輸對象
//            if (publicFieldCount > 2) {
//                return true;
//            }
//        }
//
//        // 檢查 getter/setter 方法
//        PsiMethod[] methods = psiClass.getMethods();
//        int getterSetterCount = 0;
//        for (PsiMethod method : methods) {
//            String methodName = method.getName();
//            if ((methodName.startsWith("get") || methodName.startsWith("set")) &&
//                    methodName.length() > 3 &&
//                    Character.isUpperCase(methodName.charAt(3))) {
//                getterSetterCount++;
//            }
//        }
//        // 如果有多個 getter/setter 方法，可能是 DTO
//        return getterSetterCount > 3;
//    }
//}