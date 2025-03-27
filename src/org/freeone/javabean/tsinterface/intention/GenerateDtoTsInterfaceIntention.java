package org.freeone.javabean.tsinterface.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.freeone.javabean.tsinterface.service.DtoTypescriptGeneratorService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 用於檢測方法簽名中的 DTO 類並提供生成 TypeScript 接口的選項
 */
public class GenerateDtoTsInterfaceIntention extends PsiElementBaseIntentionAction implements IntentionAction {

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        // 尋找包含當前元素的方法聲明
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method == null) {
            return;
        }

        // 收集所有需要處理的 DTO 類
        List<PsiClass> dtoClasses = collectDtoClasses(method);
        if (dtoClasses.isEmpty()) {
            return;
        }

        // 生成 TypeScript 接口
        DtoTypescriptGeneratorService.generateTypescriptInterfaces(project, dtoClasses, false);
    }

    /**
     * 收集方法簽名中的所有 DTO 類
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

        return className.endsWith("DTO")
                || className.endsWith("Dto")
                || className.endsWith("Request")
                || className.endsWith("Response")
                || className.endsWith("Rq")
                || className.endsWith("Rs")
                || className.endsWith("Tranrq")
                || className.endsWith("Tranrs");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // 檢查是否在方法聲明中
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method == null) {
            return false;
        }

        // 只對帶有 RequestMapping 相關註解的方法啟用
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

        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return "生成 DTO 的 TypeScript 接口";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Java DTO 到 TypeScript 接口";
    }
}