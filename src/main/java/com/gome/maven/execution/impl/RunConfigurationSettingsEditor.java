package com.gome.maven.execution.impl;

import com.gome.maven.execution.RunnerAndConfigurationSettings;
import com.gome.maven.openapi.options.SettingsEditor;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public interface RunConfigurationSettingsEditor {

    void setOwner(SettingsEditor<RunnerAndConfigurationSettings> owner);
}
