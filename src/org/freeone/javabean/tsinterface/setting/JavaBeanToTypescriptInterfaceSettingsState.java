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
 * 負責持久化用戶設定
 * <p>
 * 存儲Java DTO到TypeScript接口的設定，包括：
 * - 是否忽略父類字段
 * - 是否將java.util.Date轉換為字符串
 * - 是否使用@JsonProperty註解
 * - 是否忽略serialVersionUID
 * - 自定義DTO後綴列表
 * </p>
 */
@State(name = "JavaBeanToTypescriptInterfaceSetting", storages = @Storage("JavaBeanToTypescriptInterfaceSettingsPlugin.xml"))
public final class JavaBeanToTypescriptInterfaceSettingsState
        implements PersistentStateComponent<JavaBeanToTypescriptInterfaceSettingsState> {

    public String userName = "TheFreeOne";

    /**
     * 控制是否將java.util.Date轉換為TypeScript的string類型
     * true: Date類型轉為string類型
     * false: Date類型轉為any類型
     */
    public boolean enableDataToString = false;

    /**
     * 控制是否使用@JsonProperty註解中指定的屬性名
     * true: 優先使用@JsonProperty註解中指定的屬性名
     * false: 使用Java字段原始名稱
     */
    public boolean useAnnotationJsonProperty = true;

    /**
     * 控制是否在項目的所有範圍內查找類
     * true: 在項目的所有範圍內查找類
     * false: 僅在當前模塊範圍內查找類
     */
    public boolean allowFindClassInAllScope = true;

    /**
     * 控制是否忽略父類字段
     * true: 生成時僅包含當前類的字段，不包含父類字段
     * false: 生成時包含當前類和所有父類的字段
     */
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
     * 用戶可以添加自己的DTO類後綴，插件會自動識別符合這些後綴的類作為DTO
     * 默認包含常見的DTO類後綴：
     * - DTO/Dto: 標準的數據傳輸對象
     * - Request/Response: 用於HTTP請求和響應
     * - Rq/Rs/Req/Resp: Request/Response的縮寫
     * - Tranrq/Tranrs: 交易請求和響應
     * - Detail/Entity/Qry/Query: 常見的業務對象
     * - Model/Info/Data/Bean: 通用數據容器
     * - VO/Vo: 顯示層對象
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

    /**
     * 重置為默認設置
     */
    public void resetToDefaults() {
        this.enableDataToString = false;
        this.useAnnotationJsonProperty = true;
        this.allowFindClassInAllScope = true;
        this.ignoreParentField = false;
        this.addOptionalMarkToAllFields = false;
        this.ignoreSerialVersionUID = true;
        this.customDtoSuffixes = new ArrayList<>(Arrays.asList(
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
    }
}