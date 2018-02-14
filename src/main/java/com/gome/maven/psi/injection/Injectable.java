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
package com.gome.maven.psi.injection;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.util.ui.EmptyIcon;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 *         Date: 01.08.13
 */
public abstract class Injectable implements Comparable<Injectable> {

    /** Unique ID among injected language and reference injector IDs */
    
    public abstract String getId();

    
    public abstract String getDisplayName();

    
    public String getAdditionalDescription() {
        return null;
    }

    
    public Icon getIcon() {
        return EmptyIcon.ICON_16;
    }

    @Override
    public int compareTo( Injectable o) {
        return getDisplayName().compareTo(o.getDisplayName());
    }

    /**
     * @return null for reference injections
     */
    
    public abstract Language getLanguage();

    public Language toLanguage() {
        return getLanguage() == null ? new Language(getId(), false) {
            
            @Override
            public String getDisplayName() {
                return Injectable.this.getDisplayName();
            }
        } : getLanguage();
    }

    public static Injectable fromLanguage(final Language language) {
        return new Injectable() {
            
            @Override
            public String getId() {
                return language.getID();
            }

            
            @Override
            public String getDisplayName() {
                return language.getDisplayName();
            }

            
            @Override
            public String getAdditionalDescription() {
                final FileType ft = language.getAssociatedFileType();
                return ft != null ? " (" + ft.getDescription() + ")" : null;
            }

            
            @Override
            public Icon getIcon() {
                final FileType ft = language.getAssociatedFileType();
                //noinspection ConstantConditions
                return ft != null && ft.getIcon() != null ? ft.getIcon() : EmptyIcon.ICON_16;
            }

            @Override
            public Language getLanguage() {
                return language;
            }
        };
    }
}
