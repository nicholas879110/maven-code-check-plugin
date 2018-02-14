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
package com.gome.maven.openapi.options;

import com.gome.maven.openapi.util.InvalidDataException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.IOException;

/**
 * @author yole
 */
public abstract class BaseSchemeProcessor<T extends ExternalizableScheme> implements SchemeProcessor<T>, SchemeExtensionProvider {
    @Override
    public void initScheme( T scheme) {
    }

    @Override
    public void onSchemeAdded( T scheme) {
    }

    @Override
    public void onSchemeDeleted( T scheme) {
    }

    @Override
    public void onCurrentSchemeChanged(Scheme newCurrentScheme) {
    }

    
    public T readScheme( Element element) throws InvalidDataException, IOException, JDOMException {
        return readScheme(new Document((Element)element.detach()));
    }

    
    /**
     * @param duringLoad If occurred during {@link SchemesManager#loadSchemes()} call
     */
    public T readScheme( Element element, boolean duringLoad) throws InvalidDataException, IOException, JDOMException {
        return readScheme(element);
    }

    @Override
    public T readScheme( Document schemeContent) throws InvalidDataException, IOException, JDOMException {
        throw new AbstractMethodError();
    }

    public enum State {
        UNCHANGED, NON_PERSISTENT, POSSIBLY_CHANGED
    }

    @Override
    public boolean shouldBeSaved( T scheme) {
        return true;
    }

    
    public State getState( T scheme) {
        return shouldBeSaved(scheme) ? State.POSSIBLY_CHANGED : State.NON_PERSISTENT;
    }

    @Override
    public boolean isUpgradeNeeded() {
        return false;
    }

    
    @Override
    public String getSchemeExtension() {
        return ".xml";
    }
}
