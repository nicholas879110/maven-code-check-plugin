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
package org.jetbrains.jps.model.java;

import com.gome.maven.openapi.util.Bitness;


import org.jetbrains.jps.service.JpsServiceManager;

import java.util.concurrent.Future;

/**
 * @author nik
 */
public abstract class JdkVersionDetector {
  public static JdkVersionDetector getInstance() {
    return JpsServiceManager.getInstance().getService(JdkVersionDetector.class);
  }

  /**
   * Returns java version for JDK located at {@code homePath} in format like<br>
   * <tt>java version "1.8.0_40"</tt><br>
   * by running '<tt>java -version</tt>' command
   * @param homePath path to JDK home directory
   * @return version string of {@code null} if version cannot be determined
   */

  public abstract String detectJdkVersion(String homePath);


  public abstract String detectJdkVersion(String homePath, ActionRunner actionRunner);


  public abstract JdkVersionInfo detectJdkVersionInfo(String homePath);


  public abstract JdkVersionInfo detectJdkVersionInfo(String homePath, ActionRunner actionRunner);

  @Deprecated

  public abstract String readVersionFromProcessOutput(String homePath, String[] command, String versionLineMarker,
                                                      ActionRunner actionRunner);

  //todo[nik] replace with a service with difference implementation for IDEA and for JPS process (need to exclude jps-builders module from IDEA classpath)
  public interface ActionRunner {
    Future<?> run(Runnable runnable);
  }

  public static final class JdkVersionInfo {
    private final String myVersion;
    private final Bitness myBitness;

    public JdkVersionInfo( String version,  Bitness bitness) {
      myVersion = version;
      myBitness = bitness;
    }


    public String getVersion() {
      return myVersion;
    }


    public Bitness getBitness() {
      return myBitness;
    }
  }
}
