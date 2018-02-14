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
package com.gome.maven.psi.stubs;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageParserDefinitions;
import com.gome.maven.lang.TreeBackedLighterAST;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.Trinity;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.StubBuilder;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.IFileElementType;
import com.gome.maven.psi.tree.IStubFileElementType;
import com.gome.maven.util.Function;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.indexing.FileContent;
import com.gome.maven.util.indexing.FileContentImpl;
import com.gome.maven.util.indexing.IndexingDataKeys;
import com.gome.maven.util.indexing.SubstitutedFileType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StubTreeBuilder {
    private static final Key<Stub> stubElementKey = Key.create("stub.tree.for.file.content");

    private StubTreeBuilder() { }

    
    public static Stub buildStubTree(final FileContent inputData) {
        Stub data = inputData.getUserData(stubElementKey);
        if (data != null) return data;

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (inputData) {
            data = inputData.getUserData(stubElementKey);
            if (data != null) return data;

            final FileType fileType = inputData.getFileType();

            final BinaryFileStubBuilder builder = BinaryFileStubBuilders.INSTANCE.forFileType(fileType);
            if (builder != null) {
                data = builder.buildStubTree(inputData);
            }
            else {
                final LanguageFileType languageFileType = (LanguageFileType)fileType;

                CharSequence contentAsText = inputData.getContentAsText();
                FileContentImpl fileContent = (FileContentImpl)inputData;
                PsiFile psi = fileContent.getPsiFileForPsiDependentIndex();
                final FileViewProvider viewProvider = psi.getViewProvider();
                psi = viewProvider.getStubBindingRoot();
                psi.putUserData(IndexingDataKeys.FILE_TEXT_CONTENT_KEY, contentAsText);
                final IStubFileElementType type = ((PsiFileImpl)psi).getElementTypeForStubBuilder();

                // if we load AST, it should be easily gc-able. See PsiFileImpl.createTreeElementPointer()
                psi.getManager().startBatchFilesProcessingMode();

                try {
                    IStubFileElementType stubFileElementType;
                    if (type != null) {
                        stubFileElementType = type;
                    }
                    else if (languageFileType instanceof SubstitutedFileType) {
                        SubstitutedFileType substituted = (SubstitutedFileType)languageFileType;
                        LanguageFileType original = (LanguageFileType)substituted.getOriginalFileType();
                        final IFileElementType originalType = LanguageParserDefinitions.INSTANCE.forLanguage(original.getLanguage()).getFileNodeType();
                        stubFileElementType = originalType instanceof IStubFileElementType ? (IStubFileElementType)originalType : null;
                    }
                    else {
                        stubFileElementType = null;
                    }
                    if (stubFileElementType != null) {
                        final StubBuilder stubBuilder = stubFileElementType.getBuilder();
                        if (stubBuilder instanceof LightStubBuilder) {
                            LightStubBuilder.FORCED_AST.set(fileContent.getLighterASTForPsiDependentIndex());
                        }
                        data = stubBuilder.buildStubTree(psi);

                        final List<Pair<IStubFileElementType, PsiFile>> stubbedRoots = getStubbedRoots(viewProvider);
                        if (stubbedRoots.size() > 1) {
                            final List<PsiFileStub> stubs = new ArrayList<PsiFileStub>(stubbedRoots.size());
                            stubs.add((PsiFileStub)data);

                            for (Pair<IStubFileElementType, PsiFile> stubbedRoot : stubbedRoots) {
                                final PsiFile secondaryPsi = stubbedRoot.second;
                                if (psi == secondaryPsi) continue;
                                final StubBuilder stubbedRootBuilder = stubbedRoot.first.getBuilder();
                                if (stubbedRootBuilder instanceof LightStubBuilder) {
                                    LightStubBuilder.FORCED_AST.set(new TreeBackedLighterAST(secondaryPsi.getNode()));
                                }
                                final StubElement element = stubbedRootBuilder.buildStubTree(secondaryPsi);
                                if (element instanceof PsiFileStub) {
                                    stubs.add((PsiFileStub)element);
                                }
                            }
                            final PsiFileStub[] stubsArray = stubs.toArray(new PsiFileStub[stubs.size()]);
                            for (PsiFileStub stub : stubsArray) {
                                if (stub instanceof PsiFileStubImpl) {
                                    ((PsiFileStubImpl)stub).setStubRoots(stubsArray);
                                }
                            }
                        }
                    }
                }
                finally {
                    psi.putUserData(IndexingDataKeys.FILE_TEXT_CONTENT_KEY, null);
                    psi.getManager().finishBatchFilesProcessingMode();
                }
            }

            inputData.putUserData(stubElementKey, data);
            return data;
        }
    }

    /** Order is deterministic. First element matches {@link FileViewProvider#getStubBindingRoot()} */
    
    public static List<Pair<IStubFileElementType, PsiFile>> getStubbedRoots( FileViewProvider viewProvider) {
        final List<Trinity<Language, IStubFileElementType, PsiFile>> roots =
                new SmartList<Trinity<Language, IStubFileElementType, PsiFile>>();
        final PsiFile stubBindingRoot = viewProvider.getStubBindingRoot();
        for (Language language : viewProvider.getLanguages()) {
            final PsiFile file = viewProvider.getPsi(language);
            if (file instanceof PsiFileImpl) {
                final IElementType type = ((PsiFileImpl)file).getElementTypeForStubBuilder();
                if (type != null) {
                    roots.add(Trinity.create(language, (IStubFileElementType)type, file));
                }
            }
        }

        ContainerUtil.sort(roots, new Comparator<Trinity<Language, IStubFileElementType, PsiFile>>() {
            @Override
            public int compare(Trinity<Language, IStubFileElementType, PsiFile> o1, Trinity<Language, IStubFileElementType, PsiFile> o2) {
                if (o1.third == stubBindingRoot) return o2.third == stubBindingRoot ? 0 : -1;
                else if (o2.third == stubBindingRoot) return 1;
                else return StringUtil.compare(o1.first.getID(), o2.first.getID(), false);
            }
        });

        return ContainerUtil.map(roots, new Function<Trinity<Language, IStubFileElementType, PsiFile>, Pair<IStubFileElementType, PsiFile>>() {
            @Override
            public Pair<IStubFileElementType, PsiFile> fun(Trinity<Language, IStubFileElementType, PsiFile> trinity) {
                return Pair.create(trinity.second, trinity.third);
            }
        });
    }
}