/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.psi;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.FileASTNode;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.diagnostic.Attachment;
import com.gome.maven.openapi.diagnostic.ExceptionWithAttachments;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.stubs.StubElement;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.ExceptionUtil;

import java.lang.ref.SoftReference;

/**
 * @author mike
 */
public class PsiInvalidElementAccessException extends RuntimeException implements ExceptionWithAttachments {
    private static final Key<Object> INVALIDATION_TRACE = Key.create("INVALIDATION_TRACE");
    private static final Key<Boolean> REPORTING_EXCEPTION = Key.create("REPORTING_EXCEPTION");

    private final SoftReference<PsiElement> myElementReference;  // to prevent leaks, since exceptions are stored in IdeaLogger
    private final Attachment[] myDiagnostic;
    private final String myMessage;

    public PsiInvalidElementAccessException(PsiElement element) {
        this(element, null, null);
    }

    public PsiInvalidElementAccessException(PsiElement element,  String message) {
        this(element, message, null);
    }

    public PsiInvalidElementAccessException(PsiElement element,  Throwable cause) {
        this(element, null, cause);
    }

    public PsiInvalidElementAccessException( PsiElement element,  String message,  Throwable cause) {
        super(null, cause);
        myElementReference = new SoftReference<PsiElement>(element);

        if (element == null) {
            myMessage = message;
            myDiagnostic = Attachment.EMPTY_ARRAY;
        }
        else if (element == PsiUtilCore.NULL_PSI_ELEMENT) {
            myMessage = "NULL_PSI_ELEMENT ;" + message;
            myDiagnostic = Attachment.EMPTY_ARRAY;
        }
        else {
            boolean recursiveInvocation = Boolean.TRUE.equals(element.getUserData(REPORTING_EXCEPTION));
            element.putUserData(REPORTING_EXCEPTION, Boolean.TRUE);

            try {
                Object trace = recursiveInvocation ? null : findInvalidationTrace(element.getNode());
                myMessage = getMessageWithReason(element, message, recursiveInvocation, trace);
                if (trace == null) {
                    myDiagnostic = Attachment.EMPTY_ARRAY;
                }
                else {
                    String diagnostic = trace instanceof Throwable ? ExceptionUtil.getThrowableText((Throwable)trace) : trace.toString();
                    myDiagnostic = new Attachment[]{new Attachment("diagnostic.txt", diagnostic)};
                }
            }
            finally {
                element.putUserData(REPORTING_EXCEPTION, null);
            }
        }
    }

    private static String getMessageWithReason( PsiElement element,
                                                String message,
                                               boolean recursiveInvocation,
                                                Object trace) {
        String reason = "Element: " + element.getClass();
        if (!recursiveInvocation) {
            String traceText = !isTrackingInvalidation() ? "disabled" :
                    trace != null ? "see attachment" :
                            "no info";
            reason += " because: " + reason(element) + "\ninvalidated at: " + traceText;
        }
        return reason + (message == null ? "" : "; " + message);
    }

    @Override
    public String getMessage() {
        return myMessage;
    }

    
    @Override
    public Attachment[] getAttachments() {
        return myDiagnostic;
    }

    public static Object findInvalidationTrace( ASTNode element) {
        while (element != null) {
            Object trace = element.getUserData(INVALIDATION_TRACE);
            if (trace != null) {
                return trace;
            }
            ASTNode parent = element.getTreeParent();
            if (parent == null && element instanceof FileASTNode) {
                PsiElement psi = element.getPsi();
                trace = psi == null ? null : psi.getUserData(INVALIDATION_TRACE);
                if (trace != null) {
                    return trace;
                }
            }
            element = parent;
        }
        return null;
    }

    
    
    private static String reason( PsiElement root) {
        if (root == PsiUtilCore.NULL_PSI_ELEMENT) return "NULL_PSI_ELEMENT";

        PsiElement element = root instanceof PsiFile ? root : root.getParent();
        if (element == null) {
            String m = "parent is null";
            if (root instanceof StubBasedPsiElement) {
                StubElement stub = ((StubBasedPsiElement)root).getStub();
                m += "; stub=" + stub;
                if (stub != null) m += "; p.s.=" + stub.getParentStub();
            }
            return m;
        }

        while (element != null && !(element instanceof PsiFile)) element = element.getParent();
        PsiFile file = (PsiFile)element;
        if (file == null) return "containing file is null";

        FileViewProvider provider = file.getViewProvider();
        VirtualFile vFile = provider.getVirtualFile();
        if (!vFile.isValid()) return vFile + " is invalid";
        if (!provider.isPhysical()) {
            PsiElement context = file.getContext();
            if (context != null && !context.isValid()) {
                return "invalid context: " + reason(context);
            }
        }

        PsiManager manager = file.getManager();
        if (manager.getProject().isDisposed()) return "project is disposed";

        Language language = file.getLanguage();
        if (language != provider.getBaseLanguage()) return "File language:" + language + " != Provider base language:" + provider.getBaseLanguage();

        FileViewProvider p = manager.findViewProvider(vFile);
        if (provider != p) return "different providers: " + provider + "(" + id(provider) + "); " + p + "(" + id(p) + ")";

        if (!provider.isPhysical()) return "non-physical provider: " + provider; // "dummy" file?

        return "psi is outdated";
    }

    private static String id(FileViewProvider provider) {
        return Integer.toHexString(System.identityHashCode(provider));
    }

    public static void setInvalidationTrace(UserDataHolder element, Object trace) {
        element.putUserData(INVALIDATION_TRACE, trace);
    }

    public static boolean isTrackingInvalidation() {
        return Registry.is("psi.track.invalidation");
    }

    
    public PsiElement getPsiElement() {
        return myElementReference.get();
    }
}
