package org.freeone.javabean.tsinterface.setting;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaBeanToTypescriptInterfaceComponent {
    private JPanel jPanel;

    // 基本設定
    private JCheckBox dateToStringCheckBox;
    private JCheckBox useJsonPropertyCheckBox;
    private JCheckBox allowFindClassInAllScope;
    private JCheckBox ignoreParentField;
    private JCheckBox addOptionalMarkToAllFields;
    private JCheckBox ignoreSerialVersionUID;
    private JCheckBox onlyProcessGenericDtoCheckBox;

    // Request DTO後綴設定
    private JTable requestDtoSuffixTable;
    private DefaultTableModel requestDtoSuffixTableModel;
    private JTextField newRequestSuffixField;
    private JButton addRequestSuffixButton;
    private JButton removeRequestSuffixButton;

    // Response DTO後綴設定
    private JTable responseDtoSuffixTable;
    private DefaultTableModel responseDtoSuffixTableModel;
    private JTextField newResponseSuffixField;
    private JButton addResponseSuffixButton;
    private JButton removeResponseSuffixButton;

    // 電文代號相關成員
    private JCheckBox useTransactionCodePrefixCheckBox;
    private JTextField requestSuffixField;
    private JTextField responseSuffixField;
    private JPanel transactionCodePanel;

    public JavaBeanToTypescriptInterfaceComponent() {
        createUI();
    }

    private void createUI() {
        jPanel = new JPanel(new BorderLayout(10, 10));
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 基本設定面板
        JPanel basicSettingsPanel = createBasicSettingsPanel();

        // 電文代號設定面板
        JPanel transactionCodePanel = createTransactionCodePanel();

        // 後綴設定面板
        JPanel suffixSettingsPanel = createSuffixSettingsPanel();

        // 使用BoxLayout垂直排列各個面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // 添加適當的間距
        mainPanel.add(basicSettingsPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(transactionCodePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(suffixSettingsPanel);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // 滑動速度更快
        scrollPane.setPreferredSize(new Dimension(650, 500));

        jPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createBasicSettingsPanel() {
        // 使用更好的佈局管理器
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                "基本設定",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                new Color(60, 60, 60)));

        // 初始化複選框
        dateToStringCheckBox = createStyledCheckBox("java.util.Date 轉換為字符串",
                "把 Date 類型轉為 string 類型而不是 any");

        useJsonPropertyCheckBox = createStyledCheckBox("使用 @JsonProperty 註解",
                "優先使用 @JsonProperty 註解中指定的屬性名");

        allowFindClassInAllScope = createStyledCheckBox("允許在所有範圍內查找類",
                "允許在項目的所有範圍內查找類定義");

        ignoreParentField = createStyledCheckBox("忽略父類字段",
                "生成時不包含父類字段，僅包含當前類的字段");

        addOptionalMarkToAllFields = createStyledCheckBox("給所有字段添加可選標記 (?: )",
                "為 TypeScript 接口中的所有屬性添加可選標記 (?:)");

        ignoreSerialVersionUID = createStyledCheckBox("忽略序列化ID (serialVersionUID)",
                "在生成的 TypeScript 接口中忽略 serialVersionUID 字段");

        // 添加新選項：只處理泛型DTO
        onlyProcessGenericDtoCheckBox = createStyledCheckBox("只處理泛型DTO (不處理外層包裝類)",
                "只為泛型DTO生成電文代號前綴，不處理如 ResponseTemplate 等外層包裝類");

        // 使用內邊距面板包裝
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(0, 1, 0, 3));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // 添加到面板
        optionsPanel.add(dateToStringCheckBox);
        optionsPanel.add(useJsonPropertyCheckBox);
        optionsPanel.add(allowFindClassInAllScope);
        optionsPanel.add(ignoreParentField);
        optionsPanel.add(addOptionalMarkToAllFields);
        optionsPanel.add(ignoreSerialVersionUID);
        optionsPanel.add(onlyProcessGenericDtoCheckBox);

        panel.add(optionsPanel);

        return panel;
    }

    private JCheckBox createStyledCheckBox(String text, String tooltip) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setToolTipText(tooltip);
        checkBox.setFocusPainted(false); // 移除焦點邊框
        checkBox.setFont(new Font(checkBox.getFont().getName(), Font.PLAIN, 13)); // 稍微調整字體
        return checkBox;
    }

    private JPanel createTransactionCodePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                "電文代號命名設定",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                new Color(60, 60, 60)));

        // 說明文字
        JTextArea description = new JTextArea(
                "設定是否使用電文代號作為生成的TypeScript介面名稱前綴，並指定後綴格式。");
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        description.setOpaque(false);
        description.setBackground(new Color(0, 0, 0, 0));
        description.setFont(new Font(description.getFont().getName(), Font.ITALIC, 12));
        description.setForeground(new Color(100, 100, 100));
        description.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 15));

        // 使用電文代號作為前綴的復選框
        useTransactionCodePrefixCheckBox = createStyledCheckBox("使用電文代號作為介面名稱前綴",
                "從控制器方法註解中提取電文代號(如RET-B-QRYSTATEMENTS)作為生成介面的前綴");

        // 設置復選框的邊距
        JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        checkBoxPanel.setOpaque(false);
        checkBoxPanel.add(useTransactionCodePrefixCheckBox);

        // 後綴輸入面板
        JPanel suffixPanel = new JPanel(new GridBagLayout());
        suffixPanel.setBorder(BorderFactory.createEmptyBorder(5, 30, 10, 20));
        suffixPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel requestSuffixLabel = new JLabel("Request後綴:");
        requestSuffixLabel.setFont(new Font(requestSuffixLabel.getFont().getName(), Font.PLAIN, 13));
        requestSuffixField = new JTextField("Req", 10);

        JLabel responseSuffixLabel = new JLabel("Response後綴:");
        responseSuffixLabel.setFont(new Font(responseSuffixLabel.getFont().getName(), Font.PLAIN, 13));
        responseSuffixField = new JTextField("Resp", 10);

        // 示例標籤
        JLabel requestExampleLabel = new JLabel("例如: QRYSTATEMENTSReq");
        requestExampleLabel.setFont(new Font(requestExampleLabel.getFont().getName(), Font.ITALIC, 11));
        requestExampleLabel.setForeground(new Color(100, 100, 100));

        JLabel responseExampleLabel = new JLabel("例如: QRYSTATEMENTSResp");
        responseExampleLabel.setFont(new Font(responseExampleLabel.getFont().getName(), Font.ITALIC, 11));
        responseExampleLabel.setForeground(new Color(100, 100, 100));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        suffixPanel.add(requestSuffixLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.8;
        suffixPanel.add(requestSuffixField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.8;
        gbc.anchor = GridBagConstraints.WEST;
        suffixPanel.add(requestExampleLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.2;
        suffixPanel.add(responseSuffixLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.8;
        suffixPanel.add(responseSuffixField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 0.8;
        gbc.anchor = GridBagConstraints.WEST;
        suffixPanel.add(responseExampleLabel, gbc);

        // 監聽器：當勾選/取消勾選時顯示/隱藏後綴設定
        useTransactionCodePrefixCheckBox.addActionListener(e -> {
            suffixPanel.setVisible(useTransactionCodePrefixCheckBox.isSelected());
        });

        // 初始狀態
        suffixPanel.setVisible(false);

        // 組合面板
        panel.add(description);
        panel.add(checkBoxPanel);
        panel.add(suffixPanel);

        this.transactionCodePanel = panel;
        return panel;
    }

    private JPanel createSuffixSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                "DTO後綴設定",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                new Color(60, 60, 60)));

        // 說明文字
        JTextArea description = new JTextArea(
                "設定識別請求和響應DTO的後綴名稱。系統會根據這些後綴來識別並正確分類Java類。");
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        description.setOpaque(false);
        description.setBackground(new Color(0, 0, 0, 0));
        description.setFont(new Font(description.getFont().getName(), Font.ITALIC, 12));
        description.setForeground(new Color(100, 100, 100));
        description.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 15));

        panel.add(description);

        // 創建請求和響應DTO設定的標籤頁面板
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // 請求DTO標籤頁
        JPanel requestPanel = createSuffixListPanel("請求",
                getRequestDtoSuffixes(),
                "Request", "Req", "Tranrq");

        // 響應DTO標籤頁
        JPanel responsePanel = createSuffixListPanel("響應",
                getResponseDtoSuffixes(),
                "Response", "Resp", "Tranrs");

        tabbedPane.addTab("請求DTO後綴", requestPanel);
        tabbedPane.addTab("響應DTO後綴", responsePanel);

        panel.add(tabbedPane);

        return panel;
    }

    private JPanel createSuffixListPanel(String type, List<String> defaultSuffixes,
                                         String... placeHolderExamples) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 建立列表模型
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String suffix : defaultSuffixes) {
            listModel.addElement(suffix);
        }

        // 創建列表和滾動面板
        JList<String> suffixList = new JList<>(listModel);
        suffixList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suffixList.setVisibleRowCount(8);

        JScrollPane listScrollPane = new JScrollPane(suffixList);
        listScrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        // 創建添加面板
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField newSuffixField = new JTextField(15);

        // 設置佔位符
        StringBuilder placeholderText = new StringBuilder("例如: ");
        for (int i = 0; i < placeHolderExamples.length; i++) {
            placeholderText.append(placeHolderExamples[i]);
            if (i < placeHolderExamples.length - 1) {
                placeholderText.append(", ");
            }
        }

        newSuffixField.putClientProperty("JTextField.placeholderText", placeholderText.toString());

        JButton addButton = new JButton("添加");
        JButton removeButton = new JButton("刪除");

        addPanel.add(newSuffixField);
        addPanel.add(addButton);
        addPanel.add(removeButton);

        // 添加按鈕動作
        addButton.addActionListener(e -> {
            String newSuffix = newSuffixField.getText().trim();
            if (!newSuffix.isEmpty() && !listModel.contains(newSuffix)) {
                listModel.addElement(newSuffix);
                newSuffixField.setText("");
            }
        });

        // 刪除按鈕動作
        removeButton.addActionListener(e -> {
            int selectedIndex = suffixList.getSelectedIndex();
            if (selectedIndex != -1) {
                listModel.remove(selectedIndex);
            }
        });

        // 設置按鈕外觀
        addButton.setFocusPainted(false);
        removeButton.setFocusPainted(false);

        panel.add(listScrollPane, BorderLayout.CENTER);
        panel.add(addPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 檢查後綴是否已存在
    private boolean containsSuffix(DefaultTableModel model, String suffix) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (Objects.equals(model.getValueAt(i, 0), suffix)) {
                return true;
            }
        }
        return false;
    }

    public JPanel getJPanel() {
        return jPanel;
    }

    public JCheckBox getDateToStringCheckBox() {
        return dateToStringCheckBox;
    }

    public JCheckBox getUseJsonPropertyCheckBox() {
        return useJsonPropertyCheckBox;
    }

    public JCheckBox getAllowFindClassInAllScope() {
        return allowFindClassInAllScope;
    }

    public JCheckBox getIgnoreParentField() {
        return ignoreParentField;
    }

    public JCheckBox getAddOptionalMarkToAllFields() {
        return addOptionalMarkToAllFields;
    }

    public JCheckBox getIgnoreSerialVersionUID() {
        return ignoreSerialVersionUID;
    }

    public JCheckBox getOnlyProcessGenericDtoCheckBox() {
        return onlyProcessGenericDtoCheckBox;
    }

    public boolean isOnlyProcessGenericDto() {
        return onlyProcessGenericDtoCheckBox.isSelected();
    }

    public void setOnlyProcessGenericDto(boolean selected) {
        onlyProcessGenericDtoCheckBox.setSelected(selected);
    }

    // 獲取請求 DTO 後綴列表（使用默認值）
    public List<String> getRequestDtoSuffixes() {
        List<String> defaultSuffixes = new ArrayList<>();
        defaultSuffixes.add("DTO");
        defaultSuffixes.add("Dto");
        defaultSuffixes.add("Request");
        defaultSuffixes.add("Req");
        defaultSuffixes.add("Rq");
        defaultSuffixes.add("Tranrq");
        defaultSuffixes.add("Qry");
        defaultSuffixes.add("Query");
        defaultSuffixes.add("Model");
        defaultSuffixes.add("Entity");
        defaultSuffixes.add("Data");
        defaultSuffixes.add("Bean");
        defaultSuffixes.add("VO");
        defaultSuffixes.add("Vo");
        return defaultSuffixes;
    }

    // 設置請求 DTO 後綴列表（不再需要，但保留方法避免錯誤）
    public void setRequestDtoSuffixes(List<String> suffixes) {
        // 由於移除了設定界面，此方法不再需要實現
    }

    // 獲取響應 DTO 後綴列表（使用默認值）
    public List<String> getResponseDtoSuffixes() {
        List<String> defaultSuffixes = new ArrayList<>();
        defaultSuffixes.add("Response");
        defaultSuffixes.add("Resp");
        defaultSuffixes.add("Rs");
        defaultSuffixes.add("Tranrs");
        defaultSuffixes.add("Result");
        defaultSuffixes.add("Results");
        defaultSuffixes.add("Detail");
        defaultSuffixes.add("Info");
        return defaultSuffixes;
    }

    // 設置響應 DTO 後綴列表（不再需要，但保留方法避免錯誤）
    public void setResponseDtoSuffixes(List<String> suffixes) {
        // 由於移除了設定界面，此方法不再需要實現
    }

    // 為了兼容現有代碼，保留這個方法，但實際上會根據後綴進行分類
    public void setCustomDtoSuffixes(List<String> suffixes) {
        List<String> requestSuffixes = new ArrayList<>();
        List<String> responseSuffixes = new ArrayList<>();

        // 依據命名規則將後綴分類
        for (String suffix : suffixes) {
            if (suffix == null || suffix.isEmpty()) {
                continue;
            }

            if (suffix.contains("Request") || suffix.contains("Req") ||
                    suffix.equals("Rq") || suffix.contains("Tranrq")) {
                requestSuffixes.add(suffix);
            } else if (suffix.contains("Response") || suffix.contains("Resp") ||
                    suffix.equals("Rs") || suffix.contains("Tranrs") ||
                    suffix.contains("Result")) {
                responseSuffixes.add(suffix);
            } else {
                // 其他後綴默認放到 request 類別
                requestSuffixes.add(suffix);
            }
        }

        setRequestDtoSuffixes(requestSuffixes);
        setResponseDtoSuffixes(responseSuffixes);
    }

    // 新增電文代號相關方法
    public boolean isUseTransactionCodePrefix() {
        return useTransactionCodePrefixCheckBox.isSelected();
    }

    public void setUseTransactionCodePrefix(boolean selected) {
        useTransactionCodePrefixCheckBox.setSelected(selected);
        // 確保UI狀態更新
        for (java.awt.event.ActionListener listener : useTransactionCodePrefixCheckBox.getActionListeners()) {
            listener.actionPerformed(new java.awt.event.ActionEvent(
                    useTransactionCodePrefixCheckBox,
                    java.awt.event.ActionEvent.ACTION_PERFORMED,
                    ""));
        }
    }

    public String getRequestSuffix() {
        return requestSuffixField.getText();
    }

    public void setRequestSuffix(String suffix) {
        requestSuffixField.setText(suffix);
    }

    public String getResponseSuffix() {
        return responseSuffixField.getText();
    }

    public void setResponseSuffix(String suffix) {
        responseSuffixField.setText(suffix);
    }

    // 獲取主面板
    public JPanel getMainPanel() {
        return jPanel;
    }

    // 获取复选框状态的方法
    public boolean isDateToString() {
        return dateToStringCheckBox.isSelected();
    }

    // 设置复选框状态的方法
    public void setDateToString(boolean selected) {
        dateToStringCheckBox.setSelected(selected);
    }

    public boolean isUseAnnotationJsonProperty() {
        return useJsonPropertyCheckBox.isSelected();
    }

    public void setUseAnnotationJsonProperty(boolean selected) {
        useJsonPropertyCheckBox.setSelected(selected);
    }

    public boolean isAllowFindClassInAllScope() {
        return allowFindClassInAllScope.isSelected();
    }

    public void setAllowFindClassInAllScope(boolean selected) {
        allowFindClassInAllScope.setSelected(selected);
    }

    public boolean isIgnoreParentField() {
        return ignoreParentField.isSelected();
    }

    public void setIgnoreParentField(boolean selected) {
        ignoreParentField.setSelected(selected);
    }

    public boolean isAddOptionalMarkToAllFields() {
        return addOptionalMarkToAllFields.isSelected();
    }

    public void setAddOptionalMarkToAllFields(boolean selected) {
        addOptionalMarkToAllFields.setSelected(selected);
    }

    public boolean isIgnoreSerialVersionUID() {
        return ignoreSerialVersionUID.isSelected();
    }

    public void setIgnoreSerialVersionUID(boolean selected) {
        ignoreSerialVersionUID.setSelected(selected);
    }
}
