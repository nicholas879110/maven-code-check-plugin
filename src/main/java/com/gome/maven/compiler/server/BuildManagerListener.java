package com.gome.maven.compiler.server;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.messages.Topic;

import java.util.UUID;

/**
 * @author Eugene Zhuravlev
 *         Date: 7/3/13
 */
public interface BuildManagerListener {
    Topic<BuildManagerListener> TOPIC = Topic.create("Build Manager", BuildManagerListener.class);

    void buildStarted(Project project, UUID sessionId, boolean isAutomake);

    void buildFinished(Project project, UUID sessionId, boolean isAutomake);
}
