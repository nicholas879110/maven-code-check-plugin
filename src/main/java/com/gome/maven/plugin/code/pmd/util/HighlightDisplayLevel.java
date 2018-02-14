package com.gome.maven.plugin.code.pmd.util;

public class HighlightDisplayLevel {


    public static final String MAJOR = "MAJOR";//
    public static final String CRITICAL = "CRITICAL";//Waring
    public static final String BLOCKER = "BLOCKER";//Weak waring

    public static final int MAJOR_LEVEL = 397;
    public static final int CRITICAL_LEVEL = 398;
    public static final int BLOCKER_LEVEL = 399;

    private String name;
    private int level;

    public HighlightDisplayLevel() {
    }

    public HighlightDisplayLevel(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
