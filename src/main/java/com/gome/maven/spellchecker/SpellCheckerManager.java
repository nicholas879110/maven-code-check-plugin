/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.spellchecker;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.impl.PsiModificationTrackerImpl;
import com.gome.maven.spellchecker.dictionary.EditableDictionary;
import com.gome.maven.spellchecker.dictionary.Loader;
import com.gome.maven.spellchecker.engine.SpellCheckerEngine;
import com.gome.maven.spellchecker.engine.SpellCheckerFactory;
import com.gome.maven.spellchecker.engine.SuggestionProvider;
import com.gome.maven.spellchecker.settings.SpellCheckerSettings;
import com.gome.maven.spellchecker.state.StateLoader;
import com.gome.maven.spellchecker.util.SPFileUtil;
import com.gome.maven.spellchecker.util.Strings;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.containers.ContainerUtil;

import java.io.InputStream;
import java.util.*;

public class SpellCheckerManager {

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.spellchecker.SpellCheckerManager");

    private static final int MAX_SUGGESTIONS_THRESHOLD = 5;
    private static final int MAX_METRICS = 1;

    private final Project project;

    private SpellCheckerEngine spellChecker;

    private EditableDictionary userDictionary;


    
    private final SuggestionProvider suggestionProvider = new BaseSuggestionProvider(this);

    private final SpellCheckerSettings settings;

    public static SpellCheckerManager getInstance(Project project) {
        return ServiceManager.getService(project, SpellCheckerManager.class);
    }

    public SpellCheckerManager(Project project, SpellCheckerSettings settings) {
        this.project = project;
        this.settings = settings;
        fullConfigurationReload();
    }

    public void fullConfigurationReload() {
        spellChecker = SpellCheckerFactory.create(project);
        fillEngineDictionary();
    }


    public void updateBundledDictionaries(final List<String> removedDictionaries) {
        for (BundledDictionaryProvider provider : Extensions.getExtensions(BundledDictionaryProvider.EP_NAME)) {
            for (String dictionary : provider.getBundledDictionaries()) {
                boolean dictionaryShouldBeLoad = settings == null || !settings.getBundledDisabledDictionariesPaths().contains(dictionary);
                boolean dictionaryIsLoad = spellChecker.isDictionaryLoad(dictionary);
                if (dictionaryIsLoad && !dictionaryShouldBeLoad) {
                    spellChecker.removeDictionary(dictionary);
                }
                else if (!dictionaryIsLoad && dictionaryShouldBeLoad) {
                    final Class<? extends BundledDictionaryProvider> loaderClass = provider.getClass();
                    final InputStream stream = loaderClass.getResourceAsStream(dictionary);
                    if (stream != null) {
                        spellChecker.loadDictionary(new StreamLoader(stream, dictionary));
                    }
                    else {
                        LOG.warn("Couldn't load dictionary '" + dictionary + "' with loader '" + loaderClass + "'");
                    }
                }
            }
        }
        if (settings != null && settings.getDictionaryFoldersPaths() != null) {
            final Set<String> disabledDictionaries = settings.getDisabledDictionariesPaths();
            for (String folder : settings.getDictionaryFoldersPaths()) {
                SPFileUtil.processFilesRecursively(folder, new Consumer<String>() {
                    @Override
                    public void consume(final String s) {
                        boolean dictionaryShouldBeLoad =!disabledDictionaries.contains(s);
                        boolean dictionaryIsLoad = spellChecker.isDictionaryLoad(s);
                        if (dictionaryIsLoad && !dictionaryShouldBeLoad) {
                            spellChecker.removeDictionary(s);
                        }
                        else if (!dictionaryIsLoad && dictionaryShouldBeLoad) {
                            spellChecker.loadDictionary(new FileLoader(s, s));
                        }

                    }
                });

            }
        }

        if (removedDictionaries != null && !removedDictionaries.isEmpty()) {
            for (final String name : removedDictionaries) {
                spellChecker.removeDictionary(name);
            }
        }

        restartInspections();
    }

    public Project getProject() {
        return project;
    }

    public EditableDictionary getUserDictionary() {
        return userDictionary;
    }

    private void fillEngineDictionary() {
        spellChecker.reset();
        final StateLoader stateLoader = new StateLoader(project);
        stateLoader.load(new Consumer<String>() {
            @Override
            public void consume(String s) {
                //do nothing - in this loader we don't worry about word list itself - the whole dictionary will be restored
            }
        });
        final List<Loader> loaders = new ArrayList<Loader>();
        // Load bundled dictionaries from corresponding jars
        for (BundledDictionaryProvider provider : Extensions.getExtensions(BundledDictionaryProvider.EP_NAME)) {
            for (String dictionary : provider.getBundledDictionaries()) {
                if (settings == null || !settings.getBundledDisabledDictionariesPaths().contains(dictionary)) {
                    final Class<? extends BundledDictionaryProvider> loaderClass = provider.getClass();
                    final InputStream stream = loaderClass.getResourceAsStream(dictionary);
                    if (stream != null) {
                        loaders.add(new StreamLoader(stream, dictionary));
                    }
                    else {
                        LOG.warn("Couldn't load dictionary '" + dictionary + "' with loader '" + loaderClass + "'");
                    }
                }
            }
        }
        if (settings != null && settings.getDictionaryFoldersPaths() != null) {
            final Set<String> disabledDictionaries = settings.getDisabledDictionariesPaths();
            for (String folder : settings.getDictionaryFoldersPaths()) {
                SPFileUtil.processFilesRecursively(folder, new Consumer<String>() {
                    @Override
                    public void consume(final String s) {
                        if (!disabledDictionaries.contains(s)) {
                            loaders.add(new FileLoader(s, s));
                        }
                    }
                });

            }
        }
        loaders.add(stateLoader);
        for (Loader loader : loaders) {
            spellChecker.loadDictionary(loader);
        }
        userDictionary = stateLoader.getDictionary();

    }


    public boolean hasProblem( String word) {
        return !spellChecker.isCorrect(word);
    }

    public void acceptWordAsCorrect( String word, Project project) {
        final String transformed = spellChecker.getTransformation().transform(word);
        if (transformed != null) {
            userDictionary.addToDictionary(transformed);
            final PsiModificationTrackerImpl modificationTracker =
                    (PsiModificationTrackerImpl)PsiManager.getInstance(project).getModificationTracker();
            modificationTracker.incCounter();
        }
    }

    public void updateUserDictionary( Collection<String> words) {
        userDictionary.replaceAll(words);
        restartInspections();
    }



    
    public static List<String> getBundledDictionaries() {
        final ArrayList<String> dictionaries = new ArrayList<String>();
        for (BundledDictionaryProvider provider : Extensions.getExtensions(BundledDictionaryProvider.EP_NAME)) {
            ContainerUtil.addAll(dictionaries, provider.getBundledDictionaries());
        }
        return dictionaries;
    }

    
    public static HighlightDisplayLevel getHighlightDisplayLevel() {
        return HighlightDisplayLevel.find(SpellCheckerSeveritiesProvider.TYPO);
    }

    
    public List<String> getSuggestions( String text) {
        return suggestionProvider.getSuggestions(text);
    }


    
    protected List<String> getRawSuggestions( String word) {
        if (!spellChecker.isCorrect(word)) {
            List<String> suggestions = spellChecker.getSuggestions(word, MAX_SUGGESTIONS_THRESHOLD, MAX_METRICS);
            if (!suggestions.isEmpty()) {
                boolean capitalized = Strings.isCapitalized(word);
                boolean upperCases = Strings.isUpperCase(word);
                if (capitalized) {
                    Strings.capitalize(suggestions);
                }
                else if (upperCases) {
                    Strings.upperCase(suggestions);
                }
            }
            return new ArrayList<String>(new LinkedHashSet<String>(suggestions));
        }
        return Collections.emptyList();
    }


    public static void restartInspections() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Project[] projects = ProjectManager.getInstance().getOpenProjects();
                for (Project project : projects) {
                    if (project.isInitialized() && project.isOpen() && !project.isDefault()) {
                        DaemonCodeAnalyzer.getInstance(project).restart();
                    }
                }
            }
        });
    }


}
