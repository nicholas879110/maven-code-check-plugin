/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.ide.plugins;

import com.gome.maven.AbstractBundle;
import com.gome.maven.CommonBundle;
import com.gome.maven.diagnostic.PluginException;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.components.ComponentConfig;
import com.gome.maven.openapi.extensions.ExtensionsArea;
import com.gome.maven.openapi.extensions.PluginId;
import com.gome.maven.openapi.extensions.impl.ExtensionsAreaImpl;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.NullableLazyValue;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.MultiMap;
import com.gome.maven.util.containers.StringInterner;
import com.gome.maven.util.xmlb.JDOMXIncluder;
import com.gome.maven.util.xmlb.XmlSerializer;
import gnu.trove.THashMap;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * @author mike
 */
public class IdeaPluginDescriptorImpl implements IdeaPluginDescriptor {
    public static final IdeaPluginDescriptorImpl[] EMPTY_ARRAY = new IdeaPluginDescriptorImpl[0];
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.ide.plugins.PluginDescriptor");
    private final NullableLazyValue<String> myDescription = new NullableLazyValue<String>() {
        @Override
        protected String compute() {
            return computeDescription();
        }
    };

    private String myName;
    private PluginId myId;
    private String myResourceBundleBaseName;
    private String myChangeNotes;
    private String myVersion;
    private String myVendor;
    private String myVendorEmail;
    private String myVendorUrl;
    private String myVendorLogoPath;
    private String myCategory;
    private String url;
    private File myPath;
    private PluginId[] myDependencies = PluginId.EMPTY_ARRAY;
    private PluginId[] myOptionalDependencies = PluginId.EMPTY_ARRAY;
    private Map<PluginId, String> myOptionalConfigs;
    private Map<PluginId, IdeaPluginDescriptorImpl> myOptionalDescriptors;
     private List<Element> myActionsElements;
    private ComponentConfig[] myAppComponents = null;
    private ComponentConfig[] myProjectComponents = null;
    private ComponentConfig[] myModuleComponents = null;
    private boolean myDeleted = false;
    private ClassLoader myLoader;
    private HelpSetPath[] myHelpSets;
     private MultiMap<String, Element> myExtensions;
     private MultiMap<String, Element> myExtensionsPoints;
    private String myDescriptionChildText;
    private String myDownloadCounter;
    private long myDate;
    private boolean myUseIdeaClassLoader;
    private boolean myUseCoreClassLoader;
    private boolean myEnabled = true;
    private String mySinceBuild;
    private String myUntilBuild;
    private Boolean mySkipped;
    private List<String> myModules = null;

    public IdeaPluginDescriptorImpl( File pluginPath) {
        myPath = pluginPath;
    }

    /**
     * @deprecated
     * use {@link com.gome.maven.util.containers.StringInterner#intern(Object)} directly instead
     */
    
    @Deprecated
    public static String intern( String s) {
        return s;
    }

    /**
     * @deprecated
     * use {@link com.gome.maven.openapi.util.JDOMUtil#internElement(org.jdom.Element, com.gome.maven.util.containers.StringInterner)}
     */
    @SuppressWarnings("unused")
    @Deprecated
    public static void internJDOMElement( Element rootElement) {
    }

    
    private static List<Element> copyElements( Element[] elements, StringInterner interner) {
        if (elements == null || elements.length == 0) {
            return null;
        }

        List<Element> result = new SmartList<Element>();
        for (Element extensionsRoot : elements) {
            for (Element element : (List<Element>)extensionsRoot.getChildren()) {
                JDOMUtil.internElement(element, interner);
                result.add(element);
            }
        }
        return result;
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    private static String createDescriptionKey(final PluginId id) {
        return "plugin." + id + ".description";
    }

    private static ComponentConfig[] mergeComponents(ComponentConfig[] first, ComponentConfig[] second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return ArrayUtil.mergeArrays(first, second);
    }

    @Override
    public File getPath() {
        return myPath;
    }

    public void setPath( File path) {
        myPath = path;
    }

    public void readExternal( Document document,  URL url) throws InvalidDataException, FileNotFoundException {
        Application application = ApplicationManager.getApplication();
        readExternal(document, url, application != null && application.isUnitTestMode());
    }

    public void readExternal( Document document,  URL url, boolean ignoreMissingInclude) throws InvalidDataException, FileNotFoundException {
        document = JDOMXIncluder.resolve(document, url.toExternalForm(), ignoreMissingInclude);
        Element rootElement = document.getRootElement();
        JDOMUtil.internElement(rootElement, new StringInterner());
        readExternal(document.getRootElement());
    }

    public void readExternal( URL url) throws InvalidDataException, FileNotFoundException {
        try {
            Document document = JDOMUtil.loadDocument(url);
            readExternal(document, url);
        }
        catch (FileNotFoundException e) {
            throw e;
        }
        catch (IOException e) {
            throw new InvalidDataException(e);
        }
        catch (JDOMException e) {
            throw new InvalidDataException(e);
        }
    }

    private void readExternal( Element element) {
        final PluginBean pluginBean = XmlSerializer.deserialize(element, PluginBean.class);

        url = pluginBean.url;
        myName = pluginBean.name;
        String idString = pluginBean.id;
        if (idString == null || idString.isEmpty()) {
            idString = myName;
        }
        myId = idString == null ? null : PluginId.getId(idString);

        String internalVersionString = pluginBean.formatVersion;
        if (internalVersionString != null) {
            try {
                //noinspection ResultOfMethodCallIgnored
                Integer.parseInt(internalVersionString);
            }
            catch (NumberFormatException e) {
                LOG.error(new PluginException("Invalid value in plugin.xml format version: '" + internalVersionString + "'", e, myId));
            }
        }
        myUseIdeaClassLoader = pluginBean.useIdeaClassLoader;
        if (pluginBean.ideaVersion != null) {
            mySinceBuild = pluginBean.ideaVersion.sinceBuild;
            myUntilBuild = pluginBean.ideaVersion.untilBuild;
        }

        myResourceBundleBaseName = pluginBean.resourceBundle;

        myDescriptionChildText = pluginBean.description;
        myChangeNotes = pluginBean.changeNotes;
        myVersion = pluginBean.pluginVersion;
        myCategory = pluginBean.category;


        if (pluginBean.vendor != null) {
            myVendor = pluginBean.vendor.name;
            myVendorEmail = pluginBean.vendor.email;
            myVendorUrl = pluginBean.vendor.url;
            myVendorLogoPath = pluginBean.vendor.logo;
        }

        // preserve items order as specified in xml (filterBadPlugins will not fail if module comes first)
        Set<PluginId> dependentPlugins = new LinkedHashSet<PluginId>();
        Set<PluginId> optionalDependentPlugins = new LinkedHashSet<PluginId>();
        if (pluginBean.dependencies != null) {
            myOptionalConfigs = new THashMap<PluginId, String>();
            for (PluginDependency dependency : pluginBean.dependencies) {
                String text = dependency.pluginId;
                if (!StringUtil.isEmpty(text)) {
                    PluginId id = PluginId.getId(text);
                    dependentPlugins.add(id);
                    if (dependency.optional) {
                        optionalDependentPlugins.add(id);
                        if (!StringUtil.isEmpty(dependency.configFile)) {
                            myOptionalConfigs.put(id, dependency.configFile);
                        }
                    }
                }
            }
        }

        myDependencies = dependentPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : dependentPlugins.toArray(new PluginId[dependentPlugins.size()]);
        myOptionalDependencies = optionalDependentPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : optionalDependentPlugins.toArray(new PluginId[optionalDependentPlugins.size()]);

        if (pluginBean.helpSets == null || pluginBean.helpSets.length == 0) {
            myHelpSets = HelpSetPath.EMPTY;
        }
        else {
            myHelpSets = new HelpSetPath[pluginBean.helpSets.length];
            PluginHelpSet[] sets = pluginBean.helpSets;
            for (int i = 0, n = sets.length; i < n; i++) {
                PluginHelpSet pluginHelpSet = sets[i];
                myHelpSets[i] = new HelpSetPath(pluginHelpSet.file, pluginHelpSet.path);
            }
        }

        myAppComponents = pluginBean.applicationComponents;
        myProjectComponents = pluginBean.projectComponents;
        myModuleComponents = pluginBean.moduleComponents;

        if (myAppComponents == null) myAppComponents = ComponentConfig.EMPTY_ARRAY;
        if (myProjectComponents == null) myProjectComponents = ComponentConfig.EMPTY_ARRAY;
        if (myModuleComponents == null) myModuleComponents = ComponentConfig.EMPTY_ARRAY;

        StringInterner interner = new StringInterner();
        List<Element> extensions = copyElements(pluginBean.extensions, interner);
        if (extensions != null) {
            myExtensions = MultiMap.createSmart();
            for (Element extension : extensions) {
                myExtensions.putValue(ExtensionsAreaImpl.extractEPName(extension), extension);
            }
        }

        List<Element> extensionPoints = copyElements(pluginBean.extensionPoints, interner);
        if (extensionPoints != null) {
            myExtensionsPoints = MultiMap.createSmart();
            for (Element extensionPoint : extensionPoints) {
                myExtensionsPoints.putValue(StringUtil.notNullize(extensionPoint.getAttributeValue(ExtensionsAreaImpl.ATTRIBUTE_AREA)), extensionPoint);
            }
        }

        myActionsElements = copyElements(pluginBean.actions, interner);

        if (pluginBean.modules != null && !pluginBean.modules.isEmpty()) {
            myModules = pluginBean.modules;
        }
    }

    // made public for Upsource
    public void registerExtensionPoints( ExtensionsArea area) {
        if (myExtensionsPoints != null) {
            for (Element element : myExtensionsPoints.get(StringUtil.notNullize(area.getAreaClass()))) {
                area.registerExtensionPoint(this, element);
            }
        }
    }

    // made public for Upsource
    public void registerExtensions( ExtensionsArea area,  String epName) {
        if (myExtensions != null) {
            for (Element element : myExtensions.get(epName)) {
                area.registerExtension(this, element);
            }
        }
    }

    @Override
    public String getDescription() {
        return myDescription.getValue();
    }

    @Override
    public String getChangeNotes() {
        return myChangeNotes;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    
    public PluginId[] getDependentPluginIds() {
        return myDependencies;
    }

    @Override
    
    public PluginId[] getOptionalDependentPluginIds() {
        return myOptionalDependencies;
    }

    @Override
    public String getVendor() {
        return myVendor;
    }

    public void setVendor( final String val )
    {
        myVendor = val;
    }

    @Override
    public String getVersion() {
        return myVersion;
    }

    @Override
    public String getResourceBundleBaseName() {
        return myResourceBundleBaseName;
    }

    @Override
    public String getCategory() {
        return myCategory;
    }

    /*
       This setter was explicitly defined to be able to set a category for a
       descriptor outside its loading from the xml file.
       Problem was that most commonly plugin authors do not publish the plugin's
       category in its .xml file so to be consistent in plugins representation
       (e.g. in the Plugins form) we have to set this value outside.
    */
    public void setCategory( String category ){
        myCategory = category;
    }

    @SuppressWarnings("UnusedDeclaration") // Used in Upsource
    
    public MultiMap<String, Element> getExtensionsPoints() {
        return myExtensionsPoints;
    }

    @SuppressWarnings("UnusedDeclaration") // Used in Upsource
    
    public MultiMap<String, Element> getExtensions() {
        return myExtensions;
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    
    public List<File> getClassPath() {
        if (myPath.isDirectory()) {
            final List<File> result = new ArrayList<File>();
            final File classesDir = new File(myPath, "classes");

            if (classesDir.exists()) {
                result.add(classesDir);
            }

            final File[] files = new File(myPath, "lib").listFiles();
            if (files != null && files.length > 0) {
                for (final File f : files) {
                    if (f.isFile()) {
                        final String name = f.getName();
                        if (StringUtil.endsWithIgnoreCase(name, ".jar") || StringUtil.endsWithIgnoreCase(name, ".zip")) {
                            result.add(f);
                        }
                    }
                    else {
                        result.add(f);
                    }
                }
            }

            return result;
        }
        else {
            return Collections.singletonList(myPath);
        }
    }

    @Override
    
    public List<Element> getActionsDescriptionElements() {
        return myActionsElements;
    }

    @Override
    
    public ComponentConfig[] getAppComponents() {
        return myAppComponents;
    }

    @Override
    
    public ComponentConfig[] getProjectComponents() {
        return myProjectComponents;
    }

    @Override
    
    public ComponentConfig[] getModuleComponents() {
        return myModuleComponents;
    }

    @Override
    public String getVendorEmail() {
        return myVendorEmail;
    }

    public void setVendorEmail( final String val )
    {
        myVendorEmail = val;
    }

    @Override
    public String getVendorUrl() {
        return myVendorUrl;
    }

    public void setVendorUrl( final String val )
    {
        myVendorUrl = val;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public void setUrl( final String val )
    {
        url = val;
    }

    public String toString() {
        return "PluginDescriptor[name='" + myName + "', classpath='" + myPath + "']";
    }

    public boolean isDeleted() {
        return myDeleted;
    }

    public void setDeleted(boolean deleted) {
        myDeleted = deleted;
    }

    public void setLoader(ClassLoader loader) {
        myLoader = loader;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdeaPluginDescriptorImpl)) return false;

        final IdeaPluginDescriptorImpl pluginDescriptor = (IdeaPluginDescriptorImpl)o;

        return myName == null ? pluginDescriptor.myName == null : myName.equals(pluginDescriptor.myName);
    }

    public int hashCode() {
        return myName != null ? myName.hashCode() : 0;
    }

    @Override
    
    public HelpSetPath[] getHelpSets() {
        return myHelpSets;
    }

    @Override
    public PluginId getPluginId() {
        return myId;
    }

    /*
       This setter was explicitly defined to be able to set downloads count for a
       descriptor outside its loading from the xml file since this information
       is available only from the site.
    */
    public void setDownloadsCount(String downloadsCount) {
        myDownloadCounter = downloadsCount;
    }

    @Override
    public String getDownloads(){
        return myDownloadCounter;
    }

    public long getDate(){
        return myDate;
    }

    /*
       This setter was explicitly defined to be able to set date for a
       descriptor outside its loading from the xml file since this information
       is available only from the site.
    */
    public void setDate( long date ){
        myDate = date;
    }

    @Override
    public ClassLoader getPluginClassLoader() {
        return myLoader != null ? myLoader : getClass().getClassLoader();
    }

    @Override
    public String getVendorLogoPath() {
        return myVendorLogoPath;
    }

    public void setVendorLogoPath(final String vendorLogoPath) {
        myVendorLogoPath = vendorLogoPath;
    }

    @Override
    public boolean getUseIdeaClassLoader() {
        return myUseIdeaClassLoader;
    }

    public boolean isUseCoreClassLoader() {
        return myUseCoreClassLoader;
    }

    public void setUseCoreClassLoader(final boolean useCoreClassLoader) {
        myUseCoreClassLoader = useCoreClassLoader;
    }

    private String computeDescription() {
        ResourceBundle bundle = null;
        if (myResourceBundleBaseName != null) {
            try {
                bundle = AbstractBundle.getResourceBundle(myResourceBundleBaseName, getPluginClassLoader());
            }
            catch (MissingResourceException e) {
                LOG.info("Cannot find plugin " + myId + " resource-bundle: " + myResourceBundleBaseName);
            }
        }

        if (bundle == null) {
            return myDescriptionChildText;
        }

        return CommonBundle.messageOrDefault(bundle, createDescriptionKey(myId), myDescriptionChildText == null ? "" : myDescriptionChildText);
    }

    public void insertDependency( IdeaPluginDescriptor d) {
        PluginId[] deps = new PluginId[getDependentPluginIds().length + 1];
        deps[0] = d.getPluginId();
        System.arraycopy(myDependencies, 0, deps, 1, deps.length - 1);
        myDependencies = deps;
    }

    @Override
    public boolean isEnabled() {
        return myEnabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        myEnabled = enabled;
    }

    @Override
    public String getSinceBuild() {
        return mySinceBuild;
    }

    @Override
    public String getUntilBuild() {
        return myUntilBuild;
    }

    Map<PluginId, String> getOptionalConfigs() {
        return myOptionalConfigs;
    }

    Map<PluginId, IdeaPluginDescriptorImpl> getOptionalDescriptors() {
        return myOptionalDescriptors;
    }

    void setOptionalDescriptors(final Map<PluginId, IdeaPluginDescriptorImpl> optionalDescriptors) {
        myOptionalDescriptors = optionalDescriptors;
    }

    void mergeOptionalConfig(final IdeaPluginDescriptorImpl descriptor) {
        if (myExtensions == null) {
            myExtensions = descriptor.myExtensions;
        }
        else if (descriptor.myExtensions != null) {
            myExtensions.putAllValues(descriptor.myExtensions);
        }

        if (myExtensionsPoints == null) {
            myExtensionsPoints = descriptor.myExtensionsPoints;
        }
        else if (descriptor.myExtensionsPoints != null) {
            myExtensionsPoints.putAllValues(descriptor.myExtensionsPoints);
        }

        if (myActionsElements == null) {
            myActionsElements = descriptor.myActionsElements;
        }
        else if (descriptor.myActionsElements != null) {
            myActionsElements.addAll(descriptor.myActionsElements);
        }

        myAppComponents = mergeComponents(myAppComponents, descriptor.myAppComponents);
        myProjectComponents = mergeComponents(myProjectComponents, descriptor.myProjectComponents);
        myModuleComponents = mergeComponents(myModuleComponents, descriptor.myModuleComponents);
    }

    public Boolean getSkipped() {
        return mySkipped;
    }

    public void setSkipped(final Boolean skipped) {
        mySkipped = skipped;
    }

    @Override
    public boolean isBundled() {
        String path;
        try {
            //to avoid paths like this /home/kb/IDEA/bin/../config/plugins/APlugin
            path = getPath().getCanonicalPath();
        } catch (IOException e) {
            path = getPath().getAbsolutePath();
        }
        if (ApplicationManager.getApplication() != null && ApplicationManager.getApplication().isInternal()) {
            if (path.startsWith(PathManager.getHomePath() + File.separator + "out" + File.separator + "classes")) {
                return true;
            }
        }

        return path.startsWith(PathManager.getPreInstalledPluginsPath());
    }

    
    public List<String> getModules() {
        return myModules;
    }
}
