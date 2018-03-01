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
package com.gome.maven.openapi.diff.impl.patch.apply;

import com.gome.maven.openapi.diff.impl.patch.BinaryFilePatch;
import com.gome.maven.openapi.diff.impl.patch.FilePatch;
import com.gome.maven.openapi.diff.impl.patch.TextFilePatch;
import com.gome.maven.openapi.vcs.changes.shelf.ShelveChangesManager;

public class ApplyFilePatchFactory {
    private ApplyFilePatchFactory() {
    }

    public static ApplyTextFilePatch create(final TextFilePatch patch) {
        return new ApplyTextFilePatch(patch);
    }

    public static ApplyBinaryFilePatch create(final BinaryFilePatch patch) {
        return new ApplyBinaryFilePatch(patch);
    }

    public static ApplyBinaryShelvedFilePatch create(final ShelveChangesManager.ShelvedBinaryFilePatch patch) {
        return new ApplyBinaryShelvedFilePatch(patch);
    }

    public static ApplyFilePatchBase createGeneral(final FilePatch patch) {
        if (patch instanceof TextFilePatch) {
            return create((TextFilePatch) patch);
        } else if (patch instanceof BinaryFilePatch) {
            return create((BinaryFilePatch) patch);
        } else if (patch instanceof ShelveChangesManager.ShelvedBinaryFilePatch) {
            return create((ShelveChangesManager.ShelvedBinaryFilePatch) patch);
        }
        throw new IllegalStateException();
    }
}
