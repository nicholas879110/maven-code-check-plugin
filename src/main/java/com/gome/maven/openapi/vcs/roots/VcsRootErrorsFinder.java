package com.gome.maven.openapi.vcs.roots;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VcsRootErrorsFinder {
     private final Project myProject;
     private final ProjectLevelVcsManager myVcsManager;
     private final VcsRootDetector myRootDetector;

    public VcsRootErrorsFinder( Project project) {
        myProject = project;
        myVcsManager = ProjectLevelVcsManager.getInstance(project);
        myRootDetector = ServiceManager.getService(myProject, VcsRootDetector.class);
    }

    
    public Collection<VcsRootError> find() {
        List<VcsDirectoryMapping> mappings = myVcsManager.getDirectoryMappings();
        Collection<VcsRoot> vcsRoots = myRootDetector.detect();
        Collection<VcsRootError> errors = new ArrayList<VcsRootError>();
        errors.addAll(findExtraMappings(mappings));
        errors.addAll(findUnregisteredRoots(mappings, vcsRoots));
        return errors;
    }

    
    private Collection<VcsRootError> findUnregisteredRoots( List<VcsDirectoryMapping> mappings,
                                                            Collection<VcsRoot> vcsRoots) {
        Collection<VcsRootError> errors = new ArrayList<VcsRootError>();
        List<String> mappedPaths = mappingsToPathsWithSelectedVcs(mappings);
        for (VcsRoot root : vcsRoots) {
            VirtualFile virtualFileFromRoot = root.getPath();
            if (virtualFileFromRoot == null) {
                continue;
            }
            String vcsPath = virtualFileFromRoot.getPath();
            if (!mappedPaths.contains(vcsPath) && root.getVcs() != null) {
                errors.add(new VcsRootErrorImpl(VcsRootError.Type.UNREGISTERED_ROOT, vcsPath, root.getVcs().getName()));
            }
        }
        return errors;
    }

    
    private Collection<VcsRootError> findExtraMappings( List<VcsDirectoryMapping> mappings) {
        Collection<VcsRootError> errors = new ArrayList<VcsRootError>();
        for (VcsDirectoryMapping mapping : mappings) {
            if (!hasVcsChecker(mapping.getVcs())) {
                continue;
            }
            if (mapping.isDefaultMapping()) {
                if (!isRoot(mapping)) {
                    errors.add(new VcsRootErrorImpl(VcsRootError.Type.EXTRA_MAPPING, VcsDirectoryMapping.PROJECT_CONSTANT, mapping.getVcs()));
                }
            }
            else {
                String mappedPath = mapping.systemIndependentPath();
                if (!isRoot(mapping)) {
                    errors.add(new VcsRootErrorImpl(VcsRootError.Type.EXTRA_MAPPING, mappedPath, mapping.getVcs()));
                }
            }
        }
        return errors;
    }

    private static boolean hasVcsChecker(String vcs) {
        if (StringUtil.isEmptyOrSpaces(vcs)) {
            return false;
        }
        VcsRootChecker[] checkers = Extensions.getExtensions(VcsRootChecker.EXTENSION_POINT_NAME);
        for (VcsRootChecker checker : checkers) {
            if (vcs.equalsIgnoreCase(checker.getSupportedVcs().getName())) {
                return true;
            }
        }
        return false;
    }

    
    public static Collection<VirtualFile> vcsRootsToVirtualFiles( Collection<VcsRoot> vcsRoots) {
        return ContainerUtil.map(vcsRoots, new Function<VcsRoot, VirtualFile>() {
            @Override
            public VirtualFile fun(VcsRoot root) {
                return root.getPath();
            }
        });
    }

    private List<String> mappingsToPathsWithSelectedVcs( List<VcsDirectoryMapping> mappings) {
        List<String> paths = new ArrayList<String>();
        for (VcsDirectoryMapping mapping : mappings) {
            if (StringUtil.isEmptyOrSpaces(mapping.getVcs())) {
                continue;
            }
            if (!mapping.isDefaultMapping()) {
                paths.add(mapping.systemIndependentPath());
            }
            else {
                String basePath = myProject.getBasePath();
                if (basePath != null) {
                    paths.add(FileUtil.toSystemIndependentName(basePath));
                }
            }
        }
        return paths;
    }

    public static VcsRootErrorsFinder getInstance(Project project) {
        return new VcsRootErrorsFinder(project);
    }

    private boolean isRoot( final VcsDirectoryMapping mapping) {
        VcsRootChecker[] checkers = Extensions.getExtensions(VcsRootChecker.EXTENSION_POINT_NAME);
        final String pathToCheck = mapping.isDefaultMapping() ? myProject.getBasePath() : mapping.getDirectory();
        return ContainerUtil.find(checkers, new Condition<VcsRootChecker>() {
            @Override
            public boolean value(VcsRootChecker checker) {
                return checker.getSupportedVcs().getName().equalsIgnoreCase(mapping.getVcs()) && checker.isRoot(pathToCheck);
            }
        }) != null;
    }
}
