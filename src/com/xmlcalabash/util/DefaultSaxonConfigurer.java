package com.xmlcalabash.util;

import com.xmlcalabash.config.SaxonConfigurer;
import net.sf.saxon.Configuration;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 9:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultSaxonConfigurer implements SaxonConfigurer {
    public void configSchematron(Configuration config) {
        // Do nothing
    }

    public void configXSD(Configuration config) {
        // Do nothing
    }

    public void configXQuery(Configuration config) {
        // Do nothing
    }

    public void configXSLT(Configuration config) {
        // Do nothing
    }
}
