package com.gome.maven.util;

import com.gome.maven.plugin.code.pmd.util.CommonClassNames;
import com.gome.maven.plugin.code.pmd.util.HardcodedMethodConstants;
import com.gome.maven.plugin.code.pmd.util.InheritanceUtil;
import net.sourceforge.pmd.lang.java.ast.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhangliewei
 * @date 2017/12/19 16:32
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class MethodUtils {

    private static final String METHOD_VARIABLE_TYPE_XPATH = "Type/ReferenceType/ClassOrInterfaceType";

    private MethodUtils() {
    }

    public static boolean isCompareTo(ASTMethodDeclaration method) {
        if (method == null) {
            return false;
        }
        return methodMatches(method, null, CommonClassNames.JAVA_LANG_INTEGER, HardcodedMethodConstants.COMPARE_TO,null);
    }

    public static boolean isHashCode(ASTMethodDeclaration method) {
        if (method == null) {
            return false;
        }
        return methodMatches(method, null, CommonClassNames.JAVA_LANG_INTEGER, HardcodedMethodConstants.HASH_CODE);
    }

    public static boolean isToString(ASTMethodDeclaration method) {
        if (method == null) {
            return false;
        }
//        final PsiClassType stringType = TypeUtils.getStringType(method);
        return methodMatches(method, null, CommonClassNames.JAVA_LANG_STRING, HardcodedMethodConstants.TO_STRING);
    }

    public static boolean isEquals(ASTMethodDeclaration method) {
        if (method == null) {
            return false;
        }
//        final PsiClassType objectType = TypeUtils.getObjectType(method);
        
        return methodMatches(method, null,  CommonClassNames.JAVA_LANG_BOOLEAN, HardcodedMethodConstants.EQUALS, CommonClassNames.JAVA_LANG_OBJECT);
    }

    /**
     * @param method              the method to compare to.
     * @param containingClassName the name of the class which contiains the
     *                            method.
     * @param returnType          the return type, specify null if any type matches
     * @param methodNamePattern   the name the method should have
     * @param parameterTypes      the type of the parameters of the method, specify
     *                            null if any number and type of parameters match or an empty array
     *                            to match zero parameters.
     * @return true, if the specified method matches the specified constraints,
     * false otherwise
     */
//    public static booc

    /**
     * @param method              the method to compare to.
     * @param containingClassName the name of the class which contiains the
     *                            method.
     * @param returnType          the return type, specify null if any type matches
     * @param methodName          the name the method should have
     * @param parameterTypes      the type of the parameters of the method, specify
     *                            null if any number and type of parameters match or an empty array
     *                            to match zero parameters.
     * @return true, if the specified method matches the specified constraints,
     * false otherwise
     */
    public static boolean methodMatches(
            ASTMethodDeclaration method,
            ASTTypeDeclaration containingClassName,
            String returnType,
            String methodName,
            String...  parameterTypes) {
        final String name = method.getName();
        if (methodName != null && !methodName.equals(name)) {
            return false;
        }
        if (parameterTypes != null) {
            final List<ASTFormalParameter> parameterList= method.findChildrenOfType(ASTFormalParameter.class);
            if (parameterList.size() != parameterTypes.length) {
                return false;
            }
            //final PsiParameter[] parameters = parameterList.getParameters();
            for (int i = 0; i < parameterList.size(); i++) {
                final ASTFormalParameter parameter = parameterList.get(i);
                final ASTType type=parameter.getTypeNode();
                final String parameterType = parameterTypes[i];
                if (parameterType==null) {
                    continue;
                }
                if (parameterType != null &&
                        !type.getTypeImage().equals( parameterType)) {
                    return false;
                }
            }
        }
        if (returnType != null) {
            final ASTResultType astResultType = method.getResultType();
            String methodReturnType="";
            ASTPrimitiveType astPrimitiveType=astResultType.getFirstChildOfType(ASTPrimitiveType.class);
            if (astPrimitiveType==null){
              ASTClassOrInterfaceType astClassOrInterfaceType=  astResultType.getFirstChildOfType(ASTClassOrInterfaceType.class);
                methodReturnType=astClassOrInterfaceType.getType().getSimpleName();
            }else{
                methodReturnType=astPrimitiveType.getType().getSimpleName();
            }
            if (!returnType.equals(methodReturnType )) {
                return false;
            }
        }
        if (containingClassName != null) {
            final ASTTypeDeclaration containingClass = method.getFirstParentOfType(ASTTypeDeclaration.class);
            return InheritanceUtil.isInheritor(containingClass, containingClassName);
        }
        return true;
    }

//    public static boolean simpleMethodMatches(
//            PsiMethod method,
//            String containingClassName,
//            String returnTypeString,
//            String methodName,
//            String... parameterTypeStrings) {
//        final Project project = method.getProject();
//        final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
//        final PsiElementFactory factory = psiFacade.getElementFactory();
//        try {
//            if (parameterTypeStrings != null) {
//                final PsiType[] parameterTypes = PsiType.createArray(parameterTypeStrings.length);
//                for (int i = 0; i < parameterTypeStrings.length; i++) {
//                    final String parameterTypeString = parameterTypeStrings[i];
//                    parameterTypes[i] = factory.createTypeFromText(parameterTypeString, method);
//                }
//                if (returnTypeString != null) {
//                    final PsiType returnType = factory.createTypeFromText(returnTypeString, method);
//                    return methodMatches(method, containingClassName, returnType, methodName, parameterTypes);
//                } else {
//                    return methodMatches(method, containingClassName, null, methodName, parameterTypes);
//                }
//            } else if (returnTypeString != null) {
//                final PsiType returnType = factory.createTypeFromText(returnTypeString, method);
//                return methodMatches(method, containingClassName, returnType, methodName);
//            } else {
//                return methodMatches(method, containingClassName, null, methodName);
//            }
//        } catch (IncorrectOperationException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static boolean hasSuper(PsiMethod method) {
//        if (method.isConstructor() || method.hasModifierProperty(PsiModifier.STATIC) || method.hasModifierProperty(PsiModifier.PRIVATE)) {
//            return false;
//        }
//        return SuperMethodsSearch.search(method, null, true, false).findFirst() != null;
//    }
//
//    public static boolean isOverridden(PsiMethod method) {
//        if (method.isConstructor() || method.hasModifierProperty(PsiModifier.STATIC) || method.hasModifierProperty(PsiModifier.PRIVATE)) {
//            return false;
//        }
//        final Query<PsiMethod> overridingMethodQuery = OverridingMethodsSearch.search(method);
//        final PsiMethod result = overridingMethodQuery.findFirst();
//        return result != null;
//    }
//
//    public static boolean isOverriddenInHierarchy(PsiMethod method, PsiClass baseClass) {
//        // previous implementation:
//        // final Query<PsiMethod> search = OverridingMethodsSearch.search(method);
//        //for (PsiMethod overridingMethod : search) {
//        //    final PsiClass aClass = overridingMethod.getContainingClass();
//        //    if (InheritanceUtil.isCorrectDescendant(aClass, baseClass, true)) {
//        //        return true;
//        //    }
//        //}
//        // was extremely slow and used an enormous amount of memory for clone()
//        final Query<PsiClass> search = ClassInheritorsSearch.search(baseClass, baseClass.getUseScope(), true, true, true);
//        for (PsiClass inheritor : search) {
//            final PsiMethod overridingMethod = inheritor.findMethodBySignature(method, false);
//            if (overridingMethod != null) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public static boolean isEmpty(PsiMethod method) {
//        final PsiCodeBlock body = method.getBody();
//        if (body == null) {
//            return true;
//        }
//        final PsiStatement[] statements = body.getStatements();
//        return statements.length == 0;
//    }
//
//    public static boolean hasInThrows(PsiMethod method, String... exceptions) {
//        if (exceptions.length == 0) {
//            throw new IllegalArgumentException("no exceptions specified");
//        }
//        final PsiReferenceList throwsList = method.getThrowsList();
//        final PsiJavaCodeReferenceElement[] references = throwsList.getReferenceElements();
//        for (PsiJavaCodeReferenceElement reference : references) {
//            final PsiElement target = reference.resolve();
//            if (!(target instanceof PsiClass)) {
//                continue;
//            }
//            final PsiClass aClass = (PsiClass) target;
//            final String qualifiedName = aClass.getQualifiedName();
//            for (String exception : exceptions) {
//                if (exception.equals(qualifiedName)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
}
