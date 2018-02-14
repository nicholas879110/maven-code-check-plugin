package com.gome.maven.openapi.roots;

import com.gome.maven.openapi.module.Module;

/**
 * @author yole
 */
public interface RootModelProvider {
    Module[] getModules();

    ModuleRootModel getRootModel( Module module);
}
