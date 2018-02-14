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
package com.gome.maven.openapi.actionSystem.ex;

import com.gome.maven.openapi.options.ExternalInfo;
import com.gome.maven.openapi.options.ExternalizableScheme;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.text.StringUtil;
import org.jdom.Element;

import java.util.Arrays;
import java.util.List;

public class QuickList implements ExternalizableScheme {
    public static final String QUICK_LIST_PREFIX = "QuickList.";
    public static final String SEPARATOR_ID = QUICK_LIST_PREFIX + "$Separator$";

    private static final String ID_TAG = "id";
    private static final String READONLY_TAG = "readonly";
    private static final String ACTION_TAG = "action";
    private static final String DISPLAY_NAME_TAG = "display";
    private static final String DESCRIPTION_TAG = "description";

    private String myName;
    private String myDescription;
    private String[] myActionIds;
    private boolean myReadonly;
    private final ExternalInfo myExternalInfo = new ExternalInfo();

    /**
     * With read external to be called immediately after in mind
     */
    QuickList() {
    }

    public QuickList( String displayName,  String description, String[] actionIds, boolean isReadonly) {
        myName = displayName;
        myDescription = StringUtil.nullize(description);
        myActionIds = actionIds;
        myReadonly = isReadonly;
    }

    @Deprecated
    public String getDisplayName() {
        return myName;
    }

    @Override
    
    public String getName() {
        return myName;
    }

    public boolean isReadonly() {
        return myReadonly;
    }

    
    public String getDescription() {
        return myDescription;
    }

    public String[] getActionIds() {
        return myActionIds;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QuickList)) {
            return false;
        }

        QuickList quickList = (QuickList)o;
        return Arrays.equals(myActionIds, quickList.myActionIds) && Comparing.strEqual(myDescription, quickList.myDescription) && myName.equals(quickList.myName);
    }

    public int hashCode() {
        return 29 * myName.hashCode() + Comparing.hashcode(myDescription);
    }

    
    public String getActionId() {
        return QUICK_LIST_PREFIX + getName();
    }

    public void writeExternal( Element groupElement) {
        groupElement.setAttribute(DISPLAY_NAME_TAG, myName);
        if (myDescription != null) {
            groupElement.setAttribute(DESCRIPTION_TAG, myDescription);
        }
        if (myReadonly) {
            groupElement.setAttribute(READONLY_TAG, "true");
        }

        for (String actionId : getActionIds()) {
            groupElement.addContent(new Element(ACTION_TAG).setAttribute(ID_TAG, actionId));
        }
    }

    public void readExternal( Element element) {
        myName = element.getAttributeValue(DISPLAY_NAME_TAG);
        myDescription = StringUtil.nullize(element.getAttributeValue(DESCRIPTION_TAG));
        myReadonly = Boolean.valueOf(element.getAttributeValue(READONLY_TAG, "false")).booleanValue();

        List<Element> actionElements = element.getChildren(ACTION_TAG);
        myActionIds = new String[actionElements.size()];
        for (int i = 0, n = actionElements.size(); i < n; i++) {
            myActionIds[i] = actionElements.get(i).getAttributeValue(ID_TAG);
        }
    }

    @Override
    
    public ExternalInfo getExternalInfo() {
        return myExternalInfo;
    }

    @Override
    public void setName( String newName) {
        myName = newName;
    }
}