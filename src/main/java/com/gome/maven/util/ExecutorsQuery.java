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
package com.gome.maven.util;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.project.IndexNotReadyException;

import java.util.List;

/**
 * @author max
 */
public final class ExecutorsQuery<Result, Parameter> extends AbstractQuery<Result> {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.util.ExecutorsQuery");

    private final List<QueryExecutor<Result, Parameter>> myExecutors;
    private final Parameter myParameters;

    public ExecutorsQuery( final Parameter params,  List<QueryExecutor<Result, Parameter>> executors) {
        myParameters = params;
        myExecutors = executors;
    }

    @Override
    protected boolean processResults( final Processor<Result> consumer) {
        for (QueryExecutor<Result, Parameter> executor : myExecutors) {
            try {
                if (!executor.execute(myParameters, consumer)) {
                    return false;
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (IndexNotReadyException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }

        return true;
    }

}
