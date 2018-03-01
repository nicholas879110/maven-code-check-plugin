/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.util.xml;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.psi.xml.XmlDocument;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.util.ConstantFunction;
import com.gome.maven.util.NotNullFunction;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ConcurrentInstanceMap;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.xml.highlighting.DomElementsAnnotator;

import javax.swing.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author peter
 *
 * @see com.gome.maven.util.xml.MergingFileDescription
 */
public class DomFileDescription<T> {
    public static final ExtensionPointName<DomFileDescription> EP_NAME = ExtensionPointName.create("com.gome.maven.dom.fileDescription");

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.util.xml.DomFileDescription");
    private final ConcurrentInstanceMap<ScopeProvider> myScopeProviders = new ConcurrentInstanceMap<ScopeProvider>();
    protected final Class<T> myRootElementClass;
    protected final String myRootTagName;
    private final String[] myAllPossibleRootTagNamespaces;
    private volatile boolean myInitialized;
    private final Map<Class<? extends DomElement>,Class<? extends DomElement>> myImplementations = new HashMap<Class<? extends DomElement>, Class<? extends DomElement>>();
    private final TypeChooserManager myTypeChooserManager = new TypeChooserManager();
    private final List<DomReferenceInjector> myInjectors = new SmartList<DomReferenceInjector>();
    private final Map<String, NotNullFunction<XmlTag, List<String>>> myNamespacePolicies =
            ContainerUtil.newConcurrentMap();

    public DomFileDescription(final Class<T> rootElementClass,  final String rootTagName,  final String... allPossibleRootTagNamespaces) {
        myRootElementClass = rootElementClass;
        myRootTagName = rootTagName;
        myAllPossibleRootTagNamespaces = allPossibleRootTagNamespaces;
    }

    public String[] getAllPossibleRootTagNamespaces() {
        return myAllPossibleRootTagNamespaces;
    }

    /**
     * Register an implementation class to provide additional functionality for DOM elements.
     *
     * @param domElementClass interface class.
     * @param implementationClass abstract implementation class.
     *
     * @deprecated use dom.implementation extension point instead
     * @see #initializeFileDescription()
     */
    public final <T extends DomElement> void registerImplementation(Class<T> domElementClass, Class<? extends T> implementationClass) {
        myImplementations.put(domElementClass, implementationClass);
    }

    /**
     * @param namespaceKey namespace identifier
     * @see com.gome.maven.util.xml.Namespace
     * @param policy function that takes XML file root tag and returns (maybe empty) list of possible namespace URLs or DTD public ids. This
     * function shouldn't use DOM since it may be not initialized for the file at the moment
     * @deprecated use {@link #registerNamespacePolicy(String, String...)} or override {@link #getAllowedNamespaces(String, com.gome.maven.psi.xml.XmlFile)} instead
     */
    protected final void registerNamespacePolicy(String namespaceKey, NotNullFunction<XmlTag,List<String>> policy) {
        myNamespacePolicies.put(namespaceKey, policy);
    }

    /**
     * @param namespaceKey namespace identifier
     * @see com.gome.maven.util.xml.Namespace
     * @param namespaces XML namespace or DTD public or system id value for the given namespaceKey
     */
    public final void registerNamespacePolicy(String namespaceKey, final String... namespaces) {
        registerNamespacePolicy(namespaceKey, new ConstantFunction<XmlTag, List<String>>(Arrays.asList(namespaces)));
    }

    /**
     * Consider using {@link DomService#getXmlFileHeader(com.gome.maven.psi.xml.XmlFile)} when implementing this.
     */
    @SuppressWarnings({"MethodMayBeStatic"})
    
    public List<String> getAllowedNamespaces( String namespaceKey,  XmlFile file) {
        final NotNullFunction<XmlTag, List<String>> function = myNamespacePolicies.get(namespaceKey);
        if (function instanceof ConstantFunction) {
            return function.fun(null);
        }

        if (function != null) {
            final XmlDocument document = file.getDocument();
            if (document != null) {
                final XmlTag tag = document.getRootTag();
                if (tag != null) {
                    return function.fun(tag);
                }
            }
        } else {
            return Collections.singletonList(namespaceKey);
        }
        return Collections.emptyList();
    }

    /**
     * @return some version. Override and change (e.g. <code>super.getVersion()+1</code>) when after some changes some files stopped being
     * described by this description or vice versa, so that the
     * {@link com.gome.maven.util.xml.DomService#getDomFileCandidates(Class, com.gome.maven.openapi.project.Project, com.gome.maven.psi.search.GlobalSearchScope)}
     * index is rebuilt correctly.
     */
    public int getVersion() {
        return myRootTagName.hashCode();
    }

    protected final void registerTypeChooser(final Type aClass, final TypeChooser typeChooser) {
        myTypeChooserManager.registerTypeChooser(aClass, typeChooser);
    }

    public final TypeChooserManager getTypeChooserManager() {
        return myTypeChooserManager;
    }

    protected final void registerReferenceInjector(DomReferenceInjector injector) {
        myInjectors.add(injector);
    }

    public List<DomReferenceInjector> getReferenceInjectors() {
        return myInjectors;
    }

    public boolean isAutomaticHighlightingEnabled() {
        return true;
    }

    
    public Icon getFileIcon(@Iconable.IconFlags int flags) {
        return null;
    }

    /**
     * The right place to call
     * <ul>
     * <li>{@link #registerNamespacePolicy(String, String...)}</li>
     * <li>{@link #registerTypeChooser(java.lang.reflect.Type, TypeChooser)}</li>
     * <li>{@link #registerReferenceInjector(DomReferenceInjector)}</li>
     * </ul>
     */
    protected void initializeFileDescription() {}

    /**
     * Create custom DOM annotator that will be used when error-highlighting DOM. The results will be collected to
     * {@link com.gome.maven.util.xml.highlighting.DomElementsProblemsHolder}. The highlighting will be most probably done in an
     * {@link com.gome.maven.util.xml.highlighting.BasicDomElementsInspection} instance.
     * @return Annotator or null
     */
    
    public DomElementsAnnotator createAnnotator() {
        return null;
    }

    public final Map<Class<? extends DomElement>,Class<? extends DomElement>> getImplementations() {
        if (!myInitialized) {
            initializeFileDescription();
            myInitialized = true;
        }
        return myImplementations;
    }

    
    public final Class<T> getRootElementClass() {
        return myRootElementClass;
    }

    public final String getRootTagName() {
        return myRootTagName;
    }

    public boolean isMyFile( XmlFile file,  final Module module) {
        final Namespace namespace = DomReflectionUtil.findAnnotationDFS(myRootElementClass, Namespace.class);
        if (namespace != null) {
            final String key = namespace.value();
            Set<String> allNs = new HashSet<String>(getAllowedNamespaces(key, file));
            if (allNs.isEmpty()) {
                return false;
            }

            XmlFileHeader header = DomService.getInstance().getXmlFileHeader(file);
            return allNs.contains(header.getPublicId()) || allNs.contains(header.getSystemId()) || allNs.contains(header.getRootTagNamespace());
        }

        return true;
    }

    public boolean acceptsOtherRootTagNames() {
        return false;
    }

    /**
     * Get dependency items (the same, as in {@link com.gome.maven.psi.util.CachedValue}) for file. On any dependency item change, the
     * {@link #isMyFile(com.gome.maven.psi.xml.XmlFile, Module)} method will be invoked once more to ensure that the file description still
     * accepts this file
     * @param file XML file to get dependencies of
     * @return dependency item set
     */
    
    public Set<?> getDependencyItems(XmlFile file) {
        return Collections.emptySet();
    }

    /**
     * @param reference DOM reference
     * @return element, whose all children will be searched for declaration
     */
    
    public DomElement getResolveScope(GenericDomValue<?> reference) {
        final DomElement annotation = getScopeFromAnnotation(reference);
        if (annotation != null) return annotation;

        return DomUtil.getRoot(reference);
    }

    /**
     * @param element DOM element
     * @return element, whose direct children names will be compared by name. Basically it's parameter element's parent (see {@link ParentScopeProvider}).
     */
    
    public DomElement getIdentityScope(DomElement element) {
        final DomElement annotation = getScopeFromAnnotation(element);
        if (annotation != null) return annotation;

        return element.getParent();
    }

    
    protected final DomElement getScopeFromAnnotation(final DomElement element) {
        final Scope scope = element.getAnnotation(Scope.class);
        if (scope != null) {
            return myScopeProviders.get(scope.value()).getScope(element);
        }
        return null;
    }

    /**
     * @see Stubbed
     * @return false
     */
    public boolean hasStubs() {
        return false;
    }

    public int getStubVersion() {
        return 0;
    }
}
