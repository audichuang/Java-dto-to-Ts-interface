package org.freeone.javabean.tsinterface.setting;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 提供應用程序設置的用戶界面
 */
public class JavaBeanToTypescriptInterfaceConfigurable implements Configurable {
    private JavaBeanToTypescriptInterfaceSettingsComponent settingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "JavaBean To TypeScript Interface";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return settingsComponent.getPanel();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsComponent = new JavaBeanToTypescriptInterfaceSettingsComponent();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        boolean modified = settingsComponent.getIgnoreParentField() != settings.ignoreParentField;
        modified |= settingsComponent.getEnableDataToString() != settings.enableDataToString;
        modified |= settingsComponent.getUseAnnotationJsonProperty() != settings.useAnnotationJsonProperty;
        modified |= settingsComponent.getAllowFindClassInAllScope() != settings.allowFindClassInAllScope;
        modified |= settingsComponent.getAddOptionalMarkToAllFields() != settings.addOptionalMarkToAllFields;
        modified |= settingsComponent.getIgnoreSerialVersionUID() != settings.ignoreSerialVersionUID;

        // 檢查DTO後綴列表是否有修改
        if (settingsComponent.getCustomDtoSuffixes().size() != settings.customDtoSuffixes.size()) {
            return true;
        }

        for (String suffix : settingsComponent.getCustomDtoSuffixes()) {
            if (!settings.customDtoSuffixes.contains(suffix)) {
                return true;
            }
        }

        return modified;
    }

    @Override
    public void apply() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        settings.ignoreParentField = settingsComponent.getIgnoreParentField();
        settings.enableDataToString = settingsComponent.getEnableDataToString();
        settings.useAnnotationJsonProperty = settingsComponent.getUseAnnotationJsonProperty();
        settings.allowFindClassInAllScope = settingsComponent.getAllowFindClassInAllScope();
        settings.addOptionalMarkToAllFields = settingsComponent.getAddOptionalMarkToAllFields();
        settings.ignoreSerialVersionUID = settingsComponent.getIgnoreSerialVersionUID();
        settings.customDtoSuffixes.clear();
        settings.customDtoSuffixes.addAll(settingsComponent.getCustomDtoSuffixes());
    }

    @Override
    public void reset() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        settingsComponent.setIgnoreParentField(settings.ignoreParentField);
        settingsComponent.setEnableDataToString(settings.enableDataToString);
        settingsComponent.setUseAnnotationJsonProperty(settings.useAnnotationJsonProperty);
        settingsComponent.setAllowFindClassInAllScope(settings.allowFindClassInAllScope);
        settingsComponent.setAddOptionalMarkToAllFields(settings.addOptionalMarkToAllFields);
        settingsComponent.setIgnoreSerialVersionUID(settings.ignoreSerialVersionUID);
        settingsComponent.setCustomDtoSuffixes(settings.customDtoSuffixes);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}