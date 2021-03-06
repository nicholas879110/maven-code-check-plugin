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
package com.gome.maven.ide.todo;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.editor.colors.EditorColorsListener;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.colors.EditorColorsScheme;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.psi.search.*;
import com.gome.maven.util.EventDispatcher;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.messages.MessageBus;
import org.jdom.Element;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

@State(
        name = "TodoConfiguration",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/editor.xml"),
                @Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml", deprecated = true)
        }
)
public class TodoConfiguration implements PersistentStateComponent<Element>, Disposable {
    private TodoPattern[] myTodoPatterns;
    private TodoFilter[] myTodoFilters;
    private IndexPattern[] myIndexPatterns;

    private final EventDispatcher<PropertyChangeListener> myPropertyChangeMulticaster = EventDispatcher.create(PropertyChangeListener.class);

     public static final String PROP_TODO_PATTERNS = "todoPatterns";
     public static final String PROP_TODO_FILTERS = "todoFilters";
     private static final String ELEMENT_PATTERN = "pattern";
     private static final String ELEMENT_FILTER = "filter";
    private final MessageBus myMessageBus;

    public TodoConfiguration( MessageBus messageBus, EditorColorsManager manager) {
        myMessageBus = messageBus;
        manager.addEditorColorsListener(new EditorColorsListener() {
            @Override
            public void globalSchemeChange(EditorColorsScheme scheme) {
                colorSettingsChanged();
            }
        }, this);
        resetToDefaultTodoPatterns();
    }

    @Override
    public void dispose() {
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void resetToDefaultTodoPatterns() {
        myTodoPatterns = getDefaultPatterns();
        myTodoFilters = new TodoFilter[]{};
        buildIndexPatterns();
    }

    
    private static TodoPattern[] getDefaultPatterns() {
        return new TodoPattern[]{
                new TodoPattern("\\btodo\\b.*", TodoAttributesUtil.createDefault(), false),
                new TodoPattern("\\bfixme\\b.*", TodoAttributesUtil.createDefault(), false),
        };
    }

    private void buildIndexPatterns() {
        myIndexPatterns = new IndexPattern[myTodoPatterns.length];
        for (int i = 0; i < myTodoPatterns.length; i++) {
            myIndexPatterns[i] = myTodoPatterns[i].getIndexPattern();
        }
    }

    public static TodoConfiguration getInstance() {
        return ServiceManager.getService(TodoConfiguration.class);
    }

    
    public TodoPattern[] getTodoPatterns() {
        return myTodoPatterns;
    }

    
    public IndexPattern[] getIndexPatterns() {
        return myIndexPatterns;
    }

    public void setTodoPatterns( TodoPattern[] todoPatterns) {
        doSetTodoPatterns(todoPatterns, true);
    }

    private void doSetTodoPatterns( TodoPattern[] todoPatterns, final boolean shouldNotifyIndices) {
        TodoPattern[] oldTodoPatterns = myTodoPatterns;
        IndexPattern[] oldIndexPatterns = myIndexPatterns;

        myTodoPatterns = todoPatterns;
        buildIndexPatterns();

        // only trigger index refresh actual index patterns have changed
        if (shouldNotifyIndices && !Arrays.deepEquals(myIndexPatterns, oldIndexPatterns)) {
            PropertyChangeEvent event = new PropertyChangeEvent(this, IndexPatternProvider.PROP_INDEX_PATTERNS, oldTodoPatterns, todoPatterns);
            myMessageBus.syncPublisher(IndexPatternProvider.INDEX_PATTERNS_CHANGED).propertyChange(event);
        }

        // only trigger gui and code daemon refresh when either the index patterns or presentation attributes have changed
        if (!Arrays.deepEquals(myTodoPatterns, oldTodoPatterns)) {
            PropertyChangeListener multicaster = myPropertyChangeMulticaster.getMulticaster();
            multicaster.propertyChange(new PropertyChangeEvent(this, PROP_TODO_PATTERNS, oldTodoPatterns, todoPatterns));
        }
    }

    /**
     * @return <code>TodoFilter</code> with specified <code>name</code>. Method returns
     *         <code>null</code> if there is no filter with <code>name</code>.
     */
    public TodoFilter getTodoFilter(String name) {
        for (TodoFilter filter : myTodoFilters) {
            if (filter.getName().equals(name)) {
                return filter;
            }
        }
        return null;
    }

    /**
     * @return all <code>TodoFilter</code>s.
     */
    
    public TodoFilter[] getTodoFilters() {
        return myTodoFilters;
    }

    public void setTodoFilters( TodoFilter[] filters) {
        TodoFilter[] oldFilters = myTodoFilters;
        myTodoFilters = filters;
        myPropertyChangeMulticaster.getMulticaster().propertyChange(new PropertyChangeEvent(this, PROP_TODO_FILTERS, oldFilters, filters));
    }

    /**
     * @deprecated use {@link TodoConfiguration#addPropertyChangeListener(PropertyChangeListener, Disposable)} instead
     */
    public void addPropertyChangeListener( PropertyChangeListener listener) {
        myPropertyChangeMulticaster.addListener(listener);
    }
    public void addPropertyChangeListener( PropertyChangeListener listener,  Disposable parentDisposable) {
        myPropertyChangeMulticaster.addListener(listener,parentDisposable);
    }
    /**
     * @deprecated use {@link TodoConfiguration#addPropertyChangeListener(PropertyChangeListener, Disposable)} instead
     */
    public void removePropertyChangeListener( PropertyChangeListener listener) {
        myPropertyChangeMulticaster.removeListener(listener);
    }

    @Override
    public void loadState(Element element) {
        List<TodoPattern> patternsList = new SmartList<TodoPattern>();
        List<TodoFilter> filtersList = new SmartList<TodoFilter>();
        for (Element child : (List<Element>)element.getChildren()) {
            if (ELEMENT_PATTERN.equals(child.getName())) {
                TodoPattern pattern = new TodoPattern(TodoAttributesUtil.createDefault());
                pattern.readExternal(child, TodoAttributesUtil.getDefaultColorSchemeTextAttributes());
                patternsList.add(pattern);
            }
            else if (ELEMENT_FILTER.equals(child.getName())) {
                TodoFilter filter = new TodoFilter();
                filter.readExternal(child, patternsList);
                filtersList.add(filter);
            }
        }
        doSetTodoPatterns(patternsList.toArray(new TodoPattern[patternsList.size()]), false);

        if (!(filtersList.isEmpty() && myTodoFilters.length == 0)) {
            setTodoFilters(filtersList.toArray(new TodoFilter[filtersList.size()]));
        }
    }

    @Override
    public Element getState() {
        Element element = new Element("state");
        TodoPattern[] todoPatterns = myTodoPatterns;
        if (!Arrays.equals(myTodoPatterns, getDefaultPatterns())) {
            for (TodoPattern pattern : todoPatterns) {
                Element child = new Element(ELEMENT_PATTERN);
                try {
                    pattern.writeExternal(child);
                }
                catch (WriteExternalException e) {
                    throw new RuntimeException(e);
                }
                element.addContent(child);
            }
        }

        for (TodoFilter filter : myTodoFilters) {
            Element child = new Element(ELEMENT_FILTER);
            filter.writeExternal(child, todoPatterns);
            element.addContent(child);
        }
        return element;
    }

    public void colorSettingsChanged() {
        for (TodoPattern pattern : myTodoPatterns) {
            TodoAttributes attributes = pattern.getAttributes();
            if (!attributes.shouldUseCustomTodoColor()) {
                attributes.setUseCustomTodoColor(false, TodoAttributesUtil.getDefaultColorSchemeTextAttributes());
            }
        }
    }
}
