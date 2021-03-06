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
package com.gome.maven.ui;


import javax.swing.*;
import java.awt.*;

public class ExpandedItemListCellRendererWrapper implements ListCellRenderer {
     private final ListCellRenderer myWrappee;
     private final ExpandableItemsHandler<Integer> myHandler;

    public ExpandedItemListCellRendererWrapper( ListCellRenderer wrappee,  ExpandableItemsHandler<Integer> handler) {
        myWrappee = wrappee;
        myHandler = handler;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component result = myWrappee.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (myHandler.getExpandedItems().contains(index)) {
            result = new ExpandedItemRendererComponentWrapper(result);
        }
        return result;
    }

    
    public ListCellRenderer getWrappee() {
        return myWrappee;
    }
}
