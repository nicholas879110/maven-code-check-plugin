package com.gome.maven.openapi.ui;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.project.Project;

import javax.swing.*;
import java.awt.*;

public interface WindowWrapper extends Disposable {
    enum Mode {FRAME, MODAL, NON_MODAL}

    void show();

    
    Project getProject();

    
    JComponent getComponent();

    
    Mode getMode();

    
    Window getWindow();

    void setTitle( String title);

    void setImage( Image image);

    void close();
}
