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
import gnu.trove.THashSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ProjectDictionary implements EditableDictionary {
     private static final String DEFAULT_CURRENT_USER_NAME = "default.user";
    private static final String DEFAULT_PROJECT_DICTIONARY_NAME = "project";
    private String activeName;
    private Set<EditableDictionary> dictionaries;

    public ProjectDictionary() {
    }

    public ProjectDictionary( Set<EditableDictionary> dictionaries) {
        this.dictionaries = dictionaries;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    
    @Override
    public String getName() {
        return DEFAULT_PROJECT_DICTIONARY_NAME;
    }

    public void setActiveName(String name) {
        activeName = name;
    }

    @Override
    
    public Boolean contains( String word) {
        if (dictionaries == null) {
            return false;
        }
        int errors = 0;
        for (Dictionary dictionary : dictionaries) {
            Boolean contains = dictionary.contains(word);
            if (contains == null) {
                errors++;
            }
            else if (contains) {
                return true;
            }
        }
        if (errors==dictionaries.size()) return null;//("WORD_OF_ENTIRELY_UNKNOWN_LETTERS_FOR_ALL");
        return false;
    }

    @Override
    public void addToDictionary(String word) {
        getActiveDictionary().addToDictionary(word);
    }

    @Override
    public void removeFromDictionary(String word) {
        getActiveDictionary().removeFromDictionary(word);
    }

    
    private EditableDictionary getActiveDictionary() {
        return ensureCurrentUserDictionary();
    }

    
    private EditableDictionary ensureCurrentUserDictionary() {
        if (activeName == null) {
            activeName = DEFAULT_CURRENT_USER_NAME;
        }
        EditableDictionary result = getDictionaryByName(activeName);
        if (result == null) {
            result = new UserDictionary(activeName);
            if (dictionaries == null) {
                dictionaries = new THashSet<EditableDictionary>();
            }
            dictionaries.add(result);
        }
        return result;
    }

    
    private EditableDictionary getDictionaryByName( String name) {
        if (dictionaries == null) {
            return null;
        }
        EditableDictionary result = null;
        for (EditableDictionary dictionary : dictionaries) {
            if (dictionary.getName().equals(name)) {
                result = dictionary;
                break;
            }
        }
        return result;
    }

    @Override
    public void replaceAll( Collection<String> words) {
        getActiveDictionary().replaceAll(words);
    }

    @Override
    public void clear() {
        getActiveDictionary().clear();
    }


    @Override
    
    public Set<String> getWords() {
        if (dictionaries == null) {
            return null;
        }
        Set<String> words = new HashSet<String>();
        for (Dictionary dictionary : dictionaries) {
            Set<String> otherWords = dictionary.getWords();
            if (otherWords != null) {
                words.addAll(otherWords);
            }
        }
        return words;
    }

    @Override
    public int size(){
        int result = 0;
        for (Dictionary dictionary : dictionaries) {
            result+=dictionary.size();
        }
        return result;
    }

    @Override
    public void traverse( final Consumer<String> consumer) {
        if (dictionaries == null) {
            return;
        }

        for (EditableDictionary dictionary : dictionaries) {
            dictionary.traverse(consumer);
        }

    }

    @Override
    
    public Set<String> getEditableWords() {
        return getActiveDictionary().getWords();
    }


    @Override
    public void addToDictionary( Collection<String> words) {
        getActiveDictionary().addToDictionary(words);
    }

    public Set<EditableDictionary> getDictionaries() {
        return dictionaries;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProjectDictionary that = (ProjectDictionary)o;

        if (activeName != null ? !activeName.equals(that.activeName) : that.activeName != null) return false;
        if (dictionaries != null ? !dictionaries.equals(that.dictionaries) : that.dictionaries != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = activeName != null ? activeName.hashCode() : 0;
        result = 31 * result + (dictionaries != null ? dictionaries.hashCode() : 0);
        return result;
    }

    
    @Override
    public String toString() {
        return "ProjectDictionary{" + "activeName='" + activeName + '\'' + ", dictionaries=" + dictionaries + '}';
    }
}