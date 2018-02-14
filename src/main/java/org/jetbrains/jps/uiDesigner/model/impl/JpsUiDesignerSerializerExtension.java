package org.jetbrains.jps.uiDesigner.model.impl;


import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;

import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class JpsUiDesignerSerializerExtension extends JpsModelSerializerExtension {

  @Override
  public List<? extends JpsProjectExtensionSerializer> getProjectExtensionSerializers() {
    return Arrays.asList(new JpsUiDesignerConfigurationSerializer());
  }
}
