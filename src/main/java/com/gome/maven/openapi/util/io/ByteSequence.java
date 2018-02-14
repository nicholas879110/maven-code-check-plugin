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
package com.gome.maven.openapi.util.io;


/**
 * @author Eugene Zhuravlev
 *         Date: 11/23/10
 */
public class ByteSequence {
    private final byte[] myBytes;
    private final int myOffset;
    private final int myLen;

    public ByteSequence( byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public ByteSequence( byte[] bytes, int offset, int len) {
        myBytes = bytes;
        myOffset = offset;
        myLen = len;
    }

    
    public byte[] getBytes() {
        return myBytes;
    }

    public int getOffset() {
        return myOffset;
    }

    public int getLength() {
        return myLen;
    }
}
