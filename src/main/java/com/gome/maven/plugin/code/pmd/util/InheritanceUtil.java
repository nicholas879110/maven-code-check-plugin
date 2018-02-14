package com.gome.maven.plugin.code.pmd.util;

import net.sourceforge.pmd.lang.java.ast.ASTTypeDeclaration;

/**
 * @author zhangliewei
 * @date 2017/12/19 15:48
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class InheritanceUtil {

    private InheritanceUtil() { }

    /**
     * @param aClass     a class to check.
     * @param baseClass  supposed base class.
     * @return true if aClass is the baseClass or baseClass inheritor
     */
    public static boolean isInheritorOrSelf(ASTTypeDeclaration aClass, ASTTypeDeclaration baseClass) {
        if (aClass == null || baseClass == null) return false;
        return aClass.equals(baseClass) || isInheritor(aClass,baseClass);
    }

    public static boolean isInheritor(ASTTypeDeclaration aClass, ASTTypeDeclaration baseClass) {
        if (aClass.getType().isAssignableFrom(baseClass.getType())){
            return true;
        }
        return false;
    }



}
