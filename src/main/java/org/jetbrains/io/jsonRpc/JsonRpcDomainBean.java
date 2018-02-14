package org.jetbrains.io.jsonRpc;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.extensions.AbstractExtensionPointBean;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.util.AtomicNotNullLazyValue;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.util.xmlb.annotations.Attribute;


public class JsonRpcDomainBean extends AbstractExtensionPointBean {
  public static final ExtensionPointName<JsonRpcDomainBean> EP_NAME = ExtensionPointName.create("org.jetbrains.jsonRpcDomain");

  @Attribute("name")
  public String name;

  @Attribute("implementation")
  public String implementation;

  @Attribute("service")
  public String service;

  @Attribute("asInstance")
  public boolean asInstance = true;

  @Attribute("overridable")
  public boolean overridable;

  private NotNullLazyValue<?> value;


  public NotNullLazyValue<?> getValue() {
    if (value == null) {
      value = new AtomicNotNullLazyValue<Object>() {

        @Override
        protected Object compute() {
          try {
            if (service == null) {
              Class<Object> aClass = findClass(implementation);
              return asInstance ? instantiate(aClass, ApplicationManager.getApplication().getPicoContainer(), true) : aClass;
            }
            else {
              return ServiceManager.getService(findClass(service));
            }
          }
          catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        }
      };
    }
    return value;
  }
}