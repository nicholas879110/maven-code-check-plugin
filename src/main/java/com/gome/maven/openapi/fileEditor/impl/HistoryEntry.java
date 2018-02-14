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
package com.gome.maven.openapi.fileEditor.impl;

import com.gome.maven.openapi.fileEditor.FileEditorProvider;
import com.gome.maven.openapi.fileEditor.FileEditorState;
import com.gome.maven.openapi.fileEditor.ex.FileEditorProviderManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.util.containers.HashMap;
import org.jdom.Element;

import java.util.List;
import java.util.Map;

final class HistoryEntry{
     static final String TAG = "entry";
    private static final String FILE_ATTR = "file";
     private static final String PROVIDER_ELEMENT = "provider";
     private static final String EDITOR_TYPE_ID_ATTR = "editor-type-id";
     private static final String SELECTED_ATTR_VALUE = "selected";
     private static final String STATE_ELEMENT = "state";

    public final VirtualFile myFile;
    /**
     * can be null when read from XML
     */
    public FileEditorProvider mySelectedProvider;
    private final HashMap<FileEditorProvider, FileEditorState> myProvider2State;

    public HistoryEntry( VirtualFile file,  FileEditorProvider[] providers,  FileEditorState[] states,  FileEditorProvider selectedProvider){
        myFile = file;
        myProvider2State = new HashMap<FileEditorProvider, FileEditorState>();
        mySelectedProvider = selectedProvider;
        for (int i = 0; i < providers.length; i++) {
            putState(providers[i], states[i]);
        }
    }

    public HistoryEntry( Project project,  Element e) throws InvalidDataException {
        myFile = getVirtualFile(e);
        myProvider2State = new HashMap<FileEditorProvider, FileEditorState>();

        List providers = e.getChildren(PROVIDER_ELEMENT);
        for (final Object provider1 : providers) {
            Element _e = (Element)provider1;

            String typeId = _e.getAttributeValue(EDITOR_TYPE_ID_ATTR);
            FileEditorProvider provider = FileEditorProviderManager.getInstance().getProvider(typeId);
            if (provider == null) {
                continue;
            }
            if (Boolean.valueOf(_e.getAttributeValue(SELECTED_ATTR_VALUE))) {
                mySelectedProvider = provider;
            }

            Element stateElement = _e.getChild(STATE_ELEMENT);
            if (stateElement == null) {
                throw new InvalidDataException();
            }

            FileEditorState state = provider.readState(stateElement, project, myFile);
            putState(provider, state);
        }
    }

    public FileEditorState getState( FileEditorProvider provider) {
        return myProvider2State.get(provider);
    }

    public void putState( FileEditorProvider provider,  FileEditorState state) {
        myProvider2State.put(provider, state);
    }

    /**
     * @return element that was added to the <code>element</code>.
     * Returned element has tag {@link #TAG}. Never null.
     */
    public Element writeExternal(Element element, Project project) {
        Element e = new Element(TAG);
        element.addContent(e);
        e.setAttribute(FILE_ATTR, myFile.getUrl());

        for (final Map.Entry<FileEditorProvider, FileEditorState> entry : myProvider2State.entrySet()) {
            FileEditorProvider provider = entry.getKey();

            Element providerElement = new Element(PROVIDER_ELEMENT);
            if (provider.equals(mySelectedProvider)) {
                providerElement.setAttribute(SELECTED_ATTR_VALUE, Boolean.TRUE.toString());
            }
            providerElement.setAttribute(EDITOR_TYPE_ID_ATTR, provider.getEditorTypeId());
            Element stateElement = new Element(STATE_ELEMENT);
            providerElement.addContent(stateElement);
            provider.writeState(entry.getValue(), project, stateElement);

            e.addContent(providerElement);
        }

        return e;
    }

    
    public static VirtualFile getVirtualFile(Element historyElement) throws InvalidDataException {
        if (!historyElement.getName().equals(TAG)) {
            throw new IllegalArgumentException("unexpected tag: " + historyElement);
        }

        String url = historyElement.getAttributeValue(FILE_ATTR);
        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
        if (file == null){
            throw new InvalidDataException("No file exists: " + url);
        }
        return file;
    }
}
