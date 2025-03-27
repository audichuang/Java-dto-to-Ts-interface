package org.freeone.javabean.tsinterface.setting;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 右上角设置的面板类
 * <p>
 * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html#implement-the-configurable-interface
 */
public class JavaBeanToTypescriptInterfaceConfigurable implements Configurable {

    private JavaBeanToTypescriptInterfaceSettingsComponent mySettingsComponent;

    @NlsContexts.ConfigurableName
    @Override
    public String getDisplayName() {
        return "DTO To TS";
    }

    @Override
    public @Nullable JComponent createComponent() {
        mySettingsComponent = new JavaBeanToTypescriptInterfaceSettingsComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        boolean modified = mySettingsComponent.getUserNameText().equals(settings.userName);
        modified |= mySettingsComponent.getEnableDataToStringCheckBox().isSelected() != settings.enableDataToString;
        modified |= mySettingsComponent.getAllowFindClassInAllScopeCheckBox()
                .isSelected() != settings.allowFindClassInAllScope;
        modified |= mySettingsComponent.getIgnoreParentFieldCheckBox().isSelected() != settings.ignoreParentField;
        modified |= mySettingsComponent.getUseAnnotationJsonPropertyCheckBox()
                .isSelected() != settings.useAnnotationJsonProperty;
        modified |= mySettingsComponent.getAddOptionalMarkToAllFieldsCheckBox()
                .isSelected() != settings.addOptionalMarkToAllFields;
        return modified;
    }

    @Override
    public void apply() throws ConfigurationException {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        settings.userName = mySettingsComponent.getUserNameText();
        settings.enableDataToString = mySettingsComponent.getEnableDataToStringCheckBox().isSelected();
        settings.allowFindClassInAllScope = mySettingsComponent.getAllowFindClassInAllScopeCheckBox().isSelected();
        settings.ignoreParentField = mySettingsComponent.getIgnoreParentFieldCheckBox().isSelected();
        settings.useAnnotationJsonProperty = mySettingsComponent.getUseAnnotationJsonPropertyCheckBox().isSelected();
        settings.addOptionalMarkToAllFields = mySettingsComponent.getAddOptionalMarkToAllFieldsCheckBox().isSelected();
    }

    @Override
    public void reset() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        mySettingsComponent.setUserNameText(settings.userName);
        mySettingsComponent.getEnableDataToStringCheckBox().setSelected(settings.enableDataToString);
        mySettingsComponent.getAllowFindClassInAllScopeCheckBox().setSelected(settings.allowFindClassInAllScope);
        mySettingsComponent.getIgnoreParentFieldCheckBox().setSelected(settings.ignoreParentField);
        mySettingsComponent.getUseAnnotationJsonPropertyCheckBox().setSelected(settings.useAnnotationJsonProperty);
        mySettingsComponent.getAddOptionalMarkToAllFieldsCheckBox().setSelected(settings.addOptionalMarkToAllFields);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}