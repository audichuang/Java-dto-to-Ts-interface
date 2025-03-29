package org.freeone.javabean.tsinterface.setting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
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
 * - 自定義Request和Response DTO後綴列表
 * </p>
 */
@State(name = "JavaBeanToTypescriptInterfaceSettings", storages = {
        @Storage("JavaBeanToTypescriptInterfaceSettings.xml")
})
@Service(Service.Level.APP)
public final class JavaBeanToTypescriptInterfaceSettingsState
        implements PersistentStateComponent<JavaBeanToTypescriptInterfaceSettingsState> {

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
     * 請求類後綴列表
     * 用戶可以添加自己的Request DTO類後綴，插件會自動識別符合這些後綴的類作為請求數據對象
     * 默認包含常見的Request DTO類後綴：
     * - DTO/Dto: 標準的數據傳輸對象
     * - Request/Req/Rq: 請求對象
     * - Tranrq: 交易請求
     * - Model/Entity/Query: 常見的業務請求對象
     */
    public List<String> requestDtoSuffixes = new ArrayList<>(Arrays.asList(
            "DTO", "Dto",
            "Request", "Req", "Rq", "Tranrq",
            "Qry", "Query",
            "Model", "Entity",
            "Data", "Bean",
            "VO", "Vo"));

    /**
     * 響應類後綴列表
     * 用戶可以添加自己的Response DTO類後綴，插件會自動識別符合這些後綴的類作為響應數據對象
     * 默認包含常見的Response DTO類後綴：
     * - Response/Resp/Rs: 響應對象
     * - Tranrs: 交易響應
     * - Result: 結果對象
     * - Detail: 詳細信息對象
     */
    public List<String> responseDtoSuffixes = new ArrayList<>(Arrays.asList(
            "Response", "Resp", "Rs", "Tranrs",
            "Result", "Results",
            "Detail", "Info"));

    /**
     * 向後兼容：自定義DTO類後綴列表（包含所有類型）
     */
    public List<String> customDtoSuffixes = new ArrayList<>();

    // 獲取全局服務實例 (已棄用)
    public static JavaBeanToTypescriptInterfaceSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(JavaBeanToTypescriptInterfaceSettingsState.class);
    }

    /**
     * 獲取項目級別的設定實例
     * 
     * @param project 當前項目
     * @return 項目級別的設定狀態
     */
    public static JavaBeanToTypescriptInterfaceSettingsState getInstance(Project project) {
        return project.getService(JavaBeanToTypescriptInterfaceSettingsState.class);
    }

    @NotNull
    @Override
    public JavaBeanToTypescriptInterfaceSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull JavaBeanToTypescriptInterfaceSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);

        // 如果還有舊版本的自定義後綴配置，分類到新的后缀列表中
        if (!customDtoSuffixes.isEmpty()) {
            for (String suffix : customDtoSuffixes) {
                if (isRequestSuffix(suffix) && !requestDtoSuffixes.contains(suffix)) {
                    requestDtoSuffixes.add(suffix);
                } else if (isResponseSuffix(suffix) && !responseDtoSuffixes.contains(suffix)) {
                    responseDtoSuffixes.add(suffix);
                }
            }
            // 清空舊的配置
            customDtoSuffixes.clear();
        }
    }

    /**
     * 判斷後綴是否屬於請求類型
     */
    private boolean isRequestSuffix(String suffix) {
        return suffix.contains("Request") || suffix.contains("Req") ||
                suffix.equals("Rq") || suffix.contains("Tranrq") ||
                suffix.contains("Qry") || suffix.contains("Query") ||
                !isResponseSuffix(suffix); // 預設非響應類型的後綴為請求類型
    }

    /**
     * 判斷後綴是否屬於響應類型
     */
    private boolean isResponseSuffix(String suffix) {
        return suffix.contains("Response") || suffix.contains("Resp") ||
                suffix.equals("Rs") || suffix.contains("Tranrs") ||
                suffix.contains("Result") || suffix.contains("Results") ||
                suffix.contains("Detail") || suffix.equals("Info");
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

    public List<String> getRequestDtoSuffixes() {
        return requestDtoSuffixes;
    }

    public void setRequestDtoSuffixes(List<String> requestDtoSuffixes) {
        this.requestDtoSuffixes = requestDtoSuffixes;
    }

    public List<String> getResponseDtoSuffixes() {
        return responseDtoSuffixes;
    }

    public void setResponseDtoSuffixes(List<String> responseDtoSuffixes) {
        this.responseDtoSuffixes = responseDtoSuffixes;
    }

    /**
     * 向後兼容：獲取所有DTO後綴
     */
    public List<String> getCustomDtoSuffixes() {
        List<String> allSuffixes = new ArrayList<>();
        allSuffixes.addAll(requestDtoSuffixes);
        allSuffixes.addAll(responseDtoSuffixes);
        return allSuffixes;
    }

    /**
     * 向後兼容：設置所有DTO後綴並進行分類
     */
    public void setCustomDtoSuffixes(List<String> suffixes) {
        if (suffixes == null || suffixes.isEmpty()) {
            return;
        }

        // 清空現有列表
        requestDtoSuffixes.clear();
        responseDtoSuffixes.clear();

        // 分類添加
        for (String suffix : suffixes) {
            if (suffix == null || suffix.isEmpty()) {
                continue;
            }

            if (isRequestSuffix(suffix)) {
                requestDtoSuffixes.add(suffix);
            } else if (isResponseSuffix(suffix)) {
                responseDtoSuffixes.add(suffix);
            } else {
                // 默認添加到請求類型
                requestDtoSuffixes.add(suffix);
            }
        }
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

        // 重置請求類後綴
        this.requestDtoSuffixes = new ArrayList<>(Arrays.asList(
                "DTO", "Dto",
                "Request", "Req", "Rq", "Tranrq",
                "Qry", "Query",
                "Model", "Entity",
                "Data", "Bean",
                "VO", "Vo"));

        // 重置響應類後綴
        this.responseDtoSuffixes = new ArrayList<>(Arrays.asList(
                "Response", "Resp", "Rs", "Tranrs",
                "Result", "Results",
                "Detail", "Info"));

        // 清空舊的配置
        this.customDtoSuffixes.clear();
    }
}