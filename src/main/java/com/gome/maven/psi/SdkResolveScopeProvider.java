package com.gome.maven.psi;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.JdkOrderEntry;
import com.gome.maven.psi.search.GlobalSearchScope;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class SdkResolveScopeProvider {
    public static final ExtensionPointName<SdkResolveScopeProvider> EP_NAME =
            ExtensionPointName.create("com.gome.maven.sdkResolveScopeProvider");


    public abstract GlobalSearchScope getScope( Project project,  JdkOrderEntry entry);
}
