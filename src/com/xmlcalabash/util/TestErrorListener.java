package com.xmlcalabash.util;

import net.sf.saxon.StandardErrorListener;

import javax.xml.transform.TransformerException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Dec 9, 2009
 * Time: 7:13:02 AM
 *
 * This class only exists to test that changing the error listener works.
 */
public class TestErrorListener extends StandardErrorListener {
    public TestErrorListener() {
        super();
    }
    
    public void error(TransformerException exception) throws TransformerException {
        System.err.println("ERROR!");
        super.error(exception);
    }

    public void fatalError(TransformerException exception) throws TransformerException {
        System.err.println("FATAL!");
        super.fatalError(exception);
    }

    public void warning(TransformerException exception) throws TransformerException {
        System.err.println("WARNING!");
        super.warning(exception);
    }
}