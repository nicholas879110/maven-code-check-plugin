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

package com.gome.maven.ui.tabs;

import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.ui.ColorUtil;
import com.gome.maven.ui.FileColorManager;
import com.gome.maven.ui.JBColor;
import com.gome.maven.ui.LightColors;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.UIUtil;
import org.jdom.Element;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
@State(
        name = "FileColors",
        storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
public class FileColorManagerImpl extends FileColorManager implements PersistentStateComponent<Element> {
    public static final String FC_ENABLED = "FileColorsEnabled";
    public static final String FC_TABS_ENABLED = "FileColorsForTabsEnabled";
    public static final String FC_PROJECT_VIEW_ENABLED = "FileColorsForProjectViewEnabled";
    private final Project myProject;
    private final FileColorsModel myModel;
    private FileColorSharedConfigurationManager mySharedConfigurationManager;

    private static final Map<String, Color> ourDefaultColors = ContainerUtil.<String, Color>immutableMapBuilder()
            .put("Blue", new JBColor(new Color(0xdcf0ff), new Color(0x3C476B)))
            .put("Green", new JBColor(new Color(231, 250, 219), new Color(0x425444)))
            .put("Orange", new JBColor(new Color(246, 224, 202), new Color(0x804A33)))
            .put("Rose", new JBColor(new Color(242, 206, 202), new Color(0x6E414E)))
            .put("Violet", new JBColor(new Color(222, 213, 241), new Color(0x504157)))
            .put("Yellow", new JBColor(new Color(255, 255, 228), new Color(0x4F4838)))
            .build();

    public FileColorManagerImpl( final Project project) {
        myProject = project;
        myModel = new FileColorsModel(project);
    }

    private void initSharedConfigurations() {
        if (mySharedConfigurationManager == null) {
            mySharedConfigurationManager = ServiceManager.getService(myProject, FileColorSharedConfigurationManager.class);
        }
    }

    @Override
    public boolean isEnabled() {
        return _isEnabled();
    }

    public static boolean _isEnabled() {
        return PropertiesComponent.getInstance().getBoolean(FC_ENABLED, true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        PropertiesComponent.getInstance().setValue(FC_ENABLED, Boolean.toString(enabled));
    }

    public void setEnabledForTabs(boolean enabled) {
        PropertiesComponent.getInstance().setValue(FC_TABS_ENABLED, Boolean.toString(enabled));
    }

    @Override
    public boolean isEnabledForTabs() {
        return _isEnabledForTabs();
    }

    public static boolean _isEnabledForTabs() {
        return PropertiesComponent.getInstance().getBoolean(FC_TABS_ENABLED, true);
    }

    @Override
    public boolean isEnabledForProjectView() {
        return _isEnabledForProjectView();
    }

    public static boolean _isEnabledForProjectView() {
        return PropertiesComponent.getInstance().getBoolean(FC_PROJECT_VIEW_ENABLED, true);
    }

    public static void setEnabledForProjectView(boolean enabled) {
        PropertiesComponent.getInstance().setValue(FC_PROJECT_VIEW_ENABLED, Boolean.toString(enabled));
    }

    public Element getState(final boolean shared) {
        Element element = new Element("state");
        myModel.save(element, shared);
        return element;
    }

    @Override
    @SuppressWarnings({"MethodMayBeStatic"})
    
    public Color getColor( final String name) {
        Color color = ourDefaultColors.get(name);
        if (color != null) {
            return color;
        }

        if ("ffffe4".equals(name) || "494539".equals(name)) {
            return new JBColor(0xffffe4, 0x494539);
        }

        if ("e7fadb".equals(name) || "2a3b2c".equals(name)) {
            return new JBColor(0xe7fadb, 0x2a3b2c);
        }

        return ColorUtil.fromHex(name, null);

    }

    @Override
    public Element getState() {
        initSharedConfigurations();
        return getState(false);
    }

    @SuppressWarnings({"AutoUnboxing"})
    void loadState(Element state, final boolean shared) {
        myModel.load(state, shared);
    }

    @Override
    @SuppressWarnings({"MethodMayBeStatic"})
    public Collection<String> getColorNames() {
        List<String> sorted = ContainerUtil.newArrayList(ourDefaultColors.keySet());
        Collections.sort(sorted);
        return sorted;
    }

    @Override
    @SuppressWarnings({"AutoUnboxing"})
    public void loadState(Element state) {
        initSharedConfigurations();
        loadState(state, false);
    }

    @Override
    public boolean isColored( final String scopeName, final boolean shared) {
        return myModel.isColored(scopeName, shared);
    }

    
    @Override
    public Color getRendererBackground(VirtualFile vFile) {
        if (vFile == null) return null;

        if (isEnabled()) {
            final Color fileColor = getFileColor(vFile);
            if (fileColor != null) return fileColor;
        }

        //todo[kb] slightly_green for darcula
        return FileEditorManager.getInstance(myProject).isFileOpen(vFile) && !UIUtil.isUnderDarcula() ? LightColors.SLIGHTLY_GREEN : null;
    }

    
    @Override
    public Color getRendererBackground(PsiFile file) {
        if (file == null) return null;

        final VirtualFile vFile = file.getVirtualFile();
        if (vFile == null) return null;

        return getRendererBackground(vFile);
    }

    @Override
    
    public Color getFileColor( final PsiFile file) {
        initSharedConfigurations();

        final String colorName = myModel.getColor(file);
        return colorName == null ? null : getColor(colorName);
    }

    @Override
    
    public Color getFileColor( final VirtualFile file) {
        initSharedConfigurations();

        final String colorName = myModel.getColor(file, getProject());
        return colorName == null ? null : getColor(colorName);
    }

    @Override
    public boolean isShared( final String scopeName) {
        return myModel.isShared(scopeName);
    }

    FileColorsModel getModel() {
        return myModel;
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    public List<FileColorConfiguration> getLocalConfigurations() {
        return myModel.getLocalConfigurations();
    }

    public List<FileColorConfiguration> getSharedConfigurations() {
        return myModel.getSharedConfigurations();
    }

    
    public static String getColorName(Color color) {
        for (String name : ourDefaultColors.keySet()) {
            if (color.equals(ourDefaultColors.get(name))) {
                return name;
            }
        }
        return null;
    }

    static String getAlias(String text) {
        if (UIUtil.isUnderDarcula()) {
            if (text.equals("Yellow")) return "Brown";
        }
        return text;
    }

}
