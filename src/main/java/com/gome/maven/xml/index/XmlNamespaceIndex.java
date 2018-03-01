/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.xml.index;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.util.NullableFunction;
import com.gome.maven.util.indexing.DataIndexer;
import com.gome.maven.util.indexing.FileBasedIndex;
import com.gome.maven.util.indexing.FileContent;
import com.gome.maven.util.indexing.ID;
import com.gome.maven.util.io.DataExternalizer;
import com.gome.maven.util.io.DataInputOutputUtil;
import com.gome.maven.util.io.IOUtil;
import com.gome.maven.util.text.CharArrayUtil;
import com.gome.maven.xml.util.XmlUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Dmitry Avdeev
 */
public class XmlNamespaceIndex extends XmlIndex<XsdNamespaceBuilder> {

    
    public static String getNamespace( VirtualFile file, final Project project, PsiFile context) {
        if (DumbService.isDumb(project) || (context != null && XmlUtil.isStubBuilding())) {
            try {
                return XsdNamespaceBuilder.computeNamespace(file.getInputStream());
            }
            catch (IOException e) {
                return null;
            }
        }
        final List<XsdNamespaceBuilder> list = FileBasedIndex.getInstance().getValues(NAME, file.getUrl(), createFilter(project));
        return list.size() == 0 ? null : list.get(0).getNamespace();
    }

    public static List<IndexedRelevantResource<String, XsdNamespaceBuilder>> getResourcesByNamespace(String namespace,
                                                                                                      Project project,
                                                                                                      Module module) {
        List<IndexedRelevantResource<String, XsdNamespaceBuilder>> resources =
                IndexedRelevantResource.getResources(NAME, namespace, module, project, null);
        Collections.sort(resources);
        return resources;
    }

    public static List<IndexedRelevantResource<String, XsdNamespaceBuilder>> getAllResources( final Module module,
                                                                                              Project project,
                                                                                              NullableFunction<List<IndexedRelevantResource<String, XsdNamespaceBuilder>>, IndexedRelevantResource<String, XsdNamespaceBuilder>> chooser) {
        return IndexedRelevantResource.getAllResources(NAME, module, project, chooser);
    }

    public static final ID<String,XsdNamespaceBuilder> NAME = ID.create("XmlNamespaces");

    @Override
    
    public ID<String, XsdNamespaceBuilder> getName() {
        return NAME;
    }

    @Override
    
    public DataIndexer<String, XsdNamespaceBuilder, FileContent> getIndexer() {
        return new DataIndexer<String, XsdNamespaceBuilder, FileContent>() {
            @Override
            
            public Map<String, XsdNamespaceBuilder> map( final FileContent inputData) {
                final XsdNamespaceBuilder builder;
                if ("dtd".equals(inputData.getFile().getExtension())) {
                    builder = new XsdNamespaceBuilder(inputData.getFileName(), "", Collections.<String>emptyList());
                }
                else {
                    builder = XsdNamespaceBuilder.computeNamespace(CharArrayUtil.readerFromCharSequence(inputData.getContentAsText()));
                }
                final HashMap<String, XsdNamespaceBuilder> map = new HashMap<String, XsdNamespaceBuilder>(2);
                String namespace = builder.getNamespace();
                if (namespace != null) {
                    map.put(namespace, builder);
                }
                // so that we could get ns by file url (see getNamespace method above)
                map.put(inputData.getFile().getUrl(), builder);
                return map;
            }
        };
    }

    
    @Override
    public DataExternalizer<XsdNamespaceBuilder> getValueExternalizer() {
        return new DataExternalizer<XsdNamespaceBuilder>() {
            @Override
            public void save( DataOutput out, XsdNamespaceBuilder value) throws IOException {
                IOUtil.writeUTF(out, value.getNamespace() == null ? "" : value.getNamespace());
                IOUtil.writeUTF(out, value.getVersion() == null ? "" : value.getVersion());
                DataInputOutputUtil.writeINT(out, value.getTags().size());
                for (String s : value.getTags()) {
                    IOUtil.writeUTF(out, s);
                }
            }

            @Override
            public XsdNamespaceBuilder read( DataInput in) throws IOException {

                int count;
                XsdNamespaceBuilder builder = new XsdNamespaceBuilder(IOUtil.readUTF(in), IOUtil.readUTF(in),
                        new ArrayList<String>(count = DataInputOutputUtil.readINT(in)));
                for (int i = 0; i < count; i++) {
                    builder.getTags().add(IOUtil.readUTF(in));
                }
                return builder;
            }
        };
    }

    @Override
    public int getVersion() {
        return 3;
    }

    
    public static IndexedRelevantResource<String, XsdNamespaceBuilder> guessSchema(String namespace,
                                                                                    final String tagName,
                                                                                    final String version,
                                                                                    String schemaLocation,
                                                                                    Module module,
                                                                                    Project project) {

        final List<IndexedRelevantResource<String, XsdNamespaceBuilder>>
                resources = getResourcesByNamespace(namespace, project, module);

        if (resources.isEmpty()) return null;
        if (resources.size() == 1) return resources.get(0);
        final String fileName = schemaLocation == null ? null : new File(schemaLocation).getName();
        IndexedRelevantResource<String, XsdNamespaceBuilder> resource =
                Collections.max(resources, new Comparator<IndexedRelevantResource<String, XsdNamespaceBuilder>>() {
                    @Override
                    public int compare(IndexedRelevantResource<String, XsdNamespaceBuilder> o1,
                                       IndexedRelevantResource<String, XsdNamespaceBuilder> o2) {
                        if (fileName != null) {
                            int i = Comparing.compare(fileName.equals(o1.getFile().getName()), fileName.equals(o2.getFile().getName()));
                            if (i != 0) return i;
                        }
                        if (tagName != null) {
                            int i = Comparing.compare(o1.getValue().hasTag(tagName), o2.getValue().hasTag(tagName));
                            if (i != 0) return i;
                        }
                        int i = o1.compareTo(o2);
                        if (i != 0) return i;
                        return o1.getValue().getRating(tagName, version) - o2.getValue().getRating(tagName, version);
                    }
                });
        if (tagName != null && !resource.getValue().hasTag(tagName)) {
            return null;
        }
        return resource;
    }

    
    public static XmlFile guessSchema(String namespace,
                                       final String tagName,
                                       final String version,
                                       String schemaLocation,
                                       PsiFile file) {

        if (DumbService.isDumb(file.getProject()) || XmlUtil.isStubBuilding()) return null;

        IndexedRelevantResource<String,XsdNamespaceBuilder> resource =
                guessSchema(namespace, tagName, version, schemaLocation, ModuleUtilCore.findModuleForPsiElement(file), file.getProject());
        if (resource == null) return null;
        return findSchemaFile(resource.getFile(), file);
    }

    
    private static XmlFile findSchemaFile(VirtualFile resourceFile, PsiFile baseFile) {
        PsiFile psiFile = baseFile.getManager().findFile(resourceFile);
        return psiFile instanceof XmlFile ? (XmlFile)psiFile : null;
    }

    
    public static XmlFile guessDtd(String dtdUri,  PsiFile baseFile) {

        if (!dtdUri.endsWith(".dtd") ||
                DumbService.isDumb(baseFile.getProject()) ||
                XmlUtil.isStubBuilding()) return null;

        String dtdFileName = new File(dtdUri).getName();
        List<IndexedRelevantResource<String, XsdNamespaceBuilder>>
                list = getResourcesByNamespace(dtdFileName, baseFile.getProject(), ModuleUtilCore.findModuleForPsiElement(baseFile));
        if (list.isEmpty()) {
            return null;
        }
        IndexedRelevantResource<String, XsdNamespaceBuilder> resource;
        if (list.size() > 1) {
            final String[] split = dtdUri.split("/");
            resource = Collections.max(list, new Comparator<IndexedRelevantResource<String, XsdNamespaceBuilder>>() {
                @Override
                public int compare(IndexedRelevantResource<String, XsdNamespaceBuilder> o1,
                                   IndexedRelevantResource<String, XsdNamespaceBuilder> o2) {

                    return weight(o1) - weight(o2);
                }

                int weight(IndexedRelevantResource<String, XsdNamespaceBuilder> o1) {
                    VirtualFile file = o1.getFile();
                    for (int i = split.length - 1; i >= 0 && file != null; i--) {
                        String s = split[i];
                        if (!s.equals(file.getName())) {
                            return split.length - i;
                        }
                        file = file.getParent();
                    }
                    return 0;
                }
            });
        }
        else {
            resource = list.get(0);
        }
        return findSchemaFile(resource.getFile(), baseFile);
    }
}
