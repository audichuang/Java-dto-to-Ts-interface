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

    public TypescriptInterfaceShowerWrapper() {
        super(true);
        typescriptInterfaceContentDisplayPanel = new TypescriptInterfaceContentDisplayPanel();
        init();
        setTitle("Ts Interface Content");
        setSize(600, 700);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return typescriptInterfaceContentDisplayPanel.mainPanel();
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

                        // 顯示成功提示（可選）
                        System.out.println("已複製 " + text.length() + " 個字符到剪貼板");
                        JOptionPane.showMessageDialog(
                                wrapper.getWindow(),
                                "已成功複製 TypeScript 接口到剪貼板",
                                "複製成功",
                                JOptionPane.INFORMATION_MESSAGE);
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
