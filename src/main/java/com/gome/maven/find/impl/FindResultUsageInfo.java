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
package com.gome.maven.find.impl;

import com.gome.maven.find.FindManager;
import com.gome.maven.find.FindModel;
import com.gome.maven.find.FindResult;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Segment;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.SmartPointerManager;
import com.gome.maven.psi.SmartPsiFileRange;
import com.gome.maven.usageView.UsageInfo;

public class FindResultUsageInfo extends UsageInfo {
    private final FindManager myFindManager;
    private final FindModel myFindModel;
    private SmartPsiFileRange myAnchor;

    private Boolean myCachedResult;
    private long myTimestamp = 0;

    private static final Key<Long> ourDocumentTimestampKey = Key.create("com.intellij.find.impl.FindResultUsageInfo.documentTimestamp");

    @Override
    public boolean isValid() {
        if (!super.isValid()) return false;

        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(getPsiFile());
        if (document == null) {
            myCachedResult = null;
            return false;
        }

        Boolean cachedResult = myCachedResult;
        if (document.getModificationStamp() == myTimestamp && cachedResult != null) {
            return cachedResult;
        }
        myTimestamp = document.getModificationStamp();

        Segment segment = getSegment();
        if (segment == null) {
            myCachedResult = false;
            return false;
        }

        VirtualFile file = getPsiFile().getVirtualFile();

        Segment searchOffset;
        if (myAnchor != null) {
            searchOffset = myAnchor.getRange();
            if (searchOffset == null) {
                myCachedResult = false;
                return false;
            }
        }
        else {
            searchOffset = segment;
        }

        int offset = searchOffset.getStartOffset();
        Long data = myFindModel.getUserData(ourDocumentTimestampKey);
        if (data == null || data != myTimestamp) {
            data = myTimestamp;
            FindManagerImpl.clearPreviousFindData(myFindModel);
        }
        myFindModel.putUserData(ourDocumentTimestampKey, data);
        FindResult result;
        do {
            result = myFindManager.findString(document.getCharsSequence(), offset, myFindModel, file);
            offset = result.getEndOffset() == offset ? offset + 1 : result.getEndOffset();
            if (!result.isStringFound()) {
                myCachedResult = false;
                return false;
            }
        } while (result.getStartOffset() < segment.getStartOffset());

        boolean ret = segment.getStartOffset() == result.getStartOffset() && segment.getEndOffset() == result.getEndOffset();
        myCachedResult = ret;
        return ret;
    }

    private PsiFile getPsiFile() {
        return (PsiFile)getElement();
    }

    public FindResultUsageInfo( FindManager finder,  PsiFile file, int offset,  FindModel findModel,  FindResult result) {
        super(file, result.getStartOffset(), result.getEndOffset());

        myFindManager = finder;
        myFindModel = findModel;

        assert result.isStringFound();

        if (myFindModel.isRegularExpressions() ||
                myFindModel.isInCommentsOnly() ||
                myFindModel.isInStringLiteralsOnly() ||
                myFindModel.isExceptStringLiterals() ||
                myFindModel.isExceptCommentsAndStringLiterals() ||
                myFindModel.isExceptComments()
                ) {
            myAnchor = SmartPointerManager.getInstance(getProject()).createSmartPsiFileRangePointer(file, TextRange.from(offset, 0));
        }

    }
}
