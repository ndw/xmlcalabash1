package com.xmlcalabash.testsuite;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by ndw on 8/19/14.
 */

/**
 * Created by ndw on 8/19/14.
 */
public class TestSerialization {
    protected static SuiteRunner suiteRunner = null;
    protected static String TESTROOT = "http://tests.xproc.org/tests/serialization/";

    @BeforeClass
    public static void setupClass() {
        suiteRunner = new SuiteRunner();
    }

    @Ignore
    public void testAll() {
        suiteRunner.runTest(TESTROOT + "test-suite.xml");
    }

    @Test
    public void testByteOrderMark001() {
        suiteRunner.runTest(TESTROOT + "byte-order-mark-001.xml");
    }

    @Test
    public void testByteOrderMark002() {
        suiteRunner.runTest(TESTROOT + "byte-order-mark-002.xml");
    }

    @Test
    public void testCdataSectionElements001() {
        suiteRunner.runTest(TESTROOT + "cdata-section-elements-001.xml");
    }

    @Test
    public void testCdataSectionElements002() {
        suiteRunner.runTest(TESTROOT + "cdata-section-elements-002.xml");
    }

    @Test
    public void testDoctypePublic001() {
        suiteRunner.runTest(TESTROOT + "doctype-public-001.xml");
    }

    @Test
    public void testDoctypeSystem001() {
        suiteRunner.runTest(TESTROOT + "doctype-system-001.xml");
    }

    @Test
    public void testEncoding001() {
        suiteRunner.runTest(TESTROOT + "encoding-001.xml");
    }

    @Test
    public void testErrD0020001() {
        suiteRunner.runTest(TESTROOT + "err-d0020-001.xml");
    }

    @Test
    public void testEscapeUri001() {
        suiteRunner.runTest(TESTROOT + "escape-uri-001.xml");
    }

    @Test
    public void testEscapeUri002() {
        suiteRunner.runTest(TESTROOT + "escape-uri-002.xml");
    }

    @Test
    public void testIncludeContentType001() {
        suiteRunner.runTest(TESTROOT + "include-content-type-001.xml");
    }

    @Test
    public void testIncludeContentType002() {
        suiteRunner.runTest(TESTROOT + "include-content-type-002.xml");
    }

    @Test
    public void testIndent001() {
        suiteRunner.runTest(TESTROOT + "indent-001.xml");
    }

    @Test
    public void testIndent002() {
        suiteRunner.runTest(TESTROOT + "indent-002.xml");
    }

    @Test
    public void testMediaType001() {
        suiteRunner.runTest(TESTROOT + "media-type-001.xml");
    }

    @Test
    public void testNormalizationForm001() {
        suiteRunner.runTest(TESTROOT + "normalization-form-001.xml");
    }

    @Test
    public void testNormalizationForm002() {
        suiteRunner.runTest(TESTROOT + "normalization-form-002.xml");
    }

    @Test
    public void testNormalizationForm003() {
        suiteRunner.runTest(TESTROOT + "normalization-form-003.xml");
    }

    @Test
    public void testOmitXmlDeclaration001() {
        suiteRunner.runTest(TESTROOT + "omit-xml-declaration-001.xml");
    }

    @Test
    public void testOmitXmlDeclaration002() {
        suiteRunner.runTest(TESTROOT + "omit-xml-declaration-002.xml");
    }

    @Test
    public void testStandalone001() {
        suiteRunner.runTest(TESTROOT + "standalone-001.xml");
    }

    @Test
    public void testStandalone002() {
        suiteRunner.runTest(TESTROOT + "standalone-002.xml");
    }

    @Test
    public void testStandalone003() {
        suiteRunner.runTest(TESTROOT + "standalone-003.xml");
    }

    @Test
    public void testUndeclarePrefixes001() {
        suiteRunner.runTest(TESTROOT + "undeclare-prefixes-001.xml");
    }

    @Test
    public void testVersion001() {
        suiteRunner.runTest(TESTROOT + "version-001.xml");
    }
}
