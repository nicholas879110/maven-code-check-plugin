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
package com.gome.maven.openapi.options;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.util.text.UniqueNameGenerator;
import gnu.trove.THashSet;

import java.util.*;

public abstract class AbstractSchemesManager<T extends Scheme, E extends ExternalizableScheme> extends SchemesManager<T, E> {
    private static final Logger LOG = Logger.getInstance(AbstractSchemesManager.class);

    protected final List<T> mySchemes = new ArrayList<T>();
    private volatile T myCurrentScheme;
    private String myCurrentSchemeName;

    @Override
    public void addNewScheme( T scheme, boolean replaceExisting) {
        int toReplace = -1;
        for (int i = 0; i < mySchemes.size(); i++) {
            T existingScheme = mySchemes.get(i);
            if (existingScheme.getName().equals(scheme.getName())) {
                toReplace = i;
                break;
            }
        }
        if (toReplace == -1) {
            mySchemes.add(scheme);
        }
        else if (replaceExisting || !(scheme instanceof ExternalizableScheme)) {
            mySchemes.set(toReplace, scheme);
        }
        else {
            //noinspection unchecked
            renameScheme((ExternalizableScheme)scheme, UniqueNameGenerator.generateUniqueName(scheme.getName(), collectExistingNames(mySchemes)));
            mySchemes.add(scheme);
        }
        schemeAdded(scheme);
        checkCurrentScheme(scheme);
    }

    protected void checkCurrentScheme( Scheme scheme) {
        if (myCurrentScheme == null && scheme.getName().equals(myCurrentSchemeName)) {
            //noinspection unchecked
            myCurrentScheme = (T)scheme;
        }
    }

    
    private Collection<String> collectExistingNames( Collection<T> schemes) {
        Set<String> result = new THashSet<String>(schemes.size());
        for (T scheme : schemes) {
            result.add(scheme.getName());
        }
        return result;
    }

    @Override
    public void clearAllSchemes() {
        for (T myScheme : mySchemes) {
            schemeDeleted(myScheme);
        }
        mySchemes.clear();
    }

    @Override
    
    public List<T> getAllSchemes() {
        return Collections.unmodifiableList(mySchemes);
    }

    @Override
    
    public T findSchemeByName( String schemeName) {
        for (T scheme : mySchemes) {
            if (scheme.getName().equals(schemeName)) {
                return scheme;
            }
        }
        return null;
    }

    @Override
    public void setCurrentSchemeName( String schemeName) {
        myCurrentSchemeName = schemeName;
        myCurrentScheme = schemeName == null ? null : findSchemeByName(schemeName);
    }

    @Override
    
    public T getCurrentScheme() {
        T currentScheme = myCurrentScheme;
        return currentScheme == null ? null : findSchemeByName(currentScheme.getName());
    }

    @Override
    public void removeScheme( T scheme) {
        for (int i = 0, n = mySchemes.size(); i < n; i++) {
            T s = mySchemes.get(i);
            if (scheme.getName().equals(s.getName())) {
                schemeDeleted(s);
                mySchemes.remove(i);
                break;
            }
        }
    }

    protected void schemeDeleted( Scheme scheme) {
        if (myCurrentScheme == scheme) {
            myCurrentScheme = null;
        }
    }

    @Override
    
    public Collection<String> getAllSchemeNames() {
        List<String> names = new ArrayList<String>(mySchemes.size());
        for (T scheme : mySchemes) {
            names.add(scheme.getName());
        }
        return names;
    }

    protected abstract void schemeAdded( T scheme);

    protected static void renameScheme( ExternalizableScheme scheme,  String newName) {
        if (!newName.equals(scheme.getName())) {
            scheme.setName(newName);
            LOG.assertTrue(newName.equals(scheme.getName()));
        }
    }
}
