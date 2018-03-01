package com.gome.maven.json;

import com.gome.maven.lang.Language;

public class JsonLanguage extends Language {
    public static final JsonLanguage INSTANCE = new JsonLanguage();

    private JsonLanguage() {
        super("JSON", "application/json");
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }
}
