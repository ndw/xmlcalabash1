package com.xmlcalabash.config;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;
import java.io.OutputStream;
import java.util.Properties;

import net.sf.saxon.s9api.XdmNode;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 6:39 AM
 * To change this template use File | Settings | File Templates.
 */
public interface FoProcessor {
    public void initialize(XProcRuntime runtime, XStep step, Properties options);
    public void format(XdmNode doc, OutputStream out, String contentType);
}
