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
package com.gome.maven.codeInsight.template.impl;

import com.gome.maven.codeInsight.lookup.AutoCompletionPolicy;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.lookup.LookupElementPresentation;
import com.gome.maven.codeInsight.lookup.RealLookupElementPresentation;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.IdeActions;
import com.gome.maven.openapi.keymap.KeymapUtil;
import com.gome.maven.openapi.util.text.StringUtil;

import java.awt.event.KeyEvent;

/**
 * @author peter
 */
abstract public class LiveTemplateLookupElement extends LookupElement {
    private final String myLookupString;
    public final boolean sudden;
    private final boolean myWorthShowingInAutoPopup;
    private final String myDescription;

    public LiveTemplateLookupElement( String lookupString,  String description, boolean sudden, boolean worthShowingInAutoPopup) {
        myDescription = description;
        this.sudden = sudden;
        myLookupString = lookupString;
        myWorthShowingInAutoPopup = worthShowingInAutoPopup;
    }

    
    @Override
    public String getLookupString() {
        return myLookupString;
    }

    
    protected String getItemText() {
        return myLookupString;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        super.renderElement(presentation);
        char shortcut = getTemplateShortcut();
        presentation.setItemText(getItemText());
        if (sudden) {
            presentation.setItemTextBold(true);
            if (!presentation.isReal() || !((RealLookupElementPresentation)presentation).isLookupSelectionTouched()) {
                if (shortcut == TemplateSettings.DEFAULT_CHAR) {
                    shortcut = TemplateSettings.getInstance().getDefaultShortcutChar();
                }
                if (shortcut != TemplateSettings.CUSTOM_CHAR) {
                    presentation.setTypeText("  [" + KeyEvent.getKeyText(shortcut) + "] ");
                } else {
                    String shortcutText =
                            KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_EXPAND_LIVE_TEMPLATE_CUSTOM));
                    if (StringUtil.isNotEmpty(shortcutText)) {
                        presentation.setTypeText("  [" + shortcutText + "] ");
                    }
                }
            }
            if (StringUtil.isNotEmpty(myDescription)) {
                presentation.setTailText(" (" + myDescription + ")", true);
            }
        }
        else {
            presentation.setTypeText(myDescription);
        }
    }

    @Override
    public AutoCompletionPolicy getAutoCompletionPolicy() {
        return AutoCompletionPolicy.NEVER_AUTOCOMPLETE;
    }

    @Override
    public boolean isWorthShowingInAutoPopup() {
        return myWorthShowingInAutoPopup;
    }

    public abstract char getTemplateShortcut();
}
