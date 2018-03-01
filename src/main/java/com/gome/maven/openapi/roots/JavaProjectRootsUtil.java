package com.gome.maven.openapi.roots;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiCodeFragment;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.search.DelegatingGlobalSearchScope;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.util.containers.ContainerUtil;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaResourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class JavaProjectRootsUtil {
    public static boolean isOutsideJavaSourceRoot( PsiFile psiFile) {
        if (psiFile == null) return false;
        if (psiFile instanceof PsiCodeFragment) return false;
        final VirtualFile file = psiFile.getVirtualFile();
        if (file == null) return false;
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(psiFile.getProject()).getFileIndex();
        return !projectFileIndex.isUnderSourceRootOfType(file, JavaModuleSourceRootTypes.SOURCES) && !projectFileIndex.isInLibrarySource(file)
                && !projectFileIndex.isInLibraryClasses(file);
    }

    /**
     * @return list of all java source roots in the project which can be suggested as a target directory for a class created by user
     */
    
    public static List<VirtualFile> getSuitableDestinationSourceRoots( Project project) {
        List<VirtualFile> roots = new ArrayList<VirtualFile>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            for (ContentEntry entry : ModuleRootManager.getInstance(module).getContentEntries()) {
                for (SourceFolder sourceFolder : entry.getSourceFolders(JavaModuleSourceRootTypes.SOURCES)) {
                    if (!isForGeneratedSources(sourceFolder)) {
                        ContainerUtil.addIfNotNull(roots, sourceFolder.getFile());
                    }
                }
            }
        }
        return roots;
    }

    private static boolean isForGeneratedSources(SourceFolder sourceFolder) {
        JavaSourceRootProperties properties = sourceFolder.getJpsElement().getProperties(JavaModuleSourceRootTypes.SOURCES);
        JavaResourceRootProperties resourceProperties = sourceFolder.getJpsElement().getProperties(JavaModuleSourceRootTypes.RESOURCES);
        return properties != null && properties.isForGeneratedSources() || resourceProperties != null && resourceProperties.isForGeneratedSources();
    }

    public static boolean isInGeneratedCode( VirtualFile file,  Project project) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        Module module = fileIndex.getModuleForFile(file);
        if (module == null) return false;

        VirtualFile sourceRoot = fileIndex.getSourceRootForFile(file);
        if (sourceRoot == null) return false;

        for (ContentEntry entry : ModuleRootManager.getInstance(module).getContentEntries()) {
            for (SourceFolder folder : entry.getSourceFolders()) {
                if (sourceRoot.equals(folder.getFile())) {
                    return isForGeneratedSources(folder);
                }
            }
        }
        return false;
    }

    public static GlobalSearchScope getScopeWithoutGeneratedSources( GlobalSearchScope baseScope,  Project project) {
        return new NonGeneratedSourceScope(baseScope, project);
    }

    private static class NonGeneratedSourceScope extends DelegatingGlobalSearchScope {
         private final Project myProject;

        private NonGeneratedSourceScope( GlobalSearchScope baseScope,  Project project) {
            super(baseScope);
            myProject = project;
        }

        @Override
        public boolean contains( VirtualFile file) {
            return super.contains(file) && !isInGeneratedCode(file, myProject);
        }
    }
}
