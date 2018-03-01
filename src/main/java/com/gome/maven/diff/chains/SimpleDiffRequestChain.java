/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.diff.chains;

import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

public class SimpleDiffRequestChain extends UserDataHolderBase implements DiffRequestChain {
     private final List<DiffRequestProducerWrapper> myRequests;
    private int myIndex = 0;

    public SimpleDiffRequestChain( DiffRequest request) {
        this(Collections.singletonList(request));
    }

    public SimpleDiffRequestChain( List<? extends DiffRequest> requests) {
        myRequests = ContainerUtil.map(requests, new Function<DiffRequest, DiffRequestProducerWrapper>() {
            @Override
            public DiffRequestProducerWrapper fun(DiffRequest request) {
                return new DiffRequestProducerWrapper(request);
            }
        });
    }

    @Override
    
    public List<DiffRequestProducerWrapper> getRequests() {
        return myRequests;
    }

    @Override
    public int getIndex() {
        return myIndex;
    }

    @Override
    public void setIndex(int index) {
        assert index >= 0 && index < myRequests.size();
        myIndex = index;
    }

    public static class DiffRequestProducerWrapper implements DiffRequestProducer {
         private final DiffRequest myRequest;

        public DiffRequestProducerWrapper( DiffRequest request) {
            myRequest = request;
        }

        
        public DiffRequest getRequest() {
            return myRequest;
        }

        
        @Override
        public String getName() {
            return StringUtil.notNullize(myRequest.getTitle(), "Change");
        }

        
        @Override
        public DiffRequest process( UserDataHolder context,  ProgressIndicator indicator)
                throws DiffRequestProducerException, ProcessCanceledException {
            return myRequest;
        }
    }
}
