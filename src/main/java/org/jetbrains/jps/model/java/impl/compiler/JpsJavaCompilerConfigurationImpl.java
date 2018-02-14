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
package org.jetbrains.jps.model.java.impl.compiler;



import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.java.compiler.JpsCompilerExcludes;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerConfiguration;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerOptions;
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.File;
import java.util.*;

/**
 * @author nik
 */
public class JpsJavaCompilerConfigurationImpl extends JpsCompositeElementBase<JpsJavaCompilerConfigurationImpl> implements JpsJavaCompilerConfiguration {
  public static final JpsElementChildRole<JpsJavaCompilerConfiguration> ROLE = JpsElementChildRoleBase.create("compiler configuration");
  private boolean myAddNotNullAssertions = true;
  private boolean myClearOutputDirectoryOnRebuild = true;
  private final JpsCompilerExcludes myCompilerExcludes = new JpsCompilerExcludesImpl();
  private final List<String> myResourcePatterns = new ArrayList<String>();
  private final List<ProcessorConfigProfile> myAnnotationProcessingProfiles = new ArrayList<ProcessorConfigProfile>();
  private final ProcessorConfigProfileImpl myDefaultAnnotationProcessingProfile = new ProcessorConfigProfileImpl("Default");
  private String myProjectByteCodeTargetLevel;
  private final Map<String, String> myModulesByteCodeTargetLevels = new HashMap<String, String>();
  private final Map<String, JpsJavaCompilerOptions> myCompilerOptions = new HashMap<String, JpsJavaCompilerOptions>();
  private String myJavaCompilerId = "Javac";
  private Map<JpsModule, ProcessorConfigProfile> myAnnotationProcessingProfileMap;
  private ResourcePatterns myCompiledPatterns;

  public JpsJavaCompilerConfigurationImpl() {
  }

  private JpsJavaCompilerConfigurationImpl(JpsJavaCompilerConfigurationImpl original) {
    super(original);
  }


  @Override
  public JpsJavaCompilerConfigurationImpl createCopy() {
    return new JpsJavaCompilerConfigurationImpl(this);
  }

  @Override
  public boolean isAddNotNullAssertions() {
    return myAddNotNullAssertions;
  }

  @Override
  public boolean isClearOutputDirectoryOnRebuild() {
    return myClearOutputDirectoryOnRebuild;
  }

  @Override
  public void setAddNotNullAssertions(boolean addNotNullAssertions) {
    myAddNotNullAssertions = addNotNullAssertions;
  }

  @Override
  public void setClearOutputDirectoryOnRebuild(boolean clearOutputDirectoryOnRebuild) {
    myClearOutputDirectoryOnRebuild = clearOutputDirectoryOnRebuild;
  }


  @Override
  public JpsCompilerExcludes getCompilerExcludes() {
    return myCompilerExcludes;
  }


  @Override
  public ProcessorConfigProfile getDefaultAnnotationProcessingProfile() {
    return myDefaultAnnotationProcessingProfile;
  }


  @Override
  public Collection<ProcessorConfigProfile> getAnnotationProcessingProfiles() {
    return myAnnotationProcessingProfiles;
  }

  @Override
  public void addResourcePattern(String pattern) {
    myResourcePatterns.add(pattern);
  }

  @Override
  public List<String> getResourcePatterns() {
    return myResourcePatterns;
  }

  @Override
  public boolean isResourceFile( File file,  File srcRoot) {
    ResourcePatterns patterns = myCompiledPatterns;
    if (patterns == null) {
      myCompiledPatterns = patterns = new ResourcePatterns(this);
    }
    return patterns.isResourceFile(file, srcRoot);
  }

  @Override

  public String getByteCodeTargetLevel(String moduleName) {
    String level = myModulesByteCodeTargetLevels.get(moduleName);
    if (level != null) {
      return level.isEmpty() ? null : level;
    }
    return myProjectByteCodeTargetLevel;
  }

  @Override
  public void setModuleByteCodeTargetLevel(String moduleName, String level) {
    myModulesByteCodeTargetLevels.put(moduleName, level);
  }


  @Override
  public String getJavaCompilerId() {
    return myJavaCompilerId;
  }

  @Override
  public void setJavaCompilerId( String compiler) {
    myJavaCompilerId = compiler;
  }


  @Override
  public JpsJavaCompilerOptions getCompilerOptions( String compilerId) {
    JpsJavaCompilerOptions options = myCompilerOptions.get(compilerId);
    if (options == null) {
      options = new JpsJavaCompilerOptions();
      myCompilerOptions.put(compilerId, options);
    }
    return options;
  }

  @Override
  public void setCompilerOptions( String compilerId,  JpsJavaCompilerOptions options) {
    myCompilerOptions.put(compilerId, options);
  }


  @Override
  public JpsJavaCompilerOptions getCurrentCompilerOptions() {
    return getCompilerOptions(getJavaCompilerId());
  }

  @Override
  public void setProjectByteCodeTargetLevel(String level) {
    myProjectByteCodeTargetLevel = level;
  }

  @Override
  public ProcessorConfigProfile addAnnotationProcessingProfile() {
    ProcessorConfigProfileImpl profile = new ProcessorConfigProfileImpl("");
    myAnnotationProcessingProfiles.add(profile);
    return profile;
  }

  @Override

  public ProcessorConfigProfile getAnnotationProcessingProfile(JpsModule module) {
    Map<JpsModule, ProcessorConfigProfile> map = myAnnotationProcessingProfileMap;
    if (map == null) {
      map = new HashMap<JpsModule, ProcessorConfigProfile>();
      final Map<String, JpsModule> namesMap = new HashMap<String, JpsModule>();
      for (JpsModule m : module.getProject().getModules()) {
        namesMap.put(m.getName(), m);
      }
      if (!namesMap.isEmpty()) {
        for (ProcessorConfigProfile profile : getAnnotationProcessingProfiles()) {
          for (String name : profile.getModuleNames()) {
            final JpsModule mod = namesMap.get(name);
            if (mod != null) {
              map.put(mod, profile);
            }
          }
        }
      }
      myAnnotationProcessingProfileMap = map;
    }
    final ProcessorConfigProfile profile = map.get(module);
    return profile != null? profile : getDefaultAnnotationProcessingProfile();
  }
}
