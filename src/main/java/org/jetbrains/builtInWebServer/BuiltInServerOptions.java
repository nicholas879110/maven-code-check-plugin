package org.jetbrains.builtInWebServer;

import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationDisplayType;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.notification.Notifications;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.SimpleConfigurable;
import com.gome.maven.openapi.util.Getter;
import com.gome.maven.util.xmlb.XmlSerializerUtil;
import com.gome.maven.util.xmlb.annotations.Attribute;
import com.gome.maven.xdebugger.settings.DebuggerConfigurableProvider;
import com.gome.maven.xdebugger.settings.DebuggerSettingsCategory;


import org.jetbrains.ide.BuiltInServerManager;
import org.jetbrains.ide.CustomPortServerManager;
import org.jetbrains.io.CustomPortServerManagerBase;

import java.util.Collection;
import java.util.Collections;

@State(
  name = "BuiltInServerOptions",
  storages = @Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")
)
public class BuiltInServerOptions implements PersistentStateComponent<BuiltInServerOptions>, Getter<BuiltInServerOptions> {
  @Attribute
  public int builtInServerPort = 63342;
  @Attribute
  public boolean builtInServerAvailableExternally = false;

  public static BuiltInServerOptions getInstance() {
    return ServiceManager.getService(BuiltInServerOptions.class);
  }

  @Override
  public BuiltInServerOptions get() {
    return this;
  }

  static final class BuiltInServerDebuggerConfigurableProvider extends DebuggerConfigurableProvider {

    @Override
    public Collection<? extends Configurable> getConfigurables( DebuggerSettingsCategory category) {
      if (category == DebuggerSettingsCategory.GENERAL) {
        return Collections.singletonList(SimpleConfigurable.create("builtInServer", "", BuiltInServerConfigurableUi.class, getInstance()));
      }
      return Collections.emptyList();
    }
  }


  @Override
  public BuiltInServerOptions getState() {
    return this;
  }

  @Override
  public void loadState(BuiltInServerOptions state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public int getEffectiveBuiltInServerPort() {
    MyCustomPortServerManager portServerManager = CustomPortServerManager.EP_NAME.findExtension(MyCustomPortServerManager.class);
    if (!portServerManager.isBound()) {
      return BuiltInServerManager.getInstance().getPort();
    }
    return builtInServerPort;
  }

  public static final class MyCustomPortServerManager extends CustomPortServerManagerBase {
    @Override
    public void cannotBind(Exception e, int port) {
      String groupDisplayId = "Built-in Web Server";
      Notifications.Bus.register(groupDisplayId, NotificationDisplayType.STICKY_BALLOON);
      new Notification(groupDisplayId, "Built-in HTTP server on custom port " + port + " disabled",
                       "Cannot start built-in HTTP server on custom port " + port + ". " +
                       "Please ensure that port is free (or check your firewall settings) and restart " + ApplicationNamesInfo.getInstance().getFullProductName(),
                       NotificationType.ERROR).notify(null);
    }

    @Override
    public int getPort() {
      return getInstance().builtInServerPort;
    }

    @Override
    public boolean isAvailableExternally() {
      return getInstance().builtInServerAvailableExternally;
    }
  }

  public static void onBuiltInServerPortChanged() {
    CustomPortServerManager.EP_NAME.findExtension(MyCustomPortServerManager.class).portChanged();
  }
}