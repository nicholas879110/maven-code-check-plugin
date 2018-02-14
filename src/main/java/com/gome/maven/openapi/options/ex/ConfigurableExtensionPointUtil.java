/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.openapi.options.ex;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurableEP;
import com.gome.maven.openapi.options.ConfigurableProvider;
import com.gome.maven.openapi.options.OptionalConfigurable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.*;

/**
 * @author nik
 */
public class ConfigurableExtensionPointUtil {

    private final static Logger LOG = Logger.getInstance(ConfigurableExtensionPointUtil.class);

    private ConfigurableExtensionPointUtil() {
    }


    public static List<Configurable> buildConfigurablesList(final ConfigurableEP<Configurable>[] extensions,
                                                            final Configurable[] components,
                                                             ConfigurableFilter filter) {
        final List<Configurable> result = new ArrayList<Configurable>();
        for (Configurable component : components) {
            if (!isSuppressed(component, filter)) {
                result.add(component);
            }
        }

        final Map<String, ConfigurableWrapper> idToConfigurable = ContainerUtil.newHashMap();
        List<String> idsInEpOrder = ContainerUtil.newArrayList();
        for (ConfigurableEP<Configurable> ep : extensions) {
            final Configurable configurable = ConfigurableWrapper.wrapConfigurable(ep);
            if (isSuppressed(configurable, filter)) continue;
            if (configurable instanceof ConfigurableWrapper) {
                final ConfigurableWrapper wrapper = (ConfigurableWrapper)configurable;
                idToConfigurable.put(wrapper.getId(), wrapper);
                idsInEpOrder.add(wrapper.getId());
            }
            else {
//        dumpConfigurable(configurablesExtensionPoint, ep, configurable);
                ContainerUtil.addIfNotNull(configurable, result);
            }
        }

        Set<String> visited = ContainerUtil.newHashSet();
        Map<String, List<String>> idTree = buildIdTree(idToConfigurable, idsInEpOrder);
        // modify configurables (append children)
        // Before adding a child to a parent, all children of the child should be already added to the child,
        //   because ConfigurableWrapper#addChild may return a new instance.
        for (final String id : idsInEpOrder) {
            addChildrenRec(id, idToConfigurable, visited, idTree);
        }
        // add roots only (i.e. configurables without parents)
        for (String id : idsInEpOrder) {
            ConfigurableWrapper wrapper = idToConfigurable.get(id);
            String parentId = wrapper.getParentId();
            if (parentId == null || !idToConfigurable.containsKey(parentId)) {
                result.add(wrapper);
            }
        }

        return result;
    }

    
    private static ConfigurableWrapper addChildrenRec( String id,
                                                       Map<String, ConfigurableWrapper> idToConfigurable,
                                                       Set<String> visited,
                                                       Map<String, List<String>> idTree) {
        ConfigurableWrapper wrapper = idToConfigurable.get(id);
        if (visited.contains(id)) {
            return wrapper;
        }
        visited.add(id);
        List<String> childIds = idTree.get(id);
        if (childIds != null) {
            for (String childId : childIds) {
                ConfigurableWrapper childWrapper = addChildrenRec(childId, idToConfigurable, visited, idTree);
                wrapper = wrapper.addChild(childWrapper);
            }
            idToConfigurable.put(id, wrapper);
        }
        return wrapper;
    }

    
    private static Map<String, List<String>> buildIdTree( Map<String, ConfigurableWrapper> idToConfigurable,
                                                          List<String> idsInEpOrder) {
        Map<String, List<String>> tree = ContainerUtil.newHashMap();
        for (String id : idsInEpOrder) {
            ConfigurableWrapper wrapper = idToConfigurable.get(id);
            String parentId = wrapper.getParentId();
            if (parentId != null) {
                ConfigurableWrapper parent = idToConfigurable.get(parentId);
                if (parent == null) {
                    LOG.warn("Can't find parent for " + parentId + " (" + wrapper + ")");
                    continue;
                }
                List<String> children = tree.get(parentId);
                if (children == null) {
                    children = ContainerUtil.newArrayListWithCapacity(5);
                    tree.put(parentId, children);
                }
                children.add(id);
            }
        }
        return tree;
    }

    private static boolean isSuppressed(Configurable each, ConfigurableFilter filter) {
        OptionalConfigurable optional = ConfigurableWrapper.cast(OptionalConfigurable.class, each);
        return each instanceof Configurable.Assistant
                || optional != null && !optional.needDisplay()
                || filter != null && !filter.isIncluded(each);
    }

  /*
  private static void dumpConfigurable(ExtensionPointName<ConfigurableEP<Configurable>> configurablesExtensionPoint,
                                       ConfigurableEP<Configurable> ep,
                                       Configurable configurable) {
    if (configurable != null && !(configurable instanceof ConfigurableGroup)) {
      if (ep.instanceClass != null && (configurable instanceof SearchableConfigurable) && (configurable instanceof Configurable.Composite)) {
        Element element = dump(ep, configurable, StringUtil.getShortName(configurablesExtensionPoint.getName()));
        final Configurable[] configurables = ((Configurable.Composite)configurable).getConfigurables();
        for (Configurable child : configurables) {
          final Element dump = dump(null, child, "configurable");
          element.addContent(dump);
        }
        final StringWriter out = new StringWriter();
        try {
          new XMLOutputter(Format.getPrettyFormat()).output(element, out);
        }
        catch (IOException e) {
        }
        System.out.println(out);
      }
    }
  }

  private static Element dump( ConfigurableEP ep,
                              Configurable configurable, String name) {
    Element element = new Element(name);
    if (ep != null) {
      element.setAttribute("instance", ep.instanceClass);
      String id = ep.id == null ? ((SearchableConfigurable)configurable).getId() : ep.id;
      element.setAttribute("id", id);
    }
    else {
      element.setAttribute("instance", configurable.getClass().getName());
      if (configurable instanceof SearchableConfigurable) {
        element.setAttribute("id", ((SearchableConfigurable)configurable).getId());
      }
    }

    CommonBundle.lastKey = null;
    String displayName = configurable.getDisplayName();
    if (CommonBundle.lastKey != null) {
      element.setAttribute("key", CommonBundle.lastKey).setAttribute("bundle", CommonBundle.lastBundle);
    }
    else {
      element.setAttribute("displayName", displayName);
    }
    if (configurable instanceof NonDefaultProjectConfigurable) {
      element.setAttribute("nonDefaultProject", "true");
    }
    return element;
  }
  */

    /**
     * @deprecated create a new instance of configurable instead
     */
    
    public static <T extends Configurable> T findProjectConfigurable( Project project,  Class<T> configurableClass) {
        return findConfigurable(project.getExtensions(Configurable.PROJECT_CONFIGURABLE), configurableClass);
    }

    
    public static <T extends Configurable> T findApplicationConfigurable( Class<T> configurableClass) {
        return findConfigurable(Configurable.APPLICATION_CONFIGURABLE.getExtensions(), configurableClass);
    }

    
    private static <T extends Configurable> T findConfigurable(ConfigurableEP<Configurable>[] extensions, Class<T> configurableClass) {
        for (ConfigurableEP<Configurable> extension : extensions) {
            if (extension.canCreateConfigurable()) {
                final Configurable configurable = extension.createConfigurable();
                if (configurableClass.isInstance(configurable)) {
                    return configurableClass.cast(configurable);
                }
            }
        }
        throw new IllegalArgumentException("Cannot find configurable of " + configurableClass);
    }

    
    public static Configurable createProjectConfigurableForProvider( Project project, Class<? extends ConfigurableProvider> providerClass) {
        return createConfigurableForProvider(project.getExtensions(Configurable.PROJECT_CONFIGURABLE), providerClass);
    }

    
    public static Configurable createApplicationConfigurableForProvider(Class<? extends ConfigurableProvider> providerClass) {
        return createConfigurableForProvider(Configurable.APPLICATION_CONFIGURABLE.getExtensions(), providerClass);
    }

    
    private static Configurable createConfigurableForProvider(ConfigurableEP<Configurable>[] extensions, Class<? extends ConfigurableProvider> providerClass) {
        for (ConfigurableEP<Configurable> extension : extensions) {
            if (extension.providerClass != null) {
                final Class<Object> aClass = extension.findClassNoExceptions(extension.providerClass);
                if (aClass != null && providerClass.isAssignableFrom(aClass)) {
                    return extension.createConfigurable();
                }
            }
        }
        return null;
    }
}
