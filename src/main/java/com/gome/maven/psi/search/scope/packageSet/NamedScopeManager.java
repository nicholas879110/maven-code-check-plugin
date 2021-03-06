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
package com.gome.maven.psi.search.scope.packageSet;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.components.State;
import com.gome.maven.openapi.components.Storage;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.xmlb.XmlSerializer;
import com.gome.maven.util.xmlb.XmlSerializerUtil;
import com.gome.maven.util.xmlb.annotations.AbstractCollection;
import com.gome.maven.util.xmlb.annotations.Tag;
import org.jdom.Element;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@State(
        name="NamedScopeManager",
        storages= {
                @Storage(
                        file = StoragePathMacros.WORKSPACE_FILE
                )}
)
public class NamedScopeManager extends NamedScopesHolder {
    public OrderState myOrderState = new OrderState();

    public NamedScopeManager(final Project project) {
        super(project);
    }

    public static NamedScopeManager getInstance(Project project) {
        return ServiceManager.getService(project, NamedScopeManager.class);
    }

    @Override
    public void loadState(Element state) {
        super.loadState(state);
        XmlSerializer.deserializeInto(myOrderState, state);
    }

    @Override
    public Element getState() {
        final Element state = super.getState();
        XmlSerializer.serializeInto(myOrderState, state);
        return state;
    }

    @Override
    public String getDisplayName() {
        return IdeBundle.message("local.scopes.node.text");
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Ide.LocalScope;
    }

    public static class OrderState {
        @Tag("order")
        @AbstractCollection(surroundWithTag = false, elementTag = "scope", elementValueAttribute = "name")
        public List<String> myOrder = new ArrayList<String>();
    }
}
