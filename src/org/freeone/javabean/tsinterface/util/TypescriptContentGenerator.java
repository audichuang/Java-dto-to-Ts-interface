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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用于生成文件
 */
public class TypescriptContentGenerator {

    /**
     * 冒号
     */
    public static final String REQUIRE_SPLIT_TAG = ": ";
    /**
     * 问好+冒号
     */
    public static final String NOT_REQUIRE_SPLIT_TAG = "?: ";

    /**
     * SUCCESS_CANONICAL_TEXT和一样，只是执行到某方法就加入
     */
    public static final List<String> CREATE_TYPESCRIPT_CONTENT_FOR_SINGLE_PSI_CLASS_ENTRY = new ArrayList<>();
    /**
     * 这个是成功才加入
     */
    private static final List<String> SUCCESS_CANONICAL_TEXT = new ArrayList<>();
    /**
     * 类的注释
     */
    private static final Map<String, String> CLASS_NAME_WITH_PACKAGE_2_TYPESCRIPT_COMMENT = new HashMap<>(8);

    /**
     * 属性类对应的interface的内容
     */
    private static final Map<String, String> CLASS_NAME_WITH_PACKAGE_2_CONTENT = new HashMap<>(8);

    // 增加項目方法關聯及電文代號緩存
    private static final Map<String, String> CLASS_TRANSACTION_CODE_MAP = new HashMap<>();

    /**
     * 類名到TypeScript介面名稱的映射
     */
    private static final Map<String, String> CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME = new HashMap<>(8);

    /**
     * 跟踪類引用關係 - 記錄每個類引用了哪些其他類
     */
    private static final Map<String, Set<String>> CLASS_REFERENCES = new HashMap<>();

    /**
     * 逆向引用關係 - 記錄每個類被哪些類引用
     */
    private static final Map<String, Set<String>> REFERENCED_BY = new HashMap<>();

    public static void processPsiClass(Project project, PsiClass selectedClass, boolean needDefault) {

        // 清空引用關係映射
        CLASS_REFERENCES.clear();
        REFERENCED_BY.clear();

        // PsiClass[] innerClasses = selectedClass.getInnerClasses();
        createTypescriptContentForSinglePsiClass(project, selectedClass);
        // for (PsiClass innerClass : innerClasses) {
        // createTypescriptContentForSinglePsiClass(innerClass);
        // }
    }

    /**
     * 合并成一个文件
     */
    public static String mergeContent(PsiClass selectedClass, boolean needDefault) {
        System.out.println("CLASS_REFERENCES 內容: " + CLASS_REFERENCES);
        System.out.println("REFERENCED_BY 內容: " + REFERENCED_BY);
        List<String> contentList = new ArrayList<>();
        String qualifiedName = selectedClass.getQualifiedName();

        // 創建用於排序的列表，使用Map保存類名和內容
        Map<String, String> requestClasses = new HashMap<>();
        Map<String, String> requestDependencyClasses = new HashMap<>();
        Map<String, String> responseClasses = new HashMap<>();
        Map<String, String> otherClasses = new HashMap<>();

        // 為了追蹤並重命名嵌套類，先收集主要類的信息
        Map<String, String> mainClassPrefixMap = new HashMap<>();

        // 跳過的類名集合
        Set<String> skippedClassNames = new HashSet<>();

        // 過濾並分類
        for (String classNameWithPackage : SUCCESS_CANONICAL_TEXT) {
            String content = CLASS_NAME_WITH_PACKAGE_2_CONTENT.get(classNameWithPackage);
            if (StringUtils.isBlank(content)) {
                continue;
            }

            // 獲取簡單類名
            String simpleClassName = classNameWithPackage.substring(classNameWithPackage.lastIndexOf('.') + 1);

            // 過濾標準庫類型
            if (classNameWithPackage.startsWith("java.time.") ||
                    simpleClassName.equals("LocalDate") ||
                    simpleClassName.equals("LocalTime") ||
                    simpleClassName.equals("LocalDateTime")) {
                continue;
            }

            // 始終過濾MwHeader和常見的頭部類
            if (simpleClassName.equals("MwHeader") ||
                    simpleClassName.contains("Header")) {
                System.out.println("跳過頭部類: " + simpleClassName);
                skippedClassNames.add(simpleClassName);
                continue;
            }

            // 檢查是否為容器類，如果設置為僅處理泛型DTO，則跳過容器類
            if (CommonUtils.getSettings().isOnlyProcessGenericDto() &&
                    (simpleClassName.contains("Template") ||
                            simpleClassName.contains("Wrapper") ||
                            simpleClassName.equals("ResponseEntity") ||
                            simpleClassName.contains("Response") && content.contains("<") && content.contains(">") ||
                            simpleClassName.contains("Request") && content.contains("<") && content.contains(">"))) {
                System.out.println("跳過容器類: " + simpleClassName);
                skippedClassNames.add(simpleClassName);
                continue;
            }

            StringBuilder stringBuilder = new StringBuilder();
            // 添加註釋
            String psiClassCComment = CLASS_NAME_WITH_PACKAGE_2_TYPESCRIPT_COMMENT.get(classNameWithPackage);
            if (psiClassCComment != null && psiClassCComment.trim().length() > 0) {
                if (psiClassCComment.endsWith("\n")) {
                    stringBuilder.append(psiClassCComment);
                } else {
                    stringBuilder.append(psiClassCComment).append("\n");
                }
            }

            stringBuilder.append("export ");
            if (needDefault && classNameWithPackage.equalsIgnoreCase(qualifiedName)) {
                // 移除默認導出，不再使用 default
                // stringBuilder.append("default ");
            }
            stringBuilder.append(content);
            String formattedContent = stringBuilder.toString();

            // 檢查並收集主要類的前綴信息
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

            // 根據類名對內容進行分類
            if (simpleClassName.endsWith("Tranrq") || simpleClassName.endsWith("Req")
                    || simpleClassName.endsWith("Request")) {
                requestClasses.put(simpleClassName, formattedContent);
            } else if (simpleClassName.endsWith("Tranrs") || simpleClassName.endsWith("Rs")
                    || simpleClassName.endsWith("Response")) {
                responseClasses.put(simpleClassName, formattedContent);
            } else {
                // 檢查是否為請求類的依賴類
                boolean isDependency = false;
                for (String reqClassName : requestClasses.keySet()) {
                    // 檢查請求類內容中是否使用了這個類
                    String reqClassContent = requestClasses.get(reqClassName);
                    // 檢查請求類中是否包含該類的引用, 如通過名稱檢查可能的使用
                    if (reqClassName.contains(simpleClassName) ||
                            content.contains(simpleClassName) ||
                            reqClassContent.contains(simpleClassName)) {
                        requestDependencyClasses.put(simpleClassName, formattedContent);
                        isDependency = true;
                        break;
                    }
                }

                if (!isDependency) {
                    // 檢查是否為響應類的依賴類
                    for (String resClassName : responseClasses.keySet()) {
                        String resClassContent = responseClasses.get(resClassName);
                        if (resClassName.contains(simpleClassName) ||
                                content.contains(simpleClassName) ||
                                resClassContent.contains(simpleClassName)) {
                            // 如果是響應類依賴，也放到響應類中
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
        }

        // 處理嵌套類命名 - 基於引用關係識別嵌套類
        processNestedClassesBasedOnReferences();

        // 在處理完所有類後，構建一個包含所有類的映射，用於最終的引用更新
        Map<String, String> allClasses = new HashMap<>();
        allClasses.putAll(requestClasses);
        allClasses.putAll(requestDependencyClasses);
        allClasses.putAll(responseClasses);
        allClasses.putAll(otherClasses);

        // 應用重命名 - 更新所有類的內容以反映新的接口名
        applyRenamingToAllClasses(allClasses);

        // 添加排序後的類到結果列表
        // 1. 請求類
        contentList.addAll(requestClasses.values());
        // 2. 請求類依賴的類
        contentList.addAll(requestDependencyClasses.values());
        // 3. 響應類
        contentList.addAll(responseClasses.values());
        // 4. 其他類
        contentList.addAll(otherClasses.values());

        // 進行最終嵌套類替換處理
        List<String> processedContentList = new ArrayList<>();
        for (String content : contentList) {
            String processedContent = content;

            // 1. 首先替換介面聲明部分 - 這是關鍵修正
            for (Map.Entry<String, String> entry : CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.entrySet()) {
                String fullClassName = entry.getKey();
                String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                String newInterfaceName = entry.getValue();

                // 檢查並替換介面聲明
                if (!simpleClassName.equals(newInterfaceName)) {
                    String interfacePattern = "interface " + simpleClassName + " \\{";
                    String replacementPattern = "interface " + newInterfaceName + " {";

                    // 使用正則表達式匹配完整的介面聲明
                    Pattern pattern = Pattern.compile(interfacePattern);
                    Matcher matcher = pattern.matcher(processedContent);

                    if (matcher.find()) {
                        processedContent = matcher.replaceAll(replacementPattern);
                        System.out.println("替換介面聲明: " + simpleClassName + " -> " + newInterfaceName);
                    }
                }
            }

            // 2. 然後替換引用部分
            for (Map.Entry<String, String> entry : CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.entrySet()) {
                String fullClassName = entry.getKey();
                String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                String newInterfaceName = entry.getValue();

                // 排除已經處理過的介面聲明
                if (!simpleClassName.equals(newInterfaceName)) {
                    // 標準類型引用
                    if (processedContent.contains(": " + simpleClassName + ";")) {
                        processedContent = processedContent.replace(": " + simpleClassName + ";",
                                ": " + newInterfaceName + ";");
                    }

                    // 數組引用
                    if (processedContent.contains(": " + simpleClassName + "[]")) {
                        processedContent = processedContent.replace(": " + simpleClassName + "[]",
                                ": " + newInterfaceName + "[]");
                    }

                    // 可選屬性引用
                    if (processedContent.contains("?: " + simpleClassName + ";")) {
                        processedContent = processedContent.replace("?: " + simpleClassName + ";",
                                "?: " + newInterfaceName + ";");
                    }
                }
            }

            // 3. 還需要處理 export interface 部分
            for (Map.Entry<String, String> entry : CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.entrySet()) {
                String fullClassName = entry.getKey();
                String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                String newInterfaceName = entry.getValue();

                if (!simpleClassName.equals(newInterfaceName)) {
                    String exportPattern = "export interface " + simpleClassName + " \\{";
                    String exportReplacement = "export interface " + newInterfaceName + " {";

                    processedContent = processedContent.replaceAll(exportPattern, exportReplacement);
                }
            }

            processedContentList.add(processedContent);
        }

        // 最後清理內容，移除對跳過類的引用，以避免未定義錯誤
        if (!skippedClassNames.isEmpty()) {
            List<String> cleanedContentList = new ArrayList<>();

            for (String content : processedContentList) { // 注意這裡使用了處理後的列表
                boolean contentModified = false;
                String modifiedContent = content;

                // 遍歷所有被跳過的類，從內容中移除對它們的引用
                for (String skippedClass : skippedClassNames) {
                    // 檢查是否包含引用
                    if (modifiedContent.contains(": " + skippedClass + ";") ||
                            modifiedContent.contains(": " + skippedClass + "[]")) {

                        // 替換對應的引用為 any
                        modifiedContent = modifiedContent.replace(": " + skippedClass + ";", ": any;");
                        modifiedContent = modifiedContent.replace(": " + skippedClass + "[];", ": any[];");
                        contentModified = true;
                    }
                }

                cleanedContentList.add(contentModified ? modifiedContent : content);
            }

            return String.join("\n", cleanedContentList);
        }

        return String.join("\n", processedContentList); // 返回處理後的內容

    }

    // 從接口內容中提取接口名稱
    private static String extractInterfaceName(String content) {
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
     * 為单独的class创建内容
     */
    public static String createTypescriptContentForSinglePsiClass(Project project, PsiClass psiClass) {
        if (psiClass != null) {
            StringBuilder contentBuilder = new StringBuilder();
            String classNameWithoutPackage = psiClass.getName();
            String classNameWithPackage = psiClass.getQualifiedName();
            // T 这种泛型值
            if (classNameWithPackage == null) {
                return classNameWithoutPackage != null ? classNameWithoutPackage : "any";
            }

            if (SUCCESS_CANONICAL_TEXT.contains(classNameWithPackage)) {
                // 如果已处理过，返回可能已经修改过的接口名称
                return getInterfaceName(classNameWithPackage);
            }

            // 避免递归调用死循环
            if (CREATE_TYPESCRIPT_CONTENT_FOR_SINGLE_PSI_CLASS_ENTRY.contains(classNameWithPackage)) {
                return getInterfaceName(classNameWithPackage);
            }
            CREATE_TYPESCRIPT_CONTENT_FOR_SINGLE_PSI_CLASS_ENTRY.add(classNameWithPackage);

            System.out.println(classNameWithoutPackage + " qualifiedName " + classNameWithPackage);

            JvmClassKind classKind = psiClass.getClassKind();
            try {
                // 泛型
                classNameWithoutPackage = CommonUtils.getClassNameWithGenerics(psiClass);
            } catch (Exception e) {
                // e.printStackTrace();
            }

            // 检查是否使用电文代号命名
            String tsInterfaceName = processInterfaceName(project, psiClass, classNameWithoutPackage);

            PsiField[] fields = psiClass.getAllFields();
            if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().ignoreParentField) {
                fields = psiClass.getFields();
            }
            PsiMethod[] allMethods = psiClass.getAllMethods();
            if (classKind.equals(JvmClassKind.CLASS)) {
                contentBuilder.append("interface ").append(tsInterfaceName).append(" {\n");
                for (int i = 0; i < fields.length; i++) {
                    PsiField fieldItem = fields[i];

                    // 检查是否需要忽略 serialVersionUID
                    if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().ignoreSerialVersionUID &&
                            "serialVersionUID".equals(fieldItem.getName())) {
                        continue;
                    }

                    String documentText = "";
                    // 获取注释
                    PsiDocComment docComment = fieldItem.getDocComment();
                    if (docComment != null && docComment.getText() != null) {
                        documentText = docComment.getText();
                    }
                    String fieldName = fieldItem.getName();
                    // 2023-12-26 判断是或否使用JsonProperty
                    if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().useAnnotationJsonProperty) {
                        String jsonPropertyValue = CommonUtils.getJsonPropertyValue(fieldItem, allMethods);
                        if (jsonPropertyValue != null) {
                            fieldName = jsonPropertyValue;
                        }
                    }

                    // 根据设置决定是否添加可选标记
                    String fieldSplitTag = REQUIRE_SPLIT_TAG; // 默认使用冒号（:）

                    // 只有在启用添加可选标记的设置时，才会添加问号
                    if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().addOptionalMarkToAllFields) {
                        fieldSplitTag = NOT_REQUIRE_SPLIT_TAG;
                        // 如果字段有必填注解，则使用冒号
                        if (CommonUtils.isFieldRequire(fieldItem.getAnnotations())) {
                            fieldSplitTag = REQUIRE_SPLIT_TAG;
                        }
                    }

                    String typeString;
                    PsiType fieldType = fieldItem.getType();
                    typeString = getTypeString(project, fieldType, psiClass);

                    // 统一格式化文档注解
                    if (documentText.trim().length() > 0) {
                        // 提取注解中的有效内容
                        String commentContent = extractCommentContent(documentText);
                        if (!commentContent.isEmpty()) {
                            contentBuilder.append("  /**\n   * ").append(commentContent).append("\n   */\n");
                        }
                    }

                    contentBuilder.append("  ").append(fieldName).append(fieldSplitTag).append(typeString)
                            .append(";\n");
                    if (i != fields.length - 1) {
                        contentBuilder.append("\n");
                    }
                }
                contentBuilder.append("}\n");
            } else if (classKind.equals(JvmClassKind.ENUM)) {
                contentBuilder.append("type ").append(tsInterfaceName).append(" = ");
                List<String> enumConstantValueList = new ArrayList<>();
                // enumConstantValueList.add("string");
                for (PsiField psiField : fields) {
                    if (psiField instanceof PsiEnumConstant) {
                        String name = psiField.getName();
                        // 将字段的名字视为字符串
                        String value = "'" + name + "'";
                        enumConstantValueList.add(value);
                    }
                }
                String join = String.join(" | ", enumConstantValueList);
                contentBuilder.append(join).append(";\n");

            } else {
                return "unknown";
            }

            String content = contentBuilder.toString();
            SUCCESS_CANONICAL_TEXT.add(classNameWithPackage);
            PsiDocComment classDocComment = psiClass.getDocComment();
            if (classDocComment != null && classDocComment.getText() != null) {
                // 统一格式化类注解
                String classComment = extractCommentContent(classDocComment.getText());
                if (!classComment.isEmpty()) {
                    String formattedComment = "/**\n * " + classComment + "\n */\n";
                    CLASS_NAME_WITH_PACKAGE_2_TYPESCRIPT_COMMENT.put(classNameWithPackage, formattedComment);
                }
            }
            CLASS_NAME_WITH_PACKAGE_2_CONTENT.put(classNameWithPackage, content);

            // 保存接口名称映射关系
            if (!tsInterfaceName.equals(classNameWithoutPackage)) {
                CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.put(classNameWithPackage, tsInterfaceName);
            }

            return tsInterfaceName;
        } else {
            return "any";
        }
    }

    /**
     * 从fieldType中获取类型並收集引用關係
     */
    private static String getTypeString(Project project, PsiType fieldType, PsiClass containingClass) {
        String typeString = "any";
        if (fieldType == null) {
            typeString = "any";
        } else if (CommonUtils.isNumberType(fieldType)) {
            typeString = "number";
        } else if (CommonUtils.isStringType(fieldType)) {
            typeString = "string";
        } else if (CommonUtils.isBooleanType(fieldType)) {
            typeString = "boolean";
        } else if (CommonUtils.isJavaUtilDateType(fieldType)
                && JavaBeanToTypescriptInterfaceSettingsState.getInstance().enableDataToString) {
            typeString = "string";
        } else if (CommonUtils.isMapType(fieldType)) {
            typeString = processMap(project, fieldType);
        } else if (CommonUtils.isArrayType(fieldType)) {
            typeString = processList(project, fieldType);
        } else {
            // 检查类型是否来自标准库
            String canonicalText = fieldType.getCanonicalText();
            if (canonicalText.startsWith("java.time.") ||
                    canonicalText.endsWith(".LocalDate") ||
                    canonicalText.endsWith(".LocalTime") ||
                    canonicalText.endsWith(".LocalDateTime")) {
                // 对于标准库日期时间类型，使用 any 替代
                return "any";
            }

            if (fieldType instanceof PsiClassReferenceType) {
                PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) fieldType;
                PsiType[] parameters = psiClassReferenceType.getParameters();
                if (parameters.length != 0) {
                    // 泛型
                    for (PsiType parameter : parameters) {
                        PsiClass parameterClass = CommonUtils.findPsiClass(project, parameter);
                        createTypescriptContentForSinglePsiClass(project, parameterClass);
                    }
                    PsiClass resolvePsiClass = psiClassReferenceType.resolve();
                    createTypescriptContentForSinglePsiClass(project, resolvePsiClass);
                    // 类似 PageModel<Student>
                    typeString = psiClassReferenceType.getPresentableText();
                } else {
                    // 普通类
                    PsiClass resolveClass = psiClassReferenceType.resolve();

                    if (resolveClass != null && containingClass != null) {
                        // 收集類引用關係
                        collectClassReference(containingClass.getQualifiedName(), resolveClass.getQualifiedName());
                    }

                    typeString = createTypescriptContentForSinglePsiClass(project, resolveClass);
                }

            } else {
                PsiClass filedClass = CommonUtils.findPsiClass(project, fieldType);

                if (filedClass != null && containingClass != null) {
                    // 收集類引用關係
                    collectClassReference(containingClass.getQualifiedName(), filedClass.getQualifiedName());
                }

                typeString = createTypescriptContentForSinglePsiClass(project, filedClass);
            }

        }
        return typeString;
    }

    /**
     * 收集類引用關係
     */
    private static void collectClassReference(String fromClass, String toClass) {
        // 添加更多的日誌
        System.out.println("嘗試收集引用: " + fromClass + " -> " + toClass);

        if (fromClass == null || toClass == null) {
            System.out.println("引用關係為空，跳過");
            return;
        }

        // 避免記錄標準庫類但輸出跳過原因
        if (toClass.startsWith("java.") || toClass.startsWith("javax.")) {
            System.out.println("跳過標準庫類: " + toClass);
            return;
        }

        // 添加引用關係並輸出日誌
        if (!CLASS_REFERENCES.containsKey(fromClass)) {
            CLASS_REFERENCES.put(fromClass, new HashSet<>());
        }
        CLASS_REFERENCES.get(fromClass).add(toClass);
        System.out.println("成功收集引用關係: " + fromClass + " -> " + toClass);

        // 添加被引用關係
        if (!REFERENCED_BY.containsKey(toClass)) {
            REFERENCED_BY.put(toClass, new HashSet<>());
        }
        REFERENCED_BY.get(toClass).add(fromClass);
    }

    private static String processList(Project project, PsiType psiType) {
        return getFirstTsTypeForArray(project, 0, psiType);
    }

    /**
     * 处理map
     */
    private static String processMap(Project project, PsiType type) {
        // 默认的value的类型是any
        String defaultVType = "any";
        if (type instanceof PsiClassReferenceType) {
            PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) type;
            PsiType[] parameters = psiClassReferenceType.getParameters();
            if (parameters.length == 2) {

                PsiType vType = parameters[1];
                boolean isNumber = CommonUtils.isNumberType(vType);
                boolean isStringType = CommonUtils.isStringType(vType);
                boolean isArrayType = CommonUtils.isArrayType(vType);

                if (isNumber) {
                    defaultVType = "number";
                } else if (isStringType) {
                    defaultVType = "string";
                } else if (isArrayType) {

                    defaultVType = getTypeString(project, vType, psiClassReferenceType.resolve());
                    System.out.println("vtype = " + defaultVType);
                    // if (vType instanceof PsiArrayType) {
                    // PsiType getDeepComponentType = vType.getDeepComponentType();
                    // defaultVType = processList(project, getDeepComponentType);
                    // } else if (vType instanceof PsiClassReferenceType) {
                    // PsiType getDeepComponentType = type.getDeepComponentType();
                    // defaultVType = getTypeString(project, getDeepComponentType);
                    // }

                } else {
                    PsiClass psiClass = CommonUtils.findPsiClass(project, vType);
                    if (psiClass == null) {
                        defaultVType = "any";
                    } else {
                        defaultVType = createTypescriptContentForSinglePsiClass(project, psiClass);
                        // defaultVType = vType.getPresentableText();
                    }
                }

            }

        }

        return "{[x:string]: " + defaultVType + "}";
    }

    private static String getFirstTsTypeForArray(Project project, int treeLevel, PsiType psiType) {
        if (treeLevel > 100) {
            return "any";
        }
        List<PsiType> numberSuperClass = Arrays.stream(psiType.getSuperTypes())
                .filter(superTypeItem -> superTypeItem.getCanonicalText().equals("java.lang.Number"))
                .collect(Collectors.toList());
        if (!numberSuperClass.isEmpty()) {
            return "number";
        }
        String canonicalText = psiType.getCanonicalText();
        if ("java.lang.Boolean".equals(canonicalText)) {
            return "boolean";
        } else if ("java.lang.String".equals(canonicalText)) {
            return "string";
        } else {

            boolean isArrayType = CommonUtils.isArrayType(psiType);
            boolean isMapType = CommonUtils.isMapType(psiType);
            // 里头还是一层 集合
            if (isArrayType) {
                if (psiType instanceof PsiClassReferenceType || psiType instanceof PsiArrayType) {

                    PsiType deepComponentType = null;
                    if (psiType instanceof PsiClassReferenceType) {
                        PsiClassReferenceType psiClassReferenceType = (PsiClassReferenceType) psiType;
                        PsiType[] parameters = psiClassReferenceType.getParameters();
                        if (parameters.length == 0) {
                            return "any[]";
                        }
                        deepComponentType = parameters[0].getDeepComponentType();
                    } else {
                        deepComponentType = psiType.getDeepComponentType();
                    }

                    String firstTsTypeForArray = getTypeString(project, deepComponentType, null);
                    return firstTsTypeForArray + "[]";
                } else {
                    return "any[]";
                }

            } else if (isMapType) {
                return psiType.getPresentableText();
            } else {
                return psiType.getPresentableText();
            }

        }
    }

    private static String extractCommentContent(String comment) {
        // 移除 Java 文档注解标记
        String content = comment.trim()
                .replaceAll("/\\*\\*", "") // 移除开头和结尾的 /**
                .replaceAll("\\*/", "") // 移除结尾的 */
                .replaceAll("^\\s*\\*\\s*", "") // 移除每行开头的 * 及其前后空格
                .replaceAll("\\n\\s*\\*\\s*", " ") // 将多行注解合并为单行，移除行开头的 * 及空格
                .trim();

        // 如果有 @param、@return 等标记，只保留主要描述
        if (content.contains("@")) {
            content = content.split("@")[0].trim();
        }

        return content;
    }

    // 修改處理接口名稱的方法
    private static String processInterfaceName(Project project, PsiClass psiClass, String originalName) {
        // 如果未啟用電文代號命名，直接返回原名稱
        if (!CommonUtils.getSettings().isUseTransactionCodePrefix()) {
            return originalName;
        }

        String qualifiedName = psiClass.getQualifiedName();

        // 檢查是否為泛型的容器類，如果設置為僅處理泛型DTO，則跳過這些類
        if (CommonUtils.getSettings().isOnlyProcessGenericDto()) {
            boolean isContainer = isContainerClass(psiClass);
            if (isContainer) {
                return originalName;
            }

            // 檢查類名是否包含容器相關字眼
            String className = psiClass.getName();
            if (className != null && (className.contains("Template") ||
                    className.contains("Wrapper") ||
                    className.equals("ResponseEntity") ||
                    className.equals("MwHeader") ||
                    className.contains("Header"))) {
                return originalName;
            }
        }

        // 檢查是否為嵌套類
        boolean isNested = qualifiedName != null && qualifiedName.contains("$");
        if (isNested) {
            // 獲取外部類全限定名
            String outerClassName = qualifiedName.substring(0, qualifiedName.indexOf("$"));
            PsiClass outerClass = CommonUtils.findPsiClass(project, CommonUtils.findPsiType(project, outerClassName));

            if (outerClass != null) {
                // 獲取外部類的接口名稱
                String outerInterfaceName = processInterfaceName(project, outerClass, outerClass.getName());

                // 提取電文代號前綴 (如 "QRYSTATEMENTS")
                String transactionCodePrefix = "";
                if (outerInterfaceName.contains("Req") || outerInterfaceName.contains("Resp")) {
                    // 嘗試提取前綴
                    int suffixIndex = Math.max(
                            outerInterfaceName.indexOf("Req"),
                            outerInterfaceName.indexOf("Resp"));
                    if (suffixIndex > 0) {
                        transactionCodePrefix = outerInterfaceName.substring(0, suffixIndex);
                    }
                }

                if (!transactionCodePrefix.isEmpty()) {
                    // 新增：分析嵌套類的使用情境
                    ClassUsageInfo usageInfo = analyzeClassUsage(project, psiClass);
                    boolean isRequest = usageInfo.isRequest;

                    // 處理嵌套類名稱
                    return processNestedClassName(transactionCodePrefix, originalName, isRequest);
                }
            }
        }

        // 檢查是否已經緩存了電文代號
        if (CLASS_TRANSACTION_CODE_MAP.containsKey(qualifiedName)) {
            String transactionCode = CLASS_TRANSACTION_CODE_MAP.get(qualifiedName);

            // 新增：使用更智能的方式分析類的角色
            ClassUsageInfo usageInfo = analyzeClassUsage(project, psiClass);
            boolean isRequest = usageInfo.isRequest;

            return TransactionCodeExtractor.generateInterfaceName(originalName, transactionCode, isRequest);
        }

        // 從類的方法使用處查找控制器方法
        PsiReference[] references = findReferences(project, psiClass);

        // 新增：分析類的使用情境
        ClassUsageInfo usageInfo = analyzeClassUsage(project, psiClass);

        for (PsiReference reference : references) {
            PsiElement element = reference.getElement();
            PsiMethod method = findEnclosingMethod(element);
            if (method != null) {
                // 提取電文代號
                String transactionCode = TransactionCodeExtractor.extractTransactionCode(method);
                if (transactionCode != null && !transactionCode.isEmpty()) {
                    // 緩存電文代號
                    CLASS_TRANSACTION_CODE_MAP.put(qualifiedName, transactionCode);

                    // 使用分析結果決定是請求還是響應
                    boolean isRequest = usageInfo.isRequest;

                    return TransactionCodeExtractor.generateInterfaceName(originalName, transactionCode, isRequest);
                }
            }
        }

        // 嘗試查找相關的主類以獲取電文代號前綴
        for (Map.Entry<String, String> entry : CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.entrySet()) {
            String tsInterfaceName = entry.getValue();

            if (tsInterfaceName.contains("Req") || tsInterfaceName.contains("Resp")) {
                // 提取前綴
                int suffixIndex = Math.max(
                        tsInterfaceName.indexOf("Req"),
                        tsInterfaceName.indexOf("Resp"));

                if (suffixIndex > 0) {
                    String prefix = tsInterfaceName.substring(0, suffixIndex);

                    // 使用分析結果決定是請求還是響應
                    boolean isRequest = usageInfo.isRequest;

                    return processNestedClassName(prefix, originalName, isRequest);
                }
            }
        }

        // 未找到電文代號，返回原名稱
        return originalName;
    }

    // 查找元素所在的方法
    private static PsiMethod findEnclosingMethod(PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof PsiMethod) {
                return (PsiMethod) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    // 查找类的引用
    private static PsiReference[] findReferences(Project project, PsiClass psiClass) {
        // 使用正确的包路径 com.intellij.psi.search.searches.ReferencesSearch
        Collection<PsiReference> references = ReferencesSearch.search(psiClass, GlobalSearchScope.projectScope(project))
                .findAll();
        return references.toArray(new PsiReference[0]);
    }

    /**
     * 根據類名和使用情境判斷是否為請求類
     *
     * @param project  當前項目
     * @param psiClass 要判斷的類
     * @return 是否為請求類
     */
    private static boolean isRequestClass(Project project, PsiClass psiClass) {
        if (psiClass == null) {
            return false;
        }

        // 使用新增的分析方法
        ClassUsageInfo usageInfo = analyzeClassUsage(project, psiClass);
        return usageInfo.isRequest;
    }

    // 获取接口名称（考虑命名规则转换）
    private static String getInterfaceName(String classNameWithPackage) {
        String tsInterfaceName = CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.get(classNameWithPackage);
        if (tsInterfaceName != null) {
            return tsInterfaceName;
        }

        // 返回原类名（无包名）
        int lastDotIndex = classNameWithPackage.lastIndexOf('.');
        return lastDotIndex > 0 ? classNameWithPackage.substring(lastDotIndex + 1) : classNameWithPackage;
    }

    // 判断一个类是否为另一个类的内部/嵌套类
    private static boolean isNestedClass(PsiClass parentClass, PsiClass nestedClass) {
        if (parentClass == null || nestedClass == null) {
            return false;
        }

        // 检查类名前缀是否匹配
        String parentName = parentClass.getName();
        String nestedName = nestedClass.getName();

        // 如果是内部类，名称通常会有父类名称作为前缀
        return nestedName != null && parentName != null &&
                (nestedName.startsWith(parentName) ||
                        nestedName.contains(parentName.replace("Tranrq", "").replace("Tranrs", "")));
    }

    // 處理嵌套類名稱
    private static String processNestedClassName(String mainClassPrefix, String nestedClassName, boolean isRequest) {
        // 從嵌套類名中移除原始前綴
        String baseName = nestedClassName;

        // 特別處理 QryStatement 開頭的類型
        if (baseName.startsWith("QryStatement")) {
            // 移除 QryStatement 前綴以及 Tranrq、Tranrs 部分
            String uniquePart = baseName
                    .replace("QryStatement", "")
                    .replace("Tranrq", "")
                    .replace("Tranrs", "");

            // 根據是請求還是響應類型決定後綴
            String suffix = isRequest ? "Req" : "Resp";

            // 如果處理後還有內容，則組合新的名稱
            if (!uniquePart.isEmpty()) {
                return mainClassPrefix + suffix + uniquePart;
            }
        }

        // 根據是請求還是響應類型決定後綴
        String suffix = isRequest ? "Req" : "Resp";

        // 對於一般嵌套類，保留其獨特部分，加上前綴和後綴
        // 首先嘗試移除包含 "Tranrq" 或 "Tranrs" 的部分
        String uniquePart = baseName
                .replace("Tranrq", "")
                .replace("Tranrs", "")
                .replace("Request", "")
                .replace("Response", "")
                .replace("Req", "")
                .replace("Resp", "");

        // 如果移除後為空，使用原名稱
        if (uniquePart.isEmpty()) {
            uniquePart = baseName;
        }

        // 返回新的名稱
        return mainClassPrefix + suffix + uniquePart;
    }

    /**
     * 創建內容
     *
     * @param fieldNameAndDocCommentMap
     * @param typeNameMap
     * @param requireFieldNameSet
     * @param project
     * @param interfaceName
     * @return
     */
    public static String createInterfaceContentByFieldInfo(Map<String, String> fieldNameAndDocCommentMap,
            Map<String, String> typeNameMap,
            Set<String> requireFieldNameSet,
            Project project,
            String interfaceName) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("export interface " + interfaceName + " {\n");

        // 預處理所有引用的類型名稱，確保嵌套和關聯類型使用一致的命名規則
        Map<String, String> processedTypeNameMap = new HashMap<>(typeNameMap);
        preProcessNestedTypes(project, processedTypeNameMap, interfaceName);

        // 根據字段名稱排序
        List<String> fieldNameList = new ArrayList<>(fieldNameAndDocCommentMap.keySet());
        fieldNameList.sort(String::compareTo);

        for (String fieldName : fieldNameList) {
            String value = fieldNameAndDocCommentMap.get(fieldName);
            if (null != value && !"".equals(value.trim())) {
                buffer.append(value);
            }
            String typeName = processedTypeNameMap.get(fieldName);

            buffer.append("  ");
            buffer.append(fieldName);
            if (!requireFieldNameSet.contains(fieldName)) {
                buffer.append("?");
            }
            buffer.append(": " + typeName);
            buffer.append(";\n\n");
        }
        buffer.append("}\n");
        System.out.println("buffer.toString() = " + buffer.toString());
        return buffer.toString();
    }

    /**
     * 預處理嵌套和關聯類型的名稱，確保使用一致的命名規則
     */
    private static void preProcessNestedTypes(Project project, Map<String, String> typeNameMap, String interfaceName) {
        // 如果介面名稱包含電文代號前綴
        if (interfaceName.contains("Req") || interfaceName.contains("Resp")) {
            int suffixIndex = Math.max(interfaceName.indexOf("Req"), interfaceName.indexOf("Resp"));
            if (suffixIndex > 0) {
                String transactionCodePrefix = interfaceName.substring(0, suffixIndex);
                boolean isRequest = interfaceName.contains("Req");

                // 處理所有類型名稱
                for (Map.Entry<String, String> entry : new HashMap<>(typeNameMap).entrySet()) {
                    String typeName = entry.getValue();

                    // 檢查是否為需要處理的類型
                    if (typeName != null &&
                            !isTypescriptPrimaryType(typeName) &&
                            !typeName.equals("any") &&
                            !typeName.equals("unknown") &&
                            !typeName.startsWith(transactionCodePrefix)) {

                        // 直接處理常見的嵌套類名稱模式
                        if (typeName.contains("QryStatement") ||
                                typeName.contains("Tranrq") ||
                                typeName.contains("Tranrs")) {

                            // 生成新的類型名稱
                            String newTypeName = transactionCodePrefix;

                            // 添加請求/響應後綴
                            newTypeName += isRequest ? "Req" : "Resp";

                            // 如果沒有特定後綴，嘗試提取類名中的唯一部分
                            String originalName = typeName;
                            int lastDotIndex = originalName.lastIndexOf('.');
                            if (lastDotIndex > 0) {
                                originalName = originalName.substring(lastDotIndex + 1);
                            }

                            // 移除已知的前綴和後綴
                            String uniquePart = originalName
                                    .replace("QryStatement", "")
                                    .replace("Tranrq", "")
                                    .replace("Tranrs", "")
                                    .replace("Request", "")
                                    .replace("Response", "")
                                    .replace("Req", "")
                                    .replace("Resp", "");

                            // 如果提取後還有內容，添加到新名稱中
                            if (!uniquePart.isEmpty()) {
                                newTypeName += uniquePart;
                            }

                            // 更新類型名稱映射
                            typeNameMap.put(entry.getKey(), newTypeName);

                            // 同時更新全局類名映射，以便其他地方引用時能夠一致
                            CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.put(typeName, newTypeName);

                            System.out.println("更新嵌套類型名稱: " + typeName + " -> " + newTypeName);
                        }
                    }
                }
            }
        }
    }

    /**
     * 判斷是否是 TypeScript 基本類型
     *
     * @param type 類型名稱
     * @return 是否為基本類型
     */
    private static boolean isTypescriptPrimaryType(String type) {
        return "number".equals(type) || "string".equals(type) || "boolean".equals(type);
    }

    /**
     * 判斷一個類是否為外層容器類（如ResponseTemplate、RequestTemplate等）
     */
    private static boolean isContainerClass(PsiClass psiClass) {
        if (psiClass == null) {
            return false;
        }

        String className = psiClass.getName();
        if (className == null) {
            return false;
        }

        // 檢查類名是否包含常見的容器名稱
        boolean hasContainerName = className.contains("Template") ||
                className.contains("Wrapper") ||
                className.equals("ResponseEntity") ||
                className.contains("ResponseWrapper") ||
                className.contains("RequestWrapper") ||
                className.contains("GenericResponse") ||
                className.equals("MwHeader") || // 新增：MwHeader 通常是與容器一起使用的
                className.contains("Response") && psiClass.hasTypeParameters() ||
                className.contains("Request") && psiClass.hasTypeParameters();

        // 檢查是否有泛型參數或者是容器相關的頭部類
        psiClass.getTypeParameters();
        boolean hasTypeParameters = psiClass.getTypeParameters().length > 0;
        boolean isHeaderClass = className.contains("Header");

        // 同時滿足名稱特徵和泛型參數條件，或者是頭部類
        return (hasContainerName && hasTypeParameters) || isHeaderClass;
    }

    private static void processNestedClassesBasedOnReferences() {
        System.out.println("執行processNestedClassesBasedOnReferences，引用關係數量: " + CLASS_REFERENCES.size());

        // 1. 找出所有主類 (包含Req或Resp後綴的類)
        Map<String, String> mainClasses = new HashMap<>(); // 類名 -> 電文代號前綴
        Map<String, String> mainClassSuffixes = new HashMap<>(); // 類名 -> 使用的後綴
        Map<String, Boolean> isRequestMap = new HashMap<>();

        for (Map.Entry<String, String> entry : CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.entrySet()) {
            String className = entry.getKey();
            String tsInterfaceName = entry.getValue();

            // 檢查電文相關後綴
            String suffix = "";
            boolean isRequest = false;

            // 檢查請求類後綴
            for (String reqSuffix : CommonUtils.getSettings().getRequestDtoSuffixes()) {
                if (tsInterfaceName.endsWith(reqSuffix)) {
                    suffix = reqSuffix;
                    isRequest = true;
                    break;
                }
            }

            // 檢查響應類後綴
            if (suffix.isEmpty()) {
                for (String respSuffix : CommonUtils.getSettings().getResponseDtoSuffixes()) {
                    if (tsInterfaceName.endsWith(respSuffix)) {
                        suffix = respSuffix;
                        isRequest = false;
                        break;
                    }
                }
            }

            // 提取電文代號前綴
            if (!suffix.isEmpty()) {
                int suffixIndex = tsInterfaceName.lastIndexOf(suffix);
                if (suffixIndex > 0) {
                    String prefix = tsInterfaceName.substring(0, suffixIndex);
                    mainClasses.put(className, prefix);
                    mainClassSuffixes.put(className, suffix);
                    isRequestMap.put(className, isRequest);

                    System.out.println("找到主類: " + className + " -> " + tsInterfaceName +
                            ", 前綴: " + prefix + ", 後綴: " + suffix +
                            ", 是請求類: " + isRequest);
                }
            }
        }

        System.out.println("找到主類數量: " + mainClasses.size() + ", 內容: " + mainClasses);
        System.out.println("主類後綴: " + mainClassSuffixes);

        // 2. 為每個被主類引用的類應用相同的命名規則
        Map<String, String> renameMap = new HashMap<>();

        for (String mainClass : mainClasses.keySet()) {
            String prefix = mainClasses.get(mainClass);
            boolean isRequest = isRequestMap.get(mainClass);
            String exactSuffix = mainClassSuffixes.get(mainClass);

            // 獲取此主類引用的所有類
            Set<String> referencedClasses = CLASS_REFERENCES.getOrDefault(mainClass, new HashSet<>());

            for (String referencedClass : referencedClasses) {
                // 跳過標準庫類
                if (referencedClass.startsWith("java.") || referencedClass.startsWith("javax.")) {
                    continue;
                }

                String simpleClassName = referencedClass.substring(referencedClass.lastIndexOf('.') + 1);

                // 提取唯一部分
                String uniquePart = extractUniqueClassPart(simpleClassName);

                // 生成新的介面名稱，使用與主類相同的後綴
                String newInterfaceName = prefix + exactSuffix + uniquePart;

                // 記錄重命名映射
                renameMap.put(referencedClass, newInterfaceName);
                System.out.println("將重命名嵌套類: " + referencedClass + " -> " + newInterfaceName);
            }

            // 遞歸處理更深層的嵌套類，傳遞確切後綴
            processNestedClassesRecursively(referencedClasses, prefix, exactSuffix, renameMap);
        }

        // 3. 更新介面名稱映射
        for (Map.Entry<String, String> entry : renameMap.entrySet()) {
            CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 遞歸處理嵌套類引用關係，使用確切的後綴
     */
    private static void processNestedClassesRecursively(Set<String> parentClasses, String prefix,
            String exactSuffix, Map<String, String> renameMap) {
        // 收集下一層的嵌套類
        Set<String> nextLevelClasses = new HashSet<>();

        for (String parentClass : parentClasses) {
            // 獲取此類引用的所有類
            Set<String> referencedClasses = CLASS_REFERENCES.getOrDefault(parentClass, new HashSet<>());

            for (String referencedClass : referencedClasses) {
                // 跳過標準庫類
                if (referencedClass.startsWith("java.") || referencedClass.startsWith("javax.")) {
                    continue;
                }

                // 避免處理已經處理過的類
                if (renameMap.containsKey(referencedClass)) {
                    continue;
                }

                String simpleClassName = referencedClass.substring(referencedClass.lastIndexOf('.') + 1);

                // 提取唯一部分
                String uniquePart = extractUniqueClassPart(simpleClassName);

                // 生成新的介面名稱，使用相同的後綴
                String newInterfaceName = prefix + exactSuffix + uniquePart;

                // 記錄重命名映射
                renameMap.put(referencedClass, newInterfaceName);
                System.out.println("遞歸重命名嵌套類: " + referencedClass + " -> " + newInterfaceName);

                // 添加到下一層處理
                nextLevelClasses.add(referencedClass);
            }
        }

        // 如果找到了新的類，繼續遞歸處理
        if (!nextLevelClasses.isEmpty()) {
            processNestedClassesRecursively(nextLevelClasses, prefix, exactSuffix, renameMap);
        }
    }

    /**
     * 提取唯一的類名部分 (去除常見前綴和後綴)
     */
    private static String extractUniqueClassPart(String simpleClassName) {

        // 移除常見的前綴和後綴
        String result = simpleClassName
                .replace("QryStatement", "")
                .replace("Tranrq", "")
                .replace("Tranrs", "")
                .replace("Request", "")
                .replace("Response", "")
                .replace("Req", "")
                .replace("Resp", "");

        // 如果完全被移除了，返回原名
        if (result.isEmpty()) {
            return simpleClassName;
        }

        // 確保首字母大寫
        if (Character.isLowerCase(result.charAt(0))) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }

        return result;
    }

    /**
     * 從TypeScript介面名稱中提取電文代號前綴
     */
    private static String extractTransactionCodePrefix(String tsInterfaceName) {
        // 尋找Req或Resp的位置
        int reqIndex = tsInterfaceName.indexOf("Req");
        int respIndex = tsInterfaceName.indexOf("Resp");
        int rqIndex = tsInterfaceName.indexOf("Rq");
        int rsIndex = tsInterfaceName.indexOf("Rs");

        int suffixIndex = -1;
        if (reqIndex > 0) {
            suffixIndex = reqIndex;
        } else if (respIndex > 0) {
            suffixIndex = respIndex;
        } else if (rqIndex > 0) {
            suffixIndex = rqIndex;
        } else if (rsIndex > 0) {
            suffixIndex = rsIndex;
        }

        if (suffixIndex > 0) {
            return tsInterfaceName.substring(0, suffixIndex);
        }

        return "";
    }

    private static void applyRenamingToAllClasses(Map<String, String> allClasses) {
        System.out.println("開始應用重命名...");
        System.out.println("介面名稱映射: " + CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME);

        // 收集所有需要重命名的介面
        Map<String, String> interfaceRenameMap = new HashMap<>();
        Map<String, String> simpleNameRenameMap = new HashMap<>();

        // 收集重命名映射
        for (Map.Entry<String, String> entry : CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.entrySet()) {
            String className = entry.getKey();
            String content = CLASS_NAME_WITH_PACKAGE_2_CONTENT.get(className);

            if (content != null) {
                String currentInterfaceName = extractInterfaceName(content);
                String newInterfaceName = entry.getValue();

                if (currentInterfaceName != null && !currentInterfaceName.equals(newInterfaceName)) {
                    // 記錄重命名映射
                    interfaceRenameMap.put(currentInterfaceName, newInterfaceName);

                    // 添加簡單名稱映射
                    String simpleClassName = getSimpleName(className);
                    simpleNameRenameMap.put(simpleClassName, newInterfaceName);

                    // 更新介面定義
                    content = content.replace("interface " + currentInterfaceName + " {",
                            "interface " + newInterfaceName + " {");

                    // 特別處理介面內容中對自身的引用 - 新增此處理
                    content = content.replace(": " + currentInterfaceName + ";",
                            ": " + newInterfaceName + ";");
                    content = content.replace(": " + currentInterfaceName + "[]",
                            ": " + newInterfaceName + "[]");

                    CLASS_NAME_WITH_PACKAGE_2_CONTENT.put(className, content);
                }
            }
        }

        System.out.println("介面重命名映射: " + interfaceRenameMap);
        System.out.println("簡單名稱重命名映射: " + simpleNameRenameMap);

        // 合併兩個映射以確保捕獲所有可能的引用格式
        Map<String, String> allRenameMap = new HashMap<>(interfaceRenameMap);
        allRenameMap.putAll(simpleNameRenameMap);

        // 更新所有類內容中的引用
        for (Map.Entry<String, String> entry : new HashMap<>(allClasses).entrySet()) {
            String className = entry.getKey();
            String content = entry.getValue();
            boolean updated = false;

            for (Map.Entry<String, String> renameEntry : allRenameMap.entrySet()) {
                String oldName = renameEntry.getKey();
                String newName = renameEntry.getValue();

                // 檢查是否存在引用
                if (content.contains(oldName)) {
                    System.out.println("在 " + className + " 中發現對 " + oldName + " 的引用");
                }

                // 使用全面的引用替換模式
                updated |= replaceAllReferences(content, oldName, newName, className);

                // 特別處理直接類型引用
                if (content.contains(": " + oldName)) {
                    System.out.println("替換類型引用: " + oldName + " -> " + newName + " in " + className);
                    content = content.replace(": " + oldName, ": " + newName);
                    updated = true;
                }

                // 數組類型引用
                if (content.contains(": " + oldName + "[]")) {
                    content = content.replace(": " + oldName + "[]", ": " + newName + "[]");
                    updated = true;
                }

                // 可選屬性引用
                if (content.contains("?: " + oldName)) {
                    content = content.replace("?: " + oldName, "?: " + newName);
                    updated = true;
                }

                // 泛型參數引用
                if (content.contains("<" + oldName + ">")) {
                    content = content.replace("<" + oldName + ">", "<" + newName + ">");
                    updated = true;
                }

                // 泛型列表參數
                if (content.contains("<" + oldName + ",")) {
                    content = content.replace("<" + oldName + ",", "<" + newName + ",");
                    updated = true;
                }
                if (content.contains(", " + oldName + ">")) {
                    content = content.replace(", " + oldName + ">", ", " + newName + ">");
                    updated = true;
                }
            }

            if (updated) {
                System.out.println("更新了 " + className + " 的內容");
                allClasses.put(className, content);
                CLASS_NAME_WITH_PACKAGE_2_CONTENT.put(className, content);
            }
        }

        // 最終檢查 - 確保所有內容都被更新
        for (Map.Entry<String, String> entry : allClasses.entrySet()) {
            String content = entry.getValue();
            for (String oldName : allRenameMap.keySet()) {
                if (content.contains(oldName)) {
                    System.out.println("警告: " + entry.getKey() + "中仍存在未替換的類型: " + oldName);
                }
            }
        }
    }

    /**
     * 使用正則表達式全面替換引用
     */
    private static boolean replaceAllReferences(String content, String oldName, String newName, String className) {
        boolean updated = false;
        String originalContent = content;

        // 使用正則表達式模式匹配各種引用形式
        String[][] patterns = {
                { "([^\\w])(", oldName, ")(\\s*[;,:\\[\\]])" }, // 普通類型引用
                { "([<,]\\s*)(", oldName, ")(\\s*[,>])" }, // 泛型參數
                { "(:\\s*)(", oldName, ")(\\[\\])" }, // 數組類型
                { "(\\?:\\s*)(", oldName, ")(\\s*[;,])" }, // 可選屬性
                { "(extends\\s+)(", oldName, ")(\\s|\\{)" }, // 繼承
                { "(implements\\s+)(", oldName, ")(\\s|\\{)" } // 實現
        };

        for (String[] patternParts : patterns) {
            String pattern = patternParts[0] + patternParts[1] + patternParts[2];
            String replacement = patternParts[0] + newName + patternParts[2];
            content = content.replaceAll(pattern, replacement);
        }

        // 檢查是否進行了替換
        if (!content.equals(originalContent)) {
            updated = true;
        }

        return updated;
    }

    /**
     * 獲取類名的簡單名稱（沒有包名的部分）
     */
    private static String getSimpleName(String classNameWithPackage) {
        int lastDotIndex = classNameWithPackage.lastIndexOf('.');
        return lastDotIndex > 0 ? classNameWithPackage.substring(lastDotIndex + 1) : classNameWithPackage;
    }

    /**
     * 從介面名稱中提取確切使用的後綴
     */
    private static String extractExactSuffix(String interfaceName) {
        for (String reqSuffix : CommonUtils.getSettings().getRequestDtoSuffixes()) {
            if (interfaceName.endsWith(reqSuffix)) {
                return reqSuffix;
            }
        }

        for (String respSuffix : CommonUtils.getSettings().getResponseDtoSuffixes()) {
            if (interfaceName.endsWith(respSuffix)) {
                return respSuffix;
            }
        }

        // 檢查常見後綴
        if (interfaceName.endsWith("Req"))
            return "Req";
        if (interfaceName.endsWith("Rq"))
            return "Rq";
        if (interfaceName.endsWith("Resp"))
            return "Resp";
        if (interfaceName.endsWith("Rs"))
            return "Rs";

        return "";
    }

    /**
     * 清空所有緩存
     */
    public static void clearCache() {
        SUCCESS_CANONICAL_TEXT.clear();
        CLASS_NAME_WITH_PACKAGE_2_CONTENT.clear();
        CREATE_TYPESCRIPT_CONTENT_FOR_SINGLE_PSI_CLASS_ENTRY.clear();
        CLASS_NAME_WITH_PACKAGE_2_TS_INTERFACE_NAME.clear();
        CLASS_TRANSACTION_CODE_MAP.clear();
        CLASS_NAME_WITH_PACKAGE_2_TYPESCRIPT_COMMENT.clear();
        CLASS_REFERENCES.clear();
        REFERENCED_BY.clear();
    }

    /**
     * 分析一個類是請求還是響應，基於它在方法中的使用方式
     * 
     * @param project  當前項目
     * @param psiClass 待分析的類
     * @return true表示是請求類，false表示是響應類
     */
    private static ClassUsageInfo analyzeClassUsage(Project project, PsiClass psiClass) {
        ClassUsageInfo usageInfo = new ClassUsageInfo();
        usageInfo.className = psiClass.getQualifiedName();

        // 如果類名明確包含標識，優先使用命名規則判斷（保留舊邏輯作為備用）
        String simpleName = psiClass.getName();
        if (simpleName.endsWith("Tranrq") ||
                simpleName.endsWith("Request") ||
                simpleName.endsWith("Req")) {
            usageInfo.nameBasedIsRequest = true;
            usageInfo.hasClearNamingPattern = true;
        } else if (simpleName.endsWith("Tranrs") ||
                simpleName.endsWith("Response") ||
                simpleName.endsWith("Resp") ||
                simpleName.endsWith("Rs")) {
            usageInfo.nameBasedIsRequest = false;
            usageInfo.hasClearNamingPattern = true;
        }

        // 查找類的所有引用
        PsiReference[] references = findReferences(project, psiClass);
        System.out.println("分析類 " + simpleName + " 的使用情境，找到 " + references.length + " 處引用");

        for (PsiReference reference : references) {
            PsiElement element = reference.getElement();

            // 檢查是否用作參數
            boolean isParameter = checkIfUsedAsParameter(element);
            if (isParameter) {
                usageInfo.usedAsParameterCount++;
                System.out.println("  - 作為參數使用於: " + getContainingMethodDescription(element));
            }

            // 檢查是否用作返回值
            boolean isReturnValue = checkIfUsedAsReturnValue(element);
            if (isReturnValue) {
                usageInfo.usedAsReturnValueCount++;
                System.out.println("  - 作為返回值使用於: " + getContainingMethodDescription(element));
            }

            // 檢查是否在控制器方法中
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

        // 進行最終判斷
        determineClassRole(usageInfo);
        System.out.println("類 " + simpleName + " 分析結果: " +
                (usageInfo.isRequest ? "Request" : "Response") +
                " (參數使用次數: " + usageInfo.usedAsParameterCount +
                ", 返回值使用次數: " + usageInfo.usedAsReturnValueCount + ")");

        return usageInfo;
    }

    /**
     * 基於使用情境信息確定類的角色
     */
    private static void determineClassRole(ClassUsageInfo usageInfo) {
        // 情況1: 有明確的控制器使用情境
        if (usageInfo.usedAsControllerParameterCount > 0 || usageInfo.usedAsControllerReturnValueCount > 0) {
            // 控制器參數通常是請求，返回值通常是響應
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
            // 參數通常是請求，返回值通常是響應
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

        // 情況4: 無法判斷，使用默認值
        // 檢查是否有關聯類
        if (usageInfo.className != null && usageInfo.className.contains("Tranrq")) {
            usageInfo.isRequest = true;
            usageInfo.confidence = "LOW";
        } else if (usageInfo.className != null && usageInfo.className.contains("Tranrs")) {
            usageInfo.isRequest = false;
            usageInfo.confidence = "LOW";
        } else {
            // 默認假設為請求類（可根據項目情況調整）
            usageInfo.isRequest = true;
            usageInfo.confidence = "VERY_LOW";
        }
    }

    /**
     * 檢查元素是否在方法參數中使用
     */
    private static boolean checkIfUsedAsParameter(PsiElement element) {
        // 向上查找父元素
        PsiElement parent = element.getParent();
        while (parent != null) {
            // 檢查是否是參數列表
            if (parent instanceof PsiParameterList) {
                return true;
            }
            // 檢查是否是參數
            if (parent instanceof PsiParameter) {
                return true;
            }
            // 如果已經到達方法體，則不是參數
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
    private static boolean checkIfUsedAsReturnValue(PsiElement element) {
        // 向上查找父元素
        PsiElement parent = element.getParent();
        while (parent != null) {
            // 檢查是否是返回語句
            if (parent instanceof PsiReturnStatement) {
                return true;
            }
            // 檢查是否是方法返回類型
            if (parent instanceof PsiTypeElement &&
                    parent.getParent() instanceof PsiMethod) {
                return true;
            }
            // 如果已經到達方法體或類，則停止
            if (parent instanceof PsiMethod || parent instanceof PsiClass) {
                break;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * 檢查方法是否為控制器方法（有@RequestMapping等註解）
     */
    private static boolean isControllerMethod(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && (qualifiedName.contains("RequestMapping") ||
                    qualifiedName.contains("GetMapping") ||
                    qualifiedName.contains("PostMapping") ||
                    qualifiedName.contains("PutMapping") ||
                    qualifiedName.contains("DeleteMapping") ||
                    qualifiedName.contains("PatchMapping"))) {
                return true;
            }
        }

        // 檢查類是否有@Controller或@RestController註解
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
            for (PsiAnnotation annotation : containingClass.getAnnotations()) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName != null && (qualifiedName.contains("Controller") ||
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
    private static String getContainingMethodDescription(PsiElement element) {
        PsiMethod method = findEnclosingMethod(element);
        if (method != null) {
            PsiClass containingClass = method.getContainingClass();
            String className = containingClass != null ? containingClass.getName() : "unknown";
            return className + "." + method.getName();
        }
        return "unknown method";
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
        String confidence = "NONE"; // 判斷的置信度: VERY_LOW, LOW, MEDIUM, HIGH
    }
}
