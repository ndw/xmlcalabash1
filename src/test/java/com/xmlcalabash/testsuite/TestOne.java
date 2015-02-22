package com.xmlcalabash.testsuite;

import com.xmlcalabash.testsuite.SuiteRunner;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by ndw on 8/19/14.
 */
public class TestOne {
    protected static SuiteRunner suiteRunner = null;
    protected static String REQROOT = "test/testsuite/required/";
    protected static String OPTROOT = "test/testsuite/optional/";

    @BeforeClass
    public static void setupClass() {
        suiteRunner = new SuiteRunner();
    }

    @Test
    public void testErrC0064001() {
        suiteRunner.runTest(OPTROOT + "err-c0064-001.xml");
    }

}
