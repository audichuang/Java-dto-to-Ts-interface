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
import java.awt.event.ActionListener;

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
        fileNameField.setEditable(true); // 將檔名欄位設為可編輯
        fileNamePanel.add(fileNameLabel, BorderLayout.WEST);
        fileNamePanel.add(fileNameField, BorderLayout.CENTER);

        JButton copyFileNameButton = new JButton("複製檔名");
        copyFileNameButton.addActionListener(e -> {
            String text = fileNameField.getText();
            if (text != null && !text.isEmpty()) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection selection = new StringSelection(text);
                clipboard.setContents(selection, null);

                // 取消彈窗提示，改用按鈕文字變化提示
                JButton button = (JButton) e.getSource();
                String originalText = button.getText();
                button.setText("已複製!");

                // 1.5秒後恢復按鈕文字
                Timer timer = new Timer(1500, evt -> {
                    button.setText(originalText);
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
            return "typescript-interface.ts";
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
                return ensureDTsExtension(baseName.toLowerCase());
            } else {
                // 如果沒有明顯的請求/響應對，取第一個類名
                return ensureDTsExtension(classNames[0].trim().toLowerCase());
            }
        }

        // 單個類名
        return ensureDTsExtension(className.toLowerCase());
    }

    /**
     * 確保文件名有 .ts 擴展名（不含 .d）
     */
    private String ensureDTsExtension(String fileName) {
        if (fileName.endsWith(".d.ts")) {
            return fileName.substring(0, fileName.length() - 5) + ".ts";
        } else if (fileName.endsWith(".ts")) {
            return fileName;
        } else {
            return fileName + ".ts";
        }
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
        // 檢查是否已經顯示或已被銷毀
        if (isShowing() || isDisposed()) {
            System.out.println("窗口已顯示或已被銷毀，無需再次顯示");
            return;
        }

        try {
            // 使用 ApplicationManager 確保在正確的模態狀態下執行
            // 使用 getModalityStateForComponent 獲取當前窗口的模態狀態，避免使用 ANY
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait(() -> {
                try {
                    // 調用父類方法顯示對話框
                    super.show();
                } catch (Exception e) {
                    System.err.println("顯示窗口時發生錯誤: " + e.getMessage());
                    e.printStackTrace();
                }
            }, com.intellij.openapi.application.ModalityState.defaultModalityState());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("顯示窗口時發生錯誤: " + e.getMessage());
            close(CANCEL_EXIT_CODE);
        }
    }

    /**
     * 自定義窗口關閉處理
     */
    @Override
    public void doCancelAction() {
        try {
            // 先標記為已處理
            if (isDisposed()) {
                return;
            }

            // 使用 ApplicationManager 確保在 EDT 線程中執行
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    // 調用父類方法關閉窗口
                    super.doCancelAction();
                } catch (Exception e) {
                    System.err.println("關閉窗口時發生錯誤: " + e.getMessage());
                    e.printStackTrace();
                    // 強制關閉
                    try {
                        close(CANCEL_EXIT_CODE);
                    } catch (Exception ex) {
                        // 忽略
                    }
                }
            }, com.intellij.openapi.application.ModalityState.defaultModalityState());
        } catch (Exception e) {
            System.err.println("關閉窗口時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        // 在處理前先檢查狀態
        if (isDisposed()) {
            return; // 避免重複處理
        }

        try {
            // 清理資源前先取消所有掛起的操作
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    // 確保 UI 組件被正確清理
                    if (typescriptInterfaceContentDisplayPanel != null) {
                        typescriptInterfaceContentDisplayPanel.dispose();
                        typescriptInterfaceContentDisplayPanel = null;
                    }

                    // 釋放其他資源
                    fileNameField = null;
                    className = null;

                    // 最後調用父類的 dispose 方法
                    super.dispose();
                } catch (Exception e) {
                    System.err.println("清理資源時發生錯誤: " + e.getMessage());
                    e.printStackTrace();
                    super.dispose(); // 確保父類的 dispose 仍然被調用
                }
            }, com.intellij.openapi.application.ModalityState.defaultModalityState());
        } catch (Exception e) {
            System.err.println("清理資源時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            super.dispose(); // 確保父類的 dispose 仍然被調用
        }
    }

    protected class CustomerCloseAction extends DialogWrapperAction {

        private final TypescriptInterfaceShowerWrapper wrapper;
        // 使用原子變量追蹤處理狀態，避免重複處理
        private final java.util.concurrent.atomic.AtomicBoolean processed = new java.util.concurrent.atomic.AtomicBoolean(
                false);

        protected CustomerCloseAction(TypescriptInterfaceShowerWrapper wrapper) {
            super("複製並關閉");
            this.wrapper = wrapper;
        }

        @Override
        protected void doAction(ActionEvent actionEvent) {
            // 確保只執行一次
            if (processed.getAndSet(true)) {
                return;
            }

            // 使用 ApplicationManager 確保在 EDT 線程中執行
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                // 先檢查內容是否包含 any 類型，如果包含則顯示確認對話框
                try {
                    JTextArea textArea = typescriptInterfaceContentDisplayPanel.getTextArea();
                    if (textArea == null) {
                        System.err.println("文本區域為空");
                        safeCancelAction();
                        return;
                    }

                    String text = textArea.getText();
                    if (text == null || text.isEmpty()) {
                        System.out.println("內容為空，無需複製");
                        safeCancelAction();
                        return;
                    }

                    // 檢查內容是否包含 any 類型
                    if (text.contains(": any") || text.contains("?: any")) {
                        // 直接使用 JOptionPane 的靜態方法，不再使用父組件
                        // 避免與已關閉窗口相關的對話框顯示問題
                        final int result = JOptionPane.showConfirmDialog(
                                null, // 使用 null 作為父組件，避免引用已關閉的窗口
                                "內容中存在未知類型(any)，建議手動檢查並修改。是否仍要複製？",
                                "類型警告",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);

                        if (result != JOptionPane.YES_OPTION) {
                            // 用戶選擇否，重置處理狀態，允許再次嘗試
                            processed.set(false);
                            return;
                        } else {
                            // 用戶選擇是，執行複製操作
                            performCopyAndClose(text);
                        }
                    } else {
                        // 內容不包含 any，直接執行複製操作
                        performCopyAndClose(text);
                    }
                } catch (Exception ex) {
                    System.err.println("操作過程中發生錯誤: " + ex.getMessage());
                    ex.printStackTrace();
                    safeCancelAction();
                }
            }, com.intellij.openapi.application.ModalityState.defaultModalityState());
        }

        /**
         * 執行複製到剪貼板並關閉窗口的操作
         */
        private void performCopyAndClose(String text) {
            try {
                // 複製到剪貼板
                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable tText = new StringSelection(text);
                systemClipboard.setContents(tText, null);

                // 顯示日誌而不是彈窗
                System.out.println("已複製 " + text.length() + " 個字符到剪貼板");

                // 使用 ApplicationManager 確保在 EDT 線程中執行關閉操作
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                    // 關閉對話框
                    try {
                        wrapper.close(DialogWrapper.OK_EXIT_CODE);
                    } catch (Exception e) {
                        // 如果關閉失敗，嘗試替代方法
                        System.err.println("關閉窗口失敗，嘗試替代方法: " + e.getMessage());
                        try {
                            wrapper.dispose();
                        } catch (Exception ex) {
                            // 最後嘗試
                            System.err.println("無法釋放資源: " + ex.getMessage());
                        }
                    }
                }, com.intellij.openapi.application.ModalityState.defaultModalityState());
            } catch (Exception ex) {
                System.err.println("複製失敗: " + ex.getMessage());
                ex.printStackTrace();
                safeCancelAction();
            }
        }

        /**
         * 安全地關閉窗口
         */
        private void safeCancelAction() {
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    if (!wrapper.isDisposed()) {
                        wrapper.close(DialogWrapper.CANCEL_EXIT_CODE);
                    }
                } catch (Exception ex) {
                    System.err.println("關閉窗口失敗: " + ex.getMessage());
                    ex.printStackTrace();
                    // 嘗試替代方法
                    try {
                        wrapper.dispose();
                    } catch (Exception e) {
                        // 忽略
                    }
                }
            }, com.intellij.openapi.application.ModalityState.defaultModalityState());
        }
    }

    // 添加內部類以處理對話框組件的資源釋放
    public static class TypescriptInterfaceContentDisplayPanel {
        private JPanel panel;
        private JTextArea textArea;
        private JScrollPane scrollPane;

        public TypescriptInterfaceContentDisplayPanel() {
            // 創建組件
            panel = new JPanel(new BorderLayout());
            textArea = new JTextArea();
            textArea.setEditable(true);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

            scrollPane = new JScrollPane(textArea);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            panel.add(scrollPane, BorderLayout.CENTER);
        }

        public JPanel mainPanel() {
            return panel;
        }

        public JTextArea getTextArea() {
            return textArea;
        }

        public void setTextContent(String content) {
            if (textArea != null && content != null) {
                textArea.setText(content);
                textArea.setCaretPosition(0);
                System.out.println("使用 setTextContent 設置內容，長度: " + content.length());
            }
        }

        public void dispose() {
            // 釋放所有組件資源
            if (textArea != null) {
                textArea.setText(null); // 清除內容釋放記憶體
                textArea = null;
            }

            if (scrollPane != null) {
                scrollPane.removeAll();
                scrollPane = null;
            }

            if (panel != null) {
                panel.removeAll();
                panel = null;
            }
        }
    }
}
