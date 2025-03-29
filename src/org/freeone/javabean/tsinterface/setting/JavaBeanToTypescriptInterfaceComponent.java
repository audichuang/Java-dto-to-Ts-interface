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

    public JavaBeanToTypescriptInterfaceComponent() {
        createUI();
    }

    private void createUI() {
        jPanel = new JPanel(new BorderLayout(10, 10));
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 基本設定面板
        JPanel basicSettingsPanel = createBasicSettingsPanel();

        // DTO後綴設定面板
        JPanel dtoSuffixesPanel = createDtoSuffixesPanel();

        // 合併面板
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.add(basicSettingsPanel, BorderLayout.NORTH);
        mainPanel.add(dtoSuffixesPanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(600, 500));

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

        // 添加到面板
        panel.add(dateToStringCheckBox);
        panel.add(useJsonPropertyCheckBox);
        panel.add(allowFindClassInAllScope);
        panel.add(ignoreParentField);
        panel.add(addOptionalMarkToAllFields);
        panel.add(ignoreSerialVersionUID);

        return panel;
    }

    private JPanel createDtoSuffixesPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "DTO 類後綴設定", TitledBorder.LEFT, TitledBorder.TOP));

        // 添加 Request DTO 後綴設定面板
        panel.add(createRequestDtoSuffixesPanel());

        // 添加 Response DTO 後綴設定面板
        panel.add(createResponseDtoSuffixesPanel());

        return panel;
    }

    private JPanel createRequestDtoSuffixesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Request DTO 後綴", TitledBorder.LEFT, TitledBorder.TOP));

        // 說明文字
        JTextArea description = new JTextArea(
                "設定用於識別請求類(Request DTO)的後綴名稱。插件會自動識別符合這些後綴的類作為請求數據對象進行轉換。");
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        description.setOpaque(false);
        description.setBackground(new Color(0, 0, 0, 0));
        description.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 5));

        // 初始化表格
        requestDtoSuffixTableModel = new DefaultTableModel(new String[] { "Request 後綴" }, 0);
        requestDtoSuffixTable = new JTable(requestDtoSuffixTableModel);
        requestDtoSuffixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScrollPane = new JScrollPane(requestDtoSuffixTable);
        tableScrollPane.setPreferredSize(new Dimension(300, 100));

        // 新增後綴面板
        JPanel addSuffixPanel = new JPanel(new BorderLayout(5, 0));
        newRequestSuffixField = new JTextField(15);
        addRequestSuffixButton = new JButton("添加");
        removeRequestSuffixButton = new JButton("刪除");

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.add(addRequestSuffixButton);
        buttonsPanel.add(removeRequestSuffixButton);

        addSuffixPanel.add(new JLabel("新增後綴: "), BorderLayout.WEST);
        addSuffixPanel.add(newRequestSuffixField, BorderLayout.CENTER);
        addSuffixPanel.add(buttonsPanel, BorderLayout.EAST);
        addSuffixPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 添加事件監聽器
        addRequestSuffixButton.addActionListener(e -> {
            String suffix = newRequestSuffixField.getText().trim();
            if (!suffix.isEmpty() && !containsSuffix(requestDtoSuffixTableModel, suffix)) {
                requestDtoSuffixTableModel.addRow(new Object[] { suffix });
                newRequestSuffixField.setText("");
            }
        });

        removeRequestSuffixButton.addActionListener(e -> {
            int selectedRow = requestDtoSuffixTable.getSelectedRow();
            if (selectedRow != -1) {
                requestDtoSuffixTableModel.removeRow(selectedRow);
            }
        });

        // 組合面板
        panel.add(description, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(addSuffixPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createResponseDtoSuffixesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Response DTO 後綴", TitledBorder.LEFT, TitledBorder.TOP));

        // 說明文字
        JTextArea description = new JTextArea(
                "設定用於識別響應類(Response DTO)的後綴名稱。插件會自動識別符合這些後綴的類作為響應數據對象進行轉換。");
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        description.setOpaque(false);
        description.setBackground(new Color(0, 0, 0, 0));
        description.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 5));

        // 初始化表格
        responseDtoSuffixTableModel = new DefaultTableModel(new String[] { "Response 後綴" }, 0);
        responseDtoSuffixTable = new JTable(responseDtoSuffixTableModel);
        responseDtoSuffixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScrollPane = new JScrollPane(responseDtoSuffixTable);
        tableScrollPane.setPreferredSize(new Dimension(300, 100));

        // 新增後綴面板
        JPanel addSuffixPanel = new JPanel(new BorderLayout(5, 0));
        newResponseSuffixField = new JTextField(15);
        addResponseSuffixButton = new JButton("添加");
        removeResponseSuffixButton = new JButton("刪除");

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.add(addResponseSuffixButton);
        buttonsPanel.add(removeResponseSuffixButton);

        addSuffixPanel.add(new JLabel("新增後綴: "), BorderLayout.WEST);
        addSuffixPanel.add(newResponseSuffixField, BorderLayout.CENTER);
        addSuffixPanel.add(buttonsPanel, BorderLayout.EAST);
        addSuffixPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 添加事件監聽器
        addResponseSuffixButton.addActionListener(e -> {
            String suffix = newResponseSuffixField.getText().trim();
            if (!suffix.isEmpty() && !containsSuffix(responseDtoSuffixTableModel, suffix)) {
                responseDtoSuffixTableModel.addRow(new Object[] { suffix });
                newResponseSuffixField.setText("");
            }
        });

        removeResponseSuffixButton.addActionListener(e -> {
            int selectedRow = responseDtoSuffixTable.getSelectedRow();
            if (selectedRow != -1) {
                responseDtoSuffixTableModel.removeRow(selectedRow);
            }
        });

        // 組合面板
        panel.add(description, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(addSuffixPanel, BorderLayout.SOUTH);

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

    public List<String> getRequestDtoSuffixes() {
        List<String> suffixes = new ArrayList<>();
        for (int i = 0; i < requestDtoSuffixTableModel.getRowCount(); i++) {
            String suffix = (String) requestDtoSuffixTableModel.getValueAt(i, 0);
            if (suffix != null && !suffix.isEmpty()) {
                suffixes.add(suffix);
            }
        }
        return suffixes;
    }

    public List<String> getResponseDtoSuffixes() {
        List<String> suffixes = new ArrayList<>();
        for (int i = 0; i < responseDtoSuffixTableModel.getRowCount(); i++) {
            String suffix = (String) responseDtoSuffixTableModel.getValueAt(i, 0);
            if (suffix != null && !suffix.isEmpty()) {
                suffixes.add(suffix);
            }
        }
        return suffixes;
    }

    // 為了兼容現有代碼，保留這個方法
    public List<String> getCustomDtoSuffixes() {
        List<String> allSuffixes = new ArrayList<>();
        allSuffixes.addAll(getRequestDtoSuffixes());
        allSuffixes.addAll(getResponseDtoSuffixes());
        return allSuffixes;
    }

    public void setRequestDtoSuffixes(List<String> suffixes) {
        // 清空表格
        while (requestDtoSuffixTableModel.getRowCount() > 0) {
            requestDtoSuffixTableModel.removeRow(0);
        }

        // 添加新數據
        for (String suffix : suffixes) {
            if (suffix != null && !suffix.isEmpty()) {
                requestDtoSuffixTableModel.addRow(new Object[] { suffix });
            }
        }
    }

    public void setResponseDtoSuffixes(List<String> suffixes) {
        // 清空表格
        while (responseDtoSuffixTableModel.getRowCount() > 0) {
            responseDtoSuffixTableModel.removeRow(0);
        }

        // 添加新數據
        for (String suffix : suffixes) {
            if (suffix != null && !suffix.isEmpty()) {
                responseDtoSuffixTableModel.addRow(new Object[] { suffix });
            }
        }
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
}
