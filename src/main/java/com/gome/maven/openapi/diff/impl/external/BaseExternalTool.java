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
package com.gome.maven.openapi.diff.impl.external;

import com.gome.maven.execution.ExecutionException;
import com.gome.maven.execution.configurations.GeneralCommandLine;
import com.gome.maven.execution.util.ExecutionErrorDialog;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.diff.*;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.config.AbstractProperty;
import com.gome.maven.util.config.BooleanProperty;
import com.gome.maven.util.config.StringProperty;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
abstract class BaseExternalTool implements DiffTool {
    private final BooleanProperty myEnableProperty;
    private final StringProperty myToolProperty;

    protected BaseExternalTool(BooleanProperty enableProperty, StringProperty toolProperty) {
        myEnableProperty = enableProperty;
        myToolProperty = toolProperty;
    }

    public final boolean canShow( DiffRequest request) {
        if (!isEnabled() || StringUtil.isEmpty(getToolPath())) return false;
        return isAvailable(request);
    }

    @Override
    public DiffViewer createComponent(String title, DiffRequest request, Window window,  Disposable parentDisposable) {
        return null;
    }

    public abstract boolean isAvailable( DiffRequest request);

    
    protected ContentExternalizer externalize( DiffRequest request, final int index) {
        final VirtualFile file = getLocalFile(request.getContents()[index].getFile());

        if (LocalFileExternalizer.canExternalizeAsFile(file)) {
            return LocalFileExternalizer.tryCreate(file);
        }

        return new ExternalToolContentExternalizer(request, index);
    }

    public static AbstractProperty.AbstractPropertyContainer getProperties() {
        return DiffManagerImpl.getInstanceEx().getProperties();
    }

    protected String getToolPath() {
        return myToolProperty.get(getProperties());
    }

    protected boolean isEnabled() {
        return myEnableProperty.value(getProperties());
    }

    
    protected List<String> getParameters( DiffRequest request) throws Exception {
        final String p1 = convertToPath(request, 0);
        final String p2 = convertToPath(request, 1);
        final List<String> params = new ArrayList<String>();
        if (p1 != null) params.add(p1);
        if (p2 != null) params.add(p2);
        return params;
    }

    public void show(DiffRequest request) {
        saveContents(request);

        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(getToolPath());
        try {
            commandLine.addParameters(getParameters(request));
            commandLine.createProcess();
        }
        catch (Exception e) {
            ExecutionErrorDialog.show(new ExecutionException(e.getMessage()),
                    DiffBundle.message("cant.launch.diff.tool.error.message"), request.getProject());
        }
    }

    protected void saveContents(DiffRequest request) {
        for (DiffContent diffContent : request.getContents()) {
            Document document = diffContent.getDocument();
            if (document != null) {
                FileDocumentManager.getInstance().saveDocument(document);
            }
        }
    }

    
    protected String convertToPath(DiffRequest request, int index) throws Exception {
        final ContentExternalizer externalize = externalize(request, index);
        return externalize == null ? null : externalize.getContentFile().getAbsolutePath();
    }

    
    protected static VirtualFile getLocalFile(VirtualFile file) {
        if (file != null && file.isInLocalFileSystem()) return file;
        return null;
    }
}