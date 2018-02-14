package org.jetbrains.jps.uiDesigner.model.impl;



import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.uiDesigner.model.JpsUiDesignerConfiguration;
import org.jetbrains.jps.uiDesigner.model.JpsUiDesignerExtensionService;

/**
 * @author nik
 */
public class JpsUiDesignerExtensionServiceImpl extends JpsUiDesignerExtensionService {

  @Override
  public JpsUiDesignerConfiguration getUiDesignerConfiguration( JpsProject project) {
    return project.getContainer().getChild(JpsUiDesignerConfigurationImpl.ROLE);
  }


  @Override
  public JpsUiDesignerConfiguration getOrCreateUiDesignerConfiguration( JpsProject project) {
    JpsUiDesignerConfiguration config = project.getContainer().getChild(JpsUiDesignerConfigurationImpl.ROLE);
    if (config == null) {
      config = new JpsUiDesignerConfigurationImpl();
      setUiDesignerConfiguration(project, config);
    }
    return config;
  }

  @Override
  public void setUiDesignerConfiguration( JpsProject project,  JpsUiDesignerConfiguration configuration) {
    project.getContainer().setChild(JpsUiDesignerConfigurationImpl.ROLE, configuration);
  }
}
