package com.gome.maven.codeInsight;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.psi.PsiClassType;
import com.gome.maven.psi.PsiElement;

public abstract class CustomExceptionHandler {
    public static final ExtensionPointName<CustomExceptionHandler> KEY = ExtensionPointName.create("com.gome.maven.custom.exception.handler");

    public abstract boolean isHandled( PsiElement element,  PsiClassType exceptionType, PsiElement topElement);
}
