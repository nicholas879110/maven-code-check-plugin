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
package com.gome.maven.psi.impl;

import com.gome.maven.codeInsight.AnnotationTargetUtil;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.FileASTNode;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.*;
import com.gome.maven.psi.filters.ElementFilter;
import com.gome.maven.psi.impl.light.LightClassReference;
import com.gome.maven.psi.impl.source.PsiClassReferenceType;
import com.gome.maven.psi.impl.source.PsiImmediateClassType;
import com.gome.maven.psi.impl.source.resolve.ResolveCache;
import com.gome.maven.psi.impl.source.tree.*;
import com.gome.maven.psi.javadoc.PsiDocComment;
import com.gome.maven.psi.scope.ElementClassHint;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.scope.processor.FilterScopeProcessor;
import com.gome.maven.psi.scope.util.PsiScopesUtil;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.LocalSearchScope;
import com.gome.maven.psi.search.PackageScope;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.PairFunction;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gome.maven.psi.PsiAnnotation.TargetType;

public class PsiImplUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.PsiImplUtil");

    private PsiImplUtil() { }

    
    public static PsiMethod[] getConstructors( PsiClass aClass) {
        List<PsiMethod> result = null;
        for (PsiMethod method : aClass.getMethods()) {
            if (method.isConstructor()) {
                if (result == null) result = ContainerUtil.newSmartList();
                result.add(method);
            }
        }
        return result == null ? PsiMethod.EMPTY_ARRAY : result.toArray(new PsiMethod[result.size()]);
    }

    
    public static PsiAnnotationMemberValue findDeclaredAttributeValue( PsiAnnotation annotation,  String attributeName) {
        if ("value".equals(attributeName)) attributeName = null;
        PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair attribute : attributes) {
             final String name = attribute.getName();
            if (Comparing.equal(name, attributeName) || attributeName == null && PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME.equals(name)) {
                return attribute.getValue();
            }
        }
        return null;
    }

    
    public static PsiAnnotationMemberValue findAttributeValue( PsiAnnotation annotation,   String attributeName) {
        final PsiAnnotationMemberValue value = findDeclaredAttributeValue(annotation, attributeName);
        if (value != null) return value;

        if (attributeName == null) attributeName = "value";
        final PsiJavaCodeReferenceElement referenceElement = annotation.getNameReferenceElement();
        if (referenceElement != null) {
            PsiElement resolved = referenceElement.resolve();
            if (resolved != null) {
                PsiMethod[] methods = ((PsiClass)resolved).findMethodsByName(attributeName, false);
                for (PsiMethod method : methods) {
                    if (PsiUtil.isAnnotationMethod(method)) {
                        return ((PsiAnnotationMethod)method).getDefaultValue();
                    }
                }
            }
        }
        return null;
    }

    
    public static PsiTypeParameter[] getTypeParameters( PsiTypeParameterListOwner owner) {
        final PsiTypeParameterList typeParameterList = owner.getTypeParameterList();
        if (typeParameterList != null) {
            return typeParameterList.getTypeParameters();
        }
        return PsiTypeParameter.EMPTY_ARRAY;
    }

    
    public static PsiJavaCodeReferenceElement[] namesToPackageReferences( PsiManager manager,  String[] names) {
        PsiJavaCodeReferenceElement[] refs = new PsiJavaCodeReferenceElement[names.length];
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            try {
                refs[i] = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory().createPackageReferenceElement(name);
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
        return refs;
    }

    public static int getParameterIndex( PsiParameter parameter,  PsiParameterList parameterList) {
        PsiElement parameterParent = parameter.getParent();
        assert parameterParent == parameterList : parameterList +"; "+parameterParent;
        PsiParameter[] parameters = parameterList.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PsiParameter paramInList = parameters[i];
            if (parameter.equals(paramInList)) return i;
        }
        String name = parameter.getName();
        PsiParameter suspect = null;
        int i;
        for (i = parameters.length - 1; i >= 0; i--) {
            PsiParameter paramInList = parameters[i];
            if (Comparing.equal(name, paramInList.getName())) {
                suspect = paramInList;
                break;
            }
        }
        String message = parameter + ":" + parameter.getClass() + " not found among parameters: " + Arrays.asList(parameters) + "." +
                " parameterList' parent: " + parameterList.getParent() + ";" +
                " parameter.isValid()=" + parameter.isValid() + ";" +
                " parameterList.isValid()= " + parameterList.isValid() + ";" +
                " parameterList stub: " + (parameterList instanceof StubBasedPsiElement ? ((StubBasedPsiElement)parameterList).getStub() : "---") + "; " +
                " parameter stub: "+(parameter instanceof StubBasedPsiElement ? ((StubBasedPsiElement)parameter).getStub() : "---") + ";" +
                " suspect: " + suspect +" (index="+i+"); " + (suspect==null?null:suspect.getClass()) +
                " suspect stub: "+(suspect instanceof StubBasedPsiElement ? ((StubBasedPsiElement)suspect).getStub() : suspect == null ? "-null-" : "---"+suspect.getClass()) + ";" +
                " parameter.equals(suspect) = " + parameter.equals(suspect) + "; " +
                " parameter.getNode() == suspect.getNode():  " + (parameter.getNode() == (suspect==null ? null : suspect.getNode())) + "; " +
                "."
                ;
        LOG.error(message);
        return i;
    }

    public static int getTypeParameterIndex( PsiTypeParameter typeParameter,  PsiTypeParameterList typeParameterList) {
        PsiTypeParameter[] typeParameters = typeParameterList.getTypeParameters();
        for (int i = 0; i < typeParameters.length; i++) {
            if (typeParameter.equals(typeParameters[i])) return i;
        }
        LOG.assertTrue(false);
        return -1;
    }

    
    public static Object[] getReferenceVariantsByFilter( PsiJavaCodeReferenceElement reference,  ElementFilter filter) {
        FilterScopeProcessor processor = new FilterScopeProcessor(filter);
        PsiScopesUtil.resolveAndWalk(processor, reference, null, true);
        return processor.getResults().toArray();
    }

    public static boolean processDeclarationsInMethod( final PsiMethod method,
                                                       final PsiScopeProcessor processor,
                                                       final ResolveState state,
                                                      final PsiElement lastParent,
                                                       final PsiElement place) {
        final boolean fromBody = lastParent instanceof PsiCodeBlock;
        final PsiTypeParameterList typeParameterList = method.getTypeParameterList();
        final PsiParameterList parameterList = method.getParameterList();
        return processDeclarationsInMethodLike(method, processor, state, place, fromBody, typeParameterList, parameterList);
    }

    public static boolean processDeclarationsInLambda( final PsiLambdaExpression lambda,
                                                       final PsiScopeProcessor processor,
                                                       final ResolveState state,
                                                      final PsiElement lastParent,
                                                       final PsiElement place) {
        final boolean fromBody = lastParent != null && lastParent == lambda.getBody();
        final PsiParameterList parameterList = lambda.getParameterList();
        return processDeclarationsInMethodLike(lambda, processor, state, place, fromBody, null, parameterList);
    }

    private static boolean processDeclarationsInMethodLike( final PsiElement element,
                                                            final PsiScopeProcessor processor,
                                                            final ResolveState state,
                                                            final PsiElement place,
                                                           final boolean fromBody,
                                                            final PsiTypeParameterList typeParameterList,
                                                            final PsiParameterList parameterList) {
        processor.handleEvent(PsiScopeProcessor.Event.SET_DECLARATION_HOLDER, element);

        if (typeParameterList != null) {
            final ElementClassHint hint = processor.getHint(ElementClassHint.KEY);
            if (hint == null || hint.shouldProcess(ElementClassHint.DeclarationKind.CLASS)) {
                if (!typeParameterList.processDeclarations(processor, state, null, place)) return false;
            }
        }

        if (fromBody) {
            final PsiParameter[] parameters = parameterList.getParameters();
            for (PsiParameter parameter : parameters) {
                if (!processor.execute(parameter, state)) return false;
            }
        }

        return true;
    }

    public static boolean processDeclarationsInResourceList( final PsiResourceList resourceList,
                                                             final PsiScopeProcessor processor,
                                                             final ResolveState state,
                                                            final PsiElement lastParent) {
        final ElementClassHint hint = processor.getHint(ElementClassHint.KEY);
        if (hint != null && !hint.shouldProcess(ElementClassHint.DeclarationKind.VARIABLE)) return true;

        final List<PsiResourceVariable> resources = resourceList.getResourceVariables();
        @SuppressWarnings({"SuspiciousMethodCalls"})
        final int lastIdx = lastParent instanceof PsiResourceVariable ? resources.indexOf(lastParent) : resources.size();
        for (int i = 0; i < lastIdx; i++) {
            if (!processor.execute(resources.get(i), state)) return false;
        }

        return true;
    }

    public static boolean hasTypeParameters( PsiTypeParameterListOwner owner) {
        final PsiTypeParameterList typeParameterList = owner.getTypeParameterList();
        return typeParameterList != null && typeParameterList.getTypeParameters().length != 0;
    }

    
    public static PsiType[] typesByReferenceParameterList( PsiReferenceParameterList parameterList) {
        PsiTypeElement[] typeElements = parameterList.getTypeParameterElements();

        return typesByTypeElements(typeElements);
    }

    
    public static PsiType[] typesByTypeElements( PsiTypeElement[] typeElements) {
        PsiType[] types = PsiType.createArray(typeElements.length);
        for (int i = 0; i < types.length; i++) {
            types[i] = typeElements[i].getType();
        }
        if (types.length == 1 && types[0] instanceof PsiDiamondType) {
            return ((PsiDiamondType)types[0]).resolveInferredTypes().getTypes();
        }
        return types;
    }

    
    public static PsiType getType( PsiClassObjectAccessExpression classAccessExpression) {
        GlobalSearchScope resolveScope = classAccessExpression.getResolveScope();
        PsiManager manager = classAccessExpression.getManager();
        final PsiClass classClass = JavaPsiFacade.getInstance(manager.getProject()).findClass("java.lang.Class", resolveScope);
        if (classClass == null) {
            return new PsiClassReferenceType(new LightClassReference(manager, "Class", "java.lang.Class", resolveScope), null);
        }
        if (!PsiUtil.isLanguageLevel5OrHigher(classAccessExpression)) {
            //Raw java.lang.Class
            return JavaPsiFacade.getInstance(manager.getProject()).getElementFactory().createType(classClass);
        }

        PsiSubstitutor substitutor = PsiSubstitutor.EMPTY;
        PsiType operandType = classAccessExpression.getOperand().getType();
        if (operandType instanceof PsiPrimitiveType && !PsiType.NULL.equals(operandType)) {
            if (PsiType.VOID.equals(operandType)) {
                operandType = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory()
                        .createTypeByFQClassName("java.lang.Void", classAccessExpression.getResolveScope());
            }
            else {
                operandType = ((PsiPrimitiveType)operandType).getBoxedType(classAccessExpression);
            }
        }
        final PsiTypeParameter[] typeParameters = classClass.getTypeParameters();
        if (typeParameters.length == 1) {
            substitutor = substitutor.put(typeParameters[0], operandType);
        }

        return new PsiImmediateClassType(classClass, substitutor);
    }

    
    public static PsiAnnotation findAnnotation( PsiAnnotationOwner annotationOwner,  String qualifiedName) {
        if (annotationOwner == null) return null;

        PsiAnnotation[] annotations = annotationOwner.getAnnotations();
        if (annotations.length == 0) return null;

        String shortName = StringUtil.getShortName(qualifiedName);
        for (PsiAnnotation annotation : annotations) {
            PsiJavaCodeReferenceElement referenceElement = annotation.getNameReferenceElement();
            if (referenceElement != null && shortName.equals(referenceElement.getReferenceName())) {
                if (qualifiedName.equals(annotation.getQualifiedName())) {
                    return annotation;
                }
            }
        }

        return null;
    }

    
    public static TargetType findApplicableTarget( PsiAnnotation annotation,  TargetType... types) {
        if (types.length != 0) {
            PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
            if (ref != null) {
                PsiElement annotationType = ref.resolve();
                if (annotationType instanceof PsiClass) {
                    return findApplicableTarget((PsiClass)annotationType, types);
                }
            }
        }

        return TargetType.UNKNOWN;
    }

    
    public static TargetType findApplicableTarget( PsiClass annotationType,  TargetType... types) {
        if (types.length != 0) {
            Set<TargetType> targets = getAnnotationTargets(annotationType);
            if (targets != null) {
                for (TargetType type : types) {
                    if (type != TargetType.UNKNOWN && targets.contains(type)) {
                        return type;
                    }
                }
                return null;
            }
        }

        return TargetType.UNKNOWN;
    }

    // todo[r.sh] cache?
    
    public static Set<TargetType> getAnnotationTargets( PsiClass annotationType) {
        if (!annotationType.isAnnotationType()) return null;
        PsiModifierList modifierList = annotationType.getModifierList();
        if (modifierList == null) return null;
        PsiAnnotation target = modifierList.findAnnotation(CommonClassNames.JAVA_LANG_ANNOTATION_TARGET);
        if (target == null) return AnnotationTargetUtil.DEFAULT_TARGETS;  // if omitted it is applicable to all but Java 8 TYPE_USE/TYPE_PARAMETERS targets

        return AnnotationTargetUtil.extractRequiredAnnotationTargets(target.findAttributeValue(null));
    }

    
    public static TargetType[] getTargetsForLocation( PsiAnnotationOwner owner) {
        return AnnotationTargetUtil.getTargetsForLocation(owner);
    }

    
    public static ASTNode findDocComment( CompositeElement element) {
        TreeElement node = element.getFirstChildNode();
        while (node != null && (isWhitespaceOrComment(node) && !(node.getPsi() instanceof PsiDocComment))) {
            node = node.getTreeNext();
        }

        if (node != null && node.getElementType() == JavaDocElementType.DOC_COMMENT) {
            return node;
        }
        else {
            return null;
        }
    }

    public static PsiType normalizeWildcardTypeByPosition( PsiType type,  PsiExpression expression) {
        PsiUtilCore.ensureValid(expression);
        PsiUtil.ensureValidType(type);

        PsiExpression toplevel = expression;
        while (toplevel.getParent() instanceof PsiArrayAccessExpression &&
                ((PsiArrayAccessExpression)toplevel.getParent()).getArrayExpression() == toplevel) {
            toplevel = (PsiExpression)toplevel.getParent();
        }

        if (toplevel instanceof PsiArrayAccessExpression && !PsiUtil.isAccessedForWriting(toplevel)) {
            return PsiUtil.captureToplevelWildcards(type, expression);
        }

        final PsiType normalized = doNormalizeWildcardByPosition(type, expression, toplevel);
        LOG.assertTrue(normalized.isValid(), type);
        if (normalized instanceof PsiClassType && !PsiUtil.isAccessedForWriting(toplevel)) {
            return PsiUtil.captureToplevelWildcards(normalized, expression);
        }

        return normalized;
    }

    private static PsiType doNormalizeWildcardByPosition(final PsiType type,  PsiExpression expression, final PsiExpression toplevel) {
        if (type instanceof PsiCapturedWildcardType) {
            final PsiWildcardType wildcardType = ((PsiCapturedWildcardType)type).getWildcard();

            if (PsiUtil.isAccessedForWriting(toplevel)) {
                return wildcardType.isSuper() ? wildcardType.getBound() : PsiCapturedWildcardType.create(wildcardType, expression);
            }
            else {
                if (wildcardType.isExtends()) {
                    return wildcardType.getBound();
                }
                else {
                    return ((PsiCapturedWildcardType)type).getUpperBound();
                }
            }
        }


        if (type instanceof PsiWildcardType) {
            final PsiWildcardType wildcardType = (PsiWildcardType)type;

            if (PsiUtil.isAccessedForWriting(toplevel)) {
                return wildcardType.isSuper() ? wildcardType.getBound() : PsiCapturedWildcardType.create(wildcardType, expression);
            }
            else {
                if (wildcardType.isExtends()) {
                    return wildcardType.getBound();
                }
                else {
                    return PsiType.getJavaLangObject(expression.getManager(), expression.getResolveScope());
                }
            }
        }
        else if (type instanceof PsiArrayType) {
            final PsiType componentType = ((PsiArrayType)type).getComponentType();
            final PsiType normalizedComponentType = doNormalizeWildcardByPosition(componentType, expression, toplevel);
            if (normalizedComponentType != componentType) {
                return normalizedComponentType.createArrayType();
            }
        }

        return type;
    }

    
    public static SearchScope getMemberUseScope( PsiMember member) {
        PsiFile file = member.getContainingFile();
        PsiElement topElement = file == null ? member : file;
        Project project = topElement.getProject();
        final GlobalSearchScope maximalUseScope = ResolveScopeManager.getInstance(project).getUseScope(topElement);
        if (isInServerPage(file)) return maximalUseScope;

        PsiClass aClass = member.getContainingClass();
        if (aClass instanceof PsiAnonymousClass) {
            //member from anonymous class can be called from outside the class
            PsiElement methodCallExpr = PsiUtil.isLanguageLevel8OrHigher(aClass) ? PsiTreeUtil.getTopmostParentOfType(aClass, PsiStatement.class)
                    : PsiTreeUtil.getParentOfType(aClass, PsiMethodCallExpression.class);
            return new LocalSearchScope(methodCallExpr != null ? methodCallExpr : aClass);
        }

        PsiModifierList modifierList = member.getModifierList();
        int accessLevel = modifierList == null ? PsiUtil.ACCESS_LEVEL_PUBLIC : PsiUtil.getAccessLevel(modifierList);
        if (accessLevel == PsiUtil.ACCESS_LEVEL_PUBLIC || accessLevel == PsiUtil.ACCESS_LEVEL_PROTECTED) {
            return maximalUseScope; // class use scope doesn't matter, since another very visible class can inherit from aClass
        }
        if (accessLevel == PsiUtil.ACCESS_LEVEL_PRIVATE) {
            PsiClass topClass = PsiUtil.getTopLevelClass(member);
            return topClass != null ? new LocalSearchScope(topClass) : file == null ? maximalUseScope : new LocalSearchScope(file);
        }
        if (file instanceof PsiJavaFile) {
            PsiPackage aPackage = JavaPsiFacade.getInstance(project).findPackage(((PsiJavaFile)file).getPackageName());
            if (aPackage != null) {
                SearchScope scope = PackageScope.packageScope(aPackage, false);
                return scope.intersectWith(maximalUseScope);
            }
        }
        return maximalUseScope;
    }

    public static boolean isInServerPage( final PsiElement element) {
        return getServerPageFile(element) != null;
    }

     public static ServerPageFile getServerPageFile(final PsiElement element) {
        final PsiFile psiFile = PsiUtilCore.getTemplateLanguageFile(element);
        return psiFile instanceof ServerPageFile ? (ServerPageFile)psiFile : null;
    }

    public static PsiElement setName( PsiElement element,  String name) throws IncorrectOperationException {
        PsiManager manager = element.getManager();
        PsiElementFactory factory = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory();
        PsiIdentifier newNameIdentifier = factory.createIdentifier(name);
        return element.replace(newNameIdentifier);
    }

    public static boolean isDeprecatedByAnnotation( PsiModifierListOwner owner) {
        PsiModifierList modifierList = owner.getModifierList();
        return modifierList != null && modifierList.findAnnotation("java.lang.Deprecated") != null;
    }

    public static boolean isDeprecatedByDocTag( PsiDocCommentOwner owner) {
        PsiDocComment docComment = owner.getDocComment();
        return docComment != null && docComment.findTagByName("deprecated") != null;
    }

    
    public static PsiAnnotationMemberValue setDeclaredAttributeValue( PsiAnnotation psiAnnotation,
                                                                      String attributeName,
                                                                      PsiAnnotationMemberValue value,
                                                                      PairFunction<Project, String, PsiAnnotation> annotationCreator) {
        final PsiAnnotationMemberValue existing = psiAnnotation.findDeclaredAttributeValue(attributeName);
        if (value == null) {
            if (existing == null) {
                return null;
            }
            existing.getParent().delete();
        } else {
            if (existing != null) {
                ((PsiNameValuePair)existing.getParent()).setValue(value);
            } else {
                final PsiNameValuePair[] attributes = psiAnnotation.getParameterList().getAttributes();
                if (attributes.length == 1 && attributes[0].getName() == null) {
                    attributes[0].replace(createNameValuePair(attributes[0].getValue(), PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME + "=", annotationCreator));
                }

                boolean allowNoName = attributes.length == 0 && ("value".equals(attributeName) || null == attributeName);
                final String namePrefix;
                if (allowNoName) {
                    namePrefix = "";
                } else {
                    namePrefix = attributeName + "=";
                }
                psiAnnotation.getParameterList().addBefore(createNameValuePair(value, namePrefix, annotationCreator), null);
            }
        }
        return psiAnnotation.findDeclaredAttributeValue(attributeName);
    }

    private static PsiNameValuePair createNameValuePair( PsiAnnotationMemberValue value,
                                                         String namePrefix,
                                                         PairFunction<Project, String, PsiAnnotation> annotationCreator) {
        return annotationCreator.fun(value.getProject(), "@A(" + namePrefix + value.getText() + ")").getParameterList().getAttributes()[0];
    }

    
    public static ASTNode skipWhitespaceAndComments(final ASTNode node) {
        return skipWhitespaceCommentsAndTokens(node, TokenSet.EMPTY);
    }

    
    public static ASTNode skipWhitespaceCommentsAndTokens(final ASTNode node, TokenSet alsoSkip) {
        ASTNode element = node;
        while (true) {
            if (element == null) return null;
            if (!isWhitespaceOrComment(element) && !alsoSkip.contains(element.getElementType())) break;
            element = element.getTreeNext();
        }
        return element;
    }

    public static boolean isWhitespaceOrComment(ASTNode element) {
        return element.getPsi() instanceof PsiWhiteSpace || element.getPsi() instanceof PsiComment;
    }

    
    public static ASTNode skipWhitespaceAndCommentsBack(final ASTNode node) {
        if (node == null) return null;
        if (!isWhitespaceOrComment(node)) return node;

        ASTNode parent = node.getTreeParent();
        ASTNode prev = node;
        while (prev instanceof CompositeElement) {
            if (!isWhitespaceOrComment(prev)) return prev;
            prev = prev.getTreePrev();
        }
        if (prev == null) return null;
        ASTNode firstChildNode = parent.getFirstChildNode();
        ASTNode lastRelevant = null;
        while (firstChildNode != prev) {
            if (!isWhitespaceOrComment(firstChildNode)) lastRelevant = firstChildNode;
            firstChildNode = firstChildNode.getTreeNext();
        }
        return lastRelevant;
    }

    
    public static ASTNode findStatementChild(CompositePsiElement statement) {
        if (DebugUtil.CHECK_INSIDE_ATOMIC_ACTION_ENABLED) {
            ApplicationManager.getApplication().assertReadAccessAllowed();
        }
        for (ASTNode element = statement.getFirstChildNode(); element != null; element = element.getTreeNext()) {
            if (element.getPsi() instanceof PsiStatement) return element;
        }
        return null;
    }

    public static PsiStatement[] getChildStatements(CompositeElement psiCodeBlock) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        // no lock is needed because all chameleons are expanded already
        int count = 0;
        for (ASTNode child1 = psiCodeBlock.getFirstChildNode(); child1 != null; child1 = child1.getTreeNext()) {
            if (child1.getPsi() instanceof PsiStatement) {
                count++;
            }
        }

        PsiStatement[] result = PsiStatement.ARRAY_FACTORY.create(count);
        if (count == 0) return result;
        int idx = 0;
        for (ASTNode child = psiCodeBlock.getFirstChildNode(); child != null && idx < count; child = child.getTreeNext()) {
            PsiElement element = child.getPsi();
            if (element instanceof PsiStatement) {
                result[idx++] = (PsiStatement)element;
            }
        }
        return result;
    }

    public static boolean isVarArgs( PsiMethod method) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        return parameters.length > 0 && parameters[parameters.length - 1].isVarArgs();
    }

    public static PsiElement handleMirror(PsiElement element) {
        return element instanceof PsiMirrorElement ? ((PsiMirrorElement)element).getPrototype() : element;
    }

    
    public static PsiModifierList findNeighbourModifierList( PsiJavaCodeReferenceElement ref) {
        PsiElement parent = PsiTreeUtil.skipParentsOfType(ref, PsiJavaCodeReferenceElement.class);
        if (parent instanceof PsiTypeElement) {
            PsiElement grandParent = parent.getParent();
            if (grandParent instanceof PsiModifierListOwner) {
                return ((PsiModifierListOwner)grandParent).getModifierList();
            }
        }

        return null;
    }

    public static boolean isTypeAnnotation( PsiElement element) {
        return element instanceof PsiAnnotation &&
                findApplicableTarget((PsiAnnotation)element, TargetType.TYPE_USE) == TargetType.TYPE_USE;
    }

    
    public static List<PsiAnnotation> getTypeUseAnnotations( PsiModifierList modifierList) {
        SmartList<PsiAnnotation> result = null;

        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            if (isTypeAnnotation(annotation)) {
                if (result == null) result = new SmartList<PsiAnnotation>();
                result.add(annotation);
            }
        }

        return result;
    }

    private static final Key<Boolean> TYPE_ANNO_MARK = Key.create("type.annotation.mark");

    public static void markTypeAnnotations( PsiTypeElement typeElement) {
        PsiElement left = PsiTreeUtil.skipSiblingsBackward(typeElement, PsiComment.class, PsiWhiteSpace.class, PsiTypeParameterList.class);
        if (left instanceof PsiModifierList) {
            for (PsiAnnotation annotation : ((PsiModifierList)left).getAnnotations()) {
                if (isTypeAnnotation(annotation)) {
                    annotation.putUserData(TYPE_ANNO_MARK, Boolean.TRUE);
                }
            }
        }
    }

    public static void deleteTypeAnnotations( PsiTypeElement typeElement) {
        PsiElement left = PsiTreeUtil.skipSiblingsBackward(typeElement, PsiComment.class, PsiWhiteSpace.class, PsiTypeParameterList.class);
        if (left instanceof PsiModifierList) {
            for (PsiAnnotation annotation : ((PsiModifierList)left).getAnnotations()) {
                if (TYPE_ANNO_MARK.get(annotation) == Boolean.TRUE) {
                    annotation.delete();
                }
            }
        }
    }

    public static boolean isLeafElementOfType( PsiElement element, IElementType type) {
        return element instanceof LeafElement && ((LeafElement)element).getElementType() == type;
    }

    public static boolean isLeafElementOfType(PsiElement element, TokenSet tokenSet) {
        return element instanceof LeafElement && tokenSet.contains(((LeafElement)element).getElementType());
    }

    public static PsiType buildTypeFromTypeString( final String typeName,  final PsiElement context,  final PsiFile psiFile) {
        PsiType resultType;
        final PsiManager psiManager = psiFile.getManager();

        if (typeName.indexOf('<') != -1 || typeName.indexOf('[') != -1 || typeName.indexOf('.') == -1) {
            try {
                return JavaPsiFacade.getInstance(psiManager.getProject()).getElementFactory().createTypeFromText(typeName, context);
            } catch(Exception ex) {} // invalid syntax will produce unresolved class type
        }

        PsiClass aClass = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(typeName, context.getResolveScope());

        if (aClass == null) {
            final LightClassReference ref = new LightClassReference(
                    psiManager,
                    PsiNameHelper.getShortClassName(typeName),
                    typeName,
                    PsiSubstitutor.EMPTY,
                    psiFile
            );
            resultType = new PsiClassReferenceType(ref, null);
        } else {
            PsiElementFactory factory = JavaPsiFacade.getInstance(psiManager.getProject()).getElementFactory();
            PsiSubstitutor substitutor = factory.createRawSubstitutor(aClass);
            resultType = factory.createType(aClass, substitutor);
        }

        return resultType;
    }

    
    public static <T extends PsiJavaCodeReferenceElement> JavaResolveResult[] multiResolveImpl(
             T element,
            boolean incompleteCode,
             ResolveCache.PolyVariantContextResolver<? super T> resolver) {

        FileASTNode fileElement = SharedImplUtil.findFileElement(element.getNode());
        if (fileElement == null) {
            PsiUtilCore.ensureValid(element);
            LOG.error("fileElement == null!");
            return JavaResolveResult.EMPTY_ARRAY;
        }
        PsiFile psiFile = SharedImplUtil.getContainingFile(fileElement);
        PsiManager manager = psiFile == null ? null : psiFile.getManager();
        if (manager == null) {
            PsiUtilCore.ensureValid(element);
            LOG.error("getManager() == null!");
            return JavaResolveResult.EMPTY_ARRAY;
        }
        boolean valid = psiFile.isValid();
        if (!valid) {
            PsiUtilCore.ensureValid(element);
            LOG.error("psiFile.isValid() == false!");
            return JavaResolveResult.EMPTY_ARRAY;
        }
        if (element instanceof PsiMethodReferenceExpression) {
            // method refs: do not cache results during parent conflict resolving, acceptable checks, etc
            final Map<PsiElement, PsiType> map = LambdaUtil.ourFunctionTypes.get();
            if (map != null && map.containsKey(element)) {
                return (JavaResolveResult[])resolver.resolve(element, psiFile, incompleteCode);
            }
        }

        return multiResolveImpl(manager.getProject(), psiFile, element, incompleteCode, resolver);
    }

    public static <T extends PsiJavaCodeReferenceElement> JavaResolveResult[] multiResolveImpl(
            Project project,
            PsiFile psiFile,
            T element,
            boolean incompleteCode,
            ResolveCache.PolyVariantContextResolver<? super T> resolver) {

        ResolveResult[] results =
                ResolveCache.getInstance(project).resolveWithCaching(element, resolver, true, incompleteCode, psiFile);
        return results.length == 0 ? JavaResolveResult.EMPTY_ARRAY : (JavaResolveResult[])results;
    }
}
