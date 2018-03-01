package com.gome.maven.codeInspection;

import com.gome.maven.lang.LanguageExtension;

public class LanguageInspectionSuppressors extends LanguageExtension<InspectionSuppressor> {
    public static final LanguageInspectionSuppressors INSTANCE = new LanguageInspectionSuppressors();

    private LanguageInspectionSuppressors() {
        super("com.gome.maven.lang.inspectionSuppressor");
    }

}