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
package com.gome.maven.openapi.options;

import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Parent;

import java.io.IOException;

/**
 * Please extend {@link BaseSchemeProcessor} to avoid compatibility issues
 */
public interface SchemeProcessor<T extends ExternalizableScheme> {
    @Deprecated
    T readScheme( Document schemeContent) throws InvalidDataException, IOException, JDOMException;

    Parent writeScheme( T scheme) throws WriteExternalException;

    @Deprecated
    /**
     * @deprecated Implement {@link BaseSchemeProcessor#getState(ExternalizableScheme)}
     */
    boolean shouldBeSaved( T scheme);

    void initScheme( T scheme);

    void onSchemeAdded( T scheme);

    void onSchemeDeleted( T scheme);

    void onCurrentSchemeChanged(final Scheme oldCurrentScheme);
}
