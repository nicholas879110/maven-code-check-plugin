package com.gome.maven.usages.impl.rules;

import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.usages.UsageGroup;

import javax.swing.*;

/**
 * @author nik
 */
public abstract class UsageGroupBase implements UsageGroup {
    @Override
    public void update() {
    }

    
    @Override
    public FileStatus getFileStatus() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Icon getIcon(boolean isOpen) {
        return null;
    }

    @Override
    public void navigate(boolean focus) {
    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }
}
