package com.xmlcalabash.util;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import java.net.URI;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by ndw on 8/27/14.
 */
public interface TestReporter {

    public void runningTest(URI testURI);
    public void startReport(Hashtable<String,String> props);
    public void endReport();
    public void startTestSuite();
    public void endTestSuite();
    public void startTestResults(boolean pass, String testfile, String title);
    public void testError(QName expectedError, QName actualError);
    public void testErrorMessages(Vector<String> errorMessages);
    public void testExpected(XdmNode expected);
    public void testActual(XdmNode actual);
    public void endTestResults(boolean pass);
}
