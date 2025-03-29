package org.freeone.javabean.tsinterface.setting;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 主要配置文件
 * Provides controller functionality for application settings.
 */
final class JavaBeanToTypescriptInterfaceSettingsConfigurable implements Configurable {

    private JavaBeanToTypescriptInterfaceComponent mySettingsComponent;

    // A default constructor with no arguments is required because this
    // implementation
    // is registered in an applicationConfigurable EP

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Java DTO to TypeScript Interface";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new JavaBeanToTypescriptInterfaceComponent();
        return mySettingsComponent.getJPanel();
    }

    @Override
    public boolean isModified() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        boolean modified = settings.enableDataToString != mySettingsComponent.getDateToStringCheckBox().isSelected();
        modified |= settings.useAnnotationJsonProperty != mySettingsComponent.getUseJsonPropertyCheckBox().isSelected();
        modified |= settings.allowFindClassInAllScope != mySettingsComponent.getAllowFindClassInAllScope().isSelected();
        modified |= settings.ignoreParentField != mySettingsComponent.getIgnoreParentField().isSelected();

        // 檢查新增的設定項
        modified |= settings.addOptionalMarkToAllFields != mySettingsComponent.getAddOptionalMarkToAllFields()
                .isSelected();
        modified |= settings.ignoreSerialVersionUID != mySettingsComponent.getIgnoreSerialVersionUID().isSelected();

        // 檢查Request DTO後綴列表是否有修改
        if (mySettingsComponent.getRequestDtoSuffixes().size() != settings.requestDtoSuffixes.size()) {
            return true;
        }

        for (String suffix : mySettingsComponent.getRequestDtoSuffixes()) {
            if (!settings.requestDtoSuffixes.contains(suffix)) {
                return true;
            }
        }

        // 檢查Response DTO後綴列表是否有修改
        if (mySettingsComponent.getResponseDtoSuffixes().size() != settings.responseDtoSuffixes.size()) {
            return true;
        }

        for (String suffix : mySettingsComponent.getResponseDtoSuffixes()) {
            if (!settings.responseDtoSuffixes.contains(suffix)) {
                return true;
            }
        }

        return modified;
    }

    @Override
    public void apply() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        settings.setEnableDataToString(mySettingsComponent.getDateToStringCheckBox().isSelected());
        settings.setUseAnnotationJsonProperty(mySettingsComponent.getUseJsonPropertyCheckBox().isSelected());
        settings.setAllowFindClassInAllScope(mySettingsComponent.getAllowFindClassInAllScope().isSelected());
        settings.setIgnoreParentField(mySettingsComponent.getIgnoreParentField().isSelected());

        // 保存新增的設定項
        settings.setAddOptionalMarkToAllFields(mySettingsComponent.getAddOptionalMarkToAllFields().isSelected());
        settings.setIgnoreSerialVersionUID(mySettingsComponent.getIgnoreSerialVersionUID().isSelected());

        // 保存Request DTO後綴列表
        settings.requestDtoSuffixes.clear();
        settings.requestDtoSuffixes.addAll(mySettingsComponent.getRequestDtoSuffixes());

        // 保存Response DTO後綴列表
        settings.responseDtoSuffixes.clear();
        settings.responseDtoSuffixes.addAll(mySettingsComponent.getResponseDtoSuffixes());
    }

    @Override
    public void reset() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        mySettingsComponent.getDateToStringCheckBox().setSelected(settings.enableDataToString);
        mySettingsComponent.getUseJsonPropertyCheckBox().setSelected(settings.useAnnotationJsonProperty);
        mySettingsComponent.getAllowFindClassInAllScope().setSelected(settings.allowFindClassInAllScope);
        mySettingsComponent.getIgnoreParentField().setSelected(settings.ignoreParentField);

        // 重置新增的設定項
        mySettingsComponent.getAddOptionalMarkToAllFields().setSelected(settings.addOptionalMarkToAllFields);
        mySettingsComponent.getIgnoreSerialVersionUID().setSelected(settings.ignoreSerialVersionUID);

        // 重置Request和Response DTO後綴列表
        mySettingsComponent.setRequestDtoSuffixes(settings.requestDtoSuffixes);
        mySettingsComponent.setResponseDtoSuffixes(settings.responseDtoSuffixes);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

}