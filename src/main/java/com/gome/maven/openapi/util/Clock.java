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

package com.gome.maven.openapi.util;


import java.util.Date;

public class Clock {
    private static long ourTime = -1;

    public static long getTime() {
        if (ourTime != -1) return ourTime;
        return System.currentTimeMillis();
    }

    
    public static void setTime(long time) {
        ourTime = time;
    }

    
    public static void setTime( Date date) {
        setTime(date.getTime());
    }

    
    public static void setTime(int year, int month, int day) {
        setTime(year, month, day, 0, 0);
    }

    
    public static void setTime(int year, int month, int day, int hours, int minutes) {
        setTime(year, month, day, hours, minutes, 0);
    }

    
    public static void setTime(int year, int month, int day, int hours, int minutes, int seconds) {
        setTime(new Date(year - 1900, month, day, hours, minutes, seconds));
    }

    
    public static void reset() {
        ourTime = -1;
    }
}
