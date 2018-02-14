/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.plugin.code.pmd.config;

/**
 * @author caikang
 * @date 2017/06/19
 */
public class P3cConfig {

    private static P3cConfig instance;

    private P3cConfig() {
    }

    public static P3cConfig getInstance() {
        if (instance == null) {
            instance = new P3cConfig();
        }
        return instance;
    }

    long astCacheTime = 1000L;
    boolean astCacheEnable = true;

    long ruleCacheTime = 1000L;
    boolean ruleCacheEnable = false;

    public static boolean analysisBeforeCheckin = false;

    public String locale = localeZh;


    public static String localeEn = "en";
    public static String localeZh = "zh";

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public long getAstCacheTime() {
        return astCacheTime;
    }

    public void setAstCacheTime(long astCacheTime) {
        this.astCacheTime = astCacheTime;
    }

    public boolean isAstCacheEnable() {
        return astCacheEnable;
    }

    public void setAstCacheEnable(boolean astCacheEnable) {
        this.astCacheEnable = astCacheEnable;
    }

    public long getRuleCacheTime() {
        return ruleCacheTime;
    }

    public void setRuleCacheTime(long ruleCacheTime) {
        this.ruleCacheTime = ruleCacheTime;
    }

    public boolean isRuleCacheEnable() {
        return ruleCacheEnable;
    }

    public void setRuleCacheEnable(boolean ruleCacheEnable) {
        this.ruleCacheEnable = ruleCacheEnable;
    }

}