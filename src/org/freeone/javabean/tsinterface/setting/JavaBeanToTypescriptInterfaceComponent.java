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

        // 合併面板
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.add(basicSettingsPanel, BorderLayout.NORTH);
        mainPanel.add(transactionCodePanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        jPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createBasicSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "基本設定", TitledBorder.LEFT, TitledBorder.TOP));

        // 初始化複選框
        dateToStringCheckBox = new JCheckBox("java.util.Date 轉換為字符串");
        dateToStringCheckBox.setToolTipText("把 Date 類型轉為 string 類型而不是 any");

        useJsonPropertyCheckBox = new JCheckBox("使用 @JsonProperty 註解");
        useJsonPropertyCheckBox.setToolTipText("優先使用 @JsonProperty 註解中指定的屬性名");

        allowFindClassInAllScope = new JCheckBox("允許在所有範圍內查找類");
        allowFindClassInAllScope.setToolTipText("允許在項目的所有範圍內查找類定義");

        ignoreParentField = new JCheckBox("忽略父類字段");
        ignoreParentField.setToolTipText("生成時不包含父類字段，僅包含當前類的字段");

        addOptionalMarkToAllFields = new JCheckBox("給所有字段添加可選標記 (?: )");
        addOptionalMarkToAllFields.setToolTipText("為 TypeScript 接口中的所有屬性添加可選標記 (?:)");

        ignoreSerialVersionUID = new JCheckBox("忽略序列化ID (serialVersionUID)");
        ignoreSerialVersionUID.setToolTipText("在生成的 TypeScript 接口中忽略 serialVersionUID 字段");

        // 添加新選項：只處理泛型DTO
        onlyProcessGenericDtoCheckBox = new JCheckBox("只處理泛型DTO (不處理外層包裝類)");
        onlyProcessGenericDtoCheckBox.setToolTipText("只為泛型DTO生成電文代號前綴，不處理如 ResponseTemplate 等外層包裝類");

        // 添加到面板
        panel.add(dateToStringCheckBox);
        panel.add(useJsonPropertyCheckBox);
        panel.add(allowFindClassInAllScope);
        panel.add(ignoreParentField);
        panel.add(addOptionalMarkToAllFields);
        panel.add(ignoreSerialVersionUID);
        panel.add(onlyProcessGenericDtoCheckBox);

        return panel;
    }

    private JPanel createTransactionCodePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "電文代號命名設定", TitledBorder.LEFT, TitledBorder.TOP));

        // 說明文字
        JTextArea description = new JTextArea(
                "設定是否使用電文代號作為生成的TypeScript介面名稱前綴，並指定後綴格式。");
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        description.setOpaque(false);
        description.setBackground(new Color(0, 0, 0, 0));
        description.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 5));

        // 使用電文代號作為前綴的復選框
        useTransactionCodePrefixCheckBox = new JCheckBox("使用電文代號作為介面名稱前綴");
        useTransactionCodePrefixCheckBox.setToolTipText("從控制器方法註解中提取電文代號(如RET-B-QRYSTATEMENTS)作為生成介面的前綴");

        // 後綴輸入面板
        JPanel suffixPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        suffixPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));

        JLabel requestSuffixLabel = new JLabel("Request後綴:");
        requestSuffixField = new JTextField("Req", 10);

        JLabel responseSuffixLabel = new JLabel("Response後綴:");
        responseSuffixField = new JTextField("Resp", 10);

        suffixPanel.add(requestSuffixLabel);
        suffixPanel.add(requestSuffixField);
        suffixPanel.add(responseSuffixLabel);
        suffixPanel.add(responseSuffixField);

        // 監聽器：當勾選/取消勾選時顯示/隱藏後綴設定
        useTransactionCodePrefixCheckBox.addActionListener(e -> {
            suffixPanel.setVisible(useTransactionCodePrefixCheckBox.isSelected());
        });

        // 初始狀態
        suffixPanel.setVisible(false);

        // 組合面板
        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.add(useTransactionCodePrefixCheckBox, BorderLayout.NORTH);
        optionsPanel.add(suffixPanel, BorderLayout.CENTER);

        panel.add(description, BorderLayout.NORTH);
        panel.add(optionsPanel, BorderLayout.CENTER);

        this.transactionCodePanel = panel;
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

    public String getRequestSuffix() {
        return requestSuffixField.getText();
    }

    public String getResponseSuffix() {
        return responseSuffixField.getText();
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

    public void setRequestSuffix(String suffix) {
        requestSuffixField.setText(suffix);
    }

    public void setResponseSuffix(String suffix) {
        responseSuffixField.setText(suffix);
    }

    // 設置請求 DTO 後綴列表（不再需要，但保留方法避免錯誤）
    public void setRequestDtoSuffixes(List<String> suffixes) {
        // 由於移除了設定界面，此方法不再需要實現
    }

    // 設置響應 DTO 後綴列表（不再需要，但保留方法避免錯誤）
    public void setResponseDtoSuffixes(List<String> suffixes) {
        // 由於移除了設定界面，此方法不再需要實現
    }
}
