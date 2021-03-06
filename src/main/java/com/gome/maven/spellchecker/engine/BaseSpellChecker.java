/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.spellchecker.engine;

import com.gome.maven.codeInsight.daemon.DaemonCodeAnalyzer;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.text.LevenshteinDistance;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.spellchecker.compress.CompressedDictionary;
import com.gome.maven.spellchecker.dictionary.Dictionary;
import com.gome.maven.spellchecker.dictionary.EditableDictionary;
import com.gome.maven.spellchecker.dictionary.EditableDictionaryLoader;
import com.gome.maven.spellchecker.dictionary.Loader;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.UIUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class BaseSpellChecker implements SpellCheckerEngine {
    static final Logger LOG = Logger.getInstance("#com.gome.maven.spellchecker.engine.BaseSpellChecker");

    private final Transformation transform = new Transformation();

    private final Set<EditableDictionary> dictionaries = new HashSet<EditableDictionary>();
    private final List<Dictionary> bundledDictionaries = ContainerUtil.createLockFreeCopyOnWriteList();
    private final LevenshteinDistance metrics = new LevenshteinDistance();

    private final AtomicBoolean myLoadingDictionaries = new AtomicBoolean(false);
    private final List<Pair<Loader, Consumer<Dictionary>>> myDictionariesToLoad = ContainerUtil.createLockFreeCopyOnWriteList();
    private final Project myProject;

    public BaseSpellChecker(final Project project) {
        myProject = project;
    }


    @Override
    public void loadDictionary( Loader loader) {
        if (loader instanceof EditableDictionaryLoader) {
            final EditableDictionary dictionary = ((EditableDictionaryLoader)loader).getDictionary();
            if (dictionary != null) {
                addModifiableDictionary(dictionary);
            }
        }
        else {
            loadCompressedDictionary(loader);
        }
    }

    private void loadCompressedDictionary( Loader loader) {
        if (ApplicationManager.getApplication().isUnitTestMode() || ApplicationManager.getApplication().isHeadlessEnvironment()) {
            final CompressedDictionary dictionary = CompressedDictionary.create(loader, transform);
            addCompressedFixedDictionary(dictionary);
        }
        else {
            loadDictionaryAsync(loader, new Consumer<Dictionary>() {
                @Override
                public void consume(final Dictionary dictionary) {
                    addCompressedFixedDictionary(dictionary);
                }
            });
        }
    }

    private void loadDictionaryAsync( final Loader loader,  final Consumer<Dictionary> consumer) {
        if (myLoadingDictionaries.compareAndSet(false, true)) {
            LOG.debug("Loading " + loader.getName());
            _doLoadDictionaryAsync(loader, consumer);
        }
        else {
            queueDictionaryLoad(loader, consumer);
        }
    }

    private void _doLoadDictionaryAsync(final Loader loader, final Consumer<Dictionary> consumer) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                LOG.debug("Loading " + loader.getName());
                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ApplicationManager.getApplication().isDisposed()) return;

                        final CompressedDictionary dictionary = CompressedDictionary.create(loader, transform);
                        LOG.debug(loader.getName() + " loaded!");
                        consumer.consume(dictionary);

                        while (!myDictionariesToLoad.isEmpty()) {
                            if (ApplicationManager.getApplication().isDisposed()) return;

                            final Pair<Loader, Consumer<Dictionary>> nextDictionary = myDictionariesToLoad.remove(0);
                            Loader nextDictionaryLoader = nextDictionary.getFirst();
                            CompressedDictionary dictionary1 = CompressedDictionary.create(nextDictionaryLoader, transform);
                            LOG.debug(nextDictionaryLoader.getName() + " loaded!");
                            nextDictionary.getSecond().consume(dictionary1);
                        }

                        LOG.debug("Loading finished, restarting daemon...");
                        myLoadingDictionaries.set(false);
                        UIUtil.invokeLaterIfNeeded(new Runnable() {
                            @Override
                            public void run() {
                                if (ApplicationManager.getApplication().isDisposed()) return;

                                for (final Project project : ProjectManager.getInstance().getOpenProjects()) {
                                    if (project.isInitialized() && project.isOpen() && !project.isDefault()) {
                                        final DaemonCodeAnalyzer instance = DaemonCodeAnalyzer.getInstance(project);
                                        if (instance != null) instance.restart();
                                    }
                                }
                            }
                        });
                    }
                });
            }
        };


        StartupManager.getInstance(myProject).runWhenProjectIsInitialized(runnable);
    }

    private void queueDictionaryLoad(final Loader loader, final Consumer<Dictionary> consumer) {
        LOG.debug("Queuing load for: " + loader.getName());
        myDictionariesToLoad.add(Pair.create(loader, consumer));
    }

    private void addModifiableDictionary( EditableDictionary dictionary) {
        dictionaries.add(dictionary);
    }

    private void addCompressedFixedDictionary( Dictionary dictionary) {
        bundledDictionaries.add(dictionary);
    }

    @Override
    public Transformation getTransformation() {
        return transform;
    }

    
    private static List<String> restore(char startFrom, int i, int j,  Collection<? extends Dictionary> dictionaries) {
        List<String> results = new ArrayList<String>();

        for (Dictionary o : dictionaries) {
            results.addAll(restore(startFrom, i, j, o));
        }
        return results;
    }

    
    private static List<String> restore(final char first, final int i, final int j,  Dictionary dictionary) {
        final List<String> result = new ArrayList<String>();
        if (dictionary instanceof CompressedDictionary) {
            result.addAll(((CompressedDictionary)dictionary).getWords(first, i, j));
        }
        else {
            dictionary.traverse(new Consumer<String>() {
                @Override
                public void consume(String s) {
                    if (StringUtil.isEmpty(s)) {
                        return;
                    }
                    if (s.charAt(0) == first && s.length() >= i && s.length() <= j) {
                        result.add(s);
                    }
                }
            });
        }

        return result;
    }

    /**
     * @param transformed
     * @param dictionaries
     * @return -1 (all)failed / 0 (any) ok / >0 all alien
     */
    private static int isCorrect( String transformed,  Collection<? extends Dictionary> dictionaries) {
        if (dictionaries == null) {
            return -1;
        }

        //System.out.println("dictionaries = " + dictionaries);
        int errors = 0;
        for (Dictionary dictionary : dictionaries) {
            if (dictionary == null) continue;
            //System.out.print("\tBSC.isCorrect " + transformed + " " + dictionary);
            Boolean contains = dictionary.contains(transformed);
            //System.out.println("\tcontains = " + contains);
            if (contains==null) ++errors;
            else if (contains) return 0;
        }
        if (errors == dictionaries.size()) return errors;
        return -1;
    }

    @Override
    public boolean isCorrect( String word) {
        //System.out.println("---\n"+word);
        final String transformed = transform.transform(word);
        if (myLoadingDictionaries.get() || transformed == null) {
            return true;
        }
        int bundled = isCorrect(transformed, bundledDictionaries);
        int user = isCorrect(transformed, dictionaries);
        //System.out.println("bundled = " + bundled);
        //System.out.println("user = " + user);
        return bundled == 0 || user == 0 || bundled > 0 && user > 0;
    }


    @Override
    
    public List<String> getSuggestions( final String word, int threshold, int quality) {
        final String transformed = transform.transform(word);
        if (transformed == null) {
            return Collections.emptyList();
        }
        final List<Suggestion> suggestions = new ArrayList<Suggestion>();
        List<String> rawSuggestions = restore(transformed.charAt(0), 0, Integer.MAX_VALUE, bundledDictionaries);
        rawSuggestions.addAll(restore(word.charAt(0), 0, Integer.MAX_VALUE, dictionaries));
        for (String rawSuggestion : rawSuggestions) {
            final int distance = metrics.calculateMetrics(transformed, rawSuggestion);
            suggestions.add(new Suggestion(rawSuggestion, distance));
        }
        List<String> result = new ArrayList<String>();
        if (suggestions.isEmpty()) {
            return result;
        }
        Collections.sort(suggestions);
        int bestMetrics = suggestions.get(0).getMetrics();
        for (int i = 0; i < threshold; i++) {

            if (suggestions.size() <= i || bestMetrics - suggestions.get(i).getMetrics() > quality) {
                break;
            }
            result.add(i, suggestions.get(i).getWord());
        }
        return result;
    }


    @Override
    
    public List<String> getVariants( String prefix) {
        //if (StringUtil.isEmpty(prefix)) {
        return Collections.emptyList();
        //}

    }

    @Override
    public void reset() {
        bundledDictionaries.clear();
        dictionaries.clear();
    }

    @Override
    public boolean isDictionaryLoad( String name) {
        return getBundledDictionaryByName(name) != null;
    }

    @Override
    public void removeDictionary( String name) {
        final Dictionary dictionaryByName = getBundledDictionaryByName(name);
        if (dictionaryByName != null) {
            bundledDictionaries.remove(dictionaryByName);
        }
    }

    
    public Dictionary getBundledDictionaryByName( String name) {
        if (bundledDictionaries == null) {
            return null;
        }
        for (Dictionary dictionary : bundledDictionaries) {
            if (name.equals(dictionary.getName())) {
                return dictionary;
            }
        }
        return null;
    }
}
