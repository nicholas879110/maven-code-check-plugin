/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.codeInspection.nullable;

import com.gome.maven.codeInsight.AnnotationUtil;
import com.gome.maven.codeInsight.NullableNotNullManager;
import com.gome.maven.codeInsight.daemon.GroupNames;
import com.gome.maven.codeInsight.daemon.impl.analysis.HighlightControlFlowUtil;
import com.gome.maven.codeInsight.intention.AddAnnotationPsiFix;
import com.gome.maven.codeInsight.intention.impl.AddNotNullAnnotationFix;
import com.gome.maven.codeInspection.*;
import com.gome.maven.codeInspection.dataFlow.DfaPsiUtil;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.codeStyle.JavaCodeStyleManager;
import com.gome.maven.psi.codeStyle.VariableKind;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.searches.OverridingMethodsSearch;
import com.gome.maven.psi.search.searches.ReferencesSearch;
import com.gome.maven.psi.util.*;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.Element;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NullableStuffInspectionBase extends BaseJavaBatchLocalInspectionTool {
    // deprecated fields remain to minimize changes to users inspection profiles (which are often located in version control).
    @Deprecated @SuppressWarnings({"WeakerAccess"}) public boolean REPORT_NULLABLE_METHOD_OVERRIDES_NOTNULL = true;
    @SuppressWarnings({"WeakerAccess"}) public boolean REPORT_NOT_ANNOTATED_METHOD_OVERRIDES_NOTNULL = true;
    @SuppressWarnings({"WeakerAccess"}) public boolean REPORT_NOTNULL_PARAMETER_OVERRIDES_NULLABLE = true;
    @Deprecated @SuppressWarnings({"WeakerAccess"}) public boolean REPORT_NOT_ANNOTATED_PARAMETER_OVERRIDES_NOTNULL = true;
    @SuppressWarnings({"WeakerAccess"}) public boolean REPORT_NOT_ANNOTATED_GETTER = true;
    @SuppressWarnings({"WeakerAccess"}) public boolean IGNORE_EXTERNAL_SUPER_NOTNULL = false;
    @SuppressWarnings({"WeakerAccess"}) public boolean REQUIRE_NOTNULL_FIELDS_INITIALIZED = true;
    @SuppressWarnings({"WeakerAccess"}) public boolean REPORT_NOTNULL_PARAMETERS_OVERRIDES_NOT_ANNOTATED = false;
    @Deprecated @SuppressWarnings({"WeakerAccess"}) public boolean REPORT_NOT_ANNOTATED_SETTER_PARAMETER = true;
    @Deprecated @SuppressWarnings({"WeakerAccess"}) public boolean REPORT_ANNOTATION_NOT_PROPAGATED_TO_OVERRIDERS = true; // remains for test
    @SuppressWarnings({"WeakerAccess"}) public boolean REPORT_NULLS_PASSED_TO_NON_ANNOTATED_METHOD = true;

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInspection.nullable.NullableStuffInspectionBase");

    @Override
    public void writeSettings( Element node) throws WriteExternalException {
        super.writeSettings(node);
        for (Element child : new ArrayList<Element>(node.getChildren())) {
            String name = child.getAttributeValue("name");
            String value = child.getAttributeValue("value");
            if ("IGNORE_EXTERNAL_SUPER_NOTNULL".equals(name) && "false".equals(value) ||
                    "REPORT_NOTNULL_PARAMETERS_OVERRIDES_NOT_ANNOTATED".equals(name) && "false".equals(value) ||
                    "REQUIRE_NOTNULL_FIELDS_INITIALIZED".equals(name) && "true".equals(value)) {
                node.removeContent(child);
            }
        }
    }

    @Override
    
    public PsiElementVisitor buildVisitor( final ProblemsHolder holder, boolean isOnTheFly) {
        if (!PsiUtil.isLanguageLevel5OrHigher(holder.getFile())) {
            return new PsiElementVisitor() { };
        }
        return new JavaElementVisitor() {
            @Override
            public void visitMethod(PsiMethod method) {
                checkNullableStuffForMethod(method, holder);
            }

            @Override
            public void visitField(PsiField field) {
                final PsiType type = field.getType();
                final Annotated annotated = check(field, holder, type);
                if (TypeConversionUtil.isPrimitiveAndNotNull(type)) {
                    return;
                }
                Project project = holder.getProject();
                final NullableNotNullManager manager = NullableNotNullManager.getInstance(project);
                if (annotated.isDeclaredNotNull ^ annotated.isDeclaredNullable) {
                    final String anno = annotated.isDeclaredNotNull ? manager.getDefaultNotNull() : manager.getDefaultNullable();
                    final List<String> annoToRemove = annotated.isDeclaredNotNull ? manager.getNullables() : manager.getNotNulls();

                    if (!AnnotationUtil.isAnnotatingApplicable(field, anno)) {
                        final PsiAnnotation notNull = AnnotationUtil.findAnnotation(field, manager.getNotNulls());
                        final PsiAnnotation nullable = AnnotationUtil.findAnnotation(field, manager.getNullables());
                        final PsiAnnotation annotation;
                        String message = "Not \'";
                        if (annotated.isDeclaredNullable) {
                            message += nullable.getQualifiedName();
                            annotation = nullable;
                        } else {
                            message += notNull.getQualifiedName();
                            annotation = notNull;
                        }
                        message += "\' but \'" + anno + "\' would be used for code generation.";
                        final PsiJavaCodeReferenceElement annotationNameReferenceElement = annotation.getNameReferenceElement();
                        holder.registerProblem(annotationNameReferenceElement != null && annotationNameReferenceElement.isPhysical() ? annotationNameReferenceElement : field.getNameIdentifier(),
                                message,
                                ProblemHighlightType.WEAK_WARNING,
                                new ChangeNullableDefaultsFix(notNull, nullable, manager));
                        return;
                    }

                    String propName = JavaCodeStyleManager.getInstance(project).variableNameToPropertyName(field.getName(), VariableKind.FIELD);
                    final boolean isStatic = field.hasModifierProperty(PsiModifier.STATIC);
                    final PsiMethod getter = PropertyUtil.findPropertyGetter(field.getContainingClass(), propName, isStatic, false);
                    final PsiIdentifier nameIdentifier = getter == null ? null : getter.getNameIdentifier();
                    if (nameIdentifier != null && nameIdentifier.isPhysical()) {
                        if (PropertyUtil.isSimpleGetter(getter)) {
                            AnnotateMethodFix getterAnnoFix = new AnnotateMethodFix(anno, ArrayUtil.toStringArray(annoToRemove)) {
                                @Override
                                public int shouldAnnotateBaseMethod(PsiMethod method, PsiMethod superMethod, Project project) {
                                    return 1;
                                }
                            };
                            if (REPORT_NOT_ANNOTATED_GETTER) {
                                if (!manager.hasNullability(getter) && !TypeConversionUtil.isPrimitiveAndNotNull(getter.getReturnType())) {
                                    holder.registerProblem(nameIdentifier, InspectionsBundle
                                                    .message("inspection.nullable.problems.annotated.field.getter.not.annotated", getPresentableAnnoName(field)),
                                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING, getterAnnoFix);
                                }
                            }
                            if (annotated.isDeclaredNotNull && isNullableNotInferred(getter, false) ||
                                    annotated.isDeclaredNullable && isNotNullNotInferred(getter, false, false)) {
                                holder.registerProblem(nameIdentifier, InspectionsBundle.message(
                                        "inspection.nullable.problems.annotated.field.getter.conflict", getPresentableAnnoName(field), getPresentableAnnoName(getter)),
                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING, getterAnnoFix);
                            }
                        }
                    }

                    final PsiClass containingClass = field.getContainingClass();
                    final PsiMethod setter = PropertyUtil.findPropertySetter(containingClass, propName, isStatic, false);
                    if (setter != null && setter.isPhysical()) {
                        final PsiParameter[] parameters = setter.getParameterList().getParameters();
                        assert parameters.length == 1 : setter.getText();
                        final PsiParameter parameter = parameters[0];
                        LOG.assertTrue(parameter != null, setter.getText());
                        AddAnnotationPsiFix addAnnoFix = new AddAnnotationPsiFix(anno, parameter, PsiNameValuePair.EMPTY_ARRAY, ArrayUtil.toStringArray(annoToRemove));
                        if (REPORT_NOT_ANNOTATED_GETTER && !manager.hasNullability(parameter) && !TypeConversionUtil.isPrimitiveAndNotNull(parameter.getType())) {
                            final PsiIdentifier nameIdentifier1 = parameter.getNameIdentifier();
                            assertValidElement(setter, parameter, nameIdentifier1);
                            holder.registerProblem(nameIdentifier1,
                                    InspectionsBundle.message("inspection.nullable.problems.annotated.field.setter.parameter.not.annotated",
                                            getPresentableAnnoName(field)),
                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                    addAnnoFix);
                        }
                        if (PropertyUtil.isSimpleSetter(setter)) {
                            if (annotated.isDeclaredNotNull && isNullableNotInferred(parameter, false)) {
                                final PsiIdentifier nameIdentifier1 = parameter.getNameIdentifier();
                                assertValidElement(setter, parameter, nameIdentifier1);
                                holder.registerProblem(nameIdentifier1, InspectionsBundle.message(
                                        "inspection.nullable.problems.annotated.field.setter.parameter.conflict",
                                        getPresentableAnnoName(field), getPresentableAnnoName(parameter)),
                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                        addAnnoFix);
                            }
                        }
                    }

                    if (REQUIRE_NOTNULL_FIELDS_INITIALIZED) {
                        if (annotated.isDeclaredNotNull && !HighlightControlFlowUtil.isFieldInitializedAfterObjectConstruction(field)) {
                            final PsiAnnotation annotation = AnnotationUtil.findAnnotation(field, manager.getNotNulls());
                            if (annotation != null) {
                                holder.registerProblem(annotation.isPhysical() ? annotation : field.getNameIdentifier(),
                                        "Not-null fields must be initialized",
                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                            }
                        }
                    }

                    for (PsiExpression rhs : DfaPsiUtil.findAllConstructorInitializers(field)) {
                        if (rhs instanceof PsiReferenceExpression) {
                            PsiElement target = ((PsiReferenceExpression)rhs).resolve();
                            if (target instanceof PsiParameter && target.isPhysical()) {
                                PsiParameter parameter = (PsiParameter)target;
                                AddAnnotationPsiFix fix = new AddAnnotationPsiFix(anno, parameter, PsiNameValuePair.EMPTY_ARRAY, ArrayUtil.toStringArray(annoToRemove));
                                if (REPORT_NOT_ANNOTATED_GETTER && !manager.hasNullability(parameter) && !TypeConversionUtil.isPrimitiveAndNotNull(parameter.getType())) {
                                    final PsiIdentifier nameIdentifier2 = parameter.getNameIdentifier();
                                    assert nameIdentifier2 != null : parameter;
                                    assert nameIdentifier2.isPhysical() : parameter;
                                    holder.registerProblem(nameIdentifier2, InspectionsBundle
                                                    .message("inspection.nullable.problems.annotated.field.constructor.parameter.not.annotated",
                                                            getPresentableAnnoName(field)),
                                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fix);
                                    continue;
                                }
                                if (annotated.isDeclaredNullable && isNotNullNotInferred(parameter, false, false)) {
                                    boolean usedAsQualifier = !ReferencesSearch.search(parameter).forEach(new Processor<PsiReference>() {
                                        @Override
                                        public boolean process(PsiReference reference) {
                                            final PsiElement element = reference.getElement();
                                            return !(element instanceof PsiReferenceExpression && element.getParent() instanceof PsiReferenceExpression);
                                        }
                                    });
                                    if (!usedAsQualifier) {
                                        final PsiIdentifier nameIdentifier2 = parameter.getNameIdentifier();
                                        assert nameIdentifier2 != null : parameter;
                                        holder.registerProblem(nameIdentifier2, InspectionsBundle.message(
                                                "inspection.nullable.problems.annotated.field.constructor.parameter.conflict", getPresentableAnnoName(field),
                                                getPresentableAnnoName(parameter)),
                                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                                fix);
                                    }
                                }

                            }
                        }
                    }
                }
            }

            private void assertValidElement(PsiMethod setter, PsiParameter parameter, PsiIdentifier nameIdentifier1) {
                LOG.assertTrue(nameIdentifier1 != null && nameIdentifier1.isPhysical(), setter.getText());
                LOG.assertTrue(parameter.isPhysical(), setter.getText());
            }

            @Override
            public void visitParameter(PsiParameter parameter) {
                check(parameter, holder, parameter.getType());
            }

            @Override
            public void visitAnnotation(PsiAnnotation annotation) {
                if (!AnnotationUtil.NOT_NULL.equals(annotation.getQualifiedName())) return;

                PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("exception");
                if (value instanceof PsiClassObjectAccessExpression) {
                    PsiClass psiClass = PsiUtil.resolveClassInClassTypeOnly(((PsiClassObjectAccessExpression)value).getOperand().getType());
                    if (psiClass != null && !hasStringConstructor(psiClass)) {
                        holder.registerProblem(value,
                                "Custom exception class should have a constructor with a single message parameter of String type",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING);

                    }
                }
            }

            private boolean hasStringConstructor(PsiClass aClass) {
                for (PsiMethod method : aClass.getConstructors()) {
                    PsiParameterList list = method.getParameterList();
                    if (list.getParametersCount() == 1 &&
                            list.getParameters()[0].getType().equalsToText(CommonClassNames.JAVA_LANG_STRING)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    
    private static String getPresentableAnnoName( PsiModifierListOwner owner) {
        NullableNotNullManager manager = NullableNotNullManager.getInstance(owner.getProject());
        Set<String> names = ContainerUtil.newHashSet(manager.getNullables());
        names.addAll(manager.getNotNulls());

        PsiAnnotation annotation = AnnotationUtil.findAnnotationInHierarchy(owner, names);
        if (annotation != null) return getPresentableAnnoName(annotation);

        String anno = manager.getNotNull(owner);
        return StringUtil.getShortName(anno != null ? anno : StringUtil.notNullize(manager.getNullable(owner), "???"));
    }

    private static String getPresentableAnnoName( PsiAnnotation annotation) {
        return StringUtil.getShortName(StringUtil.notNullize(annotation.getQualifiedName(), "???"));
    }

    private static class Annotated {
        private final boolean isDeclaredNotNull;
        private final boolean isDeclaredNullable;

        private Annotated(final boolean isDeclaredNotNull, final boolean isDeclaredNullable) {
            this.isDeclaredNotNull = isDeclaredNotNull;
            this.isDeclaredNullable = isDeclaredNullable;
        }
    }
    private static Annotated check(final PsiModifierListOwner parameter, final ProblemsHolder holder, PsiType type) {
        final NullableNotNullManager manager = NullableNotNullManager.getInstance(holder.getProject());
        PsiAnnotation isDeclaredNotNull = AnnotationUtil.findAnnotation(parameter, manager.getNotNulls());
        PsiAnnotation isDeclaredNullable = AnnotationUtil.findAnnotation(parameter, manager.getNullables());
        if (isDeclaredNullable != null && isDeclaredNotNull != null) {
            reportNullableNotNullConflict(holder, parameter, isDeclaredNullable, isDeclaredNotNull);
        }
        if ((isDeclaredNotNull != null || isDeclaredNullable != null) && type != null && TypeConversionUtil.isPrimitive(type.getCanonicalText())) {
            PsiAnnotation annotation = isDeclaredNotNull == null ? isDeclaredNullable : isDeclaredNotNull;
            reportPrimitiveType(holder, annotation, annotation, parameter);
        }
        return new Annotated(isDeclaredNotNull != null,isDeclaredNullable != null);
    }

    private static void reportPrimitiveType(final ProblemsHolder holder, final PsiElement psiElement, final PsiAnnotation annotation,
                                            final PsiModifierListOwner listOwner) {
        holder.registerProblem(psiElement.isPhysical() ? psiElement : listOwner.getNavigationElement(),
                InspectionsBundle.message("inspection.nullable.problems.primitive.type.annotation"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new RemoveAnnotationQuickFix(annotation, listOwner));
    }

    @Override
    
    public String getDisplayName() {
        return InspectionsBundle.message("inspection.nullable.problems.display.name");
    }

    @Override
    
    public String getGroupDisplayName() {
        return GroupNames.BUGS_GROUP_NAME;
    }

    @Override
    
    public String getShortName() {
        return "NullableProblems";
    }

    private void checkNullableStuffForMethod(PsiMethod method, final ProblemsHolder holder) {
        Annotated annotated = check(method, holder, method.getReturnType());

        List<PsiMethod> superMethods = ContainerUtil.map(
                method.findSuperMethodSignaturesIncludingStatic(true), new Function<MethodSignatureBackedByPsiMethod, PsiMethod>() {
                    @Override
                    public PsiMethod fun(MethodSignatureBackedByPsiMethod signature) {
                        return signature.getMethod();
                    }
                });

        final NullableNotNullManager nullableManager = NullableNotNullManager.getInstance(holder.getProject());

        checkSupers(method, holder, annotated, superMethods, nullableManager);
        checkParameters(method, holder, superMethods, nullableManager);
        checkOverriders(method, holder, annotated, nullableManager);
    }

    private void checkSupers(PsiMethod method,
                             ProblemsHolder holder,
                             Annotated annotated,
                             List<PsiMethod> superMethods, NullableNotNullManager nullableManager) {
        if (REPORT_NOTNULL_PARAMETER_OVERRIDES_NULLABLE) {
            for (PsiMethod superMethod : superMethods) {
                if (annotated.isDeclaredNullable && isNotNullNotInferred(superMethod, true, false)) {
                    final PsiAnnotation annotation = AnnotationUtil.findAnnotation(method, nullableManager.getNullables(), true);
                    holder.registerProblem(annotation != null ? annotation : method.getNameIdentifier(),
                            InspectionsBundle.message("inspection.nullable.problems.Nullable.method.overrides.NotNull",
                                    getPresentableAnnoName(method), getPresentableAnnoName(superMethod)),
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    break;
                }
            }
        }

        if (REPORT_NOT_ANNOTATED_METHOD_OVERRIDES_NOTNULL) {
            for (PsiMethod superMethod : superMethods) {
                if (!nullableManager.hasNullability(method) && isNotNullNotInferred(superMethod, true, IGNORE_EXTERNAL_SUPER_NOTNULL)) {
                    final String defaultNotNull = nullableManager.getDefaultNotNull();
                    final String[] annotationsToRemove = ArrayUtil.toStringArray(nullableManager.getNullables());
                    final LocalQuickFix fix = AnnotationUtil.isAnnotatingApplicable(method, defaultNotNull)
                            ? createAnnotateMethodFix(defaultNotNull, annotationsToRemove)
                            : createChangeDefaultNotNullFix(nullableManager, superMethod);
                    holder.registerProblem(method.getNameIdentifier(),
                            InspectionsBundle.message("inspection.nullable.problems.method.overrides.NotNull", getPresentableAnnoName(superMethod)),
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            wrapFix(fix));
                    break;
                }
            }
        }
    }

    private void checkParameters(PsiMethod method, ProblemsHolder holder, List<PsiMethod> superMethods, NullableNotNullManager nullableManager) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PsiParameter parameter = parameters[i];

            List<PsiParameter> superParameters = ContainerUtil.newArrayList();
            for (PsiMethod superMethod : superMethods) {
                PsiParameter[] _superParameters = superMethod.getParameterList().getParameters();
                if (_superParameters.length == parameters.length) {
                    superParameters.add(_superParameters[i]);
                }
            }

            if (REPORT_NOTNULL_PARAMETER_OVERRIDES_NULLABLE) {
                for (PsiParameter superParameter : superParameters) {
                    if (isNotNullNotInferred(parameter, false, false) &&
                            isNullableNotInferred(superParameter, false)) {
                        final PsiAnnotation annotation = AnnotationUtil.findAnnotation(parameter, nullableManager.getNotNulls(), true);
                        holder.registerProblem(annotation != null ? annotation : parameter.getNameIdentifier(),
                                InspectionsBundle.message("inspection.nullable.problems.NotNull.parameter.overrides.Nullable",
                                        getPresentableAnnoName(parameter),
                                        getPresentableAnnoName(superParameter)),
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                        break;
                    }
                }
            }
            if (REPORT_NOT_ANNOTATED_METHOD_OVERRIDES_NOTNULL) {
                for (PsiParameter superParameter : superParameters) {
                    if (!nullableManager.hasNullability(parameter) && isNotNullNotInferred(superParameter, false, IGNORE_EXTERNAL_SUPER_NOTNULL)) {
                        final LocalQuickFix fix = AnnotationUtil.isAnnotatingApplicable(parameter, nullableManager.getDefaultNotNull())
                                ? new AddNotNullAnnotationFix(parameter)
                                : createChangeDefaultNotNullFix(nullableManager, superParameter);
                        holder.registerProblem(parameter.getNameIdentifier(),
                                InspectionsBundle.message("inspection.nullable.problems.parameter.overrides.NotNull", getPresentableAnnoName(superParameter)),
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                wrapFix(fix));
                        break;
                    }
                }
            }
            if (REPORT_NOTNULL_PARAMETERS_OVERRIDES_NOT_ANNOTATED) {
                for (PsiParameter superParameter : superParameters) {
                    if (!nullableManager.hasNullability(superParameter) && isNotNullNotInferred(parameter, false, false)) {
                        PsiAnnotation notNullAnnotation = nullableManager.getNotNullAnnotation(parameter, false);
                        assert notNullAnnotation != null;
                        boolean physical = PsiTreeUtil.isAncestor(parameter, notNullAnnotation, true);
                        final LocalQuickFix fix = physical ? new RemoveAnnotationQuickFix(notNullAnnotation, parameter) : null;
                        holder.registerProblem(physical ? notNullAnnotation : parameter.getNameIdentifier(),
                                InspectionsBundle.message("inspection.nullable.problems.NotNull.parameter.overrides.not.annotated", getPresentableAnnoName(parameter)),
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                wrapFix(fix));
                        break;
                    }
                }
            }
        }
    }

    private void checkOverriders(PsiMethod method,
                                 ProblemsHolder holder,
                                 Annotated annotated,
                                 NullableNotNullManager nullableManager) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (REPORT_ANNOTATION_NOT_PROPAGATED_TO_OVERRIDERS) {
            boolean[] parameterAnnotated = new boolean[parameters.length];
            boolean[] parameterQuickFixSuggested = new boolean[parameters.length];
            boolean hasAnnotatedParameter = false;
            for (int i = 0; i < parameters.length; i++) {
                PsiParameter parameter = parameters[i];
                parameterAnnotated[i] = isNotNullNotInferred(parameter, false, false);
                hasAnnotatedParameter |= parameterAnnotated[i];
            }
            if (hasAnnotatedParameter || annotated.isDeclaredNotNull) {
                PsiManager manager = method.getManager();
                final String defaultNotNull = nullableManager.getDefaultNotNull();
                final boolean superMethodApplicable = AnnotationUtil.isAnnotatingApplicable(method, defaultNotNull);
                PsiMethod[] overridings =
                        OverridingMethodsSearch.search(method, GlobalSearchScope.allScope(manager.getProject()), true).toArray(PsiMethod.EMPTY_ARRAY);
                boolean methodQuickFixSuggested = false;
                for (PsiMethod overriding : overridings) {
                    if (!manager.isInProject(overriding)) continue;

                    final boolean applicable = AnnotationUtil.isAnnotatingApplicable(overriding, defaultNotNull);
                    if (!methodQuickFixSuggested
                            && annotated.isDeclaredNotNull
                            && !isNotNullNotInferred(overriding, false, false)
                            && (isNullableNotInferred(overriding, false) || !isNullableNotInferred(overriding, true))) {
                        method.getNameIdentifier(); //load tree
                        PsiAnnotation annotation = AnnotationUtil.findAnnotation(method, nullableManager.getNotNulls());
                        final String[] annotationsToRemove = ArrayUtil.toStringArray(nullableManager.getNullables());

                        final LocalQuickFix fix;
                        if (applicable) {
                            fix = new MyAnnotateMethodFix(defaultNotNull, annotationsToRemove);
                        }
                        else {
                            fix = superMethodApplicable ? null : createChangeDefaultNotNullFix(nullableManager, method);
                        }

                        PsiElement psiElement = annotation;
                        if (!annotation.isPhysical()) {
                            psiElement = method.getNameIdentifier();
                            if (psiElement == null) continue;
                        }
                        holder.registerProblem(psiElement, InspectionsBundle.message("nullable.stuff.problems.overridden.methods.are.not.annotated"),
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                wrapFix(fix));
                        methodQuickFixSuggested = true;
                    }
                    if (hasAnnotatedParameter) {
                        PsiParameter[] psiParameters = overriding.getParameterList().getParameters();
                        for (int i = 0; i < psiParameters.length; i++) {
                            if (parameterQuickFixSuggested[i]) continue;
                            PsiParameter parameter = psiParameters[i];
                            if (parameterAnnotated[i] && !isNotNullNotInferred(parameter, false, false) && !isNullableNotInferred(parameter, false)) {
                                parameters[i].getNameIdentifier(); //be sure that corresponding tree element available
                                PsiAnnotation annotation = AnnotationUtil.findAnnotation(parameters[i], nullableManager.getNotNulls());
                                PsiElement psiElement = annotation;
                                if (annotation == null || !annotation.isPhysical()) {
                                    psiElement = parameters[i].getNameIdentifier();
                                    if (psiElement == null) continue;
                                }
                                holder.registerProblem(psiElement,
                                        InspectionsBundle.message("nullable.stuff.problems.overridden.method.parameters.are.not.annotated"),
                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                        wrapFix(!applicable
                                                ? createChangeDefaultNotNullFix(nullableManager, parameters[i])
                                                : new AnnotateOverriddenMethodParameterFix(defaultNotNull,
                                                nullableManager.getDefaultNullable())));
                                parameterQuickFixSuggested[i] = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isNotNullNotInferred( PsiModifierListOwner owner, boolean checkBases, boolean skipExternal) {
        Project project = owner.getProject();
        NullableNotNullManager manager = NullableNotNullManager.getInstance(project);
        if (!manager.isNotNull(owner, checkBases)) return false;

        PsiAnnotation anno = manager.getNotNullAnnotation(owner, checkBases);
        if (anno == null || AnnotationUtil.isInferredAnnotation(anno)) return false;
        if (skipExternal && AnnotationUtil.isExternalAnnotation(anno)) return false;
        return true;
    }

    public static boolean isNullableNotInferred( PsiModifierListOwner owner, boolean checkBases) {
        Project project = owner.getProject();
        NullableNotNullManager manager = NullableNotNullManager.getInstance(project);
        if (!manager.isNullable(owner, checkBases)) return false;

        PsiAnnotation anno = manager.getNullableAnnotation(owner, checkBases);
        return !(anno != null && AnnotationUtil.isInferredAnnotation(anno));
    }

    
    private static LocalQuickFix[] wrapFix(LocalQuickFix fix) {
        if (fix == null) return LocalQuickFix.EMPTY_ARRAY;
        return new LocalQuickFix[]{fix};
    }

    private static LocalQuickFix createChangeDefaultNotNullFix(NullableNotNullManager nullableManager, PsiModifierListOwner modifierListOwner) {
        final PsiAnnotation annotation = AnnotationUtil.findAnnotation(modifierListOwner, nullableManager.getNotNulls());
        if (annotation != null) {
            final PsiJavaCodeReferenceElement referenceElement = annotation.getNameReferenceElement();
            if (referenceElement != null && referenceElement.resolve() != null) {
                return new ChangeNullableDefaultsFix(annotation.getQualifiedName(), null, nullableManager);
            }
        }
        return null;
    }

    protected AnnotateMethodFix createAnnotateMethodFix(final String defaultNotNull, final String[] annotationsToRemove) {
        return new AnnotateMethodFix(defaultNotNull, annotationsToRemove);
    }

    private static void reportNullableNotNullConflict(final ProblemsHolder holder, final PsiModifierListOwner listOwner, final PsiAnnotation declaredNullable,
                                                      final PsiAnnotation declaredNotNull) {
        final String bothNullableNotNullMessage = InspectionsBundle.message("inspection.nullable.problems.Nullable.NotNull.conflict",
                getPresentableAnnoName(declaredNullable),
                getPresentableAnnoName(declaredNotNull));
        holder.registerProblem(declaredNotNull.isPhysical() ? declaredNotNull : listOwner.getNavigationElement(),
                bothNullableNotNullMessage,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new RemoveAnnotationQuickFix(declaredNotNull, listOwner));
        holder.registerProblem(declaredNullable.isPhysical() ? declaredNullable : listOwner.getNavigationElement(),
                bothNullableNotNullMessage,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new RemoveAnnotationQuickFix(declaredNullable, listOwner));
    }

    @Override
    public JComponent createOptionsPanel() {
        throw new RuntimeException("No UI in headless mode");
    }

    private static class MyAnnotateMethodFix extends AnnotateMethodFix {
        public MyAnnotateMethodFix(String defaultNotNull, String[] annotationsToRemove) {
            super(defaultNotNull, annotationsToRemove);
        }

        @Override
        protected boolean annotateOverriddenMethods() {
            return true;
        }

        @Override
        public int shouldAnnotateBaseMethod(PsiMethod method, PsiMethod superMethod, Project project) {
            return 1;
        }

        @Override
        
        public String getName() {
            return InspectionsBundle.message("annotate.overridden.methods.as.notnull", ClassUtil.extractClassName(myAnnotation));
        }
    }
}
