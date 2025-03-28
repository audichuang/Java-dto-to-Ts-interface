package org.freeone.javabean.tsinterface.util;

import com.intellij.lang.jvm.JvmClassKind;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.javadoc.PsiDocComment;
import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceSettingsState;

import java.util.*;
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

    public static void processPsiClass(Project project, PsiClass selectedClass, boolean needDefault) {

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
        List<String> contentList = new ArrayList<>();
        String qualifiedName = selectedClass.getQualifiedName();

        // 創建用於排序的列表，使用Map保存類名和內容
        Map<String, String> requestClasses = new HashMap<>();
        Map<String, String> requestDependencyClasses = new HashMap<>();
        Map<String, String> responseClasses = new HashMap<>();
        Map<String, String> otherClasses = new HashMap<>();

        // 過濾並分類
        for (String classNameWithPackage : SUCCESS_CANONICAL_TEXT) {
            String content = CLASS_NAME_WITH_PACKAGE_2_CONTENT.get(classNameWithPackage);
            if (content == null || content.length() == 0) {
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

        // 添加排序後的類到結果列表
        // 1. 請求類
        contentList.addAll(requestClasses.values());
        // 2. 請求類依賴的類
        contentList.addAll(requestDependencyClasses.values());
        // 3. 響應類
        contentList.addAll(responseClasses.values());
        // 4. 其他類
        contentList.addAll(otherClasses.values());

        return String.join("\n", contentList);
    }

    public static void clearCache() {
        SUCCESS_CANONICAL_TEXT.clear();
        CLASS_NAME_WITH_PACKAGE_2_CONTENT.clear();
        CREATE_TYPESCRIPT_CONTENT_FOR_SINGLE_PSI_CLASS_ENTRY.clear();
    }

    /**
     * 为单独的class创建内容
     *
     * @param psiClass
     * @return
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
                return classNameWithoutPackage;
            }
            // 避免递归调用死循环
            if (CREATE_TYPESCRIPT_CONTENT_FOR_SINGLE_PSI_CLASS_ENTRY.contains(classNameWithPackage)) {
                return classNameWithoutPackage;
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

            PsiField[] fields = psiClass.getAllFields();
            if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().ignoreParentField) {
                fields = psiClass.getFields();
            }
            PsiMethod[] allMethods = psiClass.getAllMethods();
            if (classKind.equals(JvmClassKind.CLASS)) {
                contentBuilder.append("interface ").append(classNameWithoutPackage).append(" {\n");
                for (int i = 0; i < fields.length; i++) {
                    PsiField fieldItem = fields[i];

                    // 檢查是否需要忽略 serialVersionUID
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

                    // 根據設置決定是否添加可選標記
                    String fieldSplitTag = REQUIRE_SPLIT_TAG; // 默認使用冒號（:）

                    // 只有在啟用添加可選標記的設置時，才會添加問號
                    if (JavaBeanToTypescriptInterfaceSettingsState.getInstance().addOptionalMarkToAllFields) {
                        fieldSplitTag = NOT_REQUIRE_SPLIT_TAG;
                        // 如果字段有必填註解，則使用冒號
                        if (CommonUtils.isFieldRequire(fieldItem.getAnnotations())) {
                            fieldSplitTag = REQUIRE_SPLIT_TAG;
                        }
                    }

                    String typeString;
                    PsiType fieldType = fieldItem.getType();
                    typeString = getTypeString(project, fieldType);

                    // 統一格式化文檔註解
                    if (documentText.trim().length() > 0) {
                        // 提取註解中的有效內容
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
                contentBuilder.append("type ").append(classNameWithoutPackage).append(" = ");
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
                // 統一格式化類註解
                String classComment = extractCommentContent(classDocComment.getText());
                if (!classComment.isEmpty()) {
                    String formattedComment = "/**\n * " + classComment + "\n */\n";
                    CLASS_NAME_WITH_PACKAGE_2_TYPESCRIPT_COMMENT.put(classNameWithPackage, formattedComment);
                }
            }
            CLASS_NAME_WITH_PACKAGE_2_CONTENT.put(classNameWithPackage, content);

            return classNameWithoutPackage;
        } else {
            return "any";
        }
    }

    /**
     * 从fieldType中获取类型
     *
     * @param project
     * @param fieldType
     * @return
     */
    private static String getTypeString(Project project, PsiType fieldType) {

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
            // 檢查類型是否來自標準庫
            String canonicalText = fieldType.getCanonicalText();
            if (canonicalText.startsWith("java.time.") ||
                    canonicalText.endsWith(".LocalDate") ||
                    canonicalText.endsWith(".LocalTime") ||
                    canonicalText.endsWith(".LocalDateTime")) {
                // 對於標準庫日期時間類型，使用 any 替代
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
                    // 類似 PageModel<Student>
                    typeString = psiClassReferenceType.getPresentableText();
                } else {
                    // 普通類
                    PsiClass resolve = psiClassReferenceType.resolve();
                    typeString = createTypescriptContentForSinglePsiClass(project, resolve);
                }

            } else {
                PsiClass filedClass = CommonUtils.findPsiClass(project, fieldType);
                typeString = createTypescriptContentForSinglePsiClass(project, filedClass);
            }

        }
        return typeString;
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

                    defaultVType = getTypeString(project, vType);
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

                    String firstTsTypeForArray = getTypeString(project, deepComponentType);
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
        // 移除 Java 文檔註解標記
        String content = comment.trim()
                .replaceAll("/\\*\\*", "") // 移除開頭的 /**
                .replaceAll("\\*/", "") // 移除結尾的 */
                .replaceAll("^\\s*\\*\\s*", "") // 移除每行開頭的 * 及其前後空格
                .replaceAll("\\n\\s*\\*\\s*", " ") // 將多行註解合併為單行，移除行開頭的 * 及空格
                .trim();

        // 如果有 @param、@return 等標記，只保留主要描述
        if (content.contains("@")) {
            content = content.split("@")[0].trim();
        }

        return content;
    }
}
