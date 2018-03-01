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
package com.gome.maven.spellchecker.dictionary;

import com.gome.maven.util.Consumer;

import java.util.Collection;
import java.util.Set;

public class AggregatedDictionary implements EditableDictionary {
     private static final String DICTIONARY_NAME = "common";
    private final EditableDictionary cachedDictionary;
    private final ProjectDictionary projectDictionary;

    
    @Override
    public String getName() {
        return DICTIONARY_NAME;
    }

    public AggregatedDictionary( ProjectDictionary projectDictionary,  EditableDictionary cachedDictionary) {
        this.projectDictionary = projectDictionary;
        this.cachedDictionary = cachedDictionary;
        this.cachedDictionary.addToDictionary(projectDictionary.getWords());
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    
    @Override
    public String toString() {
        return "AggregatedDictionary{" +
                "cachedDictionary=" + cachedDictionary +
                ", projectDictionary=" + projectDictionary +
                '}';
    }

    @Override
    
    public Boolean contains( String word) {
        return cachedDictionary.contains(word);
    }

    @Override
    public void addToDictionary(String word) {
        getProjectDictionary().addToDictionary(word);
        getCachedDictionary().addToDictionary(word);
    }

    @Override
    public void removeFromDictionary(String word) {
        getProjectDictionary().removeFromDictionary(word);
        getCachedDictionary().removeFromDictionary(word);
    }

    @Override
    public void replaceAll( Collection<String> words) {
        Set<String> oldWords = getProjectDictionary().getWords();
        getProjectDictionary().replaceAll(words);
        if (oldWords != null) {
            for (String word : oldWords) {
                if (words == null || !words.contains(word)) {
                    getCachedDictionary().removeFromDictionary(word);
                }
            }
        }
    }

    @Override
    public void clear() {
        getProjectDictionary().clear();
    }

    @Override
    public void traverse( final Consumer<String> consumer) {
        cachedDictionary.traverse(consumer);
    }

    @Override
    public Set<String> getWords() {
        return cachedDictionary.getWords();
    }

    @Override
    public int size() {
        return cachedDictionary.size();
    }

    @Override
    
    public Set<String> getEditableWords() {
        return getProjectDictionary().getEditableWords();
    }

    @Override
    public void addToDictionary( Collection<String> words) {
        getProjectDictionary().addToDictionary(words);
        getCachedDictionary().addToDictionary(words);
    }

    public EditableDictionary getCachedDictionary() {
        return cachedDictionary;
    }

    public ProjectDictionary getProjectDictionary() {
        return projectDictionary;
    }
}
