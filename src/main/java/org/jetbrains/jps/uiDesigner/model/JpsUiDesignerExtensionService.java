package org.jetbrains.jps.uiDesigner.model;



import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.service.JpsServiceManager;

/**
 * @author nik
 */
public abstract class JpsUiDesignerExtensionService {
  public static JpsUiDesignerExtensionService getInstance() {
    return JpsServiceManager.getInstance().getService(JpsUiDesignerExtensionService.class);
  }


  public abstract JpsUiDesignerConfiguration getUiDesignerConfiguration( JpsProject project);

  public abstract void setUiDesignerConfiguration( JpsProject project,  JpsUiDesignerConfiguration configuration);


  public abstract JpsUiDesignerConfiguration getOrCreateUiDesignerConfiguration( JpsProject project);
}
