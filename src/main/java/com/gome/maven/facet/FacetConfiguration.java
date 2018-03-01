/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.facet;

import com.gome.maven.openapi.util.JDOMExternalizable;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.facet.ui.FacetEditorTab;
import com.gome.maven.facet.ui.FacetEditorContext;
import com.gome.maven.facet.ui.FacetValidatorsManager;
import org.jdom.Element;

/**
 * Implementations of this interface contain settings of a specific facet.
 *
 * <p>
 * Implement {@link com.gome.maven.openapi.components.PersistentStateComponent} instead of {@link com.gome.maven.openapi.util.JDOMExternalizable}
 * in your implementation of {@link com.gome.maven.facet.FacetConfiguration}
 *
 * @author nik
 */
public interface FacetConfiguration extends JDOMExternalizable {

    /**
     * Creates editor which will be used to edit this facet configuration
     * @param editorContext context
     * @param validatorsManager validatorsManager
     * @return
     */
    FacetEditorTab[] createEditorTabs(final FacetEditorContext editorContext, final FacetValidatorsManager validatorsManager);

    /**
     * @deprecated implement {@link com.gome.maven.openapi.components.PersistentStateComponent#loadState(Object)} instead
     */
    @Override
    @Deprecated
    void readExternal(final Element element) throws InvalidDataException;

    /**
     * @deprecated implement {@link com.gome.maven.openapi.components.PersistentStateComponent#getState()} instead
     */
    @Override
    @Deprecated
    void writeExternal(final Element element) throws WriteExternalException;
}
