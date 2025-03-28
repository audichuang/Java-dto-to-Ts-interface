package org.freeone.javabean.tsinterface.setting;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

/**
 * 設置界面組件
 */
public class JavaBeanToTypescriptInterfaceSettingsComponent {
    private final JPanel mainPanel;
    private final JBCheckBox ignoreParentField = new JBCheckBox("忽略父類字段");
    private final JBCheckBox enableDataToString = new JBCheckBox("日期轉字符串");
    private final JBCheckBox useAnnotationJsonProperty = new JBCheckBox("使用 @JsonProperty 註解");
    private final JBCheckBox allowFindClassInAllScope = new JBCheckBox("允許在所有範圍內查找類");
    private final JBCheckBox addOptionalMarkToAllFields = new JBCheckBox("給所有字段添加可選標記 (?: )");
    private final JBCheckBox ignoreSerialVersionUID = new JBCheckBox("忽略序列化ID (serialVersionUID)");

    // 自定義 DTO 後綴列表
    private final DefaultTableModel dtoSuffixTableModel;
    private final JTable dtoSuffixTable;
    private final JBTextField newSuffixField = new JBTextField();
    private final JButton addSuffixButton = new JButton("添加");
    private final JButton removeSuffixButton = new JButton("移除");

    public JavaBeanToTypescriptInterfaceSettingsComponent() {
        // 初始化 DTO 後綴表格
        dtoSuffixTableModel = new DefaultTableModel(new String[] { "DTO 後綴" }, 0);
        dtoSuffixTable = new JTable(dtoSuffixTableModel);
        dtoSuffixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JBScrollPane tableScrollPane = new JBScrollPane(dtoSuffixTable);
        tableScrollPane.setPreferredSize(new Dimension(300, 200));

        // 後綴添加面板
        JPanel suffixAddPanel = new JPanel(new BorderLayout());
        suffixAddPanel.add(newSuffixField, BorderLayout.CENTER);
        suffixAddPanel.add(addSuffixButton, BorderLayout.EAST);

        // 後綴操作面板
        JPanel suffixActionPanel = new JPanel(new BorderLayout());
        suffixActionPanel.add(suffixAddPanel, BorderLayout.CENTER);
        suffixActionPanel.add(removeSuffixButton, BorderLayout.EAST);

        // 後綴設置面板
        JPanel suffixPanel = new JPanel(new BorderLayout());
        suffixPanel.add(new JBLabel("DTO 類後綴列表:"), BorderLayout.NORTH);
        suffixPanel.add(tableScrollPane, BorderLayout.CENTER);
        suffixPanel.add(suffixActionPanel, BorderLayout.SOUTH);
        suffixPanel.setBorder(JBUI.Borders.empty(10));

        // 添加按鈕事件
        addSuffixButton.addActionListener(e -> {
            String suffix = newSuffixField.getText().trim();
            if (!suffix.isEmpty() && !containsSuffix(suffix)) {
                dtoSuffixTableModel.addRow(new Object[] { suffix });
                newSuffixField.setText("");
            }
        });

        // 移除按鈕事件
        removeSuffixButton.addActionListener(e -> {
            int selectedRow = dtoSuffixTable.getSelectedRow();
            if (selectedRow != -1) {
                dtoSuffixTableModel.removeRow(selectedRow);
            }
        });

        // 構建主面板
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("基本設置"))
                .addComponent(ignoreParentField)
                .addComponent(enableDataToString)
                .addComponent(useAnnotationJsonProperty)
                .addComponent(allowFindClassInAllScope)
                .addComponent(addOptionalMarkToAllFields)
                .addComponent(ignoreSerialVersionUID)
                .addComponent(suffixPanel)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return mainPanel;
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

    // 獲取界面上的設置值
    public boolean getIgnoreParentField() {
        return ignoreParentField.isSelected();
    }

    public boolean getEnableDataToString() {
        return enableDataToString.isSelected();
    }

    public boolean getUseAnnotationJsonProperty() {
        return useAnnotationJsonProperty.isSelected();
    }

    public boolean getAllowFindClassInAllScope() {
        return allowFindClassInAllScope.isSelected();
    }

    public boolean getAddOptionalMarkToAllFields() {
        return addOptionalMarkToAllFields.isSelected();
    }

    public boolean getIgnoreSerialVersionUID() {
        return ignoreSerialVersionUID.isSelected();
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

    // 設置界面值
    public void setIgnoreParentField(boolean value) {
        ignoreParentField.setSelected(value);
    }

    public void setEnableDataToString(boolean value) {
        enableDataToString.setSelected(value);
    }

    public void setUseAnnotationJsonProperty(boolean value) {
        useAnnotationJsonProperty.setSelected(value);
    }

    public void setAllowFindClassInAllScope(boolean value) {
        allowFindClassInAllScope.setSelected(value);
    }

    public void setAddOptionalMarkToAllFields(boolean value) {
        addOptionalMarkToAllFields.setSelected(value);
    }

    public void setIgnoreSerialVersionUID(boolean value) {
        ignoreSerialVersionUID.setSelected(value);
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