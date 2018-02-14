package com.gome.maven.psi.search;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.psi.PsiElement;

/**
 * Excludes scope from use scope of PSI element. The extension should be used only for optimization, i.e. it should throw off scopes
 * which can't contain references to provided PSI element.
 *
 * @author Konstantin.Ulitin
 */
public abstract class UseScopeOptimizer {
    public static final ExtensionPointName<UseScopeOptimizer> EP_NAME = ExtensionPointName.create("com.intellij.useScopeOptimizer");

    
    public abstract GlobalSearchScope getScopeToExclude( PsiElement element);
}
