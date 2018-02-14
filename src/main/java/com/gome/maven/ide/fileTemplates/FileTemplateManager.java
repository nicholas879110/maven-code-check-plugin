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

package com.gome.maven.ide.fileTemplates;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.util.Key;

import java.util.Collection;
import java.util.Properties;

/**
 * @author MYakovlev
 * Date: Jul 24, 2002
 */
public abstract class FileTemplateManager{
    public static final Key<Properties> DEFAULT_TEMPLATE_PROPERTIES = Key.create("DEFAULT_TEMPLATE_PROPERTIES");
    public static final int RECENT_TEMPLATES_SIZE = 25;

    
    public static final String INTERNAL_HTML_TEMPLATE_NAME = "Html";
    
    public static final String INTERNAL_HTML5_TEMPLATE_NAME = "Html5";
    
    public static final String INTERNAL_XHTML_TEMPLATE_NAME = "Xhtml";
    
    public static final String FILE_HEADER_TEMPLATE_NAME = "File Header";

    public static final String DEFAULT_TEMPLATES_CATEGORY = "Default";
    public static final String INTERNAL_TEMPLATES_CATEGORY = "Internal";
    public static final String INCLUDES_TEMPLATES_CATEGORY = "Includes";
    public static final String CODE_TEMPLATES_CATEGORY = "Code";
    public static final String J2EE_TEMPLATES_CATEGORY = "J2EE";

    public static final String PROJECT_NAME_VARIABLE = "PROJECT_NAME";

    public static FileTemplateManager getInstance( Project project){
        return ServiceManager.getService(project, FileTemplateManager.class).checkInitialized();
    }

    protected FileTemplateManager checkInitialized() { return this; }

    /** Use {@link #getInstance(Project)} instead */
    @Deprecated
    public static FileTemplateManager getInstance(){
        return getDefaultInstance();
    }

    public static FileTemplateManager getDefaultInstance(){
        return getInstance(ProjectManager.getInstance().getDefaultProject());
    }

    
    public abstract FileTemplatesScheme getCurrentScheme();

    public abstract void setCurrentScheme( FileTemplatesScheme scheme);

    /**
     * @return Project scheme, or null if manager is created for default project.
     */
    public abstract FileTemplatesScheme getProjectScheme();

    public abstract FileTemplate[] getTemplates(String category);

    /**
     *  Returns all templates from "Default" category.
     */
    
    public abstract FileTemplate[] getAllTemplates();

    public abstract FileTemplate getTemplate(  String templateName);

    
    public abstract Properties getDefaultProperties();

    @Deprecated /** Use {@link #getDefaultProperties()} instead */
    
    public Properties getDefaultProperties( Project project) {
        Properties properties = getDefaultProperties();
        properties.setProperty(PROJECT_NAME_VARIABLE, project.getName());
        return properties;
    }

    /**
     * Creates a new template with specified name, and adds it to the list of default templates.
     * @return created template
     */
    
    public abstract FileTemplate addTemplate(  String name,   String extension);

    public abstract void removeTemplate( FileTemplate template);

    
    public abstract Collection<String> getRecentNames();

    public abstract void addRecentName(  String name);

    public abstract FileTemplate getInternalTemplate(  String templateName);

    
    public abstract FileTemplate[] getInternalTemplates();

    public abstract FileTemplate getJ2eeTemplate(  String templateName);
    public abstract FileTemplate getCodeTemplate(  String templateName);

    
    public abstract FileTemplate[] getAllPatterns();

    
    public abstract FileTemplate[] getAllCodeTemplates();

    
    public abstract FileTemplate[] getAllJ2eeTemplates();

    
    public abstract String internalTemplateToSubject(  String templateName);

    @Deprecated
    
    public abstract String localizeInternalTemplateName( FileTemplate template);

    public abstract FileTemplate getPattern(  String name);

    /**
     * Returns template with default (bundled) text.
     */
    
    public abstract FileTemplate getDefaultTemplate(  String name);

    public abstract void setTemplates( String templatesCategory,  Collection<FileTemplate> templates);

    public abstract void saveAllTemplates();
}
