package com.gome.maven.plugin.code.check;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractMavenMojo extends AbstractMojo {

    @Parameter(
            defaultValue = "${project}",
            readonly = true,
            required = true
    )
    protected MavenProject project;
    @Parameter(
            property = "encoding",
            defaultValue = "${project.build.sourceEncoding}",
            readonly = true
    )
    private String inputEncoding;


    public AbstractMavenMojo() {
    }


    private Map<String, Object> getTemplateProperties() {
        Map<String, Object> templateProperties = new HashMap();
        templateProperties.put("project", this.getProject());
        templateProperties.put("inputEncoding", this.getInputEncoding());
        Iterator i$ = this.getProject().getProperties().entrySet().iterator();

        while (i$.hasNext()) {
            Map.Entry<Object, Object> entry = (Map.Entry) i$.next();
            templateProperties.put((String) entry.getKey(), entry.getValue());
        }

        return templateProperties;
    }



    public String getCategoryName() {
        return "Project Reports";
    }

    protected MavenProject getProject() {
        return this.project;
    }



    protected String getInputEncoding() {
        return this.inputEncoding == null ? "ISO-8859-1" : this.inputEncoding;
    }

}
