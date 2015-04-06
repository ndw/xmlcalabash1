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
public class TestXMLCalabash {
    protected static SuiteRunner suiteRunner = null;
    protected static String TESTROOT = "test/testsuite/xmlcalabash/";

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

    @Ignore
    public void testAdhocQuery001() {
        suiteRunner.runTest(TESTROOT + "adhoc-query-001.xml");
    }

    @Test
    public void testCollectionManager001() {
        suiteRunner.runTest(TESTROOT + "collection-manager-001.xml");
    }

    @Ignore
    public void testCompare001() {
        suiteRunner.runTest(TESTROOT + "compare-001.xml");
    }

    @Ignore
    public void testCompress001() {
        suiteRunner.runTest(TESTROOT + "compress-001.xml");
    }

    @Ignore
    public void testCopy001() {
        suiteRunner.runTest(TESTROOT + "copy-001.xml");
    }

    @Ignore
    public void testCssFormatter001() {
        suiteRunner.runTest(TESTROOT + "css-formatter-001.xml");
    }

    @Ignore
    public void testDelete001() {
        suiteRunner.runTest(TESTROOT + "delete-001.xml");
    }

    @Ignore
    public void testDeltaXml001() {
        suiteRunner.runTest(TESTROOT + "delta-xml-001.xml");
    }

    @Test
    public void testEval001() {
        suiteRunner.runTest(TESTROOT + "eval-001.xml");
    }

    @Test
    public void testExfCwd001() {
        suiteRunner.runTest(TESTROOT + "exf-cwd-001.xml");
    }

    @Test
    public void testGetCookies001() {
        suiteRunner.runTest(TESTROOT + "get-cookies-001.xml");
    }

    @Ignore
    public void testHead001() {
        suiteRunner.runTest(TESTROOT + "head-001.xml");
    }

    @Ignore
    public void testInfo001() {
        suiteRunner.runTest(TESTROOT + "info-001.xml");
    }

    @Ignore
    public void testInsertDocument001() {
        suiteRunner.runTest(TESTROOT + "insert-document-001.xml");
    }

    @Ignore
    public void testInvokeModule001() {
        suiteRunner.runTest(TESTROOT + "invoke-module-001.xml");
    }

    @Test
    public void testJavaProperties001() {
        suiteRunner.runTest(TESTROOT + "java-properties-001.xml");
    }

    @Test
    public void testMessage001() {
        suiteRunner.runTest(TESTROOT + "message-001.xml");
    }

    @Ignore
    public void testMetadataExtractor001() {
        suiteRunner.runTest(TESTROOT + "metadata-extractor-001.xml");
    }

    @Test
    public void testNamespaceDelete001() {
        suiteRunner.runTest(TESTROOT + "namespace-delete-001.xml");
    }

    @Test
    public void testPrettyPrint001() {
        suiteRunner.runTest(TESTROOT + "pretty-print-001.xml");
    }

    @Ignore
    public void testPxpSetBaseUri001() {
        suiteRunner.runTest(TESTROOT + "pxp-set-base-uri-001.xml");
    }

    @Ignore
    public void testRdfa001() {
        suiteRunner.runTest(TESTROOT + "rdfa-001.xml");
    }

    @Ignore
    public void testRdfLoad001() {
        suiteRunner.runTest(TESTROOT + "rdf-load-001.xml");
    }

    @Test
    public void testSetCookies001() {
        suiteRunner.runTest(TESTROOT + "set-cookies-001.xml");
    }

    @Ignore
    public void testSparql001() {
        suiteRunner.runTest(TESTROOT + "sparql-001.xml");
    }

    @Test
    public void testUnzip001() {
        suiteRunner.runTest(TESTROOT + "unzip-001.xml");
    }

    @Test
    public void testUnzip002() {
        suiteRunner.runTest(TESTROOT + "unzip-002.xml");
    }

    @Test
    public void testUriInfo001() {
        suiteRunner.runTest(TESTROOT + "uri-info-001.xml");
    }

    @Test
    public void testXInclude001() {
        suiteRunner.runTest(TESTROOT + "xinclude-001.xml");
    }
}
