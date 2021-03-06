/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.packaging.impl.compiler;

import com.gome.maven.openapi.application.ReadAction;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.packaging.artifacts.Artifact;
import com.gome.maven.packaging.artifacts.ArtifactManager;
import com.gome.maven.util.containers.MultiMap;
import org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;
import org.jetbrains.jps.incremental.artifacts.ArtifactBuildTargetType;

import java.util.List;

/**
 * @author nik
 */
public class ArtifactCompilerUtil {
    private ArtifactCompilerUtil() {
    }


    public static boolean containsArtifacts(List<TargetTypeBuildScope> scopes) {
        for (TargetTypeBuildScope scope : scopes) {
            if (ArtifactBuildTargetType.INSTANCE.getTypeId().equals(scope.getTypeId())) {
                return true;
            }
        }
        return false;
    }

    public static MultiMap<String, Artifact> createOutputToArtifactMap(final Project project) {
        final MultiMap<String, Artifact> result = MultiMap.create(FileUtil.PATH_HASHING_STRATEGY);
        new ReadAction() {
            protected void run(final Result r) {
                for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
                    String outputPath = artifact.getOutputFilePath();
                    if (!StringUtil.isEmpty(outputPath)) {
                        result.putValue(outputPath, artifact);
                    }
                }
            }
        }.execute();


        return result;
    }
}
