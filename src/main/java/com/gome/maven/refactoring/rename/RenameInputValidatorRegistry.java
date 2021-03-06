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

/**
 * @author Dmitry Avdeev
 */
package com.gome.maven.refactoring.rename;

import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.Function;
import com.gome.maven.util.ProcessingContext;
import com.gome.maven.util.containers.hash.LinkedHashMap;

import java.util.ArrayList;
import java.util.List;

public class RenameInputValidatorRegistry {
    private RenameInputValidatorRegistry() {
    }

    
    public static Condition<String> getInputValidator(final PsiElement element) {
        final LinkedHashMap<RenameInputValidator, ProcessingContext> acceptedValidators = new LinkedHashMap<RenameInputValidator, ProcessingContext>();
        for(final RenameInputValidator validator: Extensions.getExtensions(RenameInputValidator.EP_NAME)) {
            final ProcessingContext context = new ProcessingContext();
            if (validator.getPattern().accepts(element, context)) {
                acceptedValidators.put(validator, context);
            }
        }
        return acceptedValidators.isEmpty() ? null : new Condition<String>() {
            @Override
            public boolean value(final String s) {
                for (RenameInputValidator validator : acceptedValidators.keySet()) {
                    if (!validator.isInputValid(s, element, acceptedValidators.get(validator))) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    
    public static Function<String, String> getInputErrorValidator(final PsiElement element) {
        final LinkedHashMap<RenameInputValidatorEx, ProcessingContext> acceptedValidators = new LinkedHashMap<RenameInputValidatorEx, ProcessingContext>();
        for(final RenameInputValidator validator: Extensions.getExtensions(RenameInputValidator.EP_NAME)) {
            final ProcessingContext context = new ProcessingContext();
            if (validator instanceof RenameInputValidatorEx && validator.getPattern().accepts(element, context)) {
                acceptedValidators.put((RenameInputValidatorEx)validator, context);
            }
        }

        return acceptedValidators.isEmpty() ? null : new Function<String, String>() {
            @Override
            public String fun(String newName) {
                for (RenameInputValidatorEx validator : acceptedValidators.keySet()) {
                    final String message = validator.getErrorMessage(newName, element.getProject());
                    if (message != null) {
                        return message;
                    }
                }
                return null;
            }
        };
    }
}
