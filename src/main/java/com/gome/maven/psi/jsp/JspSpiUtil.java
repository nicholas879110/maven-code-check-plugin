/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.psi.jsp;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.roots.OrderEnumerator;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.vfs.JarFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiReference;
import com.gome.maven.psi.impl.source.jsp.jspJava.JspClass;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.util.PsiTreeUtil;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.Processor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public abstract class JspSpiUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.jsp.tagLibrary.JspTagInfoImpl");
     private static final String JAR_EXTENSION = "jar";

    
    private static JspSpiUtil getJspSpiUtil() {
        return ServiceManager.getService(JspSpiUtil.class);
    }

    public static int escapeCharsInJspContext(JspFile file, int offset, String toEscape) throws IncorrectOperationException {
        final JspSpiUtil util = getJspSpiUtil();
        return util != null ? util._escapeCharsInJspContext(file, offset, toEscape) : 0;
    }

    protected abstract int _escapeCharsInJspContext(JspFile file, int offset, String toEscape) throws IncorrectOperationException;

    public static void visitAllIncludedFilesRecursively(BaseJspFile jspFile, Processor<BaseJspFile> visitor) {
        final JspSpiUtil util = getJspSpiUtil();
        if (util != null) {
            util._visitAllIncludedFilesRecursively(jspFile, visitor);
        }
    }

    protected abstract void _visitAllIncludedFilesRecursively(BaseJspFile jspFile, Processor<BaseJspFile> visitor);

    
    public static PsiElement resolveMethodPropertyReference( PsiReference reference,  PsiClass resolvedClass, boolean readable) {
        final JspSpiUtil util = getJspSpiUtil();
        return util == null ? null : util._resolveMethodPropertyReference(reference, resolvedClass, readable);
    }

    
    protected abstract PsiElement _resolveMethodPropertyReference( PsiReference reference,  PsiClass resolvedClass, boolean readable);

    
    public static Object[] getMethodPropertyReferenceVariants( PsiReference reference,  PsiClass resolvedClass, boolean readable) {
        final JspSpiUtil util = getJspSpiUtil();
        return util == null ? ArrayUtil.EMPTY_OBJECT_ARRAY : util._getMethodPropertyReferenceVariants(reference, resolvedClass, readable);
    }

    protected abstract Object[] _getMethodPropertyReferenceVariants( PsiReference reference,  PsiClass resolvedClass, boolean readable);

    public static boolean isIncludedOrIncludesSomething( JspFile file) {
        return isIncludingAnything(file) || isIncluded(file);
    }

    public static boolean isIncluded( JspFile jspFile) {
        final JspSpiUtil util = getJspSpiUtil();
        return util != null && util._isIncluded(jspFile);
    }

    public abstract boolean _isIncluded( final JspFile jspFile);

    public static boolean isIncludingAnything( JspFile jspFile) {
        final JspSpiUtil util = getJspSpiUtil();
        return util != null && util._isIncludingAnything(jspFile);
    }

    protected abstract boolean _isIncludingAnything( final JspFile jspFile);

    public static PsiFile[] getIncludedFiles( JspFile jspFile) {
        final JspSpiUtil util = getJspSpiUtil();
        return util == null ? PsiFile.EMPTY_ARRAY : util._getIncludedFiles(jspFile);
    }

    public static PsiFile[] getIncludingFiles( JspFile jspFile) {
        final JspSpiUtil util = getJspSpiUtil();
        return util == null ? PsiFile.EMPTY_ARRAY : util._getIncludingFiles(jspFile);
    }

    protected abstract PsiFile[] _getIncludingFiles( PsiFile file);

    
    protected abstract PsiFile[] _getIncludedFiles( final JspFile jspFile);

    public static boolean isJavaContext(PsiElement position) {
        if(PsiTreeUtil.getContextOfType(position, JspClass.class, false) != null) return true;
        return false;
    }

    public static boolean isJarFile( VirtualFile file) {
        if (file != null){
            final String ext = file.getExtension();
            if(ext != null && ext.equalsIgnoreCase(JAR_EXTENSION)) {
                return true;
            }
        }

        return false;
    }

    public static List<URL> buildUrls( final VirtualFile virtualFile,  final Module module) {
        return buildUrls(virtualFile, module, true);
    }

    public static List<URL> buildUrls( final VirtualFile virtualFile,  final Module module, boolean includeModuleOutput) {
        final List<URL> urls = new ArrayList<URL>();
        processClassPathItems(virtualFile, module, new Consumer<VirtualFile>() {
            public void consume(final VirtualFile file) {
                addUrl(urls, file);
            }
        }, includeModuleOutput);
        return urls;
    }

    public static void processClassPathItems(final VirtualFile virtualFile, final Module module, final Consumer<VirtualFile> consumer) {
        processClassPathItems(virtualFile, module, consumer, true);
    }

    public static void processClassPathItems(final VirtualFile virtualFile, final Module module, final Consumer<VirtualFile> consumer,
                                             boolean includeModuleOutput) {
        if (isJarFile(virtualFile)){
            consumer.consume(virtualFile);
        }

        if (module != null) {
            OrderEnumerator enumerator = ModuleRootManager.getInstance(module).orderEntries().recursively();
            if (!includeModuleOutput) {
                enumerator = enumerator.withoutModuleSourceEntries();
            }
            for (VirtualFile root : enumerator.getClassesRoots()) {
                final VirtualFile file;
                if (root.getFileSystem().getProtocol().equals(JarFileSystem.PROTOCOL)) {
                    file = JarFileSystem.getInstance().getVirtualFileForJar(root);
                }
                else {
                    file = root;
                }
                consumer.consume(file);
            }
        }
    }

    private static void addUrl(List<URL> urls, VirtualFile file) {
        if (file == null || !file.isValid()) return;
        final URL url = getUrl(file);
        if (url != null) {
            urls.add(url);
        }
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    
    private static URL getUrl(VirtualFile file) {
        if (file.getFileSystem() instanceof JarFileSystem && file.getParent() != null) return null;

        String path = file.getPath();
        if (path.endsWith(JarFileSystem.JAR_SEPARATOR)) {
            path = path.substring(0, path.length() - 2);
        }

        String url;
        if (SystemInfo.isWindows) {
            url = "file:/" + path;
        }
        else {
            url = "file://" + path;
        }

        if (file.isDirectory() && !(file.getFileSystem() instanceof JarFileSystem)) url += "/";


        try {
            return new URL(url);
        }
        catch (MalformedURLException e) {
            LOG.error(e);
            return null;
        }
    }

    
    public static IElementType getJspElementType( final JspElementType.Kind kind) {
        final JspSpiUtil spiUtil = getJspSpiUtil();
        return spiUtil != null ? spiUtil._getJspElementType(kind) : null;
    }

    
    public static IElementType getJspScriptletType() {
        return getJspElementType(JspElementType.Kind.JSP_SCRIPTLET);
    }

    
    public static IElementType getJspExpressionType() {
        return getJspElementType(JspElementType.Kind.JSP_EXPRESSION);
    }

    protected abstract IElementType _getJspElementType( final JspElementType.Kind kind);
}
