/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jps.model.serialization;

import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.ObjectUtils;
import com.gome.maven.util.SystemProperties;
import com.gome.maven.util.containers.ContainerUtil;




import java.io.File;
import java.util.Map;

import static com.gome.maven.openapi.util.io.FileUtil.toSystemIndependentName;

/**
 * @author nik
 */
public class PathMacroUtil {
   public static final String PROJECT_DIR_MACRO_NAME = "PROJECT_DIR";
   public static final String MODULE_DIR_MACRO_NAME = "MODULE_DIR";
   public static final String DIRECTORY_STORE_NAME = ".idea";
   public static final String APPLICATION_HOME_DIR = "APPLICATION_HOME_DIR";
   public static final String APPLICATION_CONFIG_DIR = "APPLICATION_CONFIG_DIR";
   public static final String APPLICATION_PLUGINS_DIR = "APPLICATION_PLUGINS_DIR";
   public static final String USER_HOME_NAME = "USER_HOME";

  private static final Map<String, String> ourGlobalMacros = ContainerUtil.<String, String>immutableMapBuilder()
    .put(APPLICATION_HOME_DIR, toSystemIndependentName(PathManager.getHomePath()))
    .put(APPLICATION_CONFIG_DIR, toSystemIndependentName(PathManager.getConfigPath()))
    .put(APPLICATION_PLUGINS_DIR, toSystemIndependentName(PathManager.getPluginsPath()))
    .put(USER_HOME_NAME, StringUtil.trimEnd(toSystemIndependentName(SystemProperties.getUserHome()), "/")).build();


  public static String getModuleDir(String moduleFilePath) {
    File moduleDirFile = new File(moduleFilePath).getParentFile();
    if (moduleDirFile == null) return null;

    // hack so that, if a module is stored inside the .idea directory, the base directory
    // rather than the .idea directory itself is considered the module root
    // (so that a Ruby IDE project doesn't break if its directory is moved together with the .idea directory)
    File moduleDirParent = moduleDirFile.getParentFile();
    if (moduleDirParent != null && moduleDirFile.getName().equals(DIRECTORY_STORE_NAME)) {
      moduleDirFile = moduleDirParent;
    }
    String moduleDir = moduleDirFile.getPath();
    moduleDir = moduleDir.replace(File.separatorChar, '/');
    if (moduleDir.endsWith(":/")) {
      moduleDir = moduleDir.substring(0, moduleDir.length() - 1);
    }
    return moduleDir;
  }


  public static String getUserHomePath() {
    return ObjectUtils.assertNotNull(getGlobalSystemMacroValue(USER_HOME_NAME));
  }


  public static Map<String, String> getGlobalSystemMacros() {
    return ourGlobalMacros;
  }


  public static String getGlobalSystemMacroValue(String name) {
    return ourGlobalMacros.get(name);
  }
}
