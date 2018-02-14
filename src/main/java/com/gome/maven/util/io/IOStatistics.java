package com.gome.maven.util.io;


import com.gome.maven.openapi.diagnostic.Logger;

class IOStatistics {
    static final boolean DEBUG = System.getProperty("io.access.debug") != null;
    static final int MIN_IO_TIME_TO_REPORT = 100;
    static final Logger LOG = Logger.getInstance("#com.gome.maven.io.IOStatistics");
    static final int KEYS_FACTOR_MASK = 0xFFFF;

    static void dump(String msg) {
        LOG.info(msg);
    }
}
