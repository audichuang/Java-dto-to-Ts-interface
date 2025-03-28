package org.freeone.javabean.tsinterface.setting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 持久化
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data
 * and the file name where
 * these persistent application settings are stored.
 * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html#the-appsettingsstate-class
 */
@State(name = "JavaBeanToTypescriptInterfaceSetting", storages = @Storage("JavaBeanToTypescriptInterfaceSettingsPlugin.xml"))
public final class JavaBeanToTypescriptInterfaceSettingsState
        implements PersistentStateComponent<JavaBeanToTypescriptInterfaceSettingsState> {

    public String userName = "TheFreeOne";

    public boolean enableDataToString = false;

    public boolean useAnnotationJsonProperty = true;

    public boolean allowFindClassInAllScope = true;

    public boolean ignoreParentField = false;

    /**
     * 控制是否為所有屬性添加可選問號（?:）
     * true: 所有屬性都添加可選問號（?:）
     * false: 所有屬性都不加問號，使用普通冒號（:）
     */
    public boolean addOptionalMarkToAllFields = false;

    /**
     * 忽略序列化ID字段
     * true: 生成TypeScript接口時忽略serialVersionUID字段
     * false: 包含所有字段
     */
    public boolean ignoreSerialVersionUID = true;

    /**
     * 自定義DTO類後綴列表
     * 用戶可以添加自己的DTO類後綴
     */
    public List<String> customDtoSuffixes = new ArrayList<>(Arrays.asList(
            "DTO", "Dto",
            "Request", "Response",
            "Rq", "Rs",
            "Tranrq", "Tranrs",
            "Req", "Resp",
            "Detail", "Entity",
            "Qry", "Query",
            "Model", "Info",
            "Data", "Bean",
            "VO", "Vo"));

    public static JavaBeanToTypescriptInterfaceSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(JavaBeanToTypescriptInterfaceSettingsState.class);
    }

    @NotNull
    @Override
    public JavaBeanToTypescriptInterfaceSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull JavaBeanToTypescriptInterfaceSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public boolean getUseAnnotationJsonProperty() {
        return useAnnotationJsonProperty;
    }

    public void setUseAnnotationJsonProperty(boolean useAnnotationJsonProperty) {
        this.useAnnotationJsonProperty = useAnnotationJsonProperty;
    }

    public boolean getEnableDataToString() {
        return enableDataToString;
    }

    public void setEnableDataToString(boolean enableDataToString) {
        this.enableDataToString = enableDataToString;
    }

    public boolean isAllowFindClassInAllScope() {
        return allowFindClassInAllScope;
    }

    public void setAllowFindClassInAllScope(boolean allowFindClassInAllScope) {
        this.allowFindClassInAllScope = allowFindClassInAllScope;
    }

    public boolean isIgnoreParentField() {
        return ignoreParentField;
    }

    public void setIgnoreParentField(boolean ignoreParentField) {
        this.ignoreParentField = ignoreParentField;
    }

    public boolean isAddOptionalMarkToAllFields() {
        return addOptionalMarkToAllFields;
    }

    public void setAddOptionalMarkToAllFields(boolean addOptionalMarkToAllFields) {
        this.addOptionalMarkToAllFields = addOptionalMarkToAllFields;
    }

    public boolean isIgnoreSerialVersionUID() {
        return ignoreSerialVersionUID;
    }

    public void setIgnoreSerialVersionUID(boolean ignoreSerialVersionUID) {
        this.ignoreSerialVersionUID = ignoreSerialVersionUID;
    }

    public List<String> getCustomDtoSuffixes() {
        return customDtoSuffixes;
    }

    public void setCustomDtoSuffixes(List<String> customDtoSuffixes) {
        this.customDtoSuffixes = customDtoSuffixes;
    }

    /**
     * 添加DTO後綴到列表中
     * 
     * @param suffix 後綴
     */
    public void addDtoSuffix(String suffix) {
        if (suffix != null && !suffix.isEmpty() && !customDtoSuffixes.contains(suffix)) {
            customDtoSuffixes.add(suffix);
        }
    }

    /**
     * 從列表中移除DTO後綴
     * 
     * @param suffix 後綴
     */
    public void removeDtoSuffix(String suffix) {
        customDtoSuffixes.remove(suffix);
    }
}