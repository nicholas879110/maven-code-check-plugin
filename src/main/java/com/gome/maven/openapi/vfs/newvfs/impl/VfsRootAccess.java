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
package com.gome.maven.openapi.vfs.newvfs.impl;

import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.application.impl.ApplicationImpl;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.roots.OrderEnumerator;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.JarFileSystem;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.newvfs.NewVirtualFileSystem;
import com.gome.maven.util.PathUtil;
import com.gome.maven.util.SystemProperties;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.THashSet;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

public class VfsRootAccess {
    public static boolean SHOULD_PERFORM_ACCESS_CHECK = System.getenv("NO_FS_ROOTS_ACCESS_CHECK") == null;
    // we don't want test subclasses to accidentally remove allowed files, added by base classes
    private static final Set<String> ourAdditionalRoots = new THashSet<String>(FileUtil.PATH_HASHING_STRATEGY);
    private static boolean insideGettingRoots;

    
    static void assertAccessInTests( VirtualFileSystemEntry child,  NewVirtualFileSystem delegate) {
        final Application application = ApplicationManager.getApplication();
        if (SHOULD_PERFORM_ACCESS_CHECK &&
                application.isUnitTestMode() &&
                application instanceof ApplicationImpl &&
                ((ApplicationImpl)application).isComponentsCreated()) {
            if (delegate != LocalFileSystem.getInstance() && delegate != JarFileSystem.getInstance()) {
                return;
            }

            // root' children are loaded always
            if (child.getParent() == null || child.getParent().getParent() == null) {
                return;
            }

            Set<String> allowed = ApplicationManager.getApplication().runReadAction(new Computable<Set<String>>() {
                @Override
                public Set<String> compute() {
                    return allowedRoots();
                }
            });
            boolean isUnder = allowed == null || allowed.isEmpty();

            if (!isUnder) {
                String childPath = child.getPath();
                if (delegate == JarFileSystem.getInstance()) {
                    VirtualFile local = JarFileSystem.getInstance().getVirtualFileForJar(child);
                    assert local != null : child;
                    childPath = local.getPath();
                }
                for (String root : allowed) {
                    if (FileUtil.startsWith(childPath, root)) {
                        isUnder = true;
                        break;
                    }
                    if (root.startsWith(JarFileSystem.PROTOCOL_PREFIX)) {
                        String rootLocalPath = FileUtil.toSystemIndependentName(PathUtil.toPresentableUrl(root));
                        isUnder = FileUtil.startsWith(childPath, rootLocalPath);
                        if (isUnder) break;
                    }
                }
            }

            assert isUnder : "File accessed outside allowed roots: " + child + ";\nAllowed roots: " + new ArrayList<String>(allowed);
        }
    }

    // null means we were unable to get roots, so do not check access

    private static Set<String> allowedRoots() {
        if (insideGettingRoots) return null;

        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 0) return null;

        final Set<String> allowed = new THashSet<String>(FileUtil.PATH_HASHING_STRATEGY);
        allowed.add(FileUtil.toSystemIndependentName(PathManager.getHomePath()));

        try {
            URL outUrl = Application.class.getResource("/");
            if (outUrl != null) {
                String output = new File(outUrl.toURI()).getParentFile().getParentFile().getPath();
                allowed.add(FileUtil.toSystemIndependentName(output));
            }
        }
        catch (URISyntaxException ignored) { }

        String javaHome = SystemProperties.getJavaHome();
        allowed.add(FileUtil.toSystemIndependentName(javaHome));
        if (SystemInfo.isMac && SystemInfo.isAppleJvm) {
            // Apple SDK has jars in the folder _next_ to the java.home
            allowed.add(FileUtil.toSystemIndependentName(new File(new File(javaHome).getParent(), "Classes").getPath()));
        }
        allowed.add(FileUtil.toSystemIndependentName(new File(FileUtil.getTempDirectory()).getParent()));
        allowed.add(FileUtil.toSystemIndependentName(System.getProperty("java.io.tmpdir")));
        allowed.add(FileUtil.toSystemIndependentName(SystemProperties.getUserHome()));

        for (final Project project : openProjects) {
            if (!project.isInitialized()) {
                return null; // all is allowed
            }
            for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
                allowed.add(root.getPath());
            }
            for (VirtualFile root : getAllRoots(project)) {
                allowed.add(StringUtil.trimEnd(root.getPath(), JarFileSystem.JAR_SEPARATOR));
            }
            String location = project.getBasePath();
            assert location != null : project;
            allowed.add(FileUtil.toSystemIndependentName(location));
        }

        allowed.addAll(ourAdditionalRoots);

        return allowed;
    }

    
    private static VirtualFile[] getAllRoots( Project project) {
        insideGettingRoots = true;
        final Set<VirtualFile> roots = new THashSet<VirtualFile>();

        final OrderEnumerator enumerator = ProjectRootManager.getInstance(project).orderEntries();
        ContainerUtil.addAll(roots, enumerator.getClassesRoots());
        ContainerUtil.addAll(roots, enumerator.getSourceRoots());

        insideGettingRoots = false;
        return VfsUtilCore.toVirtualFileArray(roots);
    }

    
    public static void allowRootAccess( String... roots) {
        for (String root : roots) {
            ourAdditionalRoots.add(FileUtil.toSystemIndependentName(root));
        }
    }

    
    public static void disallowRootAccess( String... roots) {
        for (String root : roots) {
            ourAdditionalRoots.remove(FileUtil.toSystemIndependentName(root));
        }
    }
}
