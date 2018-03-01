package com.gome.maven.javaee;

import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.xml.Html5SchemaProvider;

/**
 * @author Eugene.Kudelevsky
 */
public class DefaultHtmlDoctypeInitialConfigurator {
    public static final int VERSION = 1;

    public DefaultHtmlDoctypeInitialConfigurator(ProjectManager projectManager,
                                                 PropertiesComponent propertiesComponent) {
        if (!propertiesComponent.getBoolean("DefaultHtmlDoctype.MigrateToHtml5", false)) {
            propertiesComponent.setValue("DefaultHtmlDoctype.MigrateToHtml5", Boolean.TRUE.toString());
            ExternalResourceManagerEx.getInstanceEx()
                    .setDefaultHtmlDoctype(Html5SchemaProvider.getHtml5SchemaLocation(), projectManager.getDefaultProject());
        }
        // sometimes VFS fails to pick up updated schema contents and we need to force refresh
        if (propertiesComponent.getOrInitInt("DefaultHtmlDoctype.Refreshed", 0) < VERSION) {
            propertiesComponent.setValue("DefaultHtmlDoctype.Refreshed", Integer.toString(VERSION));
            final String schemaUrl = VfsUtilCore.pathToUrl(Html5SchemaProvider.getHtml5SchemaLocation());
            final VirtualFile schemaFile = VirtualFileManager.getInstance().findFileByUrl(schemaUrl);
            if (schemaFile != null) {
                schemaFile.getParent().refresh(false, true);
            }
        }
    }
}
