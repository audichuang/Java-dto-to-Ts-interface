package org.freeone.javabean.tsinterface.util;

import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 電文代號提取工具
 * 用於從控制器方法中提取電文代號
 */
public class TransactionCodeExtractor {

    // 匹配電文代號的模式，例如 RET-B-QRYSTATEMENTS 中的 QRYSTATEMENTS
    private static final Pattern TRANSACTION_CODE_PATTERN = Pattern.compile("\\b[A-Z0-9]+-(?:[A-Z0-9-]+-)*([A-Z0-9]+)\\b");

    /**
     * 從控制器方法中提取電文代號
     * 
     * @param method 控制器方法
     * @return 提取的電文代號，如果沒有找到則返回null
     */
    public static String extractTransactionCode(PsiMethod method) {
        if (method == null) {
            return null;
        }

        // 首先嘗試從方法的JavaDoc註解中提取
        String codeFromDoc = extractFromJavaDoc(method);
        if (codeFromDoc != null) {
            return codeFromDoc;
        }

        // 嘗試從方法上的註解中提取
        return extractFromAnnotations(method);
    }

    /**
     * 從JavaDoc註解中提取電文代號
     */
    private static String extractFromJavaDoc(PsiMethod method) {
        PsiDocComment docComment = method.getDocComment();
        if (docComment == null) {
            return null;
        }

        // 從描述文本中提取
        String commentText = docComment.getText();
        if (StringUtils.isNotBlank(commentText)) {
            return extractFromText(commentText);
        }

        // 嘗試從特定的doc標籤中提取
        PsiDocTag[] tags = docComment.getTags();
        for (PsiDocTag tag : tags) {
            String tagText = tag.getText();
            String extracted = extractFromText(tagText);
            if (extracted != null) {
                return extracted;
            }
        }

        return null;
    }

    /**
     * 從方法的註解中提取電文代號
     */
    private static String extractFromAnnotations(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String annotationText = annotation.getText();
            String extracted = extractFromText(annotationText);
            if (extracted != null) {
                return extracted;
            }

            // 檢查註解的屬性值
            PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
            for (PsiNameValuePair attribute : attributes) {
                PsiElement value = attribute.getValue();
                if (value != null) {
                    extracted = extractFromText(value.getText());
                    if (extracted != null) {
                        return extracted;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 從文本中提取電文代號
     */
    private static String extractFromText(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        Matcher matcher = TRANSACTION_CODE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 根據電文代號和設置生成介面名稱
     * 
     * @param originalName    原始Java類名
     * @param transactionCode 提取的電文代號
     * @param isRequest       是否為請求類
     * @return 新的介面名稱
     */
    public static String generateInterfaceName(String originalName, String transactionCode, boolean isRequest) {
        if (transactionCode == null || transactionCode.isEmpty()) {
            return originalName;
        }

        // 獲取設置
        String suffix = isRequest
                ? CommonUtils.getSettings().getRequestSuffix()
                : CommonUtils.getSettings().getResponseSuffix();

        // 創建基本前綴+後綴名稱
        String baseName = transactionCode + suffix;

        // 檢查是否為嵌套類型（如 Address、Building 等）
        if (originalName.endsWith("Address")) {
            return baseName + "Address";
        } else if (originalName.endsWith("Building")) {
            return baseName + "Building";
        } else if (originalName.endsWith("Detail")) {
            return baseName + "Detail";
        } else if (originalName.endsWith("Info")) {
            return baseName + "Info";
        } else if (originalName.endsWith("Data")) {
            return baseName + "Data";
        } else if (originalName.endsWith("Item")) {
            return baseName + "Item";
        } else if (originalName.contains("Address")) {
            // 處理類似 QryStatementTranrqAddress 的情況
            return baseName + "Address";
        } else if (originalName.contains("Building")) {
            // 處理類似 QryStatementTranrqBuilding 的情況
            return baseName + "Building";
        }

        // 如果不是嵌套類型，則返回基本名稱
        return baseName;
    }
}