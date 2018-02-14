package org.jetbrains.builtInWebServer;

import com.gome.maven.openapi.options.ConfigurableUi;
import com.gome.maven.ui.PortField;


import javax.swing.*;

class BuiltInServerConfigurableUi implements ConfigurableUi<BuiltInServerOptions> {
  private JPanel mainPanel;

  private PortField builtInServerPort;
  private JCheckBox builtInServerAvailableExternallyCheckBox;

  public BuiltInServerConfigurableUi() {
    builtInServerPort.setMin(1024);
  }

  @Override

  public JComponent getComponent() {
    return mainPanel;
  }

  @Override
  public boolean isModified( BuiltInServerOptions settings) {
    return builtInServerPort.getNumber() != settings.builtInServerPort ||
           builtInServerAvailableExternallyCheckBox.isSelected() != settings.builtInServerAvailableExternally;
  }

  @Override
  public void apply( BuiltInServerOptions settings) {
    boolean builtInServerPortChanged = settings.builtInServerPort != builtInServerPort.getNumber() || settings.builtInServerAvailableExternally != builtInServerAvailableExternallyCheckBox.isSelected();
    if (builtInServerPortChanged) {
      settings.builtInServerPort = builtInServerPort.getNumber();
      settings.builtInServerAvailableExternally = builtInServerAvailableExternallyCheckBox.isSelected();

      BuiltInServerOptions.onBuiltInServerPortChanged();
    }
  }

  @Override
  public void reset( BuiltInServerOptions settings) {
    builtInServerPort.setNumber(settings.builtInServerPort);
    builtInServerAvailableExternallyCheckBox.setSelected(settings.builtInServerAvailableExternally);
  }
}
