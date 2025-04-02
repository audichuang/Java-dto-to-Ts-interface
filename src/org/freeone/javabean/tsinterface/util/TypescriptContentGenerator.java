package org.freeone.javabean.tsinterface.util;

import com.intellij.lang.jvm.JvmClassKind;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.apache.commons.lang3.StringUtils;
import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceSettingsState;

import java.util.*;

/**
 * TypeScript 介面生成器
 * 用於將 Java 類轉換為 TypeScript 介面
 */
public class TypescriptContentGenerator {

    // ==================== 常量定義 ====================

    /**
     * 必填屬性分隔符
     */
    public static final String REQUIRE_SPLIT_TAG = ": ";

    /**
     * 可選屬性分隔符
     */
    public static final String NOT_REQUIRE_SPLIT_TAG = "?: ";

    // ==================== 狀態與緩存 ====================

    /**
     * 處理入口追蹤 - 避免循環引用
     */
    private final List<String> processEntryList = new ArrayList<>();

    /**
     * 已成功處理的類
     */
    private final List<String> processedClasses = new ArrayList<>();

    /**
     * 類的註釋映射
     */
    private final Map<String, String> classCommentMap = new HashMap<>();

    /**
     * 類對應的TypeScript介面內容
     */
    private final Map<String, String> classContentMap = new HashMap<>();

    /**
     * 電文代號緩存
     */
    private final Map<String, String> transactionCodeMap = new HashMap<>();

    /**
     * 類名到TypeScript介面名稱的映射
     */
    private final Map<String, String> tsInterfaceNameMap = new HashMap<>();

    /**
     * 類引用關係 - 記錄每個類引用了哪些其他類
     */
    private final Map<String, Set<String>> classReferences = new HashMap<>();

    /**
     * 反向引用關係 - 記錄每個類被哪些類引用
     */
    private final Map<String, Set<String>> referencedByMap = new HashMap<>();

    /**
     * 項目對象
     */
    private final Project project;

    /**
     * 構造器
     */
    public TypescriptContentGenerator(Project project) {
        this.project = project;
    }

    // ==================== 公共接口方法 ====================

    /**
     * 處理Java類並生成TypeScript介面
     *
     * @param selectedClass 選中的類
     * @param needDefault 是否需要默認值
     */
    public void processPsiClass(PsiClass selectedClass, boolean needDefault) {
        // 重置狀態
        clearState();

        // 生成類的TypeScript內容
        createTypescriptContentForClass(selectedClass);
    }

    /**
     * 生成最終合併的TypeScript內容
     *
     * @param selectedClass 選中的類
     * @param needDefault 是否需要默認值
     * @return 合併後的TypeScript內容
     */
    public String mergeContent(PsiClass selectedClass, boolean needDefault) {
        // 日誌輸出引用關係
        logReferenceRelationships();

        List<String> contentList = new ArrayList<>();
        String qualifiedName = selectedClass.getQualifiedName();

        // 對內容分類及排序的容器
        Map<String, String> requestClasses = new LinkedHashMap<>();
        Map<String, String> requestDependencyClasses = new LinkedHashMap<>();
        Map<String, String> responseClasses = new LinkedHashMap<>();
        Map<String, String> otherClasses = new LinkedHashMap<>();

        // 用於追蹤主類前綴的映射
        Map<String, String> mainClassPrefixMap = new HashMap<>();

        // 跳過的類名集合
        Set<String> skippedClassNames = new HashSet<>();

        // 處理主類
        processPrimaryClass(selectedClass, requestClasses, responseClasses, mainClassPrefixMap, skippedClassNames);

        // 處理其他類
        processSecondaryClasses(qualifiedName, requestClasses, requestDependencyClasses, responseClasses, otherClasses, skippedClassNames);

        // 處理嵌套類命名
        processNestedClassesNaming();

        // 構建所有類的映射
        Map<String, String> allClasses = new HashMap<>();
        allClasses.putAll(requestClasses);
        allClasses.putAll(requestDependencyClasses);
        allClasses.putAll(responseClasses);
        allClasses.putAll(otherClasses);

        // 應用重命名
        applyRenamingToAllClasses(allClasses);

        // 對請求依賴類和響應類進行拓撲排序
        sortByDependencies(requestDependencyClasses);
        sortByDependencies(responseClasses);

        // 按順序添加到結果列表
        contentList.addAll(requestClasses.values());
        contentList.addAll(requestDependencyClasses.values());
        contentList.addAll(responseClasses.values());
        contentList.addAll(otherClasses.values());

        // 處理最終內容
        List<String> processedContentList = performFinalCleanup(contentList);

        // 處理跳過的類引用
        if (!skippedClassNames.isEmpty()) {
            return cleanupSkippedClassReferences(processedContentList, skippedClassNames);
        }

        return String.join("\n", processedContentList);
    }

    /**
     * 清空所有緩存和狀態
     */
    public void clearState() {
        processedClasses.clear();
        classContentMap.clear();
        processEntryList.clear();
        tsInterfaceNameMap.clear();
        transactionCodeMap.clear();
        classCommentMap.clear();
        classReferences.clear();
        referencedByMap.clear();
    }

    // ==================== 核心處理邏輯 ====================

    /**
     * 為單個類生成TypeScript介面內容
     *
     * @param psiClass Java類對象
     * @return 生成的介面名稱
     */
    private String createTypescriptContentForClass(PsiClass psiClass) {
        if (psiClass == null) {
            return "any";
        }

        // 獲取類的基本信息
        String simpleClassName = psiClass.getName();
        String qualifiedName = psiClass.getQualifiedName();

        // 處理泛型或特殊情況
        if (qualifiedName == null) {
            return simpleClassName != null ? simpleClassName : "any";
        }

        // 檢查是否已處理過
        if (processedClasses.contains(qualifiedName)) {
            return getInterfaceName(qualifiedName);
        }

        // 避免循環依賴
        if (processEntryList.contains(qualifiedName)) {
            return getInterfaceName(qualifiedName);
        }
        processEntryList.add(qualifiedName);

        // 日誌
        System.out.println(simpleClassName + " qualifiedName " + qualifiedName);

        // 處理類名和泛型
        try {
            simpleClassName = CommonUtils.getClassNameWithGenerics(psiClass);
        } catch (Exception e) {
            // 忽略異常
        }

        // 根據規則處理接口名稱
        String tsInterfaceName = processInterfaceName(psiClass, simpleClassName);

        // 根據類型生成不同的TypeScript定義
        JvmClassKind classKind = psiClass.getClassKind();
        StringBuilder contentBuilder = new StringBuilder();

        if (classKind.equals(JvmClassKind.CLASS)) {
            processClassType(psiClass, tsInterfaceName, contentBuilder);
        } else if (classKind.equals(JvmClassKind.ENUM)) {
            processEnumType(psiClass, tsInterfaceName, contentBuilder);
        } else {
            return "unknown";
        }

        // 記錄處理結果
        String content = contentBuilder.toString();
        processedClasses.add(qualifiedName);

        // 處理類的註釋
        processClassComment(psiClass, qualifiedName);

        // 保存內容映射
        classContentMap.put(qualifiedName, content);

        // 保存接口名稱映射
        if (!tsInterfaceName.equals(simpleClassName)) {
            tsInterfaceNameMap.put(qualifiedName, tsInterfaceName);
        }

        return tsInterfaceName;
    }

    /**
     * 處理類型為 CLASS 的 Java 類
     */
    private void processClassType(PsiClass psiClass, String tsInterfaceName, StringBuilder contentBuilder) {
        contentBuilder.append("interface ").append(tsInterfaceName).append(" {\n");

        // 獲取字段
        PsiField[] fields = getClassFields(psiClass);
        PsiMethod[] allMethods = psiClass.getAllMethods();

        for (int i = 0; i < fields.length; i++) {
            PsiField field = fields[i];

            // 檢查是否需要跳過 serialVersionUID
            if (shouldSkipField(field)) {
                continue;
            }

            // 處理字段註釋
            String documentText = extractFieldComment(field);

            // 處理字段名稱
            String fieldName = processFieldName(field, allMethods);

            // 確定字段分隔符
            String fieldSplitTag = determineFieldSplitTag(field);

            // 獲取字段類型
            String typeString = getTypeString(field.getType(), psiClass);

            // 添加註釋
            if (!documentText.trim().isEmpty()) {
                String commentContent = extractCommentContent(documentText);
                if (!commentContent.isEmpty()) {
                    contentBuilder.append("  /**\n   * ").append(commentContent).append("\n   */\n");
                }
            }

            // 添加字段定義
            contentBuilder.append("  ").append(fieldName).append(fieldSplitTag).append(typeString)
                    .append(";\n");

            // 添加額外的換行（除了最後一個字段）
            if (i != fields.length - 1) {
                contentBuilder.append("\n");
            }
        }

        contentBuilder.append("}\n");
    }

    /**
     * 處理類型為 ENUM 的 Java 類
     */
    private void processEnumType(PsiClass psiClass, String tsInterfaceName, StringBuilder contentBuilder) {
        contentBuilder.append("type ").append(tsInterfaceName).append(" = ");

        List<String> enumConstantValueList = new ArrayList<>();
        for (PsiField psiField : psiClass.getFields()) {
            if (psiField instanceof PsiEnumConstant) {
                String name = psiField.getName();
                String value = "'" + name + "'";
                enumConstantValueList.add(value);
            }
        }

        String join = String.join(" | ", enumConstantValueList);
        contentBuilder.append(join).append(";\n");
    }

    /**
     * 處理主類，確保它優先被分類
     */
    private void processPrimaryClass(PsiClass selectedClass,
                                     Map<String, String> requestClasses,
                                     Map<String, String> responseClasses,
                                     Map<String, String> mainClassPrefixMap,
                                     Set<String> skippedClassNames) {
        if (selectedClass == null) {
            return;
        }

        String qualifiedName = selectedClass.getQualifiedName();
        if (qualifiedName == null) {
            return;
        }

        String content = classContentMap.get(qualifiedName);
        if (StringUtils.isBlank(content)) {
            return;
        }

        String simpleClassName = selectedClass.getName();
        if (simpleClassName == null) {
            return;
        }

        // 檢查是否需要跳過主類
        if (shouldSkipClass(selectedClass, simpleClassName, content)) {
            System.out.println("主類被跳過: " + simpleClassName);
            skippedClassNames.add(simpleClassName);
            return;
        }

        // 格式化內容
        String formattedContent = formatClassContent(qualifiedName, content);

        // 提取接口名稱和前綴信息
        extractMainClassPrefix(formattedContent, simpleClassName, mainClassPrefixMap);

        // 分析類的使用情境並分類
        ClassUsageInfo usageInfo = analyzeClassUsage(selectedClass);

        if (usageInfo.isRequest) {
            System.out.println("主類 " + simpleClassName + " 被分類為請求類");
            requestClasses.put(simpleClassName, formattedContent);
        } else {
            System.out.println("主類 " + simpleClassName + " 被分類為響應類");
            responseClasses.put(simpleClassName, formattedContent);
        }
    }

    /**
     * 處理次要類（非主類）
     */
    private void processSecondaryClasses(String mainClassQualifiedName,
                                         Map<String, String> requestClasses,
                                         Map<String, String> requestDependencyClasses,
                                         Map<String, String> responseClasses,
                                         Map<String, String> otherClasses,
                                         Set<String> skippedClassNames) {
        // 遍歷所有已處理的類
        for (String classNameWithPackage : processedClasses) {
            // 跳過主類
            if (classNameWithPackage.equals(mainClassQualifiedName)) {
                continue;
            }

            String content = classContentMap.get(classNameWithPackage);
            if (StringUtils.isBlank(content)) {
                continue;
            }

            // 獲取簡單類名
            String simpleClassName = getSimpleClassName(classNameWithPackage);

            // 檢查是否應該跳過
            if (shouldSkipByTypeName(classNameWithPackage, simpleClassName)) {
                System.out.println("跳過類: " + simpleClassName);
                skippedClassNames.add(simpleClassName);
                continue;
            }

            // 檢查是否為容器類
            if (shouldSkipContainerClass(simpleClassName, content)) {
                System.out.println("跳過容器類: " + simpleClassName);
                skippedClassNames.add(simpleClassName);
                continue;
            }

            // 格式化內容
            String formattedContent = formatClassContent(classNameWithPackage, content);

            // 嘗試根據使用情境分類
            classifyByUsage(classNameWithPackage, simpleClassName, formattedContent,
                    requestClasses, requestDependencyClasses, responseClasses, otherClasses);
        }
    }

    /**
     * 根據使用情境將類分類到不同組別
     */
    private void classifyByUsage(String classNameWithPackage, String simpleClassName, String formattedContent,
                                 Map<String, String> requestClasses,
                                 Map<String, String> requestDependencyClasses,
                                 Map<String, String> responseClasses,
                                 Map<String, String> otherClasses) {
        boolean classified = false;

        try {
            PsiClass psiClass = CommonUtils.findPsiClass(project,
                    CommonUtils.findPsiType(project, classNameWithPackage));

            if (psiClass != null) {
                // 分析類的使用情境
                ClassUsageInfo usageInfo = analyzeClassUsage(psiClass);

                // 基於使用情境分類
                if (usageInfo.isRequest) {
                    System.out.println("基於使用情境分析，" + simpleClassName + " 被分類為請求類");
                    requestDependencyClasses.put(simpleClassName, formattedContent);
                    classified = true;
                } else {
                    System.out.println("基於使用情境分析，" + simpleClassName + " 被分類為響應類");
                    responseClasses.put(simpleClassName, formattedContent);
                    classified = true;
                }
            }
        } catch (Exception e) {
            System.out.println("分析 " + simpleClassName + " 的使用情境時出錯: " + e.getMessage());
        }

        // 如果基於使用情境的分類失敗，使用基於名稱的分類
        if (!classified) {
            classifyByName(simpleClassName, formattedContent,
                    requestClasses, requestDependencyClasses, responseClasses, otherClasses);
        }
    }

    /**
     * 根據名稱模式對類進行分類
     */
    private void classifyByName(String simpleClassName, String formattedContent,
                                Map<String, String> requestClasses,
                                Map<String, String> requestDependencyClasses,
                                Map<String, String> responseClasses,
                                Map<String, String> otherClasses) {
        if (isRequestClassName(simpleClassName)) {
            System.out.println("基於名稱模式，" + simpleClassName + " 被分類為請求類");
            requestDependencyClasses.put(simpleClassName, formattedContent);
        } else if (isResponseClassName(simpleClassName)) {
            System.out.println("基於名稱模式，" + simpleClassName + " 被分類為響應類");
            responseClasses.put(simpleClassName, formattedContent);
        } else {
            // 檢查是否為請求類或響應類的依賴
            classifyByDependency(simpleClassName, formattedContent, requestClasses,
                    requestDependencyClasses, responseClasses, otherClasses);
        }
    }

    /**
     * 根據依賴關係對類進行分類
     */
    private void classifyByDependency(String simpleClassName, String formattedContent,
                                      Map<String, String> requestClasses,
                                      Map<String, String> requestDependencyClasses,
                                      Map<String, String> responseClasses,
                                      Map<String, String> otherClasses) {
        boolean isDependency = false;

        // 檢查是否為請求類的依賴
        for (String reqClassName : requestClasses.keySet()) {
            String reqClassContent = requestClasses.get(reqClassName);
            if (isClassDependency(reqClassName, simpleClassName, reqClassContent, formattedContent)) {
                requestDependencyClasses.put(simpleClassName, formattedContent);
                isDependency = true;
                break;
            }
        }

        if (!isDependency) {
            // 檢查是否為響應類的依賴
            for (String resClassName : responseClasses.keySet()) {
                String resClassContent = responseClasses.get(resClassName);
                if (isClassDependency(resClassName, simpleClassName, resClassContent, formattedContent)) {
                    responseClasses.put(simpleClassName, formattedContent);
                    isDependency = true;
                    break;
                }
            }

            // 如果不是任何類的依賴，則放到其他類中
            if (!isDependency) {
                otherClasses.put(simpleClassName, formattedContent);
            }
        }
    }

    /**
     * 從TypeScript介面內容中提取接口名稱
     */
    private String extractInterfaceName(String content) {
        if (content == null || !content.contains("interface ")) {
            return null;
        }

        int startIndex = content.indexOf("interface ") + "interface ".length();
        int endIndex = content.indexOf(" {", startIndex);
        if (endIndex > startIndex) {
            return content.substring(startIndex, endIndex);
        }
        return null;
    }

    /**
     * 獲取類的TypeScript類型字串
     */
    private String getTypeString(PsiType fieldType, PsiClass containingClass) {
        if (fieldType == null) {
            return "any";
        }

        // 處理基本類型和常見類型
        if (CommonUtils.isNumberType(fieldType)) {
            return "number";
        } else if (CommonUtils.isStringType(fieldType)) {
            return "string";
        } else if (CommonUtils.isBooleanType(fieldType)) {
            return "boolean";
        } else if (CommonUtils.isJavaUtilDateType(fieldType)
                && JavaBeanToTypescriptInterfaceSettingsState.getInstance().enableDataToString) {
            return "string";
        } else if (CommonUtils.isMapType(fieldType)) {
            return processMapType(fieldType);
        } else if (CommonUtils.isArrayType(fieldType)) {
            return processArrayType(fieldType);
        } else {
            // 處理其他引用類型
            return processReferenceType(fieldType, containingClass);
        }
    }

    /**
     * 處理映射類型（Map）
     */
    private String processMapType(PsiType type) {
        String defaultVType = "any";

        if (type instanceof PsiClassReferenceType) {
            PsiClassReferenceType refType = (PsiClassReferenceType) type;
            PsiType[] parameters = refType.getParameters();

            if (parameters.length == 2) {
                PsiType valueType = parameters[1];

                if (CommonUtils.isNumberType(valueType)) {
                    defaultVType = "number";
                } else if (CommonUtils.isStringType(valueType)) {
                    defaultVType = "string";
                } else if (CommonUtils.isArrayType(valueType)) {
                    defaultVType = getTypeString(valueType, refType.resolve());
                } else {
                    PsiClass psiClass = CommonUtils.findPsiClass(project, valueType);
                    if (psiClass != null) {
                        defaultVType = createTypescriptContentForClass(psiClass);
                    }
                }
            }
        }

        return "{[x:string]: " + defaultVType + "}";
    }

    /**
     * 處理數組和集合類型
     */
    private String processArrayType(PsiType type) {
        if (type instanceof PsiArrayType) {
            // 原生數組
            PsiType componentType = ((PsiArrayType) type).getComponentType();
            String componentTypeString = getTypeString(componentType, null);
            return componentTypeString + "[]";
        } else if (type instanceof PsiClassReferenceType) {
            // 集合類型
            PsiClassReferenceType refType = (PsiClassReferenceType) type;
            PsiType[] parameters = refType.getParameters();

            if (parameters.length > 0) {
                String itemTypeString = getTypeString(parameters[0], null);
                return itemTypeString + "[]";
            }
        }

        return "any[]";
    }

    /**
     * 處理引用類型
     */
    private String processReferenceType(PsiType fieldType, PsiClass containingClass) {
        // 檢查是否為標準庫類型
        String canonicalText = fieldType.getCanonicalText();
        if (isStandardLibraryType(canonicalText)) {
            return "any";
        }

        if (fieldType instanceof PsiClassReferenceType) {
            PsiClassReferenceType refType = (PsiClassReferenceType) fieldType;
            PsiClass resolvedClass = refType.resolve();

            // 處理集合類型
            if (resolvedClass != null && CommonUtils.isCollectionClass(resolvedClass)) {
                return processCollectionType(refType, containingClass);
            }

            // 處理其他引用類型
            PsiType[] parameters = refType.getParameters();
            if (parameters.length != 0 && resolvedClass != null) {
                // 處理泛型類
                return processGenericType(parameters, resolvedClass, containingClass);
            } else {
                // 處理普通類
                return processNormalClassType(refType, containingClass);
            }
        } else {
            // 其他 PsiType 情況
            return processOtherPsiType(fieldType, containingClass);
        }
    }

    /**
     * 處理集合類型
     */
    private String processCollectionType(PsiClassReferenceType refType, PsiClass containingClass) {
        PsiType[] parameters = refType.getParameters();
        if (parameters.length == 1) {
            // 獲取泛型參數
            PsiType genericType = parameters[0];
            String genericTypeString = getTypeString(genericType, containingClass);

            // 收集引用關係
            PsiClass genericPsiClass = CommonUtils.findPsiClass(project, genericType);
            if (containingClass != null && genericPsiClass != null && genericPsiClass.getQualifiedName() != null) {
                collectClassReference(containingClass.getQualifiedName(), genericPsiClass.getQualifiedName());
            }

            // 確保處理泛型參數類型
            createTypescriptContentForClass(genericPsiClass);

            return genericTypeString + "[]";
        }

        return "any[]";
    }

    /**
     * 處理泛型類型
     */
    private String processGenericType(PsiType[] parameters, PsiClass resolvedClass, PsiClass containingClass) {
        // 處理所有泛型參數
        for (PsiType parameter : parameters) {
            PsiClass parameterClass = CommonUtils.findPsiClass(project, parameter);
            createTypescriptContentForClass(parameterClass);
        }

        // 處理基類
        createTypescriptContentForClass(resolvedClass);

        // 收集引用關係
        if (containingClass != null && resolvedClass.getQualifiedName() != null) {
            collectClassReference(containingClass.getQualifiedName(), resolvedClass.getQualifiedName());
        }

        return resolvedClass.getName();
    }

    /**
     * 處理普通類類型
     */
    private String processNormalClassType(PsiClassReferenceType refType, PsiClass containingClass) {
        PsiClass resolveClass = refType.resolve();
        if (resolveClass != null && containingClass != null && resolveClass.getQualifiedName() != null) {
            // 收集類引用關係
            collectClassReference(containingClass.getQualifiedName(), resolveClass.getQualifiedName());
        }

        return createTypescriptContentForClass(resolveClass);
    }

    /**
     * 處理其他PsiType類型
     */
    private String processOtherPsiType(PsiType fieldType, PsiClass containingClass) {
        if (CommonUtils.isArrayType(fieldType)) {
            PsiType componentType = ((PsiArrayType) fieldType).getComponentType();
            String componentTypeString = getTypeString(componentType, containingClass);

            // 處理組件類型
            PsiClass componentClass = CommonUtils.findPsiClass(project, componentType);
            createTypescriptContentForClass(componentClass);

            // 收集引用關係
            if (containingClass != null && componentClass != null && componentClass.getQualifiedName() != null) {
                collectClassReference(containingClass.getQualifiedName(), componentClass.getQualifiedName());
            }

            return componentTypeString + "[]";
        } else {
            PsiClass fieldClass = CommonUtils.findPsiClass(project, fieldType);
            if (fieldClass != null && containingClass != null && fieldClass.getQualifiedName() != null) {
                // 收集類引用關係
                collectClassReference(containingClass.getQualifiedName(), fieldClass.getQualifiedName());
            }

            return createTypescriptContentForClass(fieldClass);
        }
    }

    /**
     * 處理介面名稱，根據設置使用電文代號
     */
    private String processInterfaceName(PsiClass psiClass, String originalName) {
        // 如果未啟用電文代號命名，直接返回原名稱
        if (!CommonUtils.getSettings().isUseTransactionCodePrefix()) {
            return originalName;
        }

        String qualifiedName = psiClass.getQualifiedName();

        // 檢查是否為泛型的容器類
        if (CommonUtils.getSettings().isOnlyProcessGenericDto()) {
            if (isContainerClass(psiClass)) {
                return originalName;
            }
        }

        // 檢查是否為嵌套類
        if (qualifiedName != null && qualifiedName.contains("$")) {
            return processNestedClassName(qualifiedName, originalName, psiClass);
        }

        // 檢查是否已經緩存了電文代號
        if (transactionCodeMap.containsKey(qualifiedName)) {
            String transactionCode = transactionCodeMap.get(qualifiedName);
            ClassUsageInfo usageInfo = analyzeClassUsage(psiClass);
            return TransactionCodeExtractor.generateInterfaceName(originalName, transactionCode, usageInfo.isRequest);
        }

        // 從類的方法使用處查找控制器方法
        PsiReference[] references = findReferences(psiClass);
        ClassUsageInfo usageInfo = analyzeClassUsage(psiClass);

        for (PsiReference reference : references) {
            PsiElement element = reference.getElement();
            PsiMethod method = findEnclosingMethod(element);
            if (method != null) {
                // 提取電文代號
                String transactionCode = TransactionCodeExtractor.extractTransactionCode(method);
                if (transactionCode != null && !transactionCode.isEmpty()) {
                    // 緩存電文代號
                    transactionCodeMap.put(qualifiedName, transactionCode);
                    return TransactionCodeExtractor.generateInterfaceName(originalName, transactionCode, usageInfo.isRequest);
                }
            }
        }

        // 未找到電文代號，返回原名稱
        return originalName;
    }

    /**
     * 處理嵌套類名稱
     */
    private String processNestedClassName(String qualifiedName, String originalName, PsiClass psiClass) {
        // 獲取外部類全限定名
        String outerClassName = qualifiedName.substring(0, qualifiedName.indexOf("$"));
        PsiClass outerClass = CommonUtils.findPsiClass(project, CommonUtils.findPsiType(project, outerClassName));

        if (outerClass != null) {
            // 獲取外部類的接口名稱
            String outerInterfaceName = processInterfaceName(outerClass, outerClass.getName());

            // 提取電文代號前綴
            String transactionCodePrefix = extractTransactionCodePrefix(outerInterfaceName);

            if (!transactionCodePrefix.isEmpty()) {
                // 分析嵌套類的使用情境
                ClassUsageInfo usageInfo = analyzeClassUsage(psiClass);
                return processNestedClassNameWithPrefix(transactionCodePrefix, originalName, usageInfo.isRequest);
            }
        }

        return originalName;
    }

    /**
     * 使用前綴處理嵌套類名稱
     */
    private String processNestedClassNameWithPrefix(String prefix, String className, boolean isRequest) {
        String suffix = isRequest ? "Req" : "Resp";
        return prefix + suffix + className;
    }

    /**
     * 根據依賴關係對類進行排序 (拓撲排序)
     */
    private void sortByDependencies(Map<String, String> classes) {
        if (classes.size() <= 1) {
            return; // 只有一個類，不需要排序
        }

        System.out.println("執行拓撲排序，共 " + classes.size() + " 個類");

        // 分析類間依賴關係
        Map<String, Set<String>> dependencyGraph = buildDependencyGraph(classes);

        // 執行拓撲排序
        List<String> sortedClassNames = topologicalSort(dependencyGraph);
        System.out.println("拓撲排序結果: " + sortedClassNames);

        // 重組映射
        Map<String, String> sortedClasses = new LinkedHashMap<>();
        for (String className : sortedClassNames) {
            if (classes.containsKey(className)) {
                sortedClasses.put(className, classes.get(className));
            }
        }

        // 更新原映射
        classes.clear();
        classes.putAll(sortedClasses);
    }

    /**
     * 進行類之間引用關係的拓撲排序
     */
    private List<String> topologicalSort(Map<String, Set<String>> graph) {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> inProcess = new HashSet<>();

        // 對每個未訪問的節點進行DFS
        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                dfs(node, graph, visited, inProcess, result);
            }
        }

        // 反轉結果
        Collections.reverse(result);
        return result;
    }

    /**
     * DFS遞歸函數
     */
    private void dfs(String node, Map<String, Set<String>> graph,
                     Set<String> visited, Set<String> inProcess, List<String> result) {
        // 檢查循環依賴
        if (inProcess.contains(node)) {
            System.out.println("警告: 發現循環依賴於 " + node);
            return;
        }

        // 檢查已訪問
        if (visited.contains(node)) {
            return;
        }

        // 標記為處理中
        inProcess.add(node);

        // 訪問所有依賴
        for (String neighbor : graph.getOrDefault(node, Collections.emptySet())) {
            dfs(neighbor, graph, visited, inProcess, result);
        }

        // 標記為已訪問並從處理中移除
        inProcess.remove(node);
        visited.add(node);

        // 添加到結果
        result.add(node);
    }

    /**
     * 處理嵌套類命名 - 基於引用關係識別嵌套類
     */
    private void processNestedClassesNaming() {
        System.out.println("執行嵌套類命名處理，引用關係數量: " + classReferences.size());

        // 1. 找出所有主類 (包含Req或Resp後綴的類)
        Map<String, String> mainClasses = new HashMap<>(); // 類名 -> 電文代號前綴
        Map<String, String> mainClassSuffixes = new HashMap<>(); // 類名 -> 使用的後綴
        Map<String, Boolean> isRequestMap = new HashMap<>();

        // 找出所有已命名的類及其後綴
        for (Map.Entry<String, String> entry : tsInterfaceNameMap.entrySet()) {
            String className = entry.getKey();
            String tsInterfaceName = entry.getValue();

            // 尋找所有可能的後綴
            String[] possibleSuffixes = {"Req", "Request", "Resp", "Response", "Res", "Rs"};

            for (String suffix : possibleSuffixes) {
                if (tsInterfaceName.endsWith(suffix)) {
                    int suffixIndex = tsInterfaceName.lastIndexOf(suffix);
                    if (suffixIndex > 0) {
                        // 找到主類
                        String prefix = tsInterfaceName.substring(0, suffixIndex);
                        mainClasses.put(className, prefix);
                        mainClassSuffixes.put(className, suffix);
                        isRequestMap.put(className, suffix.startsWith("Req"));

                        System.out.println("找到主類: " + className + " -> " + tsInterfaceName +
                                ", 前綴: " + prefix + ", 後綴: " + suffix +
                                ", 是請求類: " + isRequestMap.get(className));
                        break;
                    }
                }
            }
        }

        System.out.println("找到主類數量: " + mainClasses.size() + ", 內容: " + mainClasses);

        // 2. 為每個被主類引用的類應用相同的命名規則
        Map<String, String> renameMap = new HashMap<>();

        for (String mainClass : mainClasses.keySet()) {
            String prefix = mainClasses.get(mainClass);
            boolean isRequest = isRequestMap.get(mainClass);
            String exactSuffix = mainClassSuffixes.get(mainClass);
            String mainSimpleClassName = getSimpleClassName(mainClass);

            System.out.println("處理主類 " + mainClass + " 使用後綴: " + exactSuffix);

            // 獲取引用的類
            Set<String> referencedClasses = classReferences.getOrDefault(mainClass, new HashSet<>());

            for (String referencedClass : referencedClasses) {
                // 跳過標準庫類
                if (referencedClass.startsWith("java.") || referencedClass.startsWith("javax.")) {
                    continue;
                }

                String simpleClassName = getSimpleClassName(referencedClass);

                // 提取唯一部分
                String uniquePart = extractUniqueClassPart(simpleClassName, mainSimpleClassName);

                // 使用相同後綴
                String newInterfaceName = prefix + exactSuffix + uniquePart;

                // 記錄重命名
                renameMap.put(referencedClass, newInterfaceName);
                System.out.println("將重命名嵌套類: " + referencedClass + " -> " + newInterfaceName);
            }

            // 遞歸處理更深層的嵌套類
            processNestedClassesRecursively(referencedClasses, prefix, exactSuffix, renameMap);
        }

        // 3. 更新接口名稱映射
        for (Map.Entry<String, String> entry : renameMap.entrySet()) {
            tsInterfaceNameMap.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 遞歸處理嵌套類引用關係
     */
    private void processNestedClassesRecursively(Set<String> parentClasses, String prefix,
                                                 String exactSuffix, Map<String, String> renameMap) {
        // 收集下一層的嵌套類
        Set<String> nextLevelClasses = new HashSet<>();

        for (String parentClass : parentClasses) {
            String parentSimpleClassName = getSimpleClassName(parentClass);

            // 獲取引用的類
            Set<String> referencedClasses = classReferences.getOrDefault(parentClass, new HashSet<>());

            for (String referencedClass : referencedClasses) {
                // 跳過標準庫類和已處理的類
                if (referencedClass.startsWith("java.") ||
                        referencedClass.startsWith("javax.") ||
                        renameMap.containsKey(referencedClass)) {
                    continue;
                }

                String simpleClassName = getSimpleClassName(referencedClass);

                // 提取唯一部分
                String uniquePart = extractUniqueClassPart(simpleClassName, parentSimpleClassName);

                // 生成新名稱
                String newInterfaceName = prefix + exactSuffix + uniquePart;

                // 記錄重命名
                renameMap.put(referencedClass, newInterfaceName);
                System.out.println("遞歸重命名嵌套類: " + referencedClass + " -> " + newInterfaceName);

                // 添加到下一層
                nextLevelClasses.add(referencedClass);
            }
        }

        // 繼續遞歸
        if (!nextLevelClasses.isEmpty()) {
            processNestedClassesRecursively(nextLevelClasses, prefix, exactSuffix, renameMap);
        }
    }

    /**
     * 應用重命名到所有類
     */
    private void applyRenamingToAllClasses(Map<String, String> allClasses) {
        System.out.println("開始應用重命名...");
        System.out.println("介面名稱映射: " + tsInterfaceNameMap);

        // 收集重命名映射
        Map<String, String> interfaceRenameMap = new HashMap<>();
        Map<String, String> simpleNameRenameMap = new HashMap<>();

        for (Map.Entry<String, String> entry : tsInterfaceNameMap.entrySet()) {
            String className = entry.getKey();
            String content = classContentMap.get(className);
            String simpleClassName = getSimpleClassName(className);
            String newInterfaceName = entry.getValue();

            // 確保所有簡單類名都有映射
            simpleNameRenameMap.put(simpleClassName, newInterfaceName);

            if (content != null) {
                String currentInterfaceName = extractInterfaceName(content);

                if (currentInterfaceName != null && !currentInterfaceName.equals(newInterfaceName)) {
                    // 記錄重命名
                    interfaceRenameMap.put(currentInterfaceName, newInterfaceName);

                    // 更新介面定義
                    content = content.replace("interface " + currentInterfaceName + " {",
                            "interface " + newInterfaceName + " {");

                    // 特別處理自身引用
                    content = content.replace(": " + currentInterfaceName + ";",
                            ": " + newInterfaceName + ";");
                    content = content.replace(": " + currentInterfaceName + "[]",
                            ": " + newInterfaceName + "[]");

                    classContentMap.put(className, content);
                }
            }
        }

        System.out.println("介面重命名映射: " + interfaceRenameMap);
        System.out.println("簡單名稱重命名映射: " + simpleNameRenameMap);

        // 多輪替換
        for (int round = 0; round < 3; round++) {
            System.out.println("執行第 " + (round + 1) + " 輪引用替換");
            boolean anyUpdated = false;

            // 更新所有類內容中的引用
            for (Map.Entry<String, String> entry : new HashMap<>(allClasses).entrySet()) {
                String className = entry.getKey();
                String content = entry.getValue();
                boolean updated = false;

                // 替換引用
                for (Map.Entry<String, String> renameEntry : simpleNameRenameMap.entrySet()) {
                    String oldName = renameEntry.getKey();
                    String newName = renameEntry.getValue();

                    // 檢查引用
                    if (hasTypeReference(content, oldName)) {
                        System.out.println("在 " + className + " 中發現對 " + oldName + " 的引用");

                        // 替換引用
                        String updatedContent = replaceAllReferences(content, oldName, newName, className);

                        if (!updatedContent.equals(content)) {
                            content = updatedContent;
                            updated = true;
                        }
                    }
                }

                if (updated) {
                    System.out.println("更新了 " + className + " 的內容");
                    allClasses.put(className, content);
                    classContentMap.put(className, content);
                    anyUpdated = true;
                }
            }

            if (!anyUpdated) {
                System.out.println("沒有更多需要替換的引用");
                break;
            }
        }

        // 最終檢查
        performFinalReferenceCheck(allClasses, simpleNameRenameMap);
    }

    /**
     * 檢查一個類是否有對另一個類型的引用
     */
    private boolean hasTypeReference(String content, String typeName) {
        return content.contains(": " + typeName + ";") ||
                content.contains("?: " + typeName + ";") ||
                content.contains(": " + typeName + "[") ||
                content.contains("?: " + typeName + "[") ||
                content.contains(" " + typeName + " ") ||
                content.contains(" " + typeName + ",") ||
                content.contains("," + typeName + " ") ||
                content.contains("<" + typeName + ">") ||
                content.contains("interface " + typeName + " {");
    }

    /**
     * 執行最終的引用檢查
     */
    private void performFinalReferenceCheck(Map<String, String> allClasses, Map<String, String> simpleNameRenameMap) {
        for (Map.Entry<String, String> entry : allClasses.entrySet()) {
            String content = entry.getValue();
            for (String oldName : simpleNameRenameMap.keySet()) {
                if (content.contains(": " + oldName + ";") ||
                        content.contains("?: " + oldName + ";") ||
                        content.contains(": " + oldName + "[")) {
                    System.out.println("警告: " + entry.getKey() + "中仍存在未替換的類型引用: " + oldName);
                }

                if (content.contains("interface " + oldName + " {")) {
                    System.out.println("警告: " + entry.getKey() + "中仍存在未替換的介面定義: " + oldName);
                }
            }
        }
    }

    /**
     * 進行最終的類型引用替換，確保引用一致性
     */
    private List<String> performFinalCleanup(List<String> contentList) {
        // 建立替換映射
        Map<String, String> allRenameMap = new HashMap<>();

        for (Map.Entry<String, String> entry : tsInterfaceNameMap.entrySet()) {
            String fullClassName = entry.getKey();
            String simpleClassName = getSimpleClassName(fullClassName);
            String newInterfaceName = entry.getValue();

            // 只收集需要重命名的類
            if (!simpleClassName.equals(newInterfaceName)) {
                allRenameMap.put(simpleClassName, newInterfaceName);
                System.out.println("最終清理 - 收集重命名: " + simpleClassName + " -> " + newInterfaceName);
            }
        }

        // 處理每個內容
        List<String> processedList = new ArrayList<>();

        for (String content : contentList) {
            // 創建副本處理
            String processedContent = content;

            // 替換引用
            for (Map.Entry<String, String> entry : allRenameMap.entrySet()) {
                String oldName = entry.getKey();
                String newName = entry.getValue();

                // 替換介面定義
                processedContent = processedContent.replaceAll(
                        "interface\\s+" + oldName + "\\s+\\{",
                        "interface " + newName + " {");

                // 替換標準類型引用
                processedContent = processedContent.replace(
                        ": " + oldName + ";",
                        ": " + newName + ";");

                // 替換數組引用
                processedContent = processedContent.replace(
                        ": " + oldName + "[]",
                        ": " + newName + "[]");

                // 替換可選屬性引用
                processedContent = processedContent.replace(
                        "?: " + oldName + ";",
                        "?: " + newName + ";");

                // 其他引用形式
                processedContent = processedContent.replace(
                        "<" + oldName + ">",
                        "<" + newName + ">");
            }

            processedList.add(processedContent);
        }

        // 檢查未替換的引用
        checkUnresolvedReferences(processedList, allRenameMap);

        return processedList;
    }

    /**
     * 清理對跳過類的引用
     */
    private String cleanupSkippedClassReferences(List<String> contentList, Set<String> skippedClassNames) {
        List<String> cleanedContentList = new ArrayList<>();

        for (String content : contentList) {
            boolean contentModified = false;
            String modifiedContent = content;

            // 替換對跳過類的引用
            for (String skippedClass : skippedClassNames) {
                if (modifiedContent.contains(": " + skippedClass + ";") ||
                        modifiedContent.contains(": " + skippedClass + "[]")) {

                    // 替換為 any
                    modifiedContent = modifiedContent.replace(": " + skippedClass + ";", ": any;");
                    modifiedContent = modifiedContent.replace(": " + skippedClass + "[];", ": any[];");
                    contentModified = true;
                }
            }

            cleanedContentList.add(contentModified ? modifiedContent : content);
        }

        return String.join("\n", cleanedContentList);
    }

    // ==================== 工具方法 ====================

    /**
     * 獲取類的字段
     */
    private PsiField[] getClassFields(PsiClass psiClass) {
        if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().ignoreParentField) {
            return psiClass.getFields();
        } else {
            return psiClass.getAllFields();
        }
    }

    /**
     * 提取字段註釋
     */
    private String extractFieldComment(PsiField field) {
        PsiDocComment docComment = field.getDocComment();
        return (docComment != null && docComment.getText() != null) ? docComment.getText() : "";
    }

    /**
     * 處理字段名稱
     */
    private String processFieldName(PsiField field, PsiMethod[] allMethods) {
        String fieldName = field.getName();

        // 檢查是否使用JsonProperty
        if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().useAnnotationJsonProperty) {
            String jsonPropertyValue = CommonUtils.getJsonPropertyValue(field, allMethods);
            if (jsonPropertyValue != null) {
                fieldName = jsonPropertyValue;
            }
        }

        return fieldName;
    }

    /**
     * 確定字段的分隔符（必填或可選）
     */
    private String determineFieldSplitTag(PsiField field) {
        String fieldSplitTag = REQUIRE_SPLIT_TAG; // 默認使用冒號（必填）

        // 只有在啟用添加可選標記的設置時，才會添加問號
        if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().addOptionalMarkToAllFields) {
            fieldSplitTag = NOT_REQUIRE_SPLIT_TAG;
            // 如果字段有必填注解，則使用冒號
            if (CommonUtils.isFieldRequire(field.getAnnotations())) {
                fieldSplitTag = REQUIRE_SPLIT_TAG;
            }
        }

        return fieldSplitTag;
    }

    /**
     * 格式化類的內容
     */
    private String formatClassContent(String classNameWithPackage, String content) {
        StringBuilder stringBuilder = new StringBuilder();

        // 添加註釋
        String classComment = classCommentMap.get(classNameWithPackage);
        if (classComment != null && !classComment.trim().isEmpty()) {
            if (classComment.endsWith("\n")) {
                stringBuilder.append(classComment);
            } else {
                stringBuilder.append(classComment).append("\n");
            }
        }

        stringBuilder.append("export ");
        stringBuilder.append(content);

        return stringBuilder.toString();
    }

    /**
     * 處理類的註釋
     */
    private void processClassComment(PsiClass psiClass, String qualifiedName) {
        PsiDocComment classDocComment = psiClass.getDocComment();
        if (classDocComment != null && classDocComment.getText() != null) {
            // 格式化類註釋
            String classComment = extractCommentContent(classDocComment.getText());
            if (!classComment.isEmpty()) {
                String formattedComment = "/**\n * " + classComment + "\n */\n";
                classCommentMap.put(qualifiedName, formattedComment);
            }
        }
    }

    /**
     * 從註釋文本中提取清晰的註釋內容
     */
    private String extractCommentContent(String comment) {
        // 移除 JavaDoc 標記
        String content = comment.trim()
                .replaceAll("/\\*\\*", "") // 移除開頭
                .replaceAll("\\*/", "") // 移除結尾
                .replaceAll("^\\s*\\*\\s*", "") // 移除每行開頭的 *
                .replaceAll("\\n\\s*\\*\\s*", " ") // 合併多行
                .trim();

        // 如果有 @param、@return 等，只保留主要描述
        if (content.contains("@")) {
            content = content.split("@")[0].trim();
        }

        return content;
    }

    /**
     * 檢查類是否應該跳過
     */
    private boolean shouldSkipClass(PsiClass psiClass, String simpleClassName, String content) {
        // 檢查是否為頭部類
        if (simpleClassName.equals("MwHeader") || simpleClassName.contains("Header")) {
            return true;
        }

        // 檢查是否為容器類
        if (CommonUtils.getSettings().isOnlyProcessGenericDto() &&
                (simpleClassName.contains("Template") ||
                        simpleClassName.contains("Wrapper") ||
                        simpleClassName.equals("ResponseEntity") ||
                        simpleClassName.contains("Response") && content.contains("<") && content.contains(">") ||
                        simpleClassName.contains("Request") && content.contains("<") && content.contains(">"))) {
            return true;
        }

        return false;
    }

    /**
     * 檢查是否為容器類
     */
    private boolean isContainerClass(PsiClass psiClass) {
        if (psiClass == null) {
            return false;
        }

        String className = psiClass.getName();
        if (className == null) {
            return false;
        }

        // 檢查類名和泛型特徵
        boolean hasContainerName = className.contains("Template") ||
                className.contains("Wrapper") ||
                className.equals("ResponseEntity") ||
                className.contains("ResponseWrapper") ||
                className.contains("RequestWrapper") ||
                className.contains("GenericResponse") ||
                className.equals("MwHeader") ||
                className.contains("Response") && psiClass.hasTypeParameters() ||
                className.contains("Request") && psiClass.hasTypeParameters();

        boolean hasTypeParameters = psiClass.getTypeParameters().length > 0;
        boolean isHeaderClass = className.contains("Header");

        return (hasContainerName && hasTypeParameters) || isHeaderClass;
    }

    /**
     * 檢查是否應基於類型名稱跳過
     */
    private boolean shouldSkipByTypeName(String classNameWithPackage, String simpleClassName) {
        // 過濾標準庫類型
        if (classNameWithPackage.startsWith("java.time.") ||
                simpleClassName.equals("LocalDate") ||
                simpleClassName.equals("LocalTime") ||
                simpleClassName.equals("LocalDateTime")) {
            return true;
        }

        // 過濾頭部類
        if (simpleClassName.equals("MwHeader") || simpleClassName.contains("Header")) {
            return true;
        }

        return false;
    }

    /**
     * 檢查是否應跳過容器類
     */
    private boolean shouldSkipContainerClass(String simpleClassName, String content) {
        return CommonUtils.getSettings().isOnlyProcessGenericDto() &&
                (simpleClassName.contains("Template") ||
                        simpleClassName.contains("Wrapper") ||
                        simpleClassName.equals("ResponseEntity") ||
                        simpleClassName.contains("Response") && content.contains("<") && content.contains(">") ||
                        simpleClassName.contains("Request") && content.contains("<") && content.contains(">"));
    }

    /**
     * 檢查是否應跳過字段
     */
    private boolean shouldSkipField(PsiField field) {
        return JavaBeanToTypescriptInterfaceSettingsState.getInstance().ignoreSerialVersionUID &&
                "serialVersionUID".equals(field.getName());
    }

    /**
     * 檢查是否為請求類名
     */
    private boolean isRequestClassName(String className) {
        return className.endsWith("Tranrq") ||
                className.endsWith("Req") ||
                className.endsWith("Request");
    }

    /**
     * 檢查是否為響應類名
     */
    private boolean isResponseClassName(String className) {
        return className.endsWith("Tranrs") ||
                className.endsWith("Rs") ||
                className.endsWith("Response") ||
                className.endsWith("Resp");
    }

    /**
     * 檢查一個類是否依賴另一個類
     */
    private boolean isClassDependency(String className, String dependencyName,
                                      String classContent, String dependencyContent) {
        return className.contains(dependencyName) ||
                dependencyContent.contains(className) ||
                classContent.contains(dependencyName);
    }

    /**
     * 從主類內容中提取前綴信息
     */
    private void extractMainClassPrefix(String formattedContent, String simpleClassName,
                                        Map<String, String> mainClassPrefixMap) {
        if (formattedContent.contains("interface ")) {
            String interfaceName = extractInterfaceName(formattedContent);
            if (interfaceName != null && (interfaceName.contains("Req") || interfaceName.contains("Resp"))) {
                int suffixIndex = Math.max(
                        interfaceName.indexOf("Req"),
                        interfaceName.indexOf("Resp"));
                if (suffixIndex > 0) {
                    String transactionCodePrefix = interfaceName.substring(0, suffixIndex);
                    mainClassPrefixMap.put(simpleClassName, transactionCodePrefix);
                }
            }
        }
    }

    /**
     * 構建依賴關係圖
     */
    private Map<String, Set<String>> buildDependencyGraph(Map<String, String> classes) {
        Map<String, Set<String>> graph = new HashMap<>();

        // 初始化圖
        for (String className : classes.keySet()) {
            graph.put(className, new HashSet<>());
        }

        // 分析每個類的依賴
        for (Map.Entry<String, String> entry : classes.entrySet()) {
            String className = entry.getKey();
            String content = entry.getValue();

            for (String otherClass : classes.keySet()) {
                if (!className.equals(otherClass)) {
                    // 檢查引用關係
                    String otherInterfaceName = extractInterfaceName(classes.get(otherClass));
                    if (otherInterfaceName != null && hasTypeReference(content, otherInterfaceName)) {
                        // 找到依賴
                        graph.get(className).add(otherClass);
                        System.out.println("發現依賴關係: " + className + " -> " + otherClass);
                    }
                }
            }
        }

        return graph;
    }

    /**
     * 檢查未解決的引用
     */
    private void checkUnresolvedReferences(List<String> processList, Map<String, String> renameMap) {
        boolean hasUnresolved = false;
        for (String content : processList) {
            for (String oldName : renameMap.keySet()) {
                if (content.contains(": " + oldName + ";") ||
                        content.contains(": " + oldName + "[") ||
                        content.contains("?: " + oldName + ";")) {
                    System.out.println("警告：仍有未替換的引用 - " + oldName);
                    hasUnresolved = true;
                }
            }
        }

        if (hasUnresolved) {
            System.out.println("警告：最終清理後仍有未解決的引用");
        } else {
            System.out.println("最終清理完成，所有引用都已解決");
        }
    }

    /**
     * 從嵌套類名中提取唯一部分
     */
    private String extractUniqueClassPart(String simpleClassName, String... parentClassName) {
        // 嘗試從嵌套類名中移除父類前綴
        if (parentClassName != null && parentClassName.length > 0 && parentClassName[0] != null) {
            String parent = parentClassName[0];
            if (simpleClassName.startsWith(parent)) {
                String remaining = simpleClassName.substring(parent.length());
                if (!remaining.isEmpty()) {
                    return remaining;
                }
            }
        }

        // 尋找最後一個大寫字母開始的部分
        int lastCapIndex = -1;
        for (int i = 1; i < simpleClassName.length(); i++) {
            if (Character.isUpperCase(simpleClassName.charAt(i))) {
                lastCapIndex = i;
            }
        }

        if (lastCapIndex > 0) {
            return simpleClassName.substring(lastCapIndex);
        }

        // 移除常見後綴
        String[] commonSuffixes = {"Dto", "Vo", "Entity", "Model", "Bean", "Pojo", "Record",
                "Tranrq", "Tranrs", "Request", "Response", "Req", "Resp"};
        for (String suffix : commonSuffixes) {
            if (simpleClassName.endsWith(suffix)) {
                return simpleClassName.substring(0, simpleClassName.length() - suffix.length());
            }
        }

        // 默認返回原始名稱
        return simpleClassName;
    }

    /**
     * 從介面名稱中提取電文代號前綴
     */
    private String extractTransactionCodePrefix(String tsInterfaceName) {
        // 尋找常見後綴
        String[] suffixes = {"Req", "Resp", "Rq", "Rs"};
        for (String suffix : suffixes) {
            int suffixIndex = tsInterfaceName.indexOf(suffix);
            if (suffixIndex > 0) {
                return tsInterfaceName.substring(0, suffixIndex);
            }
        }
        return "";
    }

    /**
     * 使用正則表達式全面替換引用
     */
    private String replaceAllReferences(String content, String oldName, String newName, String className) {
        String originalContent = content;

        // 替換引用的各種形式
        // 1. 常規字段引用
        if (content.contains(": " + oldName + ";")) {
            content = content.replace(": " + oldName + ";", ": " + newName + ";");
        }

        // 2. 數組引用
        if (content.contains(": " + oldName + "[]")) {
            content = content.replace(": " + oldName + "[]", ": " + newName + "[]");
        }

        // 3. 可選屬性引用
        if (content.contains("?: " + oldName + ";")) {
            content = content.replace("?: " + oldName + ";", "?: " + newName + ";");
        }

        // 4. 介面定義
        String interfacePattern = "interface " + oldName + " \\{";
        String replacementPattern = "interface " + newName + " {";
        content = content.replaceAll(interfacePattern, replacementPattern);

        // 5. 使用正則表達式處理其他引用形式
        String[][] patterns = {
                {"([^\\w])(", oldName, ")(\\s*[;,:\\[\\]])"}, // 普通類型引用
                {"([<,]\\s*)(", oldName, ")(\\s*[,>])"}, // 泛型參數
                {"(:\\s*)(", oldName, ")(\\[\\])"}, // 數組類型
                {"(\\?:\\s*)(", oldName, ")(\\s*[;,])"}, // 可選屬性
                {"(extends\\s+)(", oldName, ")(\\s|\\{)"}, // 繼承
                {"(implements\\s+)(", oldName, ")(\\s|\\{)"} // 實現
        };

        for (String[] patternParts : patterns) {
            String pattern = patternParts[0] + patternParts[1] + patternParts[2];
            String replacement = patternParts[0] + newName + patternParts[2];
            content = content.replaceAll(pattern, replacement);
        }

        // 記錄替換結果
        if (!content.equals(originalContent)) {
            System.out.println("在 " + className + " 中成功替換: " + oldName + " -> " + newName);
        }

        return content;
    }

    /**
     * 分析類的使用情境
     */
    private ClassUsageInfo analyzeClassUsage(PsiClass psiClass) {
        ClassUsageInfo usageInfo = new ClassUsageInfo();
        usageInfo.className = psiClass.getQualifiedName();

        // 查找類的所有引用
        PsiReference[] references = findReferences(psiClass);
        String simpleName = psiClass.getName();
        System.out.println("分析類 " + simpleName + " 的使用情境，找到 " + references.length + " 處引用");

        for (PsiReference reference : references) {
            PsiElement element = reference.getElement();

            // 檢查參數使用
            boolean isParameter = checkIfUsedAsParameter(element);
            if (isParameter) {
                usageInfo.usedAsParameterCount++;
                System.out.println("  - 作為參數使用於: " + getContainingMethodDescription(element));
            }

            // 檢查返回值使用
            boolean isReturnValue = checkIfUsedAsReturnValue(element);
            if (isReturnValue) {
                usageInfo.usedAsReturnValueCount++;
                System.out.println("  - 作為返回值使用於: " + getContainingMethodDescription(element));
            }

            // 檢查控制器方法
            PsiMethod method = findEnclosingMethod(element);
            if (method != null && isControllerMethod(method)) {
                if (isParameter) {
                    usageInfo.usedAsControllerParameterCount++;
                }
                if (isReturnValue) {
                    usageInfo.usedAsControllerReturnValueCount++;
                }
            }
        }

        // 最終判斷
        determineClassRole(usageInfo);
        System.out.println("類 " + simpleName + " 分析結果: " +
                (usageInfo.isRequest ? "Request" : "Response") +
                " (參數: " + usageInfo.usedAsParameterCount +
                ", 返回值: " + usageInfo.usedAsReturnValueCount + ")");

        return usageInfo;
    }

    /**
     * 獲取處理後的接口名稱
     */
    private String getInterfaceName(String classNameWithPackage) {
        String tsInterfaceName = tsInterfaceNameMap.get(classNameWithPackage);
        if (tsInterfaceName != null) {
            return tsInterfaceName;
        }

        // 返回原類名
        return getSimpleClassName(classNameWithPackage);
    }

    /**
     * 獲取簡單類名（無包名）
     */
    private String getSimpleClassName(String classNameWithPackage) {
        int lastDotIndex = classNameWithPackage.lastIndexOf('.');
        return lastDotIndex > 0 ? classNameWithPackage.substring(lastDotIndex + 1) : classNameWithPackage;
    }

    /**
     * 收集類引用關係
     */
    private void collectClassReference(String fromClass, String toClass) {
        System.out.println("收集引用: " + fromClass + " -> " + toClass);

        if (fromClass == null || toClass == null) {
            System.out.println("引用關係為空，跳過");
            return;
        }

        // 跳過標準庫類
        if (toClass.startsWith("java.") || toClass.startsWith("javax.")) {
            System.out.println("跳過標準庫類: " + toClass);
            return;
        }

        // 添加引用關係
        if (!classReferences.containsKey(fromClass)) {
            classReferences.put(fromClass, new HashSet<>());
        }
        classReferences.get(fromClass).add(toClass);
        System.out.println("記錄引用關係: " + fromClass + " -> " + toClass);

        // 添加被引用關係
        if (!referencedByMap.containsKey(toClass)) {
            referencedByMap.put(toClass, new HashSet<>());
        }
        referencedByMap.get(toClass).add(fromClass);
    }

    /**
     * 檢查標準庫類型
     */
    private boolean isStandardLibraryType(String canonicalText) {
        return canonicalText.startsWith("java.time.") ||
                canonicalText.endsWith(".LocalDate") ||
                canonicalText.endsWith(".LocalTime") ||
                canonicalText.endsWith(".LocalDateTime");
    }

    /**
     * 查找元素所在的方法
     */
    private PsiMethod findEnclosingMethod(PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof PsiMethod) {
                return (PsiMethod) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * 查找類的引用
     */
    private PsiReference[] findReferences(PsiClass psiClass) {
        Collection<PsiReference> references = ReferencesSearch.search(psiClass, GlobalSearchScope.projectScope(project))
                .findAll();
        return references.toArray(new PsiReference[0]);
    }

    /**
     * 檢查元素是否在方法參數中使用
     */
    private boolean checkIfUsedAsParameter(PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof PsiParameterList || parent instanceof PsiParameter) {
                return true;
            }
            if (parent instanceof PsiMethod || parent instanceof PsiClass) {
                break;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * 檢查元素是否用作返回值
     */
    private boolean checkIfUsedAsReturnValue(PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof PsiReturnStatement) {
                return true;
            }
            if (parent instanceof PsiTypeElement && parent.getParent() instanceof PsiMethod) {
                return true;
            }
            if (parent instanceof PsiMethod || parent instanceof PsiClass) {
                break;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * 檢查方法是否為控制器方法
     */
    private boolean isControllerMethod(PsiMethod method) {
        // 檢查方法註解
        for (PsiAnnotation annotation : method.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && (
                    qualifiedName.contains("RequestMapping") ||
                            qualifiedName.contains("GetMapping") ||
                            qualifiedName.contains("PostMapping") ||
                            qualifiedName.contains("PutMapping") ||
                            qualifiedName.contains("DeleteMapping") ||
                            qualifiedName.contains("PatchMapping"))) {
                return true;
            }
        }

        // 檢查類註解是否有@Controller或@RestController註解
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
            for (PsiAnnotation annotation : containingClass.getAnnotations()) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName != null && (
                        qualifiedName.contains("Controller") ||
                                qualifiedName.contains("RestController"))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 獲取包含方法的描述信息
     */
    private String getContainingMethodDescription(PsiElement element) {
        PsiMethod method = findEnclosingMethod(element);
        if (method != null) {
            PsiClass containingClass = method.getContainingClass();
            String className = containingClass != null ? containingClass.getName() : "unknown";
            return className + "." + method.getName();
        }
        return "unknown method";
    }

    /**
     * 基於使用情境確定類的角色
     */
    private void determineClassRole(ClassUsageInfo usageInfo) {
        // 情況1: 有明確的控制器使用情境
        if (usageInfo.usedAsControllerParameterCount > 0 || usageInfo.usedAsControllerReturnValueCount > 0) {
            if (usageInfo.usedAsControllerParameterCount > usageInfo.usedAsControllerReturnValueCount) {
                usageInfo.isRequest = true;
                usageInfo.confidence = "HIGH";
                return;
            } else if (usageInfo.usedAsControllerReturnValueCount > usageInfo.usedAsControllerParameterCount) {
                usageInfo.isRequest = false;
                usageInfo.confidence = "HIGH";
                return;
            }
        }

        // 情況2: 有普通方法的使用情境
        if (usageInfo.usedAsParameterCount > 0 || usageInfo.usedAsReturnValueCount > 0) {
            if (usageInfo.usedAsParameterCount > usageInfo.usedAsReturnValueCount) {
                usageInfo.isRequest = true;
                usageInfo.confidence = "MEDIUM";
                return;
            } else if (usageInfo.usedAsReturnValueCount > usageInfo.usedAsParameterCount) {
                usageInfo.isRequest = false;
                usageInfo.confidence = "MEDIUM";
                return;
            }
        }

        // 情況3: 沒有明顯使用情境但有命名模式
        if (usageInfo.hasClearNamingPattern) {
            usageInfo.isRequest = usageInfo.nameBasedIsRequest;
            usageInfo.confidence = "MEDIUM";
            return;
        }

        // 情況4: 通過類名判斷
        if (usageInfo.className != null) {
            if (usageInfo.className.contains("Tranrq")) {
                usageInfo.isRequest = true;
                usageInfo.confidence = "LOW";
            } else if (usageInfo.className.contains("Tranrs")) {
                usageInfo.isRequest = false;
                usageInfo.confidence = "LOW";
            } else {
                // 默認為請求類
                usageInfo.isRequest = true;
                usageInfo.confidence = "VERY_LOW";
            }
        }
    }

    /**
     * 日誌輸出引用關係
     */
    private void logReferenceRelationships() {
        System.out.println("CLASS_REFERENCES 內容: " + classReferences);
        System.out.println("REFERENCED_BY 內容: " + referencedByMap);
    }

    /**
     * 類使用情境信息
     */
    static class ClassUsageInfo {
        String className;
        boolean isRequest = false; // 最終判斷結果
        boolean nameBasedIsRequest = false; // 基於名稱的判斷
        boolean hasClearNamingPattern = false; // 是否有明確的命名模式
        int usedAsParameterCount = 0; // 作為參數使用的次數
        int usedAsReturnValueCount = 0; // 作為返回值使用的次數
        int usedAsControllerParameterCount = 0; // 在控制器方法中作為參數使用的次數
        int usedAsControllerReturnValueCount = 0; // 在控制器方法中作為返回值使用的次數
        String confidence = "NONE"; // 判斷的置信度
    }
}