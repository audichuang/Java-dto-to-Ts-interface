package org.freeone.javabean.tsinterface.util.compat;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;

/**
 * API 相容性工具，用於處理不同版本 IntelliJ 平台 API 的差異
 */
public class ApiCompatUtil {

    private static final Logger LOG = Logger.getInstance(ApiCompatUtil.class);

    /**
     * 檢查當前 IntelliJ 平台版本
     *
     * @return 平台的主要版本號
     */
    public static int getPlatformVersion() {
        try {
            String buildString = ApplicationInfo.getInstance().getBuild().asString();
            // 格式通常是如 "IC-231.8109.175"
            String[] parts = buildString.split("\\.");
            if (parts.length > 0) {
                String majorVersionPart = parts[0].replaceAll("[^0-9]", "");
                if (!majorVersionPart.isEmpty()) {
                    return Integer.parseInt(majorVersionPart);
                }
            }
            return 0; // 無法識別
        } catch (Exception e) {
            LOG.warn("Failed to get platform version", e);
            return 0;
        }
    }

    /**
     * 檢查是否是 2023 或更高版本
     */
    public static boolean isVersion2023OrLater() {
        int version = getPlatformVersion();
        return version >= 231;
    }

    /**
     * 檢查是否是 2024 或更高版本
     */
    public static boolean isVersion2024OrLater() {
        int version = getPlatformVersion();
        return version >= 241;
    }
}