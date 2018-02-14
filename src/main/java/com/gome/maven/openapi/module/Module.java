//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.openapi.module;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ComponentManager;
import com.gome.maven.openapi.extensions.AreaInstance;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.search.GlobalSearchScope;


public interface Module extends ComponentManager, AreaInstance, Disposable {
    Module[] EMPTY_ARRAY = new Module[0];
    
    String ELEMENT_TYPE = "type";

    
    VirtualFile getModuleFile();

    
    String getModuleFilePath();

    
    Project getProject();

    
    String getName();

    boolean isDisposed();

    boolean isLoaded();

    void setOption( String var1,  String var2);

    void clearOption( String var1);

    
    String getOptionValue( String var1);

    
    GlobalSearchScope getModuleScope();

    
    GlobalSearchScope getModuleScope(boolean var1);

    
    GlobalSearchScope getModuleWithLibrariesScope();

    
    GlobalSearchScope getModuleWithDependenciesScope();

    
    GlobalSearchScope getModuleContentScope();

    
    GlobalSearchScope getModuleContentWithDependenciesScope();

    
    GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(boolean var1);

    
    GlobalSearchScope getModuleWithDependentsScope();

    
    GlobalSearchScope getModuleTestsWithDependentsScope();

    
    GlobalSearchScope getModuleRuntimeScope(boolean var1);
}
