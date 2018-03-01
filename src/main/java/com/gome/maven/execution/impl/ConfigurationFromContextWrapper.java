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
package com.gome.maven.execution.impl;

import com.gome.maven.execution.RunnerAndConfigurationSettings;
import com.gome.maven.execution.actions.ConfigurationContext;
import com.gome.maven.execution.actions.ConfigurationFromContext;
import com.gome.maven.execution.junit.RuntimeConfigurationProducer;
import com.gome.maven.psi.PsiElement;

/**
 * Wraps a {@link com.gome.maven.execution.junit.RuntimeConfigurationProducer} in a {@link com.gome.maven.execution.actions.ConfigurationFromContext}.
 *
 * @author yole
 */
public class ConfigurationFromContextWrapper extends ConfigurationFromContext {
    private final RuntimeConfigurationProducer myProducer;

    public ConfigurationFromContextWrapper(RuntimeConfigurationProducer producer) {
        myProducer = producer;
    }

    @Override
    public void onFirstRun(ConfigurationContext context, Runnable startRunnable) {
        myProducer.perform(context, startRunnable);
    }

    
    @Override
    public RunnerAndConfigurationSettings getConfigurationSettings() {
        return myProducer.getConfiguration();
    }

    @Override
    public void setConfigurationSettings(RunnerAndConfigurationSettings configurationSettings) {
        myProducer.setConfiguration(configurationSettings);
    }

    
    @Override
    public PsiElement getSourceElement() {
        return myProducer.getSourceElement();
    }

    @Override
    public boolean isPreferredTo(ConfigurationFromContext other) {
        return other instanceof ConfigurationFromContextWrapper &&
                myProducer.compareTo(((ConfigurationFromContextWrapper) other).myProducer) < 0;
    }

    @Override
    public boolean shouldReplace(ConfigurationFromContext other) {
        return other instanceof ConfigurationFromContextWrapper &&
                myProducer.compareTo(((ConfigurationFromContextWrapper) other).myProducer) > 0;
    }
}
