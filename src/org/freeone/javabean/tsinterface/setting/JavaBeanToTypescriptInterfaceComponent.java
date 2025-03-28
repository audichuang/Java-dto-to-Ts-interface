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

    // DTO後綴設定
    private JTable dtoSuffixTable;
    private DefaultTableModel dtoSuffixTableModel;
    private JTextField newSuffixField;
    private JButton addSuffixButton;
    private JButton removeSuffixButton;

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

        jPanel.add(mainPanel, BorderLayout.NORTH);
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
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "DTO 類後綴設定", TitledBorder.LEFT, TitledBorder.TOP));

        // 說明文字
        JTextArea description = new JTextArea(
                "設定用於識別DTO類的後綴名稱。插件會自動識別符合這些後綴的類作為DTO進行轉換。\n" +
                        "如需添加自定義後綴，請在下方輸入框中輸入後點擊「添加」按鈕。");
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        description.setOpaque(false);
        description.setBackground(new Color(0, 0, 0, 0));
        description.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 5));

        // 初始化表格
        dtoSuffixTableModel = new DefaultTableModel(new String[] { "DTO 後綴" }, 0);
        dtoSuffixTable = new JTable(dtoSuffixTableModel);
        dtoSuffixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScrollPane = new JScrollPane(dtoSuffixTable);
        tableScrollPane.setPreferredSize(new Dimension(300, 150));

        // 新增後綴面板
        JPanel addSuffixPanel = new JPanel(new BorderLayout(5, 0));
        newSuffixField = new JTextField(15);
        addSuffixButton = new JButton("添加");
        removeSuffixButton = new JButton("刪除");

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.add(addSuffixButton);
        buttonsPanel.add(removeSuffixButton);

        addSuffixPanel.add(new JLabel("新增後綴: "), BorderLayout.WEST);
        addSuffixPanel.add(newSuffixField, BorderLayout.CENTER);
        addSuffixPanel.add(buttonsPanel, BorderLayout.EAST);
        addSuffixPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 添加事件監聽器
        addSuffixButton.addActionListener(e -> {
            String suffix = newSuffixField.getText().trim();
            if (!suffix.isEmpty() && !containsSuffix(suffix)) {
                dtoSuffixTableModel.addRow(new Object[] { suffix });
                newSuffixField.setText("");
            }
        });

        removeSuffixButton.addActionListener(e -> {
            int selectedRow = dtoSuffixTable.getSelectedRow();
            if (selectedRow != -1) {
                dtoSuffixTableModel.removeRow(selectedRow);
            }
        });

        // 組合面板
        panel.add(description, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(addSuffixPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 檢查後綴是否已存在
    private boolean containsSuffix(String suffix) {
        for (int i = 0; i < dtoSuffixTableModel.getRowCount(); i++) {
            if (Objects.equals(dtoSuffixTableModel.getValueAt(i, 0), suffix)) {
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

    public List<String> getCustomDtoSuffixes() {
        List<String> suffixes = new ArrayList<>();
        for (int i = 0; i < dtoSuffixTableModel.getRowCount(); i++) {
            String suffix = (String) dtoSuffixTableModel.getValueAt(i, 0);
            if (suffix != null && !suffix.isEmpty()) {
                suffixes.add(suffix);
            }
        }
        return suffixes;
    }

    public void setCustomDtoSuffixes(List<String> suffixes) {
        // 清空表格
        while (dtoSuffixTableModel.getRowCount() > 0) {
            dtoSuffixTableModel.removeRow(0);
        }

        // 添加新數據
        for (String suffix : suffixes) {
            if (suffix != null && !suffix.isEmpty()) {
                dtoSuffixTableModel.addRow(new Object[] { suffix });
            }
        }
    }
}
