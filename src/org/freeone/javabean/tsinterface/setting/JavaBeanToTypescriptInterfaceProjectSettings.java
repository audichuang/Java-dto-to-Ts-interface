package org.freeone.javabean.tsinterface.setting;

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
 * 項目級別設定類
 * <p>
 * 負責持久化項目級別的設定，包括：
 * - 自定義Request和Response DTO後綴列表
 * </p>
 */
@State(name = "JavaBeanToTypescriptInterfaceProjectSettings", storages = {
        @Storage("JavaBeanToTypescriptInterfaceProjectSettings.xml")
})
@Service(Service.Level.PROJECT)
public final class JavaBeanToTypescriptInterfaceProjectSettings
        implements PersistentStateComponent<JavaBeanToTypescriptInterfaceProjectSettings> {

    static {
        // 確保在類加載時就輸出一條消息，方便調試
        System.out.println("JavaBeanToTypescriptInterfaceProjectSettings類已加載");
    }

    /**
     * 請求類後綴列表
     * 用戶可以添加自己的Request DTO類後綴，插件會自動識別符合這些後綴的類作為請求數據對象
     */
    public List<String> requestDtoSuffixes = new ArrayList<>();

    /**
     * 響應類後綴列表
     * 用戶可以添加自己的Response DTO類後綴，插件會自動識別符合這些後綴的類作為響應數據對象
     */
    public List<String> responseDtoSuffixes = new ArrayList<>();

    /**
     * 是否使用全局設定
     * true: 使用全局設定
     * false: 使用項目自定義設定
     */
    public boolean useGlobalSettings = true;

    /**
     * 獲取項目級別的設定實例
     * 
     * @param project 當前項目
     * @return 項目級別的設定狀態
     */
    public static JavaBeanToTypescriptInterfaceProjectSettings getInstance(Project project) {
        JavaBeanToTypescriptInterfaceProjectSettings instance = project
                .getService(JavaBeanToTypescriptInterfaceProjectSettings.class);
        // 強制初始化後綴列表
        instance.initializeFromGlobalSettings();
        return instance;
    }

    @NotNull
    @Override
    public JavaBeanToTypescriptInterfaceProjectSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull JavaBeanToTypescriptInterfaceProjectSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 獲取項目或全局的請求DTO後綴
     * 
     * @return 請求DTO後綴列表
     */
    public List<String> getEffectiveRequestDtoSuffixes() {
        if (useGlobalSettings) {
            return JavaBeanToTypescriptInterfaceSettingsState.getInstance().getRequestDtoSuffixes();
        }
        return requestDtoSuffixes;
    }

    /**
     * 獲取項目或全局的響應DTO後綴
     * 
     * @return 響應DTO後綴列表
     */
    public List<String> getEffectiveResponseDtoSuffixes() {
        if (useGlobalSettings) {
            return JavaBeanToTypescriptInterfaceSettingsState.getInstance().getResponseDtoSuffixes();
        }
        return responseDtoSuffixes;
    }

    /**
     * 從全局設置初始化後綴列表
     * 無論requestDtoSuffixes和responseDtoSuffixes是否為空，都強制從全局設置中初始化
     */
    public void initializeFromGlobalSettings() {
        JavaBeanToTypescriptInterfaceSettingsState globalSettings = JavaBeanToTypescriptInterfaceSettingsState
                .getInstance();

        // 無論列表是否為空，都強制從全局設置初始化
        this.requestDtoSuffixes.clear();
        this.requestDtoSuffixes.addAll(globalSettings.getRequestDtoSuffixes());

        this.responseDtoSuffixes.clear();
        this.responseDtoSuffixes.addAll(globalSettings.getResponseDtoSuffixes());

        // 輸出調試信息
        System.out.println("已初始化項目設置 - 請求DTO後綴: " + this.requestDtoSuffixes);
        System.out.println("已初始化項目設置 - 響應DTO後綴: " + this.responseDtoSuffixes);
    }

    /**
     * 獲取項目或全局的允許在全局範圍內查找類的設定
     * 
     * @return 是否允許在全局範圍內查找類
     */
    public boolean isAllowFindClassInAllScope() {
        if (useGlobalSettings) {
            return JavaBeanToTypescriptInterfaceSettingsState.getInstance().isAllowFindClassInAllScope();
        }
        return false; // 項目級別默認不允許
    }

    /**
     * 插件啟動時初始化設置
     * 
     * @param project 當前項目
     */
    public static void initializeOnStartup(Project project) {
        try {
            JavaBeanToTypescriptInterfaceProjectSettings instance = getInstance(project);
            instance.initializeFromGlobalSettings();
            System.out.println("成功初始化項目 '" + project.getName() + "' 的設定");
        } catch (Exception e) {
            System.out.println("初始化項目設定時發生異常: " + e.getMessage());
        }
    }
}