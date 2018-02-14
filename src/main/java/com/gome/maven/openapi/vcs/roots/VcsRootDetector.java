package com.gome.maven.openapi.vcs.roots;

import com.gome.maven.openapi.vcs.VcsRoot;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.util.Collection;

/**
 * Interface for detecting VCS roots in the project.
 *
 * @author Nadya Zabrodina
 */
public interface VcsRootDetector {

    /**
     * Detect vcs roots for whole project
     */
    
    Collection<VcsRoot> detect();

    /**
     * Detect vcs roots for startDir
     */
    
    Collection<VcsRoot> detect( VirtualFile startDir);
}
