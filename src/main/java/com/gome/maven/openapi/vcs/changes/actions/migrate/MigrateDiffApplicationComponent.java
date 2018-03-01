package com.gome.maven.openapi.vcs.changes.actions.migrate;

import com.gome.maven.openapi.components.ApplicationComponent;
import com.gome.maven.openapi.diff.DiffManager;

public class MigrateDiffApplicationComponent implements ApplicationComponent {
    @Override
    public void initComponent() {
        DiffManager.getInstance().registerDiffTool(MigrateDiffTool.INSTANCE);
    }

    @Override
    public void disposeComponent() {
    }


    @Override
    public String getComponentName() {
        return "MigrateDiffApplicationComponent";
    }
}
