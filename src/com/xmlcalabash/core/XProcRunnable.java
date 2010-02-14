package com.xmlcalabash.core;

import net.sf.saxon.s9api.SaxonApiException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Dec 19, 2009
 * Time: 2:56:33 PM
 * To change this template use File | Settings | File Templates.
 */
public interface XProcRunnable {
    public void reset();
    public void run() throws SaxonApiException;
}
