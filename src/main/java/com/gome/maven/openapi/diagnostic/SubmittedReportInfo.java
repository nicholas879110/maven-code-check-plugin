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
package com.gome.maven.openapi.diagnostic;


/**
 * Simple bean representing error submission status.
 */
public class SubmittedReportInfo {
    public enum SubmissionStatus {
        /**
         * Issue have been successfully created
         */
        NEW_ISSUE,

        /**
         * Issue is actually a duplicate of existing one
         */
        DUPLICATE,

        /**
         * Submission failed (e.g. because of network problem)
         */
        FAILED
    }

    private final String myUrl;
    private final String myLinkText;
    private final SubmissionStatus myStatus;

    public SubmittedReportInfo(SubmissionStatus status) {
        this(null, null, status);
    }

    public SubmittedReportInfo( String url,  String linkText,  SubmissionStatus status) {
        myUrl = url;
        myLinkText = linkText;
        myStatus = status;
    }

    public String getURL() {
        return myUrl;
    }

    public String getLinkText() {
        return myLinkText;
    }

    public SubmissionStatus getStatus() {
        return myStatus;
    }
}
