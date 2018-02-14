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

/**
 * @author cdr
 */
package com.gome.maven.codeInsight.intention.impl.config;

import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.ide.plugins.cl.PluginClassLoader;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.PluginId;
import com.gome.maven.util.lang.UrlClassLoader;

import java.net.MalformedURLException;
import java.net.URL;


public final class IntentionActionMetaData extends BeforeAfterActionMetaData {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.intention.impl.config.IntentionActionMetaData");
     private final IntentionAction myAction;
     public final String[] myCategory;
    private URL myDirURL = null;
     private static final String INTENTION_DESCRIPTION_FOLDER = "intentionDescriptions";

    public IntentionActionMetaData( IntentionAction action,
                                    ClassLoader loader,
                                    String[] category,
                                    String descriptionDirectoryName) {
        super(loader, descriptionDirectoryName);

        myAction = action;
        myCategory = category;
    }

    public IntentionActionMetaData( final IntentionAction action,
                                    final String[] category,
                                   final TextDescriptor description,
                                   final TextDescriptor[] exampleUsagesBefore,
                                   final TextDescriptor[] exampleUsagesAfter) {
        super(description, exampleUsagesBefore, exampleUsagesAfter);

        myAction = action;
        myCategory = category;
    }

    public String toString() {
        return getFamily();
    }

    
    private static URL getIntentionDescriptionDirURL(ClassLoader aClassLoader, String intentionFolderName) {
        final URL pageURL = aClassLoader.getResource(INTENTION_DESCRIPTION_FOLDER + "/" + intentionFolderName + "/" + DESCRIPTION_FILE_NAME);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Path:" + "intentionDescriptions/" + intentionFolderName);
            LOG.debug("URL:" + pageURL);
        }
        if (pageURL != null) {
            try {
                final String url = pageURL.toExternalForm();
                return UrlClassLoader.internProtocol(new URL(url.substring(0, url.lastIndexOf('/'))));
            }
            catch (MalformedURLException e) {
                LOG.error(e);
            }
        }
        return null;
    }

    
    public PluginId getPluginId() {
        if (myLoader instanceof PluginClassLoader) {
            return ((PluginClassLoader)myLoader).getPluginId();
        }
        return null;
    }

    
    public String getFamily() {
        return myAction.getFamilyName();
    }

    
    public IntentionAction getAction() {
        return myAction;
    }

    protected URL getDirURL() {
        if (myDirURL == null) {
            myDirURL = getIntentionDescriptionDirURL(myLoader, myDescriptionDirectoryName);
        }
        if (myDirURL == null) { //plugin compatibility
            myDirURL = getIntentionDescriptionDirURL(myLoader, getFamily());
        }
        LOG.assertTrue(myDirURL != null, "Intention Description Dir URL is null: " +
                getFamily() + "; " + myDescriptionDirectoryName + ", " + myLoader);
        return myDirURL;
    }
}
