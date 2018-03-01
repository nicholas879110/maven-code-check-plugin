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
package com.gome.maven.codeInspection;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiLanguageInjectionHost;
import com.gome.maven.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.gome.maven.util.ResourceUtil;
import com.gome.maven.util.ThreeState;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.xmlb.SerializationFilter;
import com.gome.maven.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.gome.maven.util.xmlb.XmlSerializationException;
import com.gome.maven.util.xmlb.XmlSerializer;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import org.jdom.Element;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author anna
 * @since 28-Nov-2005
 */
public abstract class InspectionProfileEntry implements BatchSuppressableTool {
    public static final String GENERAL_GROUP_NAME = InspectionsBundle.message("inspection.general.tools.group.name");

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInspection.InspectionProfileEntry");

    private static final SkipDefaultValuesSerializationFilters DEFAULT_FILTER = new SkipDefaultValuesSerializationFilters();
    private static Set<String> ourBlackList = null;
    private static final Object BLACK_LIST_LOCK = new Object();
    private Boolean myUseNewSerializer = null;

    
    
    public String getAlternativeID() {
        return null;
    }

    @Override
    public boolean isSuppressedFor( PsiElement element) {
        Set<InspectionSuppressor> suppressors = getSuppressors(element);
        String toolId = getSuppressId();
        for (InspectionSuppressor suppressor : suppressors) {
            if (isSuppressed(toolId, suppressor, element)) {
                return true;
            }
        }
        return false;
    }

    
    protected String getSuppressId() {
        return getShortName();
    }

    
    @Override
    public SuppressQuickFix[] getBatchSuppressActions( PsiElement element) {
        if (element == null) {
            return SuppressQuickFix.EMPTY_ARRAY;
        }
        Set<SuppressQuickFix> fixes = new THashSet<SuppressQuickFix>(new TObjectHashingStrategy<SuppressQuickFix>() {
            @Override
            public int computeHashCode(SuppressQuickFix object) {
                int result = object instanceof InjectionAwareSuppressQuickFix
                        ? ((InjectionAwareSuppressQuickFix)object).isShouldBeAppliedToInjectionHost().hashCode()
                        : 0;
                return 31 * result + object.getName().hashCode();
            }

            @Override
            public boolean equals(SuppressQuickFix o1, SuppressQuickFix o2) {
                if (o1 instanceof InjectionAwareSuppressQuickFix && o2 instanceof InjectionAwareSuppressQuickFix) {
                    if (((InjectionAwareSuppressQuickFix)o1).isShouldBeAppliedToInjectionHost() != ((InjectionAwareSuppressQuickFix)o2).isShouldBeAppliedToInjectionHost()) {
                        return false;
                    }
                }
                return o1.getName().equals(o2.getName());
            }
        });

        Set<InspectionSuppressor> suppressors = getSuppressors(element);
        final PsiLanguageInjectionHost injectionHost = InjectedLanguageManager.getInstance(element.getProject()).getInjectionHost(element);
        if (injectionHost != null) {
            Set<InspectionSuppressor> injectionHostSuppressors = getSuppressors(injectionHost);
            for (InspectionSuppressor suppressor : injectionHostSuppressors) {
                addAllSuppressActions(fixes, injectionHost, suppressor, ThreeState.YES, getShortName());
            }
        }

        for (InspectionSuppressor suppressor : suppressors) {
            addAllSuppressActions(fixes, element, suppressor, injectionHost != null ? ThreeState.NO : ThreeState.UNSURE, getShortName());
        }
        return fixes.toArray(new SuppressQuickFix[fixes.size()]);
    }

    private static void addAllSuppressActions(Set<SuppressQuickFix> fixes,
                                              PsiElement element,
                                              InspectionSuppressor suppressor,
                                              ThreeState appliedToInjectionHost,
                                              String toolShortName) {
        final SuppressQuickFix[] actions = suppressor.getSuppressActions(element, toolShortName);
        for (SuppressQuickFix action : actions) {
            if (action instanceof InjectionAwareSuppressQuickFix) {
                ((InjectionAwareSuppressQuickFix)action).setShouldBeAppliedToInjectionHost(appliedToInjectionHost);
            }
            fixes.add(action);
        }
    }

    private boolean isSuppressed( String toolId,
                                  InspectionSuppressor suppressor,
                                  PsiElement element) {
        if (suppressor.isSuppressedFor(element, toolId)) {
            return true;
        }
        final String alternativeId = getAlternativeID();
        return alternativeId != null && !alternativeId.equals(toolId) && suppressor.isSuppressedFor(element, alternativeId);
    }

    
    public static Set<InspectionSuppressor> getSuppressors( PsiElement element) {
        FileViewProvider viewProvider = element.getContainingFile().getViewProvider();
        final InspectionSuppressor elementLanguageSuppressor = LanguageInspectionSuppressors.INSTANCE.forLanguage(element.getLanguage());
        if (viewProvider instanceof TemplateLanguageFileViewProvider) {
            Set<InspectionSuppressor> suppressors = new LinkedHashSet<InspectionSuppressor>();
            ContainerUtil.addIfNotNull(suppressors, LanguageInspectionSuppressors.INSTANCE.forLanguage(viewProvider.getBaseLanguage()));
            for (Language language : viewProvider.getLanguages()) {
                ContainerUtil.addIfNotNull(suppressors, LanguageInspectionSuppressors.INSTANCE.forLanguage(language));
            }
            ContainerUtil.addIfNotNull(suppressors, elementLanguageSuppressor);
            return suppressors;
        }
        return elementLanguageSuppressor != null
                ? Collections.singleton(elementLanguageSuppressor)
                : Collections.<InspectionSuppressor>emptySet();
    }

    public void cleanup(Project project) {

    }

    interface DefaultNameProvider {
         String getDefaultShortName();
         String getDefaultDisplayName();
         String getDefaultGroupDisplayName();
    }

    protected volatile DefaultNameProvider myNameProvider = null;

    /**
     * @see InspectionEP#groupDisplayName
     * @see InspectionEP#groupKey
     * @see InspectionEP#groupBundle
     */
    
    
    public String getGroupDisplayName() {
        if (myNameProvider != null) {
            final String name = myNameProvider.getDefaultGroupDisplayName();
            if (name != null) {
                return name;
            }
        }
        LOG.error(getClass() + ": group display name should be overridden or configured via XML " + getClass());
        return "";
    }

    /**
     * @see InspectionEP#groupPath
     */
    
    public String[] getGroupPath() {
        String groupDisplayName = getGroupDisplayName();
        if (groupDisplayName.isEmpty()) {
            groupDisplayName = GENERAL_GROUP_NAME;
        }
        return new String[]{groupDisplayName};
    }

    /**
     * @see InspectionEP#displayName
     * @see InspectionEP#key
     * @see InspectionEP#bundle
     */
    
    
    public String getDisplayName() {
        if (myNameProvider != null) {
            final String name = myNameProvider.getDefaultDisplayName();
            if (name != null) {
                return name;
            }
        }
        LOG.error(getClass() + ": display name should be overridden or configured via XML " + getClass());
        return "";
    }

    /**
     * DO NOT OVERRIDE this method.
     *
     * @see InspectionEP#shortName
     */
    
    
    public String getShortName() {
        if (myNameProvider != null) {
            final String name = myNameProvider.getDefaultShortName();
            if (name != null) {
                return name;
            }
        }
        return getShortName(getClass().getSimpleName());
    }

    
    public static String getShortName( String className) {
        return StringUtil.trimEnd(StringUtil.trimEnd(className, "Inspection"), "InspectionBase");
    }

    /**
     * DO NOT OVERRIDE this method.
     *
     * @see InspectionEP#level
     */
    
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    /**
     * DO NOT OVERRIDE this method.
     *
     * @see InspectionEP#enabledByDefault
     */
    public boolean isEnabledByDefault() {
        return false;
    }

    /**
     * This method is called each time UI is shown.
     *
     * @return null if no UI options required.
     */
    
    public JComponent createOptionsPanel() {
        return null;
    }

    /**
     * Read in settings from XML config.
     * Default implementation uses XmlSerializer so you may use public fields (like <code>int TOOL_OPTION</code>)
     * and bean-style getters/setters (like <code>int getToolOption(), void setToolOption(int)</code>) to store your options.
     *
     * @param node to read settings from.
     * @throws InvalidDataException if the loaded data was not valid.
     */
    @SuppressWarnings("deprecation")
    public void readSettings( Element node) throws InvalidDataException {
        if (useNewSerializer()) {
            try {
                XmlSerializer.deserializeInto(this, node);
            }
            catch (XmlSerializationException e) {
                throw new InvalidDataException(e);
            }
        }
        else {
            //noinspection UnnecessaryFullyQualifiedName
            com.gome.maven.openapi.util.DefaultJDOMExternalizer.readExternal(this, node);
        }
    }

    /**
     * Store current settings in XML config.
     * Default implementation uses XmlSerializer so you may use public fields (like <code>int TOOL_OPTION</code>)
     * and bean-style getters/setters (like <code>int getToolOption(), void setToolOption(int)</code>) to store your options.
     *
     * @param node to store settings to.
     * @throws WriteExternalException if no data should be saved for this component.
     */
    @SuppressWarnings("deprecation")
    public void writeSettings( Element node) throws WriteExternalException {
        if (useNewSerializer()) {
            XmlSerializer.serializeInto(this, node, getSerializationFilter());
        }
        else {
            //noinspection UnnecessaryFullyQualifiedName
            com.gome.maven.openapi.util.DefaultJDOMExternalizer.writeExternal(this, node);
        }
    }

    private synchronized boolean useNewSerializer() {
        if (myUseNewSerializer == null) {
            myUseNewSerializer = !getBlackList().contains(getClass().getName());
        }
        return myUseNewSerializer;
    }

    private static void loadBlackList() {
        ourBlackList = ContainerUtil.newHashSet();

        final URL url = InspectionProfileEntry.class.getResource("inspection-black-list.txt");
        if (url == null) {
            LOG.error("Resource not found");
            return;
        }

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) ourBlackList.add(line);
                }
            }
            finally {
                reader.close();
            }
        }
        catch (IOException e) {
            LOG.error("Unable to load resource: " + url, e);
        }
    }

    
    public static Collection<String> getBlackList() {
        synchronized (BLACK_LIST_LOCK) {
            if (ourBlackList == null) {
                loadBlackList();
            }
            return ourBlackList;
        }
    }

    /**
     * Returns filter used to omit default values on saving inspection settings.
     * Default implementation uses SkipDefaultValuesSerializationFilters.
     *
     * @return serialization filter.
     */
    @SuppressWarnings("MethodMayBeStatic")
    
    protected SerializationFilter getSerializationFilter() {
        return DEFAULT_FILTER;
    }

    /**
     * Initialize inspection with project. Is called on project opened for all profiles as well as on profile creation.
     *
     * @param project to be associated with this entry
     * @deprecated this won't work for inspections configured via {@link InspectionEP}
     */
    public void projectOpened( Project project) {
    }

    /**
     * Cleanup inspection settings corresponding to the project. Is called on project closed for all profiles as well as on profile deletion.
     *
     * @param project to be disassociated from this entry
     * @deprecated this won't work for inspections configured via {@link InspectionEP}
     */
    public void projectClosed( Project project) {
    }

    /**
     * Override this method to return a html inspection description. Otherwise it will be loaded from resources using ID.
     *
     * @return hard-code inspection description.
     */
    
    public String getStaticDescription() {
        return null;
    }

    
    public String getDescriptionFileName() {
        return null;
    }

    
    protected URL getDescriptionUrl() {
        final String fileName = getDescriptionFileName();
        if (fileName == null) return null;
        return ResourceUtil.getResource(getDescriptionContextClass(), "/inspectionDescriptions", fileName);
    }

    
    protected Class<? extends InspectionProfileEntry> getDescriptionContextClass() {
        return getClass();
    }

    public boolean isInitialized() {
        return true;
    }

    /**
     * @return short name of tool whose results will be used
     */
    
    public String getMainToolId() {
        return null;
    }

    
    public String loadDescription() {
        final String description = getStaticDescription();
        if (description != null) return description;

        try {
            URL descriptionUrl = getDescriptionUrl();
            if (descriptionUrl == null) return null;
            return ResourceUtil.loadText(descriptionUrl);
        }
        catch (IOException ignored) {
        }

        return null;
    }
}
