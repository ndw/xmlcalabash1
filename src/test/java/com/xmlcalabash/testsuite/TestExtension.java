package com.xmlcalabash.testsuite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by ndw on 8/19/14.
 */

/**
 * Created by ndw on 8/19/14.
 */
public class TestExtension {
    protected static SuiteRunner suiteRunner = null;
    protected static String TESTROOT = "test/testsuite/extension/";

    @BeforeClass
    public static void setupClass() {
        suiteRunner = new SuiteRunner();
    }

    @AfterClass
    public static void teardownClass() {
        suiteRunner.close();
    }

    @Ignore
    public void testAll() {
        suiteRunner.runTest(TESTROOT + "test-suite.xml");
    }

    @Test
    public void testUnimpl001() {
        suiteRunner.runTest(TESTROOT + "unimpl-001.xml");
    }
}
