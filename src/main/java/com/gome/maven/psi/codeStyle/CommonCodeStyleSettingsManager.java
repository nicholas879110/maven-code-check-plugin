/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.psi.codeStyle;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMExternalizable;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.containers.HashMap;
import org.jdom.Content;
import org.jdom.Element;

import java.util.*;

/**
 * Manages common code style settings for every language using them.
 *
 * @author Rustam Vishnyakov
 */
public class CommonCodeStyleSettingsManager implements JDOMExternalizable {
    private volatile Map<Language, CommonCodeStyleSettings> myCommonSettingsMap;
    private volatile Map<String, Content> myUnknownSettingsMap;

     private final CodeStyleSettings myParentSettings;

     private static final String COMMON_SETTINGS_TAG = "codeStyleSettings";
    private static final String LANGUAGE_ATTR = "language";


    CommonCodeStyleSettingsManager( CodeStyleSettings parentSettings) {
        myParentSettings = parentSettings;
    }

    /**
     * Attempts to get language-specific common settings from <code>LanguageCodeStyleSettingsProvider</code>.
     *
     * @param lang The language to get settings for.
     * @return If the provider for the language exists and is able to create language-specific default settings
     *         (<code>LanguageCodeStyleSettingsProvider.getDefaultCommonSettings()</code> doesn't return null)
     *         returns the instance of settings for this language. Otherwise returns the instance of parent settings
     *         shared between several languages.
     */
    public CommonCodeStyleSettings getCommonSettings( Language lang) {
        Map<Language, CommonCodeStyleSettings> commonSettingsMap = getCommonSettingsMap();
        CommonCodeStyleSettings settings = commonSettingsMap.get(lang);
        if (settings == null && lang != null) {
            settings = commonSettingsMap.get(lang.getBaseLanguage());
        }
        if (settings != null) {
            return settings;
        }
        return myParentSettings;
    }

    
    private Map<Language, CommonCodeStyleSettings> getCommonSettingsMap() {
        Map<Language, CommonCodeStyleSettings> commonSettingsMap = myCommonSettingsMap;
        if (commonSettingsMap == null) {
            synchronized (this) {
                commonSettingsMap = myCommonSettingsMap;
                if (commonSettingsMap == null) {
                    commonSettingsMap = initCommonSettingsMap();
                    initNonReadSettings();
                }
            }
        }
        return commonSettingsMap;
    }

    /**
     * Get common code style settings by language name. <code>getCommonSettings(Language)</code> is a preferred method but
     * sometimes (for example, in plug-ins which do not depend on a specific language support) language settings can be
     * obtained by name.
     *
     * @param langName The display name of the language whose settings must be returned.
     * @return Common code style settings for the given language or parent (shared) settings if not found.
     */
    
    public CommonCodeStyleSettings getCommonSettings( String langName) {
        Map<Language, CommonCodeStyleSettings> map = getCommonSettingsMap();
        for (Map.Entry<Language, CommonCodeStyleSettings> entry : map.entrySet()) {
            if (langName.equals(entry.getKey().getDisplayName())) {
                return entry.getValue();
            }
        }
        return myParentSettings;
    }


    private void initNonReadSettings() {
        final LanguageCodeStyleSettingsProvider[] providers = Extensions.getExtensions(LanguageCodeStyleSettingsProvider.EP_NAME);
        for (final LanguageCodeStyleSettingsProvider provider : providers) {
            Language target = provider.getLanguage();
            if (!myCommonSettingsMap.containsKey(target)) {
                CommonCodeStyleSettings initialSettings = provider.getDefaultCommonSettings();
                if (initialSettings != null) {
                    init(initialSettings, target);
                }
            }
        }
    }

    private void init( CommonCodeStyleSettings initialSettings,  Language target) {
        initialSettings.setRootSettings(myParentSettings);
        registerCommonSettings(target, initialSettings);
    }

    private Map<Language, CommonCodeStyleSettings> initCommonSettingsMap() {
        Map<Language, CommonCodeStyleSettings> map = new LinkedHashMap<Language, CommonCodeStyleSettings>();
        myCommonSettingsMap = map;
        myUnknownSettingsMap = new LinkedHashMap<String, Content>();
        return map;
    }

    private void registerCommonSettings( Language lang,  CommonCodeStyleSettings settings) {
        synchronized (this) {
            if (!myCommonSettingsMap.containsKey(lang)) {
                myCommonSettingsMap.put(lang, settings);
                settings.getRootSettings(); // check not null
            }
        }
    }

    
    public CommonCodeStyleSettingsManager clone( CodeStyleSettings parentSettings) {
        synchronized (this) {
            CommonCodeStyleSettingsManager settingsManager = new CommonCodeStyleSettingsManager(parentSettings);
            if (myCommonSettingsMap != null && !myCommonSettingsMap.isEmpty()) {
                settingsManager.initCommonSettingsMap();
                for (Map.Entry<Language, CommonCodeStyleSettings> entry : myCommonSettingsMap.entrySet()) {
                    CommonCodeStyleSettings clonedSettings = entry.getValue().clone(parentSettings);
                    settingsManager.registerCommonSettings(entry.getKey(), clonedSettings);
                }
            }
            return settingsManager;
        }
    }

    @Override
    public void readExternal( Element element) throws InvalidDataException {
        synchronized (this) {
            initCommonSettingsMap();
            final List list = element.getChildren(COMMON_SETTINGS_TAG);
            for (Object o : list) {
                final Element commonSettingsElement = (Element)o;
                final String languageId = commonSettingsElement.getAttributeValue(LANGUAGE_ATTR);
                if (languageId != null && !languageId.isEmpty()) {
                    Language target = Language.findLanguageByID(languageId);
                    boolean isKnownLanguage = target != null;
                    if (isKnownLanguage) {
                        final LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(target);
                        if (provider != null) {
                            CommonCodeStyleSettings settings = provider.getDefaultCommonSettings();
                            if (settings != null) {
                                settings.readExternal(commonSettingsElement);
                                init(settings, target);
                            }
                        }
                        else {
                            isKnownLanguage = false;
                        }
                    }
                    if (!isKnownLanguage) {
                        myUnknownSettingsMap.put(languageId, (Content)commonSettingsElement.clone());
                    }
                }
            }
            initNonReadSettings();
        }
    }

    @Override
    public void writeExternal( Element element) throws WriteExternalException {
        synchronized (this) {
            if (myCommonSettingsMap == null) return;

            final Map<String, Language> id2lang = new HashMap<String, Language>();
            for (final Language language : myCommonSettingsMap.keySet()) {
                id2lang.put(language.getID(), language);
            }

            final Set<String> langIdList = new HashSet<String>();
            langIdList.addAll(myUnknownSettingsMap.keySet());
            langIdList.addAll(id2lang.keySet());

            final String[] languages = ArrayUtil.toStringArray(langIdList);
            Arrays.sort(languages, new Comparator<String>() {
                @Override
                public int compare( final String o1, final String o2) {
                    return o1.compareTo(o2);
                }
            });

            for (final String id : languages) {
                final Language language = id2lang.get(id);
                if (language != null) {
                    final CommonCodeStyleSettings commonSettings = myCommonSettingsMap.get(language);
                    Element commonSettingsElement = new Element(COMMON_SETTINGS_TAG);
                    commonSettings.writeExternal(commonSettingsElement);
                    commonSettingsElement.setAttribute(LANGUAGE_ATTR, language.getID());
                    if (!commonSettingsElement.getChildren().isEmpty()) {
                        element.addContent(commonSettingsElement);
                    }
                } else {
                    final Content unknown = myUnknownSettingsMap.get(id);
                    if (unknown != null) element.addContent(unknown.detach());
                }
            }
        }
    }

    public static void copy( CommonCodeStyleSettings source,  CommonCodeStyleSettings target) {
        CommonCodeStyleSettings.copyPublicFields(source, target);
        CommonCodeStyleSettings.IndentOptions targetIndentOptions = target.getIndentOptions();
        if (targetIndentOptions != null) {
            CommonCodeStyleSettings.IndentOptions sourceIndentOptions = source.getIndentOptions();
            if (sourceIndentOptions != null) {
                CommonCodeStyleSettings.copyPublicFields(sourceIndentOptions, targetIndentOptions);
            }
        }
    }
}
