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
package com.gome.maven.psi.stubs;

import com.gome.maven.openapi.diagnostic.LogUtil;
import com.gome.maven.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.gome.maven.util.containers.RecentStringInterner;
import com.gome.maven.util.io.AbstractStringEnumerator;
import com.gome.maven.util.io.DataInputOutputUtil;
import com.gome.maven.util.io.IOUtil;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: dmitrylomov
 */
public class StubSerializationHelper {
    private final AbstractStringEnumerator myNameStorage;

    protected final TIntObjectHashMap<ObjectStubSerializer> myIdToSerializer = new TIntObjectHashMap<ObjectStubSerializer>();
    protected final TObjectIntHashMap<ObjectStubSerializer> mySerializerToId = new TObjectIntHashMap<ObjectStubSerializer>();

    public StubSerializationHelper( AbstractStringEnumerator nameStorage) {
        myNameStorage = nameStorage;
    }

    public void assignId( final ObjectStubSerializer serializer) throws IOException {
        final int id = persistentId(serializer);
        final ObjectStubSerializer old = myIdToSerializer.put(id, serializer);
        assert old == null : "ID: " + serializer.getExternalId() + " is not unique; Already registered serializer with this ID: " + old.getClass().getName();

        final int oldId = mySerializerToId.put(serializer, id);
        assert oldId == 0 : "Serializer " + serializer + " is already registered; Old ID:" + oldId;
    }

    private int persistentId( final ObjectStubSerializer serializer) throws IOException {
        return myNameStorage.enumerate(serializer.getExternalId());
    }

    private void doSerialize( Stub rootStub,  StubOutputStream stream) throws IOException {
        final ObjectStubSerializer serializer = StubSerializationUtil.getSerializer(rootStub);

        DataInputOutputUtil.writeINT(stream, getClassId(serializer));
        serializer.serialize(rootStub, stream);

        final List<? extends Stub> children = rootStub.getChildrenStubs();
        final int childrenSize = children.size();
        DataInputOutputUtil.writeINT(stream, childrenSize);
        for (int i = 0; i < childrenSize; ++i) {
            doSerialize(children.get(i), stream);
        }
    }

    public void serialize( Stub rootStub,  OutputStream stream) throws IOException {
        BufferExposingByteArrayOutputStream out = new BufferExposingByteArrayOutputStream();
        FileLocalStringEnumerator storage = new FileLocalStringEnumerator(true);
        StubOutputStream stubOutputStream = new StubOutputStream(out, storage);

        if (rootStub instanceof PsiFileStub) {
            final PsiFileStub[] roots = ((PsiFileStub)rootStub).getStubRoots();
            DataInputOutputUtil.writeINT(stubOutputStream, roots.length);
            for (PsiFileStub root : roots) {
                doSerialize(root, stubOutputStream);
            }
        }
        else {
            DataInputOutputUtil.writeINT(stubOutputStream, 1);
            doSerialize(rootStub, stubOutputStream);
        }
        DataOutputStream resultStream = new DataOutputStream(stream);
        DataInputOutputUtil.writeINT(resultStream, storage.myStrings.size());
        byte[] buffer = IOUtil.allocReadWriteUTFBuffer();
        for(String s:storage.myStrings) {
            IOUtil.writeUTFFast(buffer, resultStream, s);
        }
        resultStream.write(out.getInternalBuffer(), 0, out.size());
    }

    private int getClassId(final ObjectStubSerializer serializer) {
        final int idValue = mySerializerToId.get(serializer);
        assert idValue != 0: "No ID found for serializer " + LogUtil.objectAndClass(serializer);
        return idValue;
    }

    private final RecentStringInterner myStringInterner = new RecentStringInterner();

    
    public Stub deserialize( InputStream stream) throws IOException, SerializerNotFoundException {
        FileLocalStringEnumerator storage = new FileLocalStringEnumerator(false);
        StubInputStream inputStream = new StubInputStream(stream, storage);
        final int numberOfStrings = DataInputOutputUtil.readINT(inputStream);
        byte[] buffer = IOUtil.allocReadWriteUTFBuffer();
        storage.myStrings.ensureCapacity(numberOfStrings);

        int i = 0;
        while(i < numberOfStrings) {
            String s = myStringInterner.get(IOUtil.readUTFFast(buffer, inputStream));
            storage.myStrings.add(s);
            ++i;
        }

        int stubFilesCount = DataInputOutputUtil.readINT(inputStream);
        if (stubFilesCount > 1) {
            final List<PsiFileStub> stubs = new ArrayList<PsiFileStub>(stubFilesCount);
            while (stubFilesCount-- > 0) {
                final PsiFileStub fileStub = (PsiFileStub)deserialize(inputStream, null);
                stubs.add(fileStub);
            }
            final PsiFileStub[] stubsArray = stubs.toArray(new PsiFileStub[stubs.size()]);
            for (PsiFileStub stub : stubsArray) {
                if (stub instanceof PsiFileStubImpl) {
                    ((PsiFileStubImpl)stub).setStubRoots(stubsArray);
                }
            }
            return stubsArray[0];
        }
        else {
            return deserialize(inputStream, null);
        }
    }

    String intern(String str) {
        return myStringInterner.get(str);
    }

    
    private Stub deserialize( StubInputStream stream,  Stub parentStub) throws IOException, SerializerNotFoundException {
        final int id = DataInputOutputUtil.readINT(stream);
        final ObjectStubSerializer serializer = getClassById(id);
        if (serializer == null) {
            String externalId = null;
            try {
                externalId = myNameStorage.valueOf(id);
            } catch (Throwable ignore) {}
            throw new SerializerNotFoundException("No serializer registered for stub: ID=" + id + ", externalId:" + externalId + "; parent stub class=" + (parentStub != null? parentStub.getClass().getName() : "null"));
        }

        Stub stub = serializer.deserialize(stream, parentStub);
        int childCount = DataInputOutputUtil.readINT(stream);
        for (int i = 0; i < childCount; i++) {
            deserialize(stream, stub);
        }
        return stub;
    }


    private ObjectStubSerializer getClassById(int id) {
        return myIdToSerializer.get(id);
    }

    private static class FileLocalStringEnumerator implements AbstractStringEnumerator {
        private final TObjectIntHashMap<String> myEnumerates;
        private final ArrayList<String> myStrings = new ArrayList<String>();

        FileLocalStringEnumerator(boolean forSavingStub) {
            if (forSavingStub) myEnumerates = new TObjectIntHashMap<String>();
            else myEnumerates = null;
        }

        @Override
        public int enumerate( String value) throws IOException {
            if (value == null) return 0;
            assert myEnumerates != null; // enumerate possible only when writing stub
            int i = myEnumerates.get(value);
            if (i == 0) {
                myEnumerates.put(value, i = myStrings.size() + 1);
                myStrings.add(value);
            }
            return i;
        }

        @Override
        public String valueOf(int idx) throws IOException {
            if (idx == 0) return null;
            return myStrings.get(idx - 1);
        }

        @Override
        public void markCorrupted() {
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public boolean isDirty() {
            return false;
        }

        @Override
        public void force() {
        }
    }
}
