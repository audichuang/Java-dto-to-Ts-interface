package org.freeone.javabean.tsinterface.marker;

import com.intellij.codeInsight.daemon.GutterIconDescriptor;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceProjectSettings;
import org.freeone.javabean.tsinterface.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 在包含 DTO 類的方法上顯示行標記
 */
public class DtoTypeScriptInterfaceLineMarkerProvider extends LineMarkerProviderDescriptor {

    @Override
    public String getName() {
        return "DTO TypeScript Interface Generator";
    }

    @Override
    public @Nullable Icon getIcon() {
        // 使用內置圖標，後續可以替換為自定義圖標
        return com.intellij.icons.AllIcons.Nodes.Interface;
    }

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        // 只處理方法聲明
        if (!(element instanceof PsiMethod)) {
            return null;
        }

        PsiMethod method = (PsiMethod) element;

        // 檢查是否是控制器方法
        boolean isControllerMethod = isControllerMethod(method);
        if (!isControllerMethod) {
            return null;
        }

        // 收集 DTO 類
        List<PsiClass> dtoClasses = collectDtoClasses(method);
        if (dtoClasses.isEmpty()) {
            return null;
        }

        // 創建行標記
        return new LineMarkerInfo<>(
                method.getNameIdentifier() != null ? method.getNameIdentifier() : element,
                method.getNameIdentifier() != null ? method.getNameIdentifier().getTextRange() : element.getTextRange(),
                getIcon(),
                psiElement -> "生成 DTO 的 TypeScript 接口",
                null,
                GutterIconRenderer.Alignment.CENTER,
                () -> "生成 DTO 的 TypeScript 接口");
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
            @NotNull Collection<? super LineMarkerInfo<?>> result) {
        // 由於我們已經在 getLineMarkerInfo 中處理了標記，這裡不需要額外實現
    }

    /**
     * 檢查方法是否是控制器方法
     */
    private boolean isControllerMethod(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && (qualifiedName.endsWith("RequestMapping") ||
                    qualifiedName.endsWith("GetMapping") ||
                    qualifiedName.endsWith("PostMapping") ||
                    qualifiedName.endsWith("PutMapping") ||
                    qualifiedName.endsWith("DeleteMapping"))) {
                return true;
            }
        }

        // 檢查類上的註解
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
            PsiAnnotation[] classAnnotations = containingClass.getAnnotations();
            for (PsiAnnotation annotation : classAnnotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName != null && (qualifiedName.endsWith("Controller") ||
                        qualifiedName.endsWith("RestController"))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 收集方法中的 DTO 類
     */
    private List<PsiClass> collectDtoClasses(PsiMethod method) {
        List<PsiClass> dtoClasses = new ArrayList<>();

        // 檢查返回類型
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            addDtoClassesFromType(returnType, dtoClasses);
        }

        // 檢查參數類型
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
            PsiType parameterType = parameter.getType();
            addDtoClassesFromType(parameterType, dtoClasses);
        }

        return dtoClasses;
    }

    /**
     * 從類型中提取 DTO 類
     */
    private void addDtoClassesFromType(PsiType type, List<PsiClass> dtoClasses) {
        // 處理泛型類型
        if (type instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) type;
            PsiClass psiClass = classType.resolve();

            if (psiClass != null && isDtoClass(psiClass)) {
                dtoClasses.add(psiClass);
            }

            // 處理泛型參數
            PsiType[] parameters = classType.getParameters();
            for (PsiType parameter : parameters) {
                addDtoClassesFromType(parameter, dtoClasses);
            }
        }
    }

    /**
     * 判斷一個類是否為 DTO 類
     */
    private boolean isDtoClass(PsiClass psiClass) {
        // 確保不是接口、枚舉或註解
        if (psiClass.isInterface() || psiClass.isEnum() || psiClass.isAnnotationType()) {
            return false;
        }

        // 獲取類名
        String className = psiClass.getName();
        if (className == null) {
            return false;
        }

        System.out.println("檢查類是否為DTO: " + className);

        // 大小寫不敏感的檢查
        String lowerClassName = className.toLowerCase();

        // 檢查是否包含Qrystatement、Query或Qry
        if (lowerClassName.contains("qrystatement") ||
                lowerClassName.contains("query") ||
                lowerClassName.contains("qry")) {
            System.out.println("  匹配到查詢關鍵字: " + className);
            return true;
        }

        // 獲取項目設置
        Project project = psiClass.getProject();
        JavaBeanToTypescriptInterfaceProjectSettings settings = CommonUtils.getProjectSettings(project);

        // 輸出調試信息
        System.out.println("  當前項目設置中請求DTO後綴: " + settings.getEffectiveRequestDtoSuffixes());
        System.out.println("  當前項目設置中響應DTO後綴: " + settings.getEffectiveResponseDtoSuffixes());

        // 檢查類名是否包含配置的請求DTO後綴
        for (String suffix : settings.getEffectiveRequestDtoSuffixes()) {
            if (className.endsWith(suffix)) {
                System.out.println("  匹配到請求DTO後綴 " + suffix + ": " + className);
                return true;
            }
        }

        // 檢查類名是否包含配置的響應DTO後綴
        for (String suffix : settings.getEffectiveResponseDtoSuffixes()) {
            if (className.endsWith(suffix)) {
                System.out.println("  匹配到響應DTO後綴 " + suffix + ": " + className);
                return true;
            }
        }

        // 檢查是否包含常見的DTO相關詞
        if (lowerClassName.contains("dto") ||
                lowerClassName.contains("model") ||
                lowerClassName.contains("bean") ||
                lowerClassName.contains("vo") ||
                lowerClassName.contains("entity") ||
                lowerClassName.contains("request") ||
                lowerClassName.contains("response") ||
                lowerClassName.contains("result")) {
            System.out.println("  匹配到常見DTO關鍵字: " + className);
            return true;
        }

        // 如果上述檢查都未通過，則檢查類結構
        boolean isDto = checkClassStructure(psiClass);
        System.out.println("  基於類結構判斷 " + className + " 是否為DTO: " + isDto);
        return isDto;
    }

    /**
     * 根據類結構判斷是否為DTO類
     */
    private boolean checkClassStructure(PsiClass psiClass) {
        // 獲取所有公共字段
        PsiField[] fields = psiClass.getFields();
        int publicFieldCount = 0;
        for (PsiField field : fields) {
            if (field.hasModifierProperty(PsiModifier.PUBLIC)) {
                publicFieldCount++;
            }
        }

        System.out.println("  類 " + psiClass.getName() + " 公共字段數量: " + publicFieldCount);

        // 獲取所有 getter/setter 方法
        PsiMethod[] methods = psiClass.getMethods();
        int getterSetterCount = 0;
        for (PsiMethod method : methods) {
            String methodName = method.getName();
            if ((methodName.startsWith("get") && methodName.length() > 3 && method.getParameterList().isEmpty())
                    || (methodName.startsWith("set") && methodName.length() > 3
                            && method.getParameterList().getParametersCount() == 1)) {
                getterSetterCount++;
            }
        }

        System.out.println("  類 " + psiClass.getName() + " getter/setter方法數量: " + getterSetterCount);

        // 如果公共字段超過2個或者 getter/setter 方法超過3個，則可能是 DTO 類
        boolean isDto = publicFieldCount > 2 || getterSetterCount > 3;
        System.out.println(
                "  基於結構判斷 " + psiClass.getName() + " 是DTO: " + isDto + " (publicFields > 2 或 getterSetter > 3)");
        return isDto;
    }
}