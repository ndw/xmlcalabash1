package com.xmlcalabash.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Utility methods for closing objects that implement the Closeable interface.
 */
public final class Closer {

    // prevent utility class from being created
    private Closer() {
    }

    
    /** 
     * Closes the given Closeable, protecting against nulls.
     */
    public static void close(Closeable c) throws IOException {
        if (c != null) {
            c.close();
        }
    }
    
}
