package org.jetbrains.jps.intellilang.model.impl;


import org.jetbrains.jps.intellilang.model.JpsIntelliLangConfiguration;
import org.jetbrains.jps.intellilang.model.JpsIntelliLangExtensionService;
import org.jetbrains.jps.model.JpsGlobal;

/**
 * @author Eugene Zhuravlev
 *         Date: 11/29/12
 */
public class JpsIntelliLangExtensionServiceImpl extends JpsIntelliLangExtensionService {

  @Override
  public JpsIntelliLangConfiguration getConfiguration( JpsGlobal global) {
    return global.getContainer().getChild(JpsIntelliLangConfigurationImpl.ROLE);
  }

  @Override
  public void setConfiguration( JpsGlobal global,  JpsIntelliLangConfiguration config) {
    global.getContainer().setChild(JpsIntelliLangConfigurationImpl.ROLE, config);
  }
}
