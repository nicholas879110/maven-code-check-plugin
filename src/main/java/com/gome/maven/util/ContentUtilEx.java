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
package com.gome.maven.util;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.ui.content.Content;
import com.gome.maven.ui.content.ContentManager;
import com.gome.maven.ui.content.TabbedContent;
import com.gome.maven.ui.content.impl.TabbedContentImpl;

import javax.swing.*;
import java.util.ArrayList;

/**
 * @author Konstantin Bulenkov
 */
public class ContentUtilEx extends ContentsUtil {
    public static void addTabbedContent(ContentManager manager, JComponent contentComponent, String groupPrefix, String tabName, boolean select) {
        addTabbedContent(manager, contentComponent, groupPrefix, tabName, select, null);
    }

    public static void addTabbedContent(ContentManager manager, JComponent contentComponent, String groupPrefix, String tabName, boolean select,  Disposable childDisposable) {
        TabbedContent tabbedContent = null;
        for (Content content : manager.getContents()) {
            if (content instanceof TabbedContent && content.getTabName().startsWith(groupPrefix + ": ")) {
                tabbedContent = (TabbedContent)content;
                break;
            }
        }

        if (tabbedContent == null) {
            final Disposable disposable = Disposer.newDisposable();
            tabbedContent = new TabbedContentImpl(contentComponent, tabName, true, groupPrefix);
            ContentsUtil.addOrReplaceContent(manager, tabbedContent, select);
            Disposer.register(tabbedContent, disposable);
        } else {
            for (Pair<String, JComponent> tab : new ArrayList<Pair<String, JComponent>>(tabbedContent.getTabs())) {
                if (Comparing.equal(tab.second, contentComponent)) {
                    tabbedContent.removeContent(tab.second);
                }
            }
            if (select) {
                manager.setSelectedContent(tabbedContent, true, true);
            }
            tabbedContent.addContent(contentComponent, tabName, true);
        }

        if (childDisposable != null) {
            Disposer.register(tabbedContent, childDisposable);
        }
    }

    public static int getSelectedTab( TabbedContent content) {
        final JComponent current = content.getComponent();
        int index = 0;
        for (Pair<String,JComponent> tab : content.getTabs()) {
            if (tab.second == current) {
                return index;
            }
            index++;
        }
        return -1;
    }
}
