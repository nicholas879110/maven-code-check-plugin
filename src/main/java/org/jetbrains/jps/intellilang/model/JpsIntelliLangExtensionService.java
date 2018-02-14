package org.jetbrains.jps.intellilang.model;


import org.jetbrains.jps.model.JpsGlobal;
import org.jetbrains.jps.service.JpsServiceManager;

/**
 * @author Eugene Zhuravlev
 *         Date: 11/29/12
 */
public abstract class JpsIntelliLangExtensionService {
  public static JpsIntelliLangExtensionService getInstance() {
    return JpsServiceManager.getInstance().getService(JpsIntelliLangExtensionService.class);
  }


  public abstract JpsIntelliLangConfiguration getConfiguration( JpsGlobal project);

  public abstract void setConfiguration( JpsGlobal project,  JpsIntelliLangConfiguration extension);
}
