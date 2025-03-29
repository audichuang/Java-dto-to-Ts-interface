package org.freeone.javabean.tsinterface.setting;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.ListCellRendererWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 設置界面組件
 */
public class JavaBeanToTypescriptInterfaceSettingsComponent {
    private final JPanel mainPanel;

    // 基本設定區塊
    private final JPanel basicSettingsPanel;
    private final JBCheckBox ignoreParentField = new JBCheckBox("忽略父類字段");
    private final JBCheckBox enableDataToString = new JBCheckBox("日期轉字符串 (java.util.Date)");
    private final JBCheckBox useAnnotationJsonProperty = new JBCheckBox("使用 @JsonProperty 註解");
    private final JBCheckBox allowFindClassInAllScope = new JBCheckBox("允許在所有範圍內查找類");
    private final JBCheckBox addOptionalMarkToAllFields = new JBCheckBox("給所有字段添加可選標記 (?: )");
    private final JBCheckBox ignoreSerialVersionUID = new JBCheckBox("忽略序列化ID (serialVersionUID)");

    // DTO 後綴設定區塊
    private final JPanel dtoSuffixesPanel;
    private final DefaultTableModel dtoSuffixTableModel = new DefaultTableModel(new String[] { "DTO 後綴" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return true; // 允許直接編輯表格單元格
        }
    };
    private final JBTable dtoSuffixTable = new JBTable(dtoSuffixTableModel);
    private final JBTextField newSuffixField = new JBTextField(15);
    private final JButton addSuffixButton = new JButton("新增");
    private final JButton removeSuffixButton = new JButton("刪除");

    private JCheckBox useTransactionCodePrefixCheckBox;
    private JTextField requestSuffixField;
    private JTextField responseSuffixField;
    private JPanel transactionCodePanel;

    public JavaBeanToTypescriptInterfaceSettingsComponent() {
        // 初始化表格屬性
        dtoSuffixTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        dtoSuffixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dtoSuffixTable.setShowGrid(true);
        dtoSuffixTable.setRowHeight(30);

        // 基本設定區塊
        basicSettingsPanel = createBasicSettingsPanel();

        // DTO 後綴設定區塊
        dtoSuffixesPanel = createDtoSuffixesPanel();

        // 添加使用電文代號作為前綴的選項
        useTransactionCodePrefixCheckBox = new JCheckBox("使用電文代號作為介面名稱前綴");
        useTransactionCodePrefixCheckBox.setToolTipText("從控制器方法註解中提取電文代號(如RET-B-QRYSTATEMENTS)作為生成介面的前綴");

        // 添加後綴輸入欄位
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

        transactionCodePanel = new JPanel(new BorderLayout());
        transactionCodePanel.add(useTransactionCodePrefixCheckBox, BorderLayout.NORTH);
        transactionCodePanel.add(suffixPanel, BorderLayout.CENTER);

        // 構建主面板
        mainPanel = new JPanel(new BorderLayout());
        JPanel contentPanel = new JPanel(new VerticalLayout(10));
        contentPanel.add(basicSettingsPanel);
        contentPanel.add(dtoSuffixesPanel);
        contentPanel.add(transactionCodePanel);
        contentPanel.setBorder(JBUI.Borders.empty(10));

        mainPanel.add(contentPanel, BorderLayout.NORTH);
    }

    private JPanel createBasicSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 5));
        panel.setBorder(IdeBorderFactory.createTitledBorder("基本設定", false));
        panel.setBorder(JBUI.Borders.empty(5, 10, 10, 10));

        // 添加複選框並設置提示說明
        panel.add(ignoreParentField);
        ignoreParentField.setToolTipText("生成時不包含父類字段，僅包含當前類的字段");

        panel.add(enableDataToString);
        enableDataToString.setToolTipText("把 Date 類型轉為 string 類型而不是 any");

        panel.add(useAnnotationJsonProperty);
        useAnnotationJsonProperty.setToolTipText("優先使用 @JsonProperty 註解中指定的屬性名");

        panel.add(allowFindClassInAllScope);
        allowFindClassInAllScope.setToolTipText("允許在項目的所有範圍內查找類定義");

        panel.add(addOptionalMarkToAllFields);
        addOptionalMarkToAllFields.setToolTipText("為 TypeScript 接口中的所有屬性添加可選標記 (?:)");

        panel.add(ignoreSerialVersionUID);
        ignoreSerialVersionUID.setToolTipText("在生成的 TypeScript 接口中忽略 serialVersionUID 字段");

        return panel;
    }

    private JPanel createDtoSuffixesPanel() {
        // 表格容器面板
        JPanel tablePanel = ToolbarDecorator.createDecorator(dtoSuffixTable)
                .setAddAction(button -> {
                    String suffix = newSuffixField.getText().trim();
                    if (!suffix.isEmpty() && !containsSuffix(suffix)) {
                        dtoSuffixTableModel.addRow(new Object[] { suffix });
                        newSuffixField.setText("");
                    }
                })
                .setRemoveAction(button -> {
                    int selectedRow = dtoSuffixTable.getSelectedRow();
                    if (selectedRow != -1) {
                        dtoSuffixTableModel.removeRow(selectedRow);
                    }
                })
                .disableUpDownActions()
                .createPanel();

        // 新增後綴區域
        JPanel addPanel = new JPanel(new BorderLayout(5, 0));
        addPanel.add(new JBLabel("新增後綴:"), BorderLayout.WEST);
        addPanel.add(newSuffixField, BorderLayout.CENTER);
        addPanel.setBorder(JBUI.Borders.empty(10, 0, 5, 0));

        // 面板容器
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(IdeBorderFactory.createTitledBorder("DTO 類後綴設定", false));

        // 說明文字
        JLabel descriptionLabel = new JBLabel("<html>設定用於識別 DTO 類的後綴名稱。插件會自動識別符合這些後綴的類作為 DTO 進行轉換。<br>" +
                "如需添加自定義後綴，請在下方輸入框中輸入並點擊 + 按鈕。</html>");
        descriptionLabel.setBorder(JBUI.Borders.empty(0, 10, 10, 10));

        panel.add(descriptionLabel, BorderLayout.NORTH);
        panel.add(tablePanel, BorderLayout.CENTER);
        panel.add(addPanel, BorderLayout.SOUTH);

        return panel;
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
        // 觸發顯示/隱藏
        for (ActionListener listener : useTransactionCodePrefixCheckBox.getActionListeners()) {
            listener.actionPerformed(new ActionEvent(useTransactionCodePrefixCheckBox,
                    ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    public void setRequestSuffix(String suffix) {
        requestSuffixField.setText(suffix);
    }

    public void setResponseSuffix(String suffix) {
        responseSuffixField.setText(suffix);
    }
}