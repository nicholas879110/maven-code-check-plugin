/*
 * Copyright 2000-2013 JetBrains s.r.o.
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



import org.jetbrains.jps.model.JpsGlobal;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.service.JpsServiceManager;

import java.io.IOException;

/**
 * @author nik
 */
public abstract class JpsSerializationManager {
  public static JpsSerializationManager getInstance() {
    return JpsServiceManager.getInstance().getService(JpsSerializationManager.class);
  }


  public abstract JpsModel loadModel( String projectPath,  String optionsPath)
    throws IOException;

  public abstract void saveGlobalSettings( JpsGlobal global,  String optionsPath) throws IOException;
}
