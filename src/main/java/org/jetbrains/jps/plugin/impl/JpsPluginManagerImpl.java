package org.jetbrains.jps.plugin.impl;

import com.gome.maven.util.containers.ContainerUtilRt;

import org.jetbrains.jps.plugin.JpsPluginManager;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * @author nik
 */
public class JpsPluginManagerImpl extends JpsPluginManager {

  @Override
  public <T> Collection<T> loadExtensions( Class<T> extensionClass) {
    ServiceLoader<T> loader = ServiceLoader.load(extensionClass, extensionClass.getClassLoader());
    return ContainerUtilRt.newArrayList(loader);
  }
}
