package com.gome.maven.ide.browsers;


import javax.swing.*;
import java.util.UUID;

public abstract class WebBrowser {
    
    public abstract String getName();

    
    public abstract UUID getId();

    
    public abstract BrowserFamily getFamily();

    
    public abstract Icon getIcon();

    
    public abstract String getPath();

    
    public abstract String getBrowserNotFoundMessage();

    
    public abstract BrowserSpecificSettings getSpecificSettings();
}