/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.vcsUtil;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.AbstractVcs;
import com.gome.maven.openapi.vcs.ProjectLevelVcsManager;
import com.gome.maven.openapi.vcs.rollback.DefaultRollbackEnvironment;
import com.gome.maven.openapi.vcs.rollback.RollbackEnvironment;

import java.util.Collection;

import static java.util.Arrays.asList;

/**
 * @author Kirill Likhodedov
 */
public class RollbackUtil {

    private RollbackUtil() {
    }


    /**
     * Finds the most appropriate name for the "Rollback" operation for the given VCSs.
     * That is: iterates through the all {@link RollbackEnvironment#getRollbackOperationName() RollbackEnvironments} and picks
     * the operation name if it is equal to all given VCSs.
     * Otherwise picks the {@link DefaultRollbackEnvironment#ROLLBACK_OPERATION_NAME default name}.
     * @param vcses affected VCSs.
     * @return name for the "rollback" operation to be used in the UI.
     */
    
    public static String getRollbackOperationName( Collection<AbstractVcs> vcses) {
        String operationName = null;
        for (AbstractVcs vcs : vcses) {
            final RollbackEnvironment rollbackEnvironment = vcs.getRollbackEnvironment();
            if (rollbackEnvironment != null) {
                if (operationName == null) {
                    operationName = rollbackEnvironment.getRollbackOperationName();
                }
                else if (!operationName.equals(rollbackEnvironment.getRollbackOperationName())) {
                    // if there are different names, use default
                    return DefaultRollbackEnvironment.ROLLBACK_OPERATION_NAME;
                }
            }
        }
        return operationName != null ? operationName : DefaultRollbackEnvironment.ROLLBACK_OPERATION_NAME;
    }

    /**
     * Finds the appropriate name for the "rollback" operation, looking through all VCSs registered in the project.
     * @see #getRollbackOperationName(java.util.Collection)
     */
    
    public static String getRollbackOperationName( Project project) {
        return getRollbackOperationName(asList(ProjectLevelVcsManager.getInstance(project).getAllActiveVcss()));
    }

}
