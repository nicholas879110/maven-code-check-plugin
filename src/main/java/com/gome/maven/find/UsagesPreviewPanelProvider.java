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
package com.gome.maven.find;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.preview.PreviewPanelProvider;
import com.gome.maven.openapi.preview.PreviewProviderId;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.usages.impl.UsageViewImpl;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

public class UsagesPreviewPanelProvider extends PreviewPanelProvider<Pair<UsageViewImpl, ? extends JTable>, Boolean> {
    public static final PreviewProviderId<Pair<UsageViewImpl, ? extends JTable>, Boolean> ID = PreviewProviderId.create("Usages");

    private JComponent myComponent;
    private Collection<UsageViewImpl> myViews = new ArrayList<UsageViewImpl>();

    public UsagesPreviewPanelProvider(Project project) {
        super(ID);
        myComponent = new JPanel(new BorderLayout()) {
            @Override
            public String toString() {
                return "UsagesPreviewPanel";
            }
        };
    }

    @Override
    public void dispose() {
        //myPresentation = null;
        myComponent.removeAll();
        for (UsageViewImpl view : myViews) {
            Disposer.dispose(view);
        }
    }

    
    @Override
    protected JComponent getComponent() {
        return myComponent;
    }

    
    @Override
    protected String getTitle( Pair<UsageViewImpl, ? extends JTable> content) {
        return content.first.getPresentation().getTabText();
    }

    
    @Override
    protected Icon getIcon( Pair<UsageViewImpl, ? extends JTable> content) {
        return AllIcons.Actions.Find;
    }

    @Override
    public float getMenuOrder() {
        return 2;
    }

    @Override
    public void showInStandardPlace( Pair<UsageViewImpl, ? extends JTable> content) {
    }

    @Override
    public boolean supportsStandardPlace() {
        return false;
    }

    @Override
    public boolean isModified(Pair<UsageViewImpl, ? extends JTable> content, boolean beforeReuse) {
        return beforeReuse;
    }

    @Override
    public void release( Pair<UsageViewImpl, ? extends JTable> content) {
        myViews.remove(content.first);
        Disposer.dispose(content.first);
        myComponent.remove(content.second);
    }

    @Override
    public boolean contentsAreEqual( Pair<UsageViewImpl, ? extends JTable> content1,  Pair<UsageViewImpl, ? extends JTable> content2) {
        return content1.getFirst().getPresentation().equals(content2.getFirst().getPresentation());
    }

    @Override
    protected Boolean initComponent(final Pair<UsageViewImpl, ? extends JTable> content, boolean requestFocus) {
        myComponent.removeAll();
        myComponent.add(content.second);
        myViews.add(content.first);
        return Boolean.TRUE;
    }
}
