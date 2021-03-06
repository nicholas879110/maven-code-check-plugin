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
package org.jetbrains.jps.incremental.storage;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.io.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author Eugene Zhuravlev
 *         Date: 10/7/11
 */
public class ProjectTimestamps {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.jps.incremental.storage.ProjectTimestamps");
  private static final String TIMESTAMP_STORAGE = "timestamps";
  private final TimestampStorage myTimestamps;
  private final File myTimestampsRoot;

  public ProjectTimestamps(final File dataStorageRoot, BuildTargetsState targetsState) throws IOException {
    myTimestampsRoot = new File(dataStorageRoot, TIMESTAMP_STORAGE);
    myTimestamps = new TimestampStorage(new File(myTimestampsRoot, "data"), targetsState);
  }

  public TimestampStorage getStorage() {
    return myTimestamps;
  }

  public void clean() throws IOException {
    final TimestampStorage timestamps = myTimestamps;
    if (timestamps != null) {
      timestamps.wipe();
    }
    else {
      FileUtil.delete(myTimestampsRoot);
    }
  }

  public void close() {
    final TimestampStorage timestamps = myTimestamps;
    if (timestamps != null) {
      try {
        timestamps.close();
      }
      catch (IOException e) {
        LOG.error(e);
        FileUtil.delete(myTimestampsRoot);
      }
    }
  }
}
