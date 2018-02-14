/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.openapi.updateSettings.impl;


import java.util.Collections;
import java.util.List;

public class CheckForUpdateResult {
    private final BuildInfo myNewBuildInSelectedChannel;
    private final UpdateChannel myUpdatedChannel;
    private final List<String> myAllChannelIds;
    private final UpdateStrategy.State myState;
    private final Exception myError;
    private UpdateChannel myChannelToPropose = null;

    public CheckForUpdateResult( UpdateChannel updated,
                                 BuildInfo newBuildInSelectedChannel,
                                 List<String> allChannelsIds) {
        myNewBuildInSelectedChannel = newBuildInSelectedChannel;
        myUpdatedChannel = updated;
        myAllChannelIds = allChannelsIds;
        myState = UpdateStrategy.State.LOADED;
        myError = null;
    }

    public CheckForUpdateResult( UpdateStrategy.State state,  Exception e) {
        myNewBuildInSelectedChannel = null;
        myUpdatedChannel = null;
        myAllChannelIds = Collections.emptyList();
        myState = state;
        myError = e;
    }

    public CheckForUpdateResult( UpdateStrategy.State state) {
        this(state, null);
    }

    
    public BuildInfo getNewBuildInSelectedChannel() {
        return myNewBuildInSelectedChannel;
    }

    
    public UpdateChannel getUpdatedChannel() {
        return myUpdatedChannel;
    }

    
    public List<String> getAllChannelsIds() {
        return myAllChannelIds;
    }

    
    public UpdateStrategy.State getState() {
        return myState;
    }

    
    public Exception getError() {
        return myError;
    }

    
    public UpdateChannel getChannelToPropose() {
        return myChannelToPropose;
    }

    public void setChannelToPropose( UpdateChannel channelToPropose) {
        myChannelToPropose = channelToPropose;
    }
}
