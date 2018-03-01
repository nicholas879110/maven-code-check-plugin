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

package com.gome.maven.util.xml.ui.actions;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.application.ApplicationBundle;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.command.WriteCommandAction;
import com.gome.maven.psi.SmartPointerManager;
import com.gome.maven.psi.SmartPsiElementPointer;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.xml.*;
import com.gome.maven.util.xml.reflect.DomCollectionChildDescription;

import javax.swing.*;
import java.lang.reflect.Type;

/**
 * User: Sergey.Vasiliev
 * Date: Mar 1, 2006
 */
public abstract class DefaultAddAction<T extends DomElement> extends AnAction {

    public DefaultAddAction() {
        super(ApplicationBundle.message("action.add"));
    }

    public DefaultAddAction(String text) {
        super(text);
    }

    public DefaultAddAction(String text, String description, Icon icon) {
        super(text, description, icon);
    }


    protected Type getElementType() {
        return getDomCollectionChildDescription().getType();
    }

    protected void tuneNewValue(T t) {
    }

    protected abstract DomCollectionChildDescription getDomCollectionChildDescription();

    protected abstract DomElement getParentDomElement();

    protected void afterAddition( T newElement) {
    }

    @Override
    public final void actionPerformed(final AnActionEvent e) {
        final T result = performElementAddition();
        if (result != null) {
            afterAddition(result);
        }
    }

    
    protected T performElementAddition() {
        final DomElement parent = getParentDomElement();
        final DomManager domManager = parent.getManager();
        final TypeChooser[] oldChoosers = new TypeChooser[]{null};
        final Type[] aClass = new Type[]{null};
        final StableElement<T> result = new WriteCommandAction<StableElement<T>>(domManager.getProject(), DomUtil.getFile(parent)) {
            @Override
            protected void run(Result<StableElement<T>> result) throws Throwable {
                final DomElement parentDomElement = getParentDomElement();
                final T t = (T)getDomCollectionChildDescription().addValue(parentDomElement, getElementType());
                tuneNewValue(t);
                aClass[0] = parent.getGenericInfo().getCollectionChildDescription(t.getXmlElementName()).getType();
                oldChoosers[0] = domManager.getTypeChooserManager().getTypeChooser(aClass[0]);
                final SmartPsiElementPointer pointer =
                        SmartPointerManager.getInstance(getProject()).createSmartPsiElementPointer(t.getXmlTag());
                domManager.getTypeChooserManager().registerTypeChooser(aClass[0], new TypeChooser() {
                    @Override
                    public Type chooseType(final XmlTag tag) {
                        if (tag == pointer.getElement()) {
                            return getElementType();
                        }
                        return oldChoosers[0].chooseType(tag);
                    }

                    @Override
                    public void distinguishTag(final XmlTag tag, final Type aClass) throws IncorrectOperationException {
                        oldChoosers[0].distinguishTag(tag, aClass);
                    }

                    @Override
                    public Type[] getChooserTypes() {
                        return oldChoosers[0].getChooserTypes();
                    }
                });
                result.setResult((StableElement<T>)t.createStableCopy());
            }
        }.execute().getResultObject();
        if (result != null) {
            domManager.getTypeChooserManager().registerTypeChooser(aClass[0], oldChoosers[0]);
            return result.getWrappedElement();
        }
        return null;
    }
}