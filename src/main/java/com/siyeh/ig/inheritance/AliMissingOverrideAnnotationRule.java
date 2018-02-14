package com.siyeh.ig.inheritance;

import com.alibaba.p3c.pmd.I18nResources;
import com.gome.maven.log.CodeCheckSystemStreamLog;
import com.gome.maven.plugin.code.pmd.util.CommonClassNames;
import com.gome.maven.plugin.code.pmd.util.InheritanceUtil;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.*;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

import java.util.List;

/**
 * @author zhangliewei
 * @date 2017/12/16 10:32
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class AliMissingOverrideAnnotationRule extends AbstractJavaRule {
    public boolean ignoreAnonymousClassMethods = false;//todo 修改成配置
    public boolean ignoreObjectMethods = false;//todo 修改成配置
    CodeCheckSystemStreamLog LOG = new CodeCheckSystemStreamLog();



    @Override
    public Object visit(ASTType node, Object data) {
        return super.visit(node, data);
    }



    @Override
    public Object visit(ASTClassOrInterfaceDeclaration clz, Object data) {
        return super.visit(clz, data);
    }

    @Override
    protected void visitAll(List<? extends Node> nodes, RuleContext ctx) {
        super.visitAll(nodes, ctx);
    }

    @Override
    public Object visit(ASTMethodDeclaration node, Object data) {
//        RuleContext ruleContext=(RuleContext)data;
//        System.out.println(ruleContext.getSourceCodeFilename());

//        ASTClassOrInterfaceDeclaration  astClassOrInterfaceDeclaration= node.getFirstParentOfType(ASTClassOrInterfaceDeclaration.class);
//        ClassScope classScope=astClassOrInterfaceDeclaration.getScope().getEnclosingScope(ClassScope.class);
//        Map<MethodNameDeclaration, List<NameOccurrence>> methods = classScope.getMethodDeclarations();


//        ASTImplementsList astImplementsList=astClassOrInterfaceDeclaration.getFirstDescendantOfType(ASTImplementsList.class);
//        if (astImplementsList!=null) {
//            ClassScope classScope = astImplementsList.getScope().getEnclosingScope(ClassScope.class);
//            Map<MethodNameDeclaration, List<NameOccurrence>> methods = classScope.getMethodDeclarations();
//        }
//        ASTExtendsList astExtendsList=astClassOrInterfaceDeclaration.getFirstDescendantOfType(ASTExtendsList.class);
//        if (astExtendsList!=null) {
//            ClassScope classScope = astExtendsList.getScope().getEnclosingScope(ClassScope.class);
//            Map<MethodNameDeclaration, List<NameOccurrence>> methods = classScope.getMethodDeclarations();
//        }

//        final ASTCompilationUnit compilationUnit = node.getFirstParentOfType(ASTCompilationUnit.class);


//        ClassTypeResolver classTypeResolver=compilationUnit.getClassTypeResolver();
//        classTypeResolver.classNameExists();
//        ASTMethodDeclarator astMethodDeclarator=null;
//        astMethodDeclarator.jjtGetParent();
//        ASTClassOrInterfaceType astClassOrInterfaceType=null;
//        astClassOrInterfaceType
//        Class<?> clazz=classTypeResolver.loadClass(getClassName(compilationUnit));
//        LOG.info("clazz="+cla);
//        compilationUnit.getClassTypeResolver().loadClass();
//        if (node.isPrivate() || node.isStatic()) {
//            return super.visit(node, data);
//        }
//
//        final Node typeNode = node.jjtGetParent().jjtGetParent().jjtGetParent();
//        if (typeNode instanceof ASTClassOrInterfaceDeclaration){
//            ASTClassOrInterfaceDeclaration  methodClass=(ASTClassOrInterfaceDeclaration)typeNode;
//            ClassScope classScope = methodClass.getScope().getEnclosingScope(ClassScope.class);
//            Map<MethodNameDeclaration, List<NameOccurrence>> methods = classScope.getMethodDeclarations();
//        }else if (typeNode instanceof ASTAllocationExpression){
//            ASTAllocationExpression  methodClass=(ASTAllocationExpression)typeNode;
//            ASTClassOrInterfaceType  astClassOrInterfaceType=methodClass.getFirstChildOfType(ASTClassOrInterfaceType.class);
//           astClassOrInterfaceType.isReferenceToClassSameCompilationUnit();
//           Class clazz=astClassOrInterfaceType.getType();
//            ClassScope  classScope= methodClass.getScope().getEnclosingScope(ClassScope.class);
//            SourceFileScope sourceFileScope= methodClass.getScope().getEnclosingScope(SourceFileScope.class);
//            Class cla1=sourceFileScope.resolveType("OverrideTest");
//            ClassTypeResolver classTypeResolver=compilationUnit.getClassTypeResolver();
//            Class<?> aClass=classTypeResolver.loadClass("def.configuration.OverrideTest");
//
//            Map<MethodNameDeclaration, List<NameOccurrence>> methods = classScope.getMethodDeclarations();
//
//        }
//        if (methodClass == null) {
//            return super.visit(node, data);
//        }
//        if (ignoreAnonymousClassMethods &&
//                methodClass.getType().isAnonymousClass()) {
//            return super.visit(node, data);
//        }
//        if (hasOverrideAnnotation(node)) {
//            return super.visit(node, data);
//        }
//        if (!isJdk6Override(node, methodClass) && !isJdk5Override(node, methodClass)) {
//            return  super.visit(node, data);
//        }
//        if (ignoreObjectMethods && (MethodUtils.isHashCode(node) ||
//                MethodUtils.isEquals(node) ||
//                MethodUtils.isToString(node))) {
//            return super.visit(node, data);
//        }
//        addViolationWithMessage(data, node,
//                "com.alibaba.p3c.idea.inspection.standalone.AliMissingOverrideAnnotationInspection.message");
        return super.visit(node, data);
    }

    @Override
    public void setDescription(String description) {
        super.setDescription(I18nResources.getMessageWithExceptionHandled(description));
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(I18nResources.getMessageWithExceptionHandled(message));
    }

    @Override
    public void addViolationWithMessage(Object data, Node node, String message) {
        super.addViolationWithMessage(data, node, I18nResources.getMessageWithExceptionHandled(message));
    }

    @Override
    public void addViolationWithMessage(Object data, Node node, String message, Object[] args) {
        super.addViolationWithMessage(data, node,
                String.format(I18nResources.getMessageWithExceptionHandled(message), args));
    }

    @Override
    public Object visit(ASTMethodDeclarator node, Object data) {
        return super.visit(node, data);
    }

    private Boolean hasOverrideAnnotation(ASTMethodDeclaration node) {
        boolean has = false;
        ASTClassOrInterfaceBodyDeclaration parent = (ASTClassOrInterfaceBodyDeclaration) node.jjtGetParent();
        for (int i = 0; i < parent.jjtGetNumChildren(); i++) {
            Node n = parent.jjtGetChild(i);
            if (n instanceof ASTAnnotation) {
                if (n.jjtGetChild(0) instanceof ASTMarkerAnnotation) {
                    // @Override is ignored
                    if ("Override".equals(((ASTName) n.jjtGetChild(0).jjtGetChild(0)).getImage())) {
                        has = true;
                        continue;
                    }
                }
            }
        }
        return has;
    }

    private Boolean isJdk6Override(ASTMethodDeclaration method, ASTTypeDeclaration methodClass){

        ASTMethodDeclaration[] superMethods = findSuperMethods(method);
        boolean hasSupers = false;
        for (ASTMethodDeclaration superMethod : superMethods) {
            ASTTypeDeclaration superClass = superMethod.getFirstParentOfType(ASTTypeDeclaration.class);
            if (!(superClass.equals(methodClass)|| InheritanceUtil.isInheritorOrSelf(methodClass, superClass))) {
                continue;
            }
            hasSupers = true;
            if (!superMethod.isProtected()) {
                return true;
            }
        }
        // is override except if this is an interface method
        // overriding a protected method in java.lang.Object
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6501053
        return hasSupers && !methodClass.getType().isInterface();
    }



    private Boolean isJdk5Override(ASTMethodDeclaration method, ASTTypeDeclaration methodClass){
        ASTMethodDeclaration[] superMethods = findSuperMethods(method);
        for (ASTMethodDeclaration superMethod : superMethods) {
            ASTTypeDeclaration superClass = superMethod.getFirstParentOfType(ASTTypeDeclaration.class);
            if (superClass == null || !InheritanceUtil.isInheritorOrSelf(methodClass, superClass)) {
                continue;
            }
            if (superClass.getType().isInterface()) {
                continue;
            }
            if (methodClass.getType().isInterface() && superMethod.isProtected()) {
                // only true for J2SE java.lang.Object.clone(), but might
                // be different on other/newer java platforms
                continue;
            }
            return true;
        }
        return false;
    }

    private static ASTMethodDeclaration[] findSuperMethods(ASTMethodDeclaration method,ASTTypeDeclaration parentClass) {
        if (!canHaveSuperMethod(method, true, false)) return new ASTMethodDeclaration[]{};
        return findSuperMethodsInternal(method, parentClass);
    }

    public static ASTMethodDeclaration[] findSuperMethods( ASTMethodDeclaration method) {
        return findSuperMethods(method, null);
    }


    private static ASTMethodDeclaration[] findSuperMethodsInternal(ASTMethodDeclaration method, ASTTypeDeclaration parentClass) {
        if (parentClass==null){

        }
//        List<MethodSignatureBackedByPsiMethod> outputMethods = findSuperMethodSignatures(method, parentClass, false);
//        return MethodSignatureUtil.convertMethodSignaturesToMethods(outputMethods);
        return new ASTMethodDeclaration[]{};
    }


    private static boolean canHaveSuperMethod(ASTMethodDeclaration method, boolean checkAccess, boolean allowStaticMethod) {
        if (!allowStaticMethod && method.isStatic()) return false;
        if (checkAccess && method.isPrivate()) return false;
        final ASTTypeDeclaration methodClass = method.getFirstParentOfType(ASTTypeDeclaration.class);
        return methodClass != null && !CommonClassNames.JAVA_LANG_OBJECT.equals(methodClass.jjtGetChild(0).getImage());
    }


    private String getClassName(ASTCompilationUnit node) {
        ASTClassOrInterfaceDeclaration classDecl = node.getFirstDescendantOfType(ASTClassOrInterfaceDeclaration.class);
        if (classDecl == null) {
            return null; // Happens if this compilation unit only contains an enum
        }
        if (node.declarationsAreInDefaultPackage()) {
            return classDecl.getImage();
        }
        ASTPackageDeclaration pkgDecl = node.getPackageDeclaration();
        return pkgDecl.getPackageNameImage() + "." + classDecl.getImage();
    }



}

