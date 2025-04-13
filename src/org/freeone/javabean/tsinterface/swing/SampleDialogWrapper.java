//package org.freeone.javabean.tsinterface.swing;
//
//import com.intellij.openapi.ui.DialogWrapper;
//import org.jetbrains.annotations.Nullable;
//
//import javax.swing.*;
//import java.awt.*;
//
///**
// * 簡單對話框，用於確認操作
// */
//public class SampleDialogWrapper extends DialogWrapper {
//    private JPanel contentPane;
//    private JLabel messageLabel;
//
//    public SampleDialogWrapper() {
//        super(true); // 使用模態對話框
//        setTitle("確認");
//        initUI();
//        init(); // 不要忘記這個調用
//        setSize(400, 150);
//    }
//
//    private void initUI() {
//        contentPane = new JPanel(new BorderLayout());
//        messageLabel = new JLabel("確定生成該內部類的 TypeScript 接口嗎？", JLabel.CENTER);
//        messageLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
//
//        contentPane.add(messageLabel, BorderLayout.CENTER);
//        contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
//    }
//
//    @Nullable
//    @Override
//    protected JComponent createCenterPanel() {
//        return contentPane;
//    }
//
//    @Override
//    public void show() {
//        super.show();
//        if (getWindow() != null) {
//            getWindow().toFront();
//        }
//    }
//}