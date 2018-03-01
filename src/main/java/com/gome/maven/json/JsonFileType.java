package com.gome.maven.json;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.fileTypes.LanguageFileType;

import javax.swing.Icon;

/**
 * @author Mikhail Golubev
 */
public class JsonFileType extends LanguageFileType{
    public static final JsonFileType INSTANCE = new JsonFileType();
    public static final String DEFAULT_EXTENSION = "json";

    public JsonFileType() {
        super(JsonLanguage.INSTANCE);
    }

    
    @Override
    public String getName() {
        return "JSON";
    }

    
    @Override
    public String getDescription() {
        return "JSON files";
    }

    
    @Override
    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }


    @Override
    public Icon getIcon() {
        // TODO: add JSON icon instead
        return AllIcons.FileTypes.Json;
    }
}
