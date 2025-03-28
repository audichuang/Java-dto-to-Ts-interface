package org.freeone.javabean.tsinterface.marker;

import com.intellij.codeInsight.daemon.GutterIconDescriptor;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
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
        // 通常 DTO 類是普通 Java 類，不是接口、枚舉或者註解
        if (psiClass.isInterface() || psiClass.isEnum() || psiClass.isAnnotationType()) {
            return false;
        }

        // 通常 DTO 類名稱包含一些特定的模式
        String className = psiClass.getName();
        if (className == null) {
            return false;
        }

        // 添加標準 DTO 後綴檢查
        if (className.endsWith("DTO")
                || className.endsWith("Dto")
                || className.endsWith("Request")
                || className.endsWith("Response")
                || className.endsWith("Rq")
                || className.endsWith("Rs")
                || className.endsWith("Tranrq")
                || className.endsWith("Tranrs")
                || className.endsWith("Req")
                || className.endsWith("Resp")
                || className.endsWith("Detail")
                || className.endsWith("BalanceDetail")
                || className.endsWith("Entity")
                || className.endsWith("Qry")
                || className.endsWith("Query")
                || className.endsWith("Model")
                || className.endsWith("Info")
                || className.endsWith("Data")
                || className.endsWith("Bean")
                || className.endsWith("VO")
                || className.endsWith("Vo")) {
            return true;
        }

        // 檢查類中是否存在公開的字段或者 getter/setter 方法
        // 通常 DTO 類會包含多個公開字段或 getter/setter 方法
        PsiField[] fields = psiClass.getFields();
        if (fields.length > 0) {
            int publicFieldCount = 0;
            for (PsiField field : fields) {
                if (field.hasModifierProperty(PsiModifier.PUBLIC)) {
                    publicFieldCount++;
                }
            }
            // 如果有多個公開字段，可能是數據傳輸對象
            if (publicFieldCount > 2) {
                return true;
            }
        }

        // 檢查 getter/setter 方法
        PsiMethod[] methods = psiClass.getMethods();
        int getterSetterCount = 0;
        for (PsiMethod method : methods) {
            String methodName = method.getName();
            if ((methodName.startsWith("get") || methodName.startsWith("set")) &&
                    methodName.length() > 3 &&
                    Character.isUpperCase(methodName.charAt(3))) {
                getterSetterCount++;
            }
        }
        // 如果有多個 getter/setter 方法，可能是 DTO
        return getterSetterCount > 3;
    }
}