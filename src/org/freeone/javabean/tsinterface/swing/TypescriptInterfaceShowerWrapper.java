package org.freeone.javabean.tsinterface.swing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;

public class TypescriptInterfaceShowerWrapper extends DialogWrapper {

    private TypescriptInterfaceContentDisplayPanel typescriptInterfaceContentDisplayPanel;
    private JTextField fileNameField;
    private String className = "";

    public TypescriptInterfaceShowerWrapper() {
        super(true);
        typescriptInterfaceContentDisplayPanel = new TypescriptInterfaceContentDisplayPanel();
        init();
        setTitle("TypeScript 接口內容");
        setSize(600, 700); // 設置更大的窗口尺寸
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 創建頂部面板包含建議的檔名文本框和複製按鈕
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel fileNamePanel = new JPanel(new BorderLayout());
        JLabel fileNameLabel = new JLabel("建議檔名: ");
        fileNameField = new JTextField();
        fileNameField.setEditable(false);
        fileNamePanel.add(fileNameLabel, BorderLayout.WEST);
        fileNamePanel.add(fileNameField, BorderLayout.CENTER);

        JButton copyFileNameButton = new JButton("複製檔名");
        copyFileNameButton.addActionListener(e -> {
            if (fileNameField.getText() != null && !fileNameField.getText().isEmpty()) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection selection = new StringSelection(fileNameField.getText());
                clipboard.setContents(selection, null);

                // 移除彈窗通知，改用更不擾人的方式
                System.out.println("已複製檔名: " + fileNameField.getText());

                // 可以選擇改變按鈕文字短暫時間來提示用戶
                copyFileNameButton.setText("已複製");
                Timer timer = new Timer(1500, evt -> {
                    copyFileNameButton.setText("複製檔名");
                });
                timer.setRepeats(false);
                timer.start();
            }
        });

        topPanel.add(fileNamePanel, BorderLayout.CENTER);
        topPanel.add(copyFileNameButton, BorderLayout.EAST);

        // 添加主內容面板
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(typescriptInterfaceContentDisplayPanel.mainPanel(), BorderLayout.CENTER);

        return mainPanel;
    }

    public void setContent(String interfaceContent) {
        if (typescriptInterfaceContentDisplayPanel != null && interfaceContent != null) {
            // 使用新的方法直接設置內容
            typescriptInterfaceContentDisplayPanel.setTextContent(interfaceContent);
            System.out.println("使用 setTextContent 設置內容，長度: " + interfaceContent.length());
        } else {
            System.err.println("錯誤：面板或內容為空");
        }
    }

    public void setClassName(String className) {
        this.className = className;
        updateFileName();
    }

    public void addClassName(String className) {
        if (this.className.isEmpty()) {
            this.className = className;
        } else {
            this.className += ", " + className;
        }
        updateFileName();
    }

    private void updateFileName() {
        if (fileNameField != null) {
            String suggestedName = generateFileName();
            fileNameField.setText(suggestedName);
        }
    }

    private String generateFileName() {
        if (className == null || className.isEmpty()) {
            return "typescript-interface.d.ts";
        }

        // 處理多個類名的情況
        if (className.contains(",")) {
            String[] classNames = className.split(",");
            // 檢查是否有明顯的請求/響應對
            boolean hasRqRs = false;
            String baseName = null;

            for (String name : classNames) {
                String trimmedName = name.trim();
                if (trimmedName.contains("Tranrq") || trimmedName.contains("Tranrs") ||
                        trimmedName.contains("Request") || trimmedName.contains("Response")) {
                    hasRqRs = true;
                    // 提取基本名稱部分
                    String baseNameCandidate = trimmedName.replaceAll("(Tranrq|Tranrs|Request|Response)$", "");
                    if (baseName == null) {
                        baseName = baseNameCandidate;
                    }
                }
            }

            if (hasRqRs && baseName != null) {
                return baseName.toLowerCase() + ".d.ts";
            } else {
                // 如果沒有明顯的請求/響應對，取第一個類名
                return classNames[0].trim().toLowerCase() + ".d.ts";
            }
        }

        // 單個類名
        return className.toLowerCase() + ".d.ts";
    }

    @NotNull
    protected Action[] createActions() {
        TypescriptInterfaceShowerWrapper.CustomerCloseAction customerCloseAction = new TypescriptInterfaceShowerWrapper.CustomerCloseAction(
                this);
        customerCloseAction.putValue(DialogWrapper.DEFAULT_ACTION, true);
        return new Action[] { customerCloseAction };
    }

    @Override
    public void show() {
        try {
            // 調用父類方法
            super.show();

            // 獲取當前窗口
            Window window = getWindow();
            if (window != null) {
                // 確保窗口在前台
                window.toFront();
                window.setAlwaysOnTop(true);
                window.setAlwaysOnTop(false);
                window.requestFocus();

                // 記錄窗口狀態
                System.out.println("窗口狀態: 可見=" + window.isVisible() +
                        ", 顯示=" + window.isShowing() +
                        ", 尺寸=" + window.getWidth() + "x" + window.getHeight());
            } else {
                System.err.println("警告：找不到窗口引用");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("顯示窗口時發生錯誤: " + e.getMessage());
        }
    }

    protected class CustomerCloseAction extends DialogWrapperAction {

        private final TypescriptInterfaceShowerWrapper wrapper;

        protected CustomerCloseAction(TypescriptInterfaceShowerWrapper wrapper) {
            super("複製並關閉");
            this.wrapper = wrapper;
        }

        @Override
        protected void doAction(ActionEvent actionEvent) {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    // 獲取文本
                    String text = typescriptInterfaceContentDisplayPanel.getTextArea().getText();
                    if (text != null && !text.isEmpty()) {
                        // 複製到剪貼板
                        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        Transferable tText = new StringSelection(text);
                        systemClipboard.setContents(tText, null);

                        // 顯示日誌而不是彈窗
                        System.out.println("已複製 " + text.length() + " 個字符到剪貼板");

                        // 可以選擇使用狀態欄通知，但要避免彈窗
                        // 只在控制台輸出日誌
                    }
                } catch (Exception ex) {
                    System.err.println("複製失敗: " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    wrapper.doCancelAction();
                }
            });
        }
    }
}
