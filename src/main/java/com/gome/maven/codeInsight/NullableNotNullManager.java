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
package com.gome.maven.codeInsight;

import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.DefaultJDOMExternalizer;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMExternalizableStringList;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.psi.*;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.Element;

import java.util.*;

/**
 * User: anna
 * Date: 1/25/11
 */
public abstract class NullableNotNullManager implements PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance("#" + NullableNotNullManager.class.getName());

    public String myDefaultNullable = AnnotationUtil.NULLABLE;
    public String myDefaultNotNull = AnnotationUtil.NOT_NULL;
    public final JDOMExternalizableStringList myNullables = new JDOMExternalizableStringList();
    public final JDOMExternalizableStringList myNotNulls = new JDOMExternalizableStringList();

    private static final String JAVAX_ANNOTATION_NULLABLE = "javax.annotation.Nullable";
    private static final String JAVAX_ANNOTATION_NONNULL = "javax.annotation.Nonnull";

    public static final String[] DEFAULT_NULLABLES = {AnnotationUtil.NULLABLE, JAVAX_ANNOTATION_NULLABLE,
            "edu.umd.cs.findbugs.annotations.Nullable", "android.support.annotation.Nullable"
    };
    public static final String[] DEFAULT_NOT_NULLS = {AnnotationUtil.NOT_NULL, JAVAX_ANNOTATION_NONNULL,
            "edu.umd.cs.findbugs.annotations.NonNull", "android.support.annotation.NonNull"
    };

    public NullableNotNullManager() {
        Collections.addAll(myNotNulls, DEFAULT_NOT_NULLS);
        Collections.addAll(myNullables, DEFAULT_NULLABLES);
    }

    public static NullableNotNullManager getInstance(Project project) {
        return ServiceManager.getService(project, NullableNotNullManager.class);
    }

    /**
     * @return if owner has a  or  annotation, or is in scope of @ParametersAreNullableByDefault or ParametersAreNonnullByDefault
     */
    public boolean hasNullability( PsiModifierListOwner owner) {
        return isNullable(owner, false) || isNotNull(owner, false);
    }

    private static void addAllIfNotPresent( Collection<String> collection,  String... annotations) {
        for (String annotation : annotations) {
            LOG.assertTrue(annotation != null);
            if (!collection.contains(annotation)) {
                collection.add(annotation);
            }
        }
    }

    public void setNotNulls( String... annotations) {
        myNotNulls.clear();
        addAllIfNotPresent(myNotNulls, DEFAULT_NOT_NULLS);
        addAllIfNotPresent(myNotNulls, annotations);
    }

    public void setNullables( String... annotations) {
        myNullables.clear();
        addAllIfNotPresent(myNullables, DEFAULT_NULLABLES);
        addAllIfNotPresent(myNullables, annotations);
    }

    
    public String getDefaultNullable() {
        return myDefaultNullable;
    }

    
    public String getNullable( PsiModifierListOwner owner) {
        PsiAnnotation annotation = getNullableAnnotation(owner, false);
        return annotation == null ? null : annotation.getQualifiedName();
    }

    private String checkContainer(PsiAnnotation annotation, boolean acceptContainer) {
        if (annotation == null) {
            return null;
        }
        else {
            if (!acceptContainer && isContainerAnnotation(annotation)) {
                return null;
            }
            return annotation.getQualifiedName();
        }
    }

    
    public PsiAnnotation getNullableAnnotation( PsiModifierListOwner owner, boolean checkBases) {
        return findNullabilityAnnotationWithDefault(owner, checkBases, true);
    }

    public boolean isContainerAnnotation( PsiAnnotation anno) {
        PsiAnnotation.TargetType[] acceptAnyTarget = PsiAnnotation.TargetType.values();
        return isNullabilityDefault(anno, true, acceptAnyTarget) || isNullabilityDefault(anno, false, acceptAnyTarget);
    }

    public void setDefaultNullable( String defaultNullable) {
        LOG.assertTrue(getNullables().contains(defaultNullable));
        myDefaultNullable = defaultNullable;
    }

    
    public String getDefaultNotNull() {
        return myDefaultNotNull;
    }

    
    public PsiAnnotation getNotNullAnnotation( PsiModifierListOwner owner, boolean checkBases) {
        return findNullabilityAnnotationWithDefault(owner, checkBases, false);
    }

    public PsiAnnotation copyNotNullAnnotation(PsiModifierListOwner owner) {
        return copyAnnotation(owner, getNotNullAnnotation(owner, false));
    }

    public PsiAnnotation copyNullableAnnotation(PsiModifierListOwner owner) {
        return copyAnnotation(owner, getNullableAnnotation(owner, false));
    }

    private PsiAnnotation copyAnnotation(PsiModifierListOwner owner,
                                         PsiAnnotation annotation) {
        final String notNull = checkContainer(annotation, false);
        if (notNull != null) {
            return JavaPsiFacade.getElementFactory(owner.getProject()).createAnnotationFromText("@" + notNull, owner);
        }
        return null;
    }

    
    public String getNotNull( PsiModifierListOwner owner) {
        PsiAnnotation annotation = getNotNullAnnotation(owner, false);
        return annotation == null ? null : annotation.getQualifiedName();
    }

    public void setDefaultNotNull( String defaultNotNull) {
        LOG.assertTrue(getNotNulls().contains(defaultNotNull));
        myDefaultNotNull = defaultNotNull;
    }

    
    private PsiAnnotation findNullabilityAnnotationWithDefault( PsiModifierListOwner owner, boolean checkBases, boolean nullable) {
        PsiAnnotation annotation = findPlainNullabilityAnnotation(owner, checkBases);
        if (annotation != null) {
            String qName = annotation.getQualifiedName();
            if (qName == null) return null;

            List<String> contradictory = nullable ? getNotNulls() : getNullables();
            if (contradictory.contains(qName)) return null;

            return annotation;
        }

        PsiType type = getOwnerType(owner);
        if (type == null || TypeConversionUtil.isPrimitiveAndNotNull(type)) return null;

        // even if javax.annotation.Nullable is not configured, it should still take precedence over ByDefault annotations
        if (AnnotationUtil.isAnnotated(owner, nullable ? Arrays.asList(DEFAULT_NOT_NULLS) : Arrays.asList(DEFAULT_NULLABLES), checkBases, false)) {
            return null;
        }

        if (!nullable && hasHardcodedContracts(owner)) {
            return null;
        }

        return findNullabilityDefaultInHierarchy(owner, nullable);
    }

    private PsiAnnotation findPlainNullabilityAnnotation( PsiModifierListOwner owner, boolean checkBases) {
        Set<String> qNames = ContainerUtil.newHashSet(getNullables());
        qNames.addAll(getNotNulls());
        return checkBases && owner instanceof PsiMethod
                ? AnnotationUtil.findAnnotationInHierarchy(owner, qNames)
                : AnnotationUtil.findAnnotation(owner, qNames);
    }

    protected boolean hasHardcodedContracts(PsiElement element) {
        return false;
    }

    
    private static PsiType getOwnerType(PsiModifierListOwner owner) {
        if (owner instanceof PsiVariable) return ((PsiVariable)owner).getType();
        if (owner instanceof PsiMethod) return ((PsiMethod)owner).getReturnType();
        return null;
    }

    public boolean isNullable( PsiModifierListOwner owner, boolean checkBases) {
        return findNullabilityAnnotationWithDefault(owner, checkBases, true) != null;
    }

    public boolean isNotNull( PsiModifierListOwner owner, boolean checkBases) {
        return findNullabilityAnnotationWithDefault(owner, checkBases, false) != null;
    }

    
    private static PsiAnnotation findNullabilityDefaultInHierarchy(PsiModifierListOwner owner, boolean nullable) {
        PsiAnnotation.TargetType[] placeTargetTypes = AnnotationTargetUtil.getTargetsForLocation(owner.getModifierList());

        PsiElement element = owner.getParent();
        while (element != null) {
            if (element instanceof PsiModifierListOwner) {
                PsiAnnotation annotation = getNullabilityDefault((PsiModifierListOwner)element, nullable, placeTargetTypes);
                if (annotation != null) {
                    return annotation;
                }
            }

            if (element instanceof PsiClassOwner) {
                String packageName = ((PsiClassOwner)element).getPackageName();
                PsiPackage psiPackage = JavaPsiFacade.getInstance(element.getProject()).findPackage(packageName);
                return psiPackage == null ? null : getNullabilityDefault(psiPackage, nullable, placeTargetTypes);
            }

            element = element.getContext();
        }
        return null;
    }

    private static PsiAnnotation getNullabilityDefault( PsiModifierListOwner container, boolean nullable, PsiAnnotation.TargetType[] placeTargetTypes) {
        PsiModifierList modifierList = container.getModifierList();
        if (modifierList == null) return null;
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            if (isNullabilityDefault(annotation, nullable, placeTargetTypes)) {
                return annotation;
            }
        }
        return null;
    }

    private static boolean isNullabilityDefault( PsiAnnotation annotation, boolean nullable, PsiAnnotation.TargetType[] placeTargetTypes) {
        PsiJavaCodeReferenceElement element = annotation.getNameReferenceElement();
        PsiElement declaration = element == null ? null : element.resolve();
        if (!(declaration instanceof PsiClass)) return false;

        if (!AnnotationUtil.isAnnotated((PsiClass)declaration,
                nullable ? JAVAX_ANNOTATION_NULLABLE : JAVAX_ANNOTATION_NONNULL,
                false,
                true)) {
            return false;
        }

        PsiAnnotation tqDefault = AnnotationUtil.findAnnotation((PsiClass)declaration, true, "javax.annotation.meta.TypeQualifierDefault");
        if (tqDefault == null) return false;

        Set<PsiAnnotation.TargetType> required = AnnotationTargetUtil.extractRequiredAnnotationTargets(tqDefault.findAttributeValue(null));
        if (required == null) return false;
        return required.isEmpty() || ContainerUtil.intersects(required, Arrays.asList(placeTargetTypes));
    }

    
    public List<String> getNullables() {
        return myNullables;
    }

    
    public List<String> getNotNulls() {
        return myNotNulls;
    }

    public boolean hasDefaultValues() {
        if (DEFAULT_NULLABLES.length != getNullables().size() || DEFAULT_NOT_NULLS.length != getNotNulls().size()) {
            return false;
        }
        if (!myDefaultNotNull.equals(AnnotationUtil.NOT_NULL) || !myDefaultNullable.equals(AnnotationUtil.NULLABLE)) {
            return false;
        }
        for (int i = 0; i < DEFAULT_NULLABLES.length; i++) {
            if (!getNullables().get(i).equals(DEFAULT_NULLABLES[i])) {
                return false;
            }
        }
        for (int i = 0; i < DEFAULT_NOT_NULLS.length; i++) {
            if (!getNotNulls().get(i).equals(DEFAULT_NOT_NULLS[i])) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Element getState() {
        final Element component = new Element("component");

        if (hasDefaultValues()) {
            return component;
        }

        try {
            DefaultJDOMExternalizer.writeExternal(this, component);
        }
        catch (WriteExternalException e) {
            LOG.error(e);
        }
        return component;
    }

    @Override
    public void loadState(Element state) {
        try {
            DefaultJDOMExternalizer.readExternal(this, state);
            if (myNullables.isEmpty()) {
                Collections.addAll(myNullables, DEFAULT_NULLABLES);
            }
            if (myNotNulls.isEmpty()) {
                Collections.addAll(myNotNulls, DEFAULT_NOT_NULLS);
            }
        }
        catch (InvalidDataException e) {
            LOG.error(e);
        }
    }

    public static boolean isNullable( PsiModifierListOwner owner) {
        return getInstance(owner.getProject()).isNullable(owner, true);
    }

    public static boolean isNotNull( PsiModifierListOwner owner) {
        return getInstance(owner.getProject()).isNotNull(owner, true);
    }
}
