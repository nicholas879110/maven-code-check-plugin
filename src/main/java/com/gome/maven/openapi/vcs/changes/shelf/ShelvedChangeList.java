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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 24.11.2006
 * Time: 20:20:04
 */
package com.gome.maven.openapi.vcs.changes.shelf;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.impl.patch.FilePatch;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.DefaultJDOMExternalizer;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMExternalizable;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vcs.FileStatus;
import org.jdom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class ShelvedChangeList implements JDOMExternalizable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.changes.shelf.ShelvedChangeList");

     private static final String ATTRIBUTE_DATE = "date";
     private static final String ELEMENT_BINARY = "binary";

    public String PATH;
    public String DESCRIPTION;
    public Date DATE;
    private List<ShelvedChange> myChanges;
    private List<ShelvedBinaryFile> myBinaryFiles;
    private boolean myRecycled;

    public ShelvedChangeList() {
    }

    public ShelvedChangeList(final String path, final String description, final List<ShelvedBinaryFile> binaryFiles) {
        this(path, description, binaryFiles, System.currentTimeMillis());
    }

    public ShelvedChangeList(final String path, final String description, final List<ShelvedBinaryFile> binaryFiles, final long time) {
        PATH = path;
        DESCRIPTION = description;
        DATE = new Date(time);
        myBinaryFiles = binaryFiles;
    }

    public boolean isRecycled() {
        return myRecycled;
    }

    public void setRecycled(final boolean recycled) {
        myRecycled = recycled;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);
        DATE = new Date(Long.parseLong(element.getAttributeValue(ATTRIBUTE_DATE)));

        //noinspection unchecked
        final List<Element> children = (List<Element>)element.getChildren(ELEMENT_BINARY);
        myBinaryFiles = new ArrayList<ShelvedBinaryFile>(children.size());
        for (Element child : children) {
            ShelvedBinaryFile binaryFile = new ShelvedBinaryFile();
            binaryFile.readExternal(child);
            myBinaryFiles.add(binaryFile);
        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        DefaultJDOMExternalizer.writeExternal(this, element);
        element.setAttribute(ATTRIBUTE_DATE, Long.toString(DATE.getTime()));
        for (ShelvedBinaryFile file : myBinaryFiles) {
            Element child = new Element(ELEMENT_BINARY);
            file.writeExternal(child);
            element.addContent(child);
        }
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }

    public List<ShelvedChange> getChanges(Project project) {
        if (myChanges == null) {
            try {
                myChanges = new ArrayList<ShelvedChange>();
                final List<? extends FilePatch> list = ShelveChangesManager.loadPatchesWithoutContent(project, PATH, null);
                for (FilePatch patch : list) {
                    FileStatus status;
                    if (patch.isNewFile()) {
                        status = FileStatus.ADDED;
                    }
                    else if (patch.isDeletedFile()) {
                        status = FileStatus.DELETED;
                    }
                    else {
                        status = FileStatus.MODIFIED;
                    }
                    myChanges.add(new ShelvedChange(PATH, patch.getBeforeName(), patch.getAfterName(), status));
                }
            }
            catch (Exception e) {
                LOG.error("Failed to parse the file patch: [" + PATH + "]", e);
            }
        }
        return myChanges;
    }

    public void clearLoadedChanges() {
        myChanges = null;
    }

    public List<ShelvedBinaryFile> getBinaryFiles() {
        return myBinaryFiles;
    }

     private static final String ELEMENT_CHANGELIST = "changelist";
     private static final String ELEMENT_RECYCLED_CHANGELIST = "recycled_changelist";

    public static Collection<ShelvedChangeList> readChanges(final Element element, final boolean recycled, final boolean checkForFileExistance) throws InvalidDataException {
        final List<Element> children = (List<Element>)element.getChildren(recycled ? ELEMENT_RECYCLED_CHANGELIST : ELEMENT_CHANGELIST);

        final List<ShelvedChangeList> result = new ArrayList<ShelvedChangeList>();

        readList(children, result, checkForFileExistance);

        if (recycled) {
            for (ShelvedChangeList list : result) {
                list.setRecycled(true);
            }
        }
        return result;
    }



    private static void readList(final List<Element> children, final List<ShelvedChangeList> sink, boolean checkForFileExistance) throws InvalidDataException {
        for (Element child : children) {
            ShelvedChangeList data = new ShelvedChangeList();
            data.readExternal(child);
            if (!checkForFileExistance || new File(data.PATH).exists()) {
                sink.add(data);
            }
        }
    }

    public static void writeChanges(final Collection<ShelvedChangeList> shelvedChangeLists, final Collection<ShelvedChangeList> recycledShelvedChangeLists,
                                    Element element) throws WriteExternalException {
        for(ShelvedChangeList data: shelvedChangeLists) {
            Element child = new Element(ELEMENT_CHANGELIST);
            data.writeExternal(child);
            element.addContent(child);
        }
        for(ShelvedChangeList data: recycledShelvedChangeLists) {
            Element child = new Element(ELEMENT_RECYCLED_CHANGELIST);
            data.writeExternal(child);
            element.addContent(child);
        }
    }

}
