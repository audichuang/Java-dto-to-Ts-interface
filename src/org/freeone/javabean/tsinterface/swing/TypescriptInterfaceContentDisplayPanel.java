package org.freeone.javabean.tsinterface.swing;

import javax.swing.*;
import java.awt.*;

public class TypescriptInterfaceContentDisplayPanel {
    private JPanel panel;
    private JTextArea textArea;
    private JScrollPane scrollPane;

    public TypescriptInterfaceContentDisplayPanel() {
        createUI();
    }

    private void createUI() {
        panel = new JPanel(new BorderLayout());

        // 創建文本區域，並設置初始大小和屬性
        textArea = new JTextArea(20, 60);
        textArea.setEditable(true);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText("正在初始化..."); // 初始化文本

        // 創建滾動面板
        scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 600));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // 添加到面板
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    public JPanel mainPanel() {
        return panel;
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    // 新增方法，直接設置文本內容，並確保可見
    public void setTextContent(String content) {
        if (textArea != null && content != null) {
            textArea.setText(content);
            textArea.setCaretPosition(0);
            textArea.invalidate();
            textArea.repaint();
        }
    }
}
