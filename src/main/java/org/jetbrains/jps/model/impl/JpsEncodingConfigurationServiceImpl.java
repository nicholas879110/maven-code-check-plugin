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
package org.jetbrains.jps.model.impl;



import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

import java.util.Map;

/**
 * @author nik
 */
public class JpsEncodingConfigurationServiceImpl extends JpsEncodingConfigurationService {
  private static final JpsElementChildRoleBase<JpsSimpleElement<String>> ENCODING_ROLE = JpsElementChildRoleBase.create("encoding");


  @Override
  public String getGlobalEncoding( JpsGlobal global) {
    JpsSimpleElement<String> encoding = global.getContainer().getChild(ENCODING_ROLE);
    return encoding != null ? encoding.getData() : null;
  }

  @Override
  public void setGlobalEncoding( JpsGlobal global,  String encoding) {
    if (encoding != null) {
      global.getContainer().setChild(ENCODING_ROLE, JpsElementFactory.getInstance().createSimpleElement(encoding));
    }
    else {
      global.getContainer().removeChild(ENCODING_ROLE);
    }
  }


  @Override
  public String getProjectEncoding( JpsModel model) {
    JpsEncodingProjectConfiguration configuration = getEncodingConfiguration(model.getProject());
    if (configuration != null) {
      String projectEncoding = configuration.getProjectEncoding();
      if (projectEncoding != null) {
        return projectEncoding;
      }
    }
    return getGlobalEncoding(model.getGlobal());
  }


  @Override
  public JpsEncodingProjectConfiguration getEncodingConfiguration( JpsProject project) {
    return project.getContainer().getChild(JpsEncodingProjectConfigurationImpl.ROLE);
  }


  @Override
  public JpsEncodingProjectConfiguration setEncodingConfiguration( JpsProject project,
                                                                   String projectEncoding,
                                                                   Map<String, String> urlToEncoding) {
    JpsEncodingProjectConfigurationImpl configuration = new JpsEncodingProjectConfigurationImpl(urlToEncoding, projectEncoding);
    return project.getContainer().setChild(JpsEncodingProjectConfigurationImpl.ROLE, configuration);
  }
}
