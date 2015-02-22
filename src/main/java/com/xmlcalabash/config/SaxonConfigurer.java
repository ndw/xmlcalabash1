package com.xmlcalabash.config;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 8:43 AM
 * To change this template use File | Settings | File Templates.
 */
public interface SaxonConfigurer {
    public void configSchematron(Configuration config);
    public void configXSD(Configuration config);
    public void configXQuery(Configuration config);
    public void configXSLT(Configuration config);
}
