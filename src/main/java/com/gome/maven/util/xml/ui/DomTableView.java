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
package com.gome.maven.util.xml.ui;

import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.command.WriteCommandAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.xml.DomElement;
import com.gome.maven.util.xml.DomUtil;
import com.gome.maven.util.SmartList;

import java.util.List;

/**
 * @author peter
 */
public class DomTableView extends AbstractTableView<DomElement> {
    private final List<TypeSafeDataProvider> myCustomDataProviders = new SmartList<TypeSafeDataProvider>();

    public DomTableView(final Project project) {
        super(project);
    }

    public DomTableView(final Project project, final String emptyPaneText, final String helpID) {
        super(project, emptyPaneText, helpID);
    }

    public void addCustomDataProvider(TypeSafeDataProvider provider) {
        myCustomDataProviders.add(provider);
    }

    @Override
    public void calcData(final DataKey key, final DataSink sink) {
        super.calcData(key, sink);
        for (final TypeSafeDataProvider customDataProvider : myCustomDataProviders) {
            customDataProvider.calcData(key, sink);
        }
    }

    @Deprecated
    protected final void installPopup(final DefaultActionGroup group) {
        installPopup(ActionPlaces.J2EE_ATTRIBUTES_VIEW_POPUP, group);
    }

    @Override
    protected void wrapValueSetting( final DomElement domElement, final Runnable valueSetter) {
        if (domElement.isValid()) {
            new WriteCommandAction(getProject(), DomUtil.getFile(domElement)) {
                @Override
                protected void run(final Result result) throws Throwable {
                    valueSetter.run();
                }
            }.execute();
            fireChanged();
        }
    }

}