package org.freeone.javabean.tsinterface.setting;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 設置組件，提供 UI 介面
 */
public class JavaBeanToTypescriptInterfaceSettingsComponent {

    private final JPanel myMainPanel;
    private final JBTextField userNameText = new JBTextField();
    private final JBCheckBox enableDataToStringCheckBox = new JBCheckBox("啟用在 toString() 中使用 Data 註解生成的 toString");
    private final JBCheckBox useAnnotationJsonPropertyCheckBox = new JBCheckBox("使用 @JsonProperty 註解中的屬性名");
    private final JBCheckBox allowFindClassInAllScopeCheckBox = new JBCheckBox("允許在項目所有範圍內查找類（推薦）");
    private final JBCheckBox ignoreParentFieldCheckBox = new JBCheckBox("忽略父類屬性");
    private final JBCheckBox addOptionalMarkToAllFieldsCheckBox = new JBCheckBox("為所有屬性添加可選標記（?:）");

    public JavaBeanToTypescriptInterfaceSettingsComponent() {
        // 添加提示信息
        addOptionalMarkToAllFieldsCheckBox.setToolTipText("選中：所有屬性都添加可選問號（?:）；未選中：所有屬性都不加問號（:）");

        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("用戶名: "), userNameText, 1, false)
                .addComponent(enableDataToStringCheckBox, 1)
                .addComponent(useAnnotationJsonPropertyCheckBox, 1)
                .addComponent(allowFindClassInAllScopeCheckBox, 1)
                .addComponent(ignoreParentFieldCheckBox, 1)
                .addComponent(addOptionalMarkToAllFieldsCheckBox, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return userNameText;
    }

    @NotNull
    public String getUserNameText() {
        return userNameText.getText();
    }

    public void setUserNameText(@NotNull String newText) {
        userNameText.setText(newText);
    }

    public JBCheckBox getEnableDataToStringCheckBox() {
        return enableDataToStringCheckBox;
    }

    public JBCheckBox getUseAnnotationJsonPropertyCheckBox() {
        return useAnnotationJsonPropertyCheckBox;
    }

    public JBCheckBox getAllowFindClassInAllScopeCheckBox() {
        return allowFindClassInAllScopeCheckBox;
    }

    public JBCheckBox getIgnoreParentFieldCheckBox() {
        return ignoreParentFieldCheckBox;
    }

    public JBCheckBox getAddOptionalMarkToAllFieldsCheckBox() {
        return addOptionalMarkToAllFieldsCheckBox;
    }
}