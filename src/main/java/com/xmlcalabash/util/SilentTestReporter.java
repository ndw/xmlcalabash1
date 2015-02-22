package com.xmlcalabash.util;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by ndw on 8/27/14.
 */
public class SilentTestReporter implements TestReporter {
    Logger logger = LoggerFactory.getLogger(SilentTestReporter.class);
    boolean passed = false;

    @Override
    public void runningTest(URI testURI) {
        // nop;
    }

    @Override
    public void startReport(Hashtable<String, String> props) {
        // nop;
    }

    @Override
    public void endReport() {
        // nop;
    }

    @Override
    public void startTestSuite() {
        // nop;
    }

    @Override
    public void endTestSuite() {
        // nop;
    }

    @Override
    public void startTestResults(boolean pass, String testfile, String title) {
        passed = pass;
        logger.trace((pass ? "PASS: " : "FAIL: ") + title);
        logger.trace(testfile);
        // nop;
    }

    @Override
    public void testError(QName expectedError, QName actualError) {
        // nop;
    }

    @Override
    public void testErrorMessages(Vector<String> errorMessages) {
        // nop;
    }

    @Override
    public void testExpected(XdmNode expected) {
        if (!passed) {
            logger.trace("Expected:");
            if (expected != null) {
                logger.trace(expected.toString());
            } else {
                logger.trace("null");
            }
        }
    }

    @Override
    public void testActual(XdmNode actual) {
        if (!passed) {
            logger.trace("Actual:");
            if (actual != null) {
                logger.trace(actual.toString());
            } else {
                logger.trace("null");
            }
        }
    }

    @Override
    public void endTestResults(boolean pass) {
        // nop;
    }
}
