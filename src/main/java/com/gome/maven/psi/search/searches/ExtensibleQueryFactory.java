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

package com.gome.maven.psi.search.searches;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.extensions.ExtensionPoint;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.extensions.SimpleSmartExtensionPoint;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.QueryExecutor;
import com.gome.maven.util.QueryFactory;
import com.gome.maven.util.SmartList;

import java.util.List;

/**
 * @author yole
 */
public class ExtensibleQueryFactory<Result, Parameters> extends QueryFactory<Result, Parameters> {
    private final NotNullLazyValue<SimpleSmartExtensionPoint<QueryExecutor<Result,Parameters>>> myPoint;

    protected ExtensibleQueryFactory() {
        this("com.gome.maven");
    }

    protected ExtensibleQueryFactory( final String epNamespace) {
        myPoint = new NotNullLazyValue<SimpleSmartExtensionPoint<QueryExecutor<Result, Parameters>>>() {
            @Override
            
            protected SimpleSmartExtensionPoint<QueryExecutor<Result, Parameters>> compute() {
                return new SimpleSmartExtensionPoint<QueryExecutor<Result, Parameters>>(new SmartList<QueryExecutor<Result, Parameters>>()){
                    @Override
                    
                    protected ExtensionPoint<QueryExecutor<Result, Parameters>> getExtensionPoint() {
                         String epName = ExtensibleQueryFactory.this.getClass().getName();
                        int pos = epName.lastIndexOf('.');
                        if (pos >= 0) {
                            epName = epName.substring(pos+1);
                        }
                        epName = epNamespace + "." + StringUtil.decapitalize(epName);
                        return Extensions.getRootArea().getExtensionPoint(epName);
                    }
                };
            }
        };
    }

    public void registerExecutor(final QueryExecutor<Result, Parameters> queryExecutor, Disposable parentDisposable) {
        registerExecutor(queryExecutor);
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                unregisterExecutor(queryExecutor);
            }
        });
    }

    @Override
    public void registerExecutor( final QueryExecutor<Result, Parameters> queryExecutor) {
        myPoint.getValue().addExplicitExtension(queryExecutor);
    }

    @Override
    public void unregisterExecutor( final QueryExecutor<Result, Parameters> queryExecutor) {
        myPoint.getValue().removeExplicitExtension(queryExecutor);
    }

    @Override
    
    protected List<QueryExecutor<Result, Parameters>> getExecutors() {
        return myPoint.getValue().getExtensions();
    }
}