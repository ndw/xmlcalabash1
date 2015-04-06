package com.xmlcalabash.testsuite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by ndw on 8/19/14.
 */
public class TestRequired {
    protected static SuiteRunner suiteRunner = null;
    protected static String TESTROOT = "test/testsuite/required/";

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
    public void testAddAttribute001() {
        suiteRunner.runTest(TESTROOT + "add-attribute-001.xml");
    }

    @Test
    public void testAddAttribute002() {
        suiteRunner.runTest(TESTROOT + "add-attribute-002.xml");
    }

    @Test
    public void testAddAttribute003() {
        suiteRunner.runTest(TESTROOT + "add-attribute-003.xml");
    }

    @Test
    public void testAddAttribute004() {
        suiteRunner.runTest(TESTROOT + "add-attribute-004.xml");
    }

    @Test
    public void testAddAttribute005() {
        suiteRunner.runTest(TESTROOT + "add-attribute-005.xml");
    }

    @Test
    public void testAddAttribute006() {
        suiteRunner.runTest(TESTROOT + "add-attribute-006.xml");
    }

    @Test
    public void testAddAttribute007() {
        suiteRunner.runTest(TESTROOT + "add-attribute-007.xml");
    }

    @Ignore
    public void testAddXmlBase001() {
        suiteRunner.runTest(TESTROOT + "add-xml-base-001.xml");
    }

    @Ignore
    public void testAddXmlBase002() {
        suiteRunner.runTest(TESTROOT + "add-xml-base-002.xml");
    }

    @Ignore
    public void testAddXmlBase003() {
        suiteRunner.runTest(TESTROOT + "add-xml-base-003.xml");
    }

    @Test
    public void testAddXmlBase004() {
        suiteRunner.runTest(TESTROOT + "add-xml-base-004.xml");
    }

    @Ignore
    public void testAddXmlBase005() {
        suiteRunner.runTest(TESTROOT + "add-xml-base-005.xml");
    }

    @Ignore
    public void testAddXmlBase006() {
        suiteRunner.runTest(TESTROOT + "add-xml-base-006.xml");
    }

    @Test
    public void testBaseUri001() {
        suiteRunner.runTest(TESTROOT + "base-uri-001.xml");
    }

    @Test
    public void testBaseUri002() {
        suiteRunner.runTest(TESTROOT + "base-uri-002.xml");
    }

    @Test
    public void testBaseUri003() {
        suiteRunner.runTest(TESTROOT + "base-uri-003.xml");
    }

    @Test
    public void testChoose001() {
        suiteRunner.runTest(TESTROOT + "choose-001.xml");
    }

    @Test
    public void testChoose002() {
        suiteRunner.runTest(TESTROOT + "choose-002.xml");
    }

    @Test
    public void testChoose003() {
        suiteRunner.runTest(TESTROOT + "choose-003.xml");
    }

    @Test
    public void testChoose004() {
        suiteRunner.runTest(TESTROOT + "choose-004.xml");
    }

    @Test
    public void testChoose005() {
        suiteRunner.runTest(TESTROOT + "choose-005.xml");
    }

    @Test
    public void testChoose006() {
        suiteRunner.runTest(TESTROOT + "choose-006.xml");
    }

    @Test
    public void testChoose007() {
        suiteRunner.runTest(TESTROOT + "choose-007.xml");
    }

    @Test
    public void testChoose008() {
        suiteRunner.runTest(TESTROOT + "choose-008.xml");
    }

    @Test
    public void testCompare001() {
        suiteRunner.runTest(TESTROOT + "compare-001.xml");
    }

    @Test
    public void testCompare002() {
        suiteRunner.runTest(TESTROOT + "compare-002.xml");
    }

    @Test
    public void testCompare003() {
        suiteRunner.runTest(TESTROOT + "compare-003.xml");
    }

    @Test
    public void testCompare004() {
        suiteRunner.runTest(TESTROOT + "compare-004.xml");
    }

    @Test
    public void testCompare005() {
        suiteRunner.runTest(TESTROOT + "compare-005.xml");
    }

    @Test
    public void testCount001() {
        suiteRunner.runTest(TESTROOT + "count-001.xml");
    }

    @Test
    public void testCount002() {
        suiteRunner.runTest(TESTROOT + "count-002.xml");
    }

    @Test
    public void testCount003() {
        suiteRunner.runTest(TESTROOT + "count-003.xml");
    }

    @Test
    public void testCount004() {
        suiteRunner.runTest(TESTROOT + "count-004.xml");
    }

    @Ignore
    public void testData001() {
        suiteRunner.runTest(TESTROOT + "data-001.xml");
    }

    @Ignore
    public void testData002() {
        suiteRunner.runTest(TESTROOT + "data-002.xml");
    }

    @Test
    public void testData003() {
        suiteRunner.runTest(TESTROOT + "data-003.xml");
    }

    @Test
    public void testData004() {
        suiteRunner.runTest(TESTROOT + "data-004.xml");
    }

    @Test
    public void testData005() {
        suiteRunner.runTest(TESTROOT + "data-005.xml");
    }

    @Ignore
    public void testData006() {
        suiteRunner.runTest(TESTROOT + "data-006.xml");
    }

    @Test
    public void testData007() {
        suiteRunner.runTest(TESTROOT + "data-007.xml");
    }

    @Test
    public void testData008() {
        suiteRunner.runTest(TESTROOT + "data-008.xml");
    }

    @Test
    public void testDeclareStep001() {
        suiteRunner.runTest(TESTROOT + "declare-step-001.xml");
    }

    @Test
    public void testDeclareStep002() {
        suiteRunner.runTest(TESTROOT + "declare-step-002.xml");
    }

    @Test
    public void testDeclareStep003() {
        suiteRunner.runTest(TESTROOT + "declare-step-003.xml");
    }

    @Test
    public void testDeclareStep004() {
        suiteRunner.runTest(TESTROOT + "declare-step-004.xml");
    }

    @Test
    public void testDeclareStep005() {
        suiteRunner.runTest(TESTROOT + "declare-step-005.xml");
    }

    @Test
    public void testDeclareStep006() {
        suiteRunner.runTest(TESTROOT + "declare-step-006.xml");
    }

    @Test
    public void testDeclareStep007() {
        suiteRunner.runTest(TESTROOT + "declare-step-007.xml");
    }

    @Test
    public void testDeclareStep008() {
        suiteRunner.runTest(TESTROOT + "declare-step-008.xml");
    }

    @Test
    public void testDeclareStep009() {
        suiteRunner.runTest(TESTROOT + "declare-step-009.xml");
    }

    @Test
    public void testDeclareStep010() {
        suiteRunner.runTest(TESTROOT + "declare-step-010.xml");
    }

    @Test
    public void testDeclareStep011() {
        suiteRunner.runTest(TESTROOT + "declare-step-011.xml");
    }

    @Test
    public void testDelete001() {
        suiteRunner.runTest(TESTROOT + "delete-001.xml");
    }

    @Test
    public void testDelete002() {
        suiteRunner.runTest(TESTROOT + "delete-002.xml");
    }

    @Test
    public void testDelete003() {
        suiteRunner.runTest(TESTROOT + "delete-003.xml");
    }

    @Test
    public void testDelete004() {
        suiteRunner.runTest(TESTROOT + "delete-004.xml");
    }

    @Test
    public void testDelete005() {
        suiteRunner.runTest(TESTROOT + "delete-005.xml");
    }

    @Ignore
    public void testDirectoryList001() {
        suiteRunner.runTest(TESTROOT + "directory-list-001.xml");
    }

    @Ignore
    public void testDirectoryList002() {
        suiteRunner.runTest(TESTROOT + "directory-list-002.xml");
    }

    @Test
    public void testDocument001() {
        suiteRunner.runTest(TESTROOT + "document-001.xml");
    }

    @Test
    public void testEbv001() {
        suiteRunner.runTest(TESTROOT + "ebv-001.xml");
    }

    @Test
    public void testEbv002() {
        suiteRunner.runTest(TESTROOT + "ebv-002.xml");
    }

    @Test
    public void testErrC0002001() {
        suiteRunner.runTest(TESTROOT + "err-c0002-001.xml");
    }

    @Test
    public void testErrC0003001() {
        suiteRunner.runTest(TESTROOT + "err-c0003-001.xml");
    }

    @Test
    public void testErrC0003002() {
        suiteRunner.runTest(TESTROOT + "err-c0003-002.xml");
    }

    @Test
    public void testErrC0004001() {
        suiteRunner.runTest(TESTROOT + "err-c0004-001.xml");
    }

    @Test
    public void testErrC0005001() {
        suiteRunner.runTest(TESTROOT + "err-c0005-001.xml");
    }

    @Test
    public void testErrC0005002() {
        suiteRunner.runTest(TESTROOT + "err-c0005-002.xml");
    }

    @Test
    public void testErrC0006001() {
        suiteRunner.runTest(TESTROOT + "err-c0006-001.xml");
    }

    @Test
    public void testErrC0010001() {
        suiteRunner.runTest(TESTROOT + "err-c0010-001.xml");
    }

    @Test
    public void testErrC0010002() {
        suiteRunner.runTest(TESTROOT + "err-c0010-002.xml");
    }

    @Test
    public void testErrC0012001() {
        suiteRunner.runTest(TESTROOT + "err-c0012-001.xml");
    }

    @Test
    public void testErrC0013001() {
        suiteRunner.runTest(TESTROOT + "err-c0013-001.xml");
    }

    @Test
    public void testErrC0014001() {
        suiteRunner.runTest(TESTROOT + "err-c0014-001.xml");
    }

    @Test
    public void testErrC0014002() {
        suiteRunner.runTest(TESTROOT + "err-c0014-002.xml");
    }

    @Test
    public void testErrC0017001() {
        suiteRunner.runTest(TESTROOT + "err-c0017-001.xml");
    }

    @Test
    public void testErrC0019001() {
        suiteRunner.runTest(TESTROOT + "err-c0019-001.xml");
    }

    @Test
    public void testErrC0020001() {
        suiteRunner.runTest(TESTROOT + "err-c0020-001.xml");
    }

    @Test
    public void testErrC0020003() {
        suiteRunner.runTest(TESTROOT + "err-c0020-003.xml");
    }

    @Test
    public void testErrC0020004() {
        suiteRunner.runTest(TESTROOT + "err-c0020-004.xml");
    }

    @Test
    public void testErrC0020005() {
        suiteRunner.runTest(TESTROOT + "err-c0020-005.xml");
    }

    @Test
    public void testErrC0020006() {
        suiteRunner.runTest(TESTROOT + "err-c0020-006.xml");
    }

    @Test
    public void testErrC0020007() {
        suiteRunner.runTest(TESTROOT + "err-c0020-007.xml");
    }

    @Test
    public void testErrC0022001() {
        suiteRunner.runTest(TESTROOT + "err-c0022-001.xml");
    }

    @Test
    public void testErrC0023001() {
        suiteRunner.runTest(TESTROOT + "err-c0023-001.xml");
    }

    @Test
    public void testErrC0023002() {
        suiteRunner.runTest(TESTROOT + "err-c0023-002.xml");
    }

    @Test
    public void testErrC0023003() {
        suiteRunner.runTest(TESTROOT + "err-c0023-003.xml");
    }

    @Test
    public void testErrC0023004() {
        suiteRunner.runTest(TESTROOT + "err-c0023-004.xml");
    }

    @Test
    public void testErrC0023005() {
        suiteRunner.runTest(TESTROOT + "err-c0023-005.xml");
    }

    @Test
    public void testErrC0023006() {
        suiteRunner.runTest(TESTROOT + "err-c0023-006.xml");
    }

    @Test
    public void testErrC0023007() {
        suiteRunner.runTest(TESTROOT + "err-c0023-007.xml");
    }

    @Test
    public void testErrC0023008() {
        suiteRunner.runTest(TESTROOT + "err-c0023-008.xml");
    }

    @Test
    public void testErrC0023009() {
        suiteRunner.runTest(TESTROOT + "err-c0023-009.xml");
    }

    @Test
    public void testErrC0025001() {
        suiteRunner.runTest(TESTROOT + "err-c0025-001.xml");
    }

    @Test
    public void testErrC0025002() {
        suiteRunner.runTest(TESTROOT + "err-c0025-002.xml");
    }

    @Test
    public void testErrC0027001() {
        suiteRunner.runTest(TESTROOT + "err-c0027-001.xml");
    }

    @Test
    public void testErrC0027002() {
        suiteRunner.runTest(TESTROOT + "err-c0027-002.xml");
    }

    @Test
    public void testErrC0027003() {
        suiteRunner.runTest(TESTROOT + "err-c0027-003.xml");
    }

    @Test
    public void testErrC0028001() {
        suiteRunner.runTest(TESTROOT + "err-c0028-001.xml");
    }

    @Test
    public void testErrC0029001() {
        suiteRunner.runTest(TESTROOT + "err-c0029-001.xml");
    }

    @Test
    public void testErrC0029002() {
        suiteRunner.runTest(TESTROOT + "err-c0029-002.xml");
    }

    @Test
    public void testErrC0030001() {
        suiteRunner.runTest(TESTROOT + "err-c0030-001.xml");
    }

    @Test
    public void testErrC0039001() {
        suiteRunner.runTest(TESTROOT + "err-c0039-001.xml");
    }

    @Test
    public void testErrC0040001() {
        suiteRunner.runTest(TESTROOT + "err-c0040-001.xml");
    }

    @Test
    public void testErrC0050001() {
        suiteRunner.runTest(TESTROOT + "err-c0050-001.xml");
    }

    @Test
    public void testErrC0051001() {
        suiteRunner.runTest(TESTROOT + "err-c0051-001.xml");
    }

    @Test
    public void testErrC0052001() {
        suiteRunner.runTest(TESTROOT + "err-c0052-001.xml");
    }

    @Test
    public void testErrC0052002() {
        suiteRunner.runTest(TESTROOT + "err-c0052-002.xml");
    }

    @Test
    public void testErrC0056001() {
        suiteRunner.runTest(TESTROOT + "err-c0056-001.xml");
    }

    @Test
    public void testErrC0056002() {
        suiteRunner.runTest(TESTROOT + "err-c0056-002.xml");
    }

    @Test
    public void testErrC0058001() {
        suiteRunner.runTest(TESTROOT + "err-c0058-001.xml");
    }

    @Test
    public void testErrC0059001() {
        suiteRunner.runTest(TESTROOT + "err-c0059-001.xml");
    }

    @Test
    public void testErrC0059002() {
        suiteRunner.runTest(TESTROOT + "err-c0059-002.xml");
    }

    @Test
    public void testErrC0062001() {
        suiteRunner.runTest(TESTROOT + "err-c0062-001.xml");
    }

    @Test
    public void testErrD0001001() {
        suiteRunner.runTest(TESTROOT + "err-d0001-001.xml");
    }

    @Test
    public void testErrD0001002() {
        suiteRunner.runTest(TESTROOT + "err-d0001-002.xml");
    }

    @Test
    public void testErrD0003001() {
        suiteRunner.runTest(TESTROOT + "err-d0003-001.xml");
    }

    @Test
    public void testErrD0004001() {
        suiteRunner.runTest(TESTROOT + "err-d0004-001.xml");
    }

    @Test
    public void testErrD0005001() {
        suiteRunner.runTest(TESTROOT + "err-d0005-001.xml");
    }

    @Test
    public void testErrD0006001() {
        suiteRunner.runTest(TESTROOT + "err-d0006-001.xml");
    }

    @Test
    public void testErrD0007001() {
        suiteRunner.runTest(TESTROOT + "err-d0007-001.xml");
    }

    @Test
    public void testErrD0007002() {
        suiteRunner.runTest(TESTROOT + "err-d0007-002.xml");
    }

    @Test
    public void testErrD0007003() {
        suiteRunner.runTest(TESTROOT + "err-d0007-003.xml");
    }

    @Test
    public void testErrD0007004() {
        suiteRunner.runTest(TESTROOT + "err-d0007-004.xml");
    }

    @Test
    public void testErrD0008001() {
        suiteRunner.runTest(TESTROOT + "err-d0008-001.xml");
    }

    @Test
    public void testErrD0009001() {
        suiteRunner.runTest(TESTROOT + "err-d0009-001.xml");
    }

    @Test
    public void testErrD0009002() {
        suiteRunner.runTest(TESTROOT + "err-d0009-002.xml");
    }

    @Test
    public void testErrD0010001() {
        suiteRunner.runTest(TESTROOT + "err-d0010-001.xml");
    }

    @Test
    public void testErrD0011001() {
        suiteRunner.runTest(TESTROOT + "err-d0011-001.xml");
    }

    @Test
    public void testErrD0011002() {
        suiteRunner.runTest(TESTROOT + "err-d0011-002.xml");
    }

    @Test
    public void testErrD0011003() {
        suiteRunner.runTest(TESTROOT + "err-d0011-003.xml");
    }

    @Test
    public void testErrD0012001() {
        suiteRunner.runTest(TESTROOT + "err-d0012-001.xml");
    }

    @Test
    public void testErrD0012002() {
        suiteRunner.runTest(TESTROOT + "err-d0012-002.xml");
    }

    @Test
    public void testErrD0012003() {
        suiteRunner.runTest(TESTROOT + "err-d0012-003.xml");
    }

    @Test
    public void testErrD0013001() {
        suiteRunner.runTest(TESTROOT + "err-d0013-001.xml");
    }

    @Test
    public void testErrD0013002() {
        suiteRunner.runTest(TESTROOT + "err-d0013-002.xml");
    }

    @Test
    public void testErrD0014001() {
        suiteRunner.runTest(TESTROOT + "err-d0014-001.xml");
    }

    @Test
    public void testErrD0014002() {
        suiteRunner.runTest(TESTROOT + "err-d0014-002.xml");
    }

    @Test
    public void testErrD0015001() {
        suiteRunner.runTest(TESTROOT + "err-d0015-001.xml");
    }

    @Test
    public void testErrD0016001() {
        suiteRunner.runTest(TESTROOT + "err-d0016-001.xml");
    }

    @Test
    public void testErrD0016002() {
        suiteRunner.runTest(TESTROOT + "err-d0016-002.xml");
    }

    @Test
    public void testErrD0018001() {
        suiteRunner.runTest(TESTROOT + "err-d0018-001.xml");
    }

    @Test
    public void testErrD0019001() {
        suiteRunner.runTest(TESTROOT + "err-d0019-001.xml");
    }

    @Test
    public void testErrD0019002() {
        suiteRunner.runTest(TESTROOT + "err-d0019-002.xml");
    }

    @Test
    public void testErrD0020001() {
        suiteRunner.runTest(TESTROOT + "err-d0020-001.xml");
    }

    @Test
    public void testErrD0020002() {
        suiteRunner.runTest(TESTROOT + "err-d0020-002.xml");
    }

    @Test
    public void testErrD0021001() {
        suiteRunner.runTest(TESTROOT + "err-d0021-001.xml");
    }

    @Test
    public void testErrD0021002() {
        suiteRunner.runTest(TESTROOT + "err-d0021-002.xml");
    }

    @Test
    public void testErrD0022001() {
        suiteRunner.runTest(TESTROOT + "err-d0022-001.xml");
    }

    @Test
    public void testErrD0023001() {
        suiteRunner.runTest(TESTROOT + "err-d0023-001.xml");
    }

    @Test
    public void testErrD0023002() {
        suiteRunner.runTest(TESTROOT + "err-d0023-002.xml");
    }

    @Test
    public void testErrD0023003() {
        suiteRunner.runTest(TESTROOT + "err-d0023-003.xml");
    }

    @Test
    public void testErrD0023004() {
        suiteRunner.runTest(TESTROOT + "err-d0023-004.xml");
    }

    @Test
    public void testErrD0023005() {
        suiteRunner.runTest(TESTROOT + "err-d0023-005.xml");
    }

    @Test
    public void testErrD0023006() {
        suiteRunner.runTest(TESTROOT + "err-d0023-006.xml");
    }

    @Test
    public void testErrD0023007() {
        suiteRunner.runTest(TESTROOT + "err-d0023-007.xml");
    }

    @Test
    public void testErrD0023008() {
        suiteRunner.runTest(TESTROOT + "err-d0023-008.xml");
    }

    @Test
    public void testErrD0023009() {
        suiteRunner.runTest(TESTROOT + "err-d0023-009.xml");
    }

    @Test
    public void testErrD0025001() {
        suiteRunner.runTest(TESTROOT + "err-d0025-001.xml");
    }

    @Test
    public void testErrD0026001() {
        suiteRunner.runTest(TESTROOT + "err-d0026-001.xml");
    }

    @Test
    public void testErrD0026002() {
        suiteRunner.runTest(TESTROOT + "err-d0026-002.xml");
    }

    @Test
    public void testErrD0026003() {
        suiteRunner.runTest(TESTROOT + "err-d0026-003.xml");
    }

    @Test
    public void testErrD0026004() {
        suiteRunner.runTest(TESTROOT + "err-d0026-004.xml");
    }

    @Test
    public void testErrD0026005() {
        suiteRunner.runTest(TESTROOT + "err-d0026-005.xml");
    }

    @Test
    public void testErrD0027001() {
        suiteRunner.runTest(TESTROOT + "err-d0027-001.xml");
    }

    @Test
    public void testErrD0028001() {
        suiteRunner.runTest(TESTROOT + "err-d0028-001.xml");
    }

    @Test
    public void testErrD0028002() {
        suiteRunner.runTest(TESTROOT + "err-d0028-002.xml");
    }

    @Test
    public void testErrD0028003() {
        suiteRunner.runTest(TESTROOT + "err-d0028-003.xml");
    }

    @Test
    public void testErrD0028004() {
        suiteRunner.runTest(TESTROOT + "err-d0028-004.xml");
    }

    @Test
    public void testErrD0029001() {
        suiteRunner.runTest(TESTROOT + "err-d0029-001.xml");
    }

    @Test
    public void testErrD0029002() {
        suiteRunner.runTest(TESTROOT + "err-d0029-002.xml");
    }

    @Test
    public void testErrD0030001() {
        suiteRunner.runTest(TESTROOT + "err-d0030-001.xml");
    }

    @Test
    public void testErrD0030002() {
        suiteRunner.runTest(TESTROOT + "err-d0030-002.xml");
    }

    @Test
    public void testErrD0031001() {
        suiteRunner.runTest(TESTROOT + "err-d0031-001.xml");
    }

    @Test
    public void testErrD0031002() {
        suiteRunner.runTest(TESTROOT + "err-d0031-002.xml");
    }

    @Test
    public void testErrD0033001() {
        suiteRunner.runTest(TESTROOT + "err-d0033-001.xml");
    }

    @Test
    public void testErrD0033002() {
        suiteRunner.runTest(TESTROOT + "err-d0033-002.xml");
    }

    @Test
    public void testErrD0034001() {
        suiteRunner.runTest(TESTROOT + "err-d0034-001.xml");
    }

    @Test
    public void testErrD0034002() {
        suiteRunner.runTest(TESTROOT + "err-d0034-002.xml");
    }

    @Test
    public void testErrD0034003() {
        suiteRunner.runTest(TESTROOT + "err-d0034-003.xml");
    }

    @Test
    public void testErrD0034004() {
        suiteRunner.runTest(TESTROOT + "err-d0034-004.xml");
    }

    @Test
    public void testErrD0034005() {
        suiteRunner.runTest(TESTROOT + "err-d0034-005.xml");
    }

    @Test
    public void testErrD0034006() {
        suiteRunner.runTest(TESTROOT + "err-d0034-006.xml");
    }

    @Test
    public void testErrD0034007() {
        suiteRunner.runTest(TESTROOT + "err-d0034-007.xml");
    }

    @Test
    public void testErrD0034008() {
        suiteRunner.runTest(TESTROOT + "err-d0034-008.xml");
    }

    @Test
    public void testErrD0034009() {
        suiteRunner.runTest(TESTROOT + "err-d0034-009.xml");
    }

    @Test
    public void testErrD0034010() {
        suiteRunner.runTest(TESTROOT + "err-d0034-010.xml");
    }

    @Test
    public void testErrD0034011() {
        suiteRunner.runTest(TESTROOT + "err-d0034-011.xml");
    }

    @Test
    public void testErrD0034012() {
        suiteRunner.runTest(TESTROOT + "err-d0034-012.xml");
    }

    @Test
    public void testErrD0034013() {
        suiteRunner.runTest(TESTROOT + "err-d0034-013.xml");
    }

    @Test
    public void testErrD0034014() {
        suiteRunner.runTest(TESTROOT + "err-d0034-014.xml");
    }

    @Test
    public void testErrD0034015() {
        suiteRunner.runTest(TESTROOT + "err-d0034-015.xml");
    }

    @Test
    public void testErrD0034016() {
        suiteRunner.runTest(TESTROOT + "err-d0034-016.xml");
    }

    @Test
    public void testErrD0034017() {
        suiteRunner.runTest(TESTROOT + "err-d0034-017.xml");
    }

    @Test
    public void testErrD0034018() {
        suiteRunner.runTest(TESTROOT + "err-d0034-018.xml");
    }

    @Test
    public void testError001() {
        suiteRunner.runTest(TESTROOT + "error-001.xml");
    }

    @Test
    public void testError002() {
        suiteRunner.runTest(TESTROOT + "error-002.xml");
    }

    @Test
    public void testError003() {
        suiteRunner.runTest(TESTROOT + "error-003.xml");
    }

    @Test
    public void testErrPrimary001() {
        suiteRunner.runTest(TESTROOT + "err-primary-001.xml");
    }

    @Test
    public void testErrS0001001() {
        suiteRunner.runTest(TESTROOT + "err-s0001-001.xml");
    }

    @Test
    public void testErrS0001002() {
        suiteRunner.runTest(TESTROOT + "err-s0001-002.xml");
    }

    @Test
    public void testErrS0001003() {
        suiteRunner.runTest(TESTROOT + "err-s0001-003.xml");
    }

    @Test
    public void testErrS0001004() {
        suiteRunner.runTest(TESTROOT + "err-s0001-004.xml");
    }

    @Test
    public void testErrS0001005() {
        suiteRunner.runTest(TESTROOT + "err-s0001-005.xml");
    }

    @Test
    public void testErrS0001006() {
        suiteRunner.runTest(TESTROOT + "err-s0001-006.xml");
    }

    @Test
    public void testErrS0001007() {
        suiteRunner.runTest(TESTROOT + "err-s0001-007.xml");
    }

    @Test
    public void testErrS0001008() {
        suiteRunner.runTest(TESTROOT + "err-s0001-008.xml");
    }

    @Test
    public void testErrS0001010() {
        suiteRunner.runTest(TESTROOT + "err-s0001-010.xml");
    }

    @Test
    public void testErrS0001011() {
        suiteRunner.runTest(TESTROOT + "err-s0001-011.xml");
    }

    @Test
    public void testErrS0002001() {
        suiteRunner.runTest(TESTROOT + "err-s0002-001.xml");
    }

    @Test
    public void testErrS0002002() {
        suiteRunner.runTest(TESTROOT + "err-s0002-002.xml");
    }

    @Test
    public void testErrS0003001() {
        suiteRunner.runTest(TESTROOT + "err-s0003-001.xml");
    }

    @Test
    public void testErrS0003002() {
        suiteRunner.runTest(TESTROOT + "err-s0003-002.xml");
    }

    @Test
    public void testErrS0003004() {
        suiteRunner.runTest(TESTROOT + "err-s0003-004.xml");
    }

    @Test
    public void testErrS0004001() {
        suiteRunner.runTest(TESTROOT + "err-s0004-001.xml");
    }

    @Test
    public void testErrS0004002() {
        suiteRunner.runTest(TESTROOT + "err-s0004-002.xml");
    }

    @Test
    public void testErrS0004003() {
        suiteRunner.runTest(TESTROOT + "err-s0004-003.xml");
    }

    @Test
    public void testErrS0004004() {
        suiteRunner.runTest(TESTROOT + "err-s0004-004.xml");
    }

    @Test
    public void testErrS0005001() {
        suiteRunner.runTest(TESTROOT + "err-s0005-001.xml");
    }

    @Test
    public void testErrS0005002() {
        suiteRunner.runTest(TESTROOT + "err-s0005-002.xml");
    }

    @Test
    public void testErrS0005003() {
        suiteRunner.runTest(TESTROOT + "err-s0005-003.xml");
    }

    @Test
    public void testErrS0005004() {
        suiteRunner.runTest(TESTROOT + "err-s0005-004.xml");
    }

    @Test
    public void testErrS0005005() {
        suiteRunner.runTest(TESTROOT + "err-s0005-005.xml");
    }

    @Test
    public void testErrS0005006() {
        suiteRunner.runTest(TESTROOT + "err-s0005-006.xml");
    }

    @Test
    public void testErrS0005007() {
        suiteRunner.runTest(TESTROOT + "err-s0005-007.xml");
    }

    @Test
    public void testErrS0005008() {
        suiteRunner.runTest(TESTROOT + "err-s0005-008.xml");
    }

    @Test
    public void testErrS0005009() {
        suiteRunner.runTest(TESTROOT + "err-s0005-009.xml");
    }

    @Test
    public void testErrS0005010() {
        suiteRunner.runTest(TESTROOT + "err-s0005-010.xml");
    }

    @Test
    public void testErrS0005011() {
        suiteRunner.runTest(TESTROOT + "err-s0005-011.xml");
    }

    @Test
    public void testErrS0005012() {
        suiteRunner.runTest(TESTROOT + "err-s0005-012.xml");
    }

    @Test
    public void testErrS0005013() {
        suiteRunner.runTest(TESTROOT + "err-s0005-013.xml");
    }

    @Test
    public void testErrS0006001() {
        suiteRunner.runTest(TESTROOT + "err-s0006-001.xml");
    }

    @Test
    public void testErrS0007001() {
        suiteRunner.runTest(TESTROOT + "err-s0007-001.xml");
    }

    @Test
    public void testErrS0007002() {
        suiteRunner.runTest(TESTROOT + "err-s0007-002.xml");
    }

    @Test
    public void testErrS0007003() {
        suiteRunner.runTest(TESTROOT + "err-s0007-003.xml");
    }

    @Test
    public void testErrS0008001() {
        suiteRunner.runTest(TESTROOT + "err-s0008-001.xml");
    }

    @Test
    public void testErrS0009001() {
        suiteRunner.runTest(TESTROOT + "err-s0009-001.xml");
    }

    @Test
    public void testErrS0009002() {
        suiteRunner.runTest(TESTROOT + "err-s0009-002.xml");
    }

    @Test
    public void testErrS0009004() {
        suiteRunner.runTest(TESTROOT + "err-s0009-004.xml");
    }

    @Test
    public void testErrS0009005() {
        suiteRunner.runTest(TESTROOT + "err-s0009-005.xml");
    }

    @Test
    public void testErrS0010001() {
        suiteRunner.runTest(TESTROOT + "err-s0010-001.xml");
    }

    @Test
    public void testErrS0010002() {
        suiteRunner.runTest(TESTROOT + "err-s0010-002.xml");
    }

    @Test
    public void testErrS0010003() {
        suiteRunner.runTest(TESTROOT + "err-s0010-003.xml");
    }

    @Test
    public void testErrS0011001() {
        suiteRunner.runTest(TESTROOT + "err-s0011-001.xml");
    }

    @Test
    public void testErrS0011002() {
        suiteRunner.runTest(TESTROOT + "err-s0011-002.xml");
    }

    @Test
    public void testErrS0011003() {
        suiteRunner.runTest(TESTROOT + "err-s0011-003.xml");
    }

    @Test
    public void testErrS0011004() {
        suiteRunner.runTest(TESTROOT + "err-s0011-004.xml");
    }

    @Test
    public void testErrS0014001() {
        suiteRunner.runTest(TESTROOT + "err-s0014-001.xml");
    }

    @Test
    public void testErrS0015001() {
        suiteRunner.runTest(TESTROOT + "err-s0015-001.xml");
    }

    @Test
    public void testErrS0017001() {
        suiteRunner.runTest(TESTROOT + "err-s0017-001.xml");
    }

    @Test
    public void testErrS0018001() {
        suiteRunner.runTest(TESTROOT + "err-s0018-001.xml");
    }

    @Test
    public void testErrS0018002() {
        suiteRunner.runTest(TESTROOT + "err-s0018-002.xml");
    }

    @Test
    public void testErrS0018003() {
        suiteRunner.runTest(TESTROOT + "err-s0018-003.xml");
    }

    @Test
    public void testErrS0019001() {
        suiteRunner.runTest(TESTROOT + "err-s0019-001.xml");
    }

    @Test
    public void testErrS0020001() {
        suiteRunner.runTest(TESTROOT + "err-s0020-001.xml");
    }

    @Test
    public void testErrS0020002() {
        suiteRunner.runTest(TESTROOT + "err-s0020-002.xml");
    }

    @Test
    public void testErrS0020003() {
        suiteRunner.runTest(TESTROOT + "err-s0020-003.xml");
    }

    @Test
    public void testErrS0022001() {
        suiteRunner.runTest(TESTROOT + "err-s0022-001.xml");
    }

    @Test
    public void testErrS0022002() {
        suiteRunner.runTest(TESTROOT + "err-s0022-002.xml");
    }

    @Test
    public void testErrS0022003() {
        suiteRunner.runTest(TESTROOT + "err-s0022-003.xml");
    }

    @Test
    public void testErrS0022004() {
        suiteRunner.runTest(TESTROOT + "err-s0022-004.xml");
    }

    @Test
    public void testErrS0022005() {
        suiteRunner.runTest(TESTROOT + "err-s0022-005.xml");
    }

    @Test
    public void testErrS0022006() {
        suiteRunner.runTest(TESTROOT + "err-s0022-006.xml");
    }

    @Test
    public void testErrS0024001() {
        suiteRunner.runTest(TESTROOT + "err-s0024-001.xml");
    }

    @Test
    public void testErrS0024002() {
        suiteRunner.runTest(TESTROOT + "err-s0024-002.xml");
    }

    @Test
    public void testErrS0025001() {
        suiteRunner.runTest(TESTROOT + "err-s0025-001.xml");
    }

    @Test
    public void testErrS0025002() {
        suiteRunner.runTest(TESTROOT + "err-s0025-002.xml");
    }

    @Test
    public void testErrS0025003() {
        suiteRunner.runTest(TESTROOT + "err-s0025-003.xml");
    }

    @Test
    public void testErrS0025004() {
        suiteRunner.runTest(TESTROOT + "err-s0025-004.xml");
    }

    @Test
    public void testErrS0026001() {
        suiteRunner.runTest(TESTROOT + "err-s0026-001.xml");
    }

    @Test
    public void testErrS0026002() {
        suiteRunner.runTest(TESTROOT + "err-s0026-002.xml");
    }

    @Test
    public void testErrS0027001() {
        suiteRunner.runTest(TESTROOT + "err-s0027-001.xml");
    }

    @Test
    public void testErrS0028001() {
        suiteRunner.runTest(TESTROOT + "err-s0028-001.xml");
    }

    @Test
    public void testErrS0028002() {
        suiteRunner.runTest(TESTROOT + "err-s0028-002.xml");
    }

    @Test
    public void testErrS0029001() {
        suiteRunner.runTest(TESTROOT + "err-s0029-001.xml");
    }

    @Test
    public void testErrS0030001() {
        suiteRunner.runTest(TESTROOT + "err-s0030-001.xml");
    }

    @Test
    public void testErrS0031001() {
        suiteRunner.runTest(TESTROOT + "err-s0031-001.xml");
    }

    @Test
    public void testErrS0031002() {
        suiteRunner.runTest(TESTROOT + "err-s0031-002.xml");
    }

    @Test
    public void testErrS0032001() {
        suiteRunner.runTest(TESTROOT + "err-s0032-001.xml");
    }

    @Test
    public void testErrS0033001() {
        suiteRunner.runTest(TESTROOT + "err-s0033-001.xml");
    }

    @Test
    public void testErrS0034001() {
        suiteRunner.runTest(TESTROOT + "err-s0034-001.xml");
    }

    @Test
    public void testErrS0034002() {
        suiteRunner.runTest(TESTROOT + "err-s0034-002.xml");
    }

    @Test
    public void testErrS0035001() {
        suiteRunner.runTest(TESTROOT + "err-s0035-001.xml");
    }

    @Test
    public void testErrS0035002() {
        suiteRunner.runTest(TESTROOT + "err-s0035-002.xml");
    }

    @Test
    public void testErrS0036001() {
        suiteRunner.runTest(TESTROOT + "err-s0036-001.xml");
    }

    @Test
    public void testErrS0036002() {
        suiteRunner.runTest(TESTROOT + "err-s0036-002.xml");
    }

    @Test
    public void testErrS0036003() {
        suiteRunner.runTest(TESTROOT + "err-s0036-003.xml");
    }

    @Test
    public void testErrS0036004() {
        suiteRunner.runTest(TESTROOT + "err-s0036-004.xml");
    }

    @Test
    public void testErrS0036005() {
        suiteRunner.runTest(TESTROOT + "err-s0036-005.xml");
    }

    @Test
    public void testErrS0037001() {
        suiteRunner.runTest(TESTROOT + "err-s0037-001.xml");
    }

    @Test
    public void testErrS0037002() {
        suiteRunner.runTest(TESTROOT + "err-s0037-002.xml");
    }

    @Test
    public void testErrS0037003() {
        suiteRunner.runTest(TESTROOT + "err-s0037-003.xml");
    }

    @Test
    public void testErrS0038001() {
        suiteRunner.runTest(TESTROOT + "err-s0038-001.xml");
    }

    @Test
    public void testErrS0038002() {
        suiteRunner.runTest(TESTROOT + "err-s0038-002.xml");
    }

    @Test
    public void testErrS0039001() {
        suiteRunner.runTest(TESTROOT + "err-s0039-001.xml");
    }

    @Test
    public void testErrS0039002() {
        suiteRunner.runTest(TESTROOT + "err-s0039-002.xml");
    }

    @Test
    public void testErrS0040001() {
        suiteRunner.runTest(TESTROOT + "err-s0040-001.xml");
    }

    @Test
    public void testErrS0041001() {
        suiteRunner.runTest(TESTROOT + "err-s0041-001.xml");
    }

    @Test
    public void testErrS0042001() {
        suiteRunner.runTest(TESTROOT + "err-s0042-001.xml");
    }

    @Test
    public void testErrS0044001() {
        suiteRunner.runTest(TESTROOT + "err-s0044-001.xml");
    }

    @Test
    public void testErrS0044002() {
        suiteRunner.runTest(TESTROOT + "err-s0044-002.xml");
    }

    @Test
    public void testErrS0044003() {
        suiteRunner.runTest(TESTROOT + "err-s0044-003.xml");
    }

    @Test
    public void testErrS0048001() {
        suiteRunner.runTest(TESTROOT + "err-s0048-001.xml");
    }

    @Test
    public void testErrS0051001() {
        suiteRunner.runTest(TESTROOT + "err-s0051-001.xml");
    }

    @Test
    public void testErrS0051002() {
        suiteRunner.runTest(TESTROOT + "err-s0051-002.xml");
    }

    @Test
    public void testErrS0052001() {
        suiteRunner.runTest(TESTROOT + "err-s0052-001.xml");
    }

    @Test
    public void testErrS0052002() {
        suiteRunner.runTest(TESTROOT + "err-s0052-002.xml");
    }

    @Test
    public void testErrS0053001() {
        suiteRunner.runTest(TESTROOT + "err-s0053-001.xml");
    }

    @Test
    public void testErrS0055001() {
        suiteRunner.runTest(TESTROOT + "err-s0055-001.xml");
    }

    @Test
    public void testErrS0055002() {
        suiteRunner.runTest(TESTROOT + "err-s0055-002.xml");
    }

    @Test
    public void testErrS0057001() {
        suiteRunner.runTest(TESTROOT + "err-s0057-001.xml");
    }

    @Test
    public void testErrS0057002() {
        suiteRunner.runTest(TESTROOT + "err-s0057-002.xml");
    }

    @Test
    public void testErrS0058001() {
        suiteRunner.runTest(TESTROOT + "err-s0058-001.xml");
    }

    @Test
    public void testErrS0059001() {
        suiteRunner.runTest(TESTROOT + "err-s0059-001.xml");
    }

    @Test
    public void testErrS0061001() {
        suiteRunner.runTest(TESTROOT + "err-s0061-001.xml");
    }

    @Test
    public void testErrS0062001() {
        suiteRunner.runTest(TESTROOT + "err-s0062-001.xml");
    }

    @Test
    public void testErrS0062002() {
        suiteRunner.runTest(TESTROOT + "err-s0062-002.xml");
    }

    @Test
    public void testErrS0063001() {
        suiteRunner.runTest(TESTROOT + "err-s0063-001.xml");
    }

    @Test
    public void testEscapeMarkup001() {
        suiteRunner.runTest(TESTROOT + "escape-markup-001.xml");
    }

    @Test
    public void testEvaluationOrder001() {
        suiteRunner.runTest(TESTROOT + "evaluation-order-001.xml");
    }

    @Test
    public void testEvaluationOrder002() {
        suiteRunner.runTest(TESTROOT + "evaluation-order-002.xml");
    }

    @Test
    public void testEvaluationOrder003() {
        suiteRunner.runTest(TESTROOT + "evaluation-order-003.xml");
    }

    @Test
    public void testExcludeInlinePrefixes001() {
        suiteRunner.runTest(TESTROOT + "exclude-inline-prefixes-001.xml");
    }

    @Test
    public void testExcludeInlinePrefixes002() {
        suiteRunner.runTest(TESTROOT + "exclude-inline-prefixes-002.xml");
    }

    @Test
    public void testExcludeInlinePrefixes003() {
        suiteRunner.runTest(TESTROOT + "exclude-inline-prefixes-003.xml");
    }

    @Test
    public void testExcludeInlinePrefixes004() {
        suiteRunner.runTest(TESTROOT + "exclude-inline-prefixes-004.xml");
    }

    @Test
    public void testExcludeInlinePrefixes005() {
        suiteRunner.runTest(TESTROOT + "exclude-inline-prefixes-005.xml");
    }

    @Test
    public void testExcludeInlinePrefixes006() {
        suiteRunner.runTest(TESTROOT + "exclude-inline-prefixes-006.xml");
    }

    @Test
    public void testExcludeInlinePrefixes007() {
        suiteRunner.runTest(TESTROOT + "exclude-inline-prefixes-007.xml");
    }

    @Test
    public void testExcludeInlinePrefixes008() {
        suiteRunner.runTest(TESTROOT + "exclude-inline-prefixes-008.xml");
    }

    @Test
    public void testExcludeInlinePrefixes009() {
        suiteRunner.runTest(TESTROOT + "exclude-inline-prefixes-009.xml");
    }

    @Test
    public void testExcludeInlinePrefixes010() {
        suiteRunner.runTest(TESTROOT + "exclude-inline-prefixes-010.xml");
    }

    @Test
    public void testFibonacci() {
        suiteRunner.runTest(TESTROOT + "fibonacci.xml");
    }

    @Test
    public void testFilter001() {
        suiteRunner.runTest(TESTROOT + "filter-001.xml");
    }

    @Test
    public void testFilter002() {
        suiteRunner.runTest(TESTROOT + "filter-002.xml");
    }

    @Test
    public void testFilter003() {
        suiteRunner.runTest(TESTROOT + "filter-003.xml");
    }

    @Test
    public void testForEach001() {
        suiteRunner.runTest(TESTROOT + "for-each-001.xml");
    }

    @Test
    public void testForEach002() {
        suiteRunner.runTest(TESTROOT + "for-each-002.xml");
    }

    @Test
    public void testForEach003() {
        suiteRunner.runTest(TESTROOT + "for-each-003.xml");
    }

    @Test
    public void testForEach004() {
        suiteRunner.runTest(TESTROOT + "for-each-004.xml");
    }

    @Test
    public void testForEach005() {
        suiteRunner.runTest(TESTROOT + "for-each-005.xml");
    }

    @Test
    public void testForEach006() {
        suiteRunner.runTest(TESTROOT + "for-each-006.xml");
    }

    @Test
    public void testForEach007() {
        suiteRunner.runTest(TESTROOT + "for-each-007.xml");
    }

    @Test
    public void testForEach008() {
        suiteRunner.runTest(TESTROOT + "for-each-008.xml");
    }

    @Test
    public void testForEach009() {
        suiteRunner.runTest(TESTROOT + "for-each-009.xml");
    }

    @Test
    public void testForEach010() {
        suiteRunner.runTest(TESTROOT + "for-each-010.xml");
    }

    @Test
    public void testForEach011() {
        suiteRunner.runTest(TESTROOT + "for-each-011.xml");
    }

    @Test
    public void testForEach012() {
        suiteRunner.runTest(TESTROOT + "for-each-012.xml");
    }

    @Test
    public void testGroup001() {
        suiteRunner.runTest(TESTROOT + "group-001.xml");
    }

    @Test
    public void testGroup002() {
        suiteRunner.runTest(TESTROOT + "group-002.xml");
    }

    @Test
    public void testGroup003() {
        suiteRunner.runTest(TESTROOT + "group-003.xml");
    }

    @Test
    public void testHttpRequest001() {
        suiteRunner.runTest(TESTROOT + "http-request-001.xml");
    }

    @Test
    public void testHttpRequest002() {
        suiteRunner.runTest(TESTROOT + "http-request-002.xml");
    }

    @Test
    public void testHttpRequest003() {
        suiteRunner.runTest(TESTROOT + "http-request-003.xml");
    }

    @Test
    public void testHttpRequest004() {
        suiteRunner.runTest(TESTROOT + "http-request-004.xml");
    }

    @Test
    public void testHttpRequest005() {
        suiteRunner.runTest(TESTROOT + "http-request-005.xml");
    }

    @Test
    public void testHttpRequest006() {
        suiteRunner.runTest(TESTROOT + "http-request-006.xml");
    }

    @Test
    public void testHttpRequest007() {
        suiteRunner.runTest(TESTROOT + "http-request-007.xml");
    }

    @Test
    public void testHttpRequest008() {
        suiteRunner.runTest(TESTROOT + "http-request-008.xml");
    }

    @Test
    public void testHttpRequest009() {
        suiteRunner.runTest(TESTROOT + "http-request-009.xml");
    }

    @Test
    public void testHttpRequest010() {
        suiteRunner.runTest(TESTROOT + "http-request-010.xml");
    }

    @Test
    public void testHttpRequest011() {
        suiteRunner.runTest(TESTROOT + "http-request-011.xml");
    }

    @Test
    public void testHttpRequest012() {
        suiteRunner.runTest(TESTROOT + "http-request-012.xml");
    }

    @Test
    public void testHttpRequest013() {
        suiteRunner.runTest(TESTROOT + "http-request-013.xml");
    }

    @Test
    public void testHttpRequest014() {
        suiteRunner.runTest(TESTROOT + "http-request-014.xml");
    }

    @Test
    public void testIdentity001() {
        suiteRunner.runTest(TESTROOT + "identity-001.xml");
    }

    @Test
    public void testIdentity002() {
        suiteRunner.runTest(TESTROOT + "identity-002.xml");
    }

    @Test
    public void testIdentity003() {
        suiteRunner.runTest(TESTROOT + "identity-003.xml");
    }

    @Test
    public void testIdentity004() {
        suiteRunner.runTest(TESTROOT + "identity-004.xml");
    }

    @Test
    public void testIdentity005() {
        suiteRunner.runTest(TESTROOT + "identity-005.xml");
    }

    @Test
    public void testImport001() {
        suiteRunner.runTest(TESTROOT + "import-001.xml");
    }

    @Test
    public void testImport002() {
        suiteRunner.runTest(TESTROOT + "import-002.xml");
    }

    @Test
    public void testImport003() {
        suiteRunner.runTest(TESTROOT + "import-003.xml");
    }

    @Test
    public void testImport004() {
        suiteRunner.runTest(TESTROOT + "import-004.xml");
    }

    @Test
    public void testImport005() {
        suiteRunner.runTest(TESTROOT + "import-005.xml");
    }

    @Ignore
    public void testImport006() {
        suiteRunner.runTest(TESTROOT + "import-006.xml");
    }

    @Ignore
    public void testImport007() {
        suiteRunner.runTest(TESTROOT + "import-007.xml");
    }

    @Test
    public void testImport008() {
        suiteRunner.runTest(TESTROOT + "import-008.xml");
    }

    @Test
    public void testImport009() {
        suiteRunner.runTest(TESTROOT + "import-009.xml");
    }

    @Test
    public void testImport010() {
        suiteRunner.runTest(TESTROOT + "import-010.xml");
    }

    @Test
    public void testImport011() {
        suiteRunner.runTest(TESTROOT + "import-011.xml");
    }

    @Test
    public void testInput001() {
        suiteRunner.runTest(TESTROOT + "input-001.xml");
    }

    @Test
    public void testInput002() {
        suiteRunner.runTest(TESTROOT + "input-002.xml");
    }

    @Test
    public void testInput003() {
        suiteRunner.runTest(TESTROOT + "input-003.xml");
    }

    @Test
    public void testInput004() {
        suiteRunner.runTest(TESTROOT + "input-004.xml");
    }

    @Test
    public void testInput005() {
        suiteRunner.runTest(TESTROOT + "input-005.xml");
    }

    @Ignore
    public void testInput006() {
        suiteRunner.runTest(TESTROOT + "input-006.xml");
    }

    @Test
    public void testInput007() {
        suiteRunner.runTest(TESTROOT + "input-007.xml");
    }

    @Test
    public void testInput008() {
        suiteRunner.runTest(TESTROOT + "input-008.xml");
    }

    @Test
    public void testInput009() {
        suiteRunner.runTest(TESTROOT + "input-009.xml");
    }

    @Test
    public void testInput010() {
        suiteRunner.runTest(TESTROOT + "input-010.xml");
    }

    @Test
    public void testInput011() {
        suiteRunner.runTest(TESTROOT + "input-011.xml");
    }

    @Test
    public void testInput012() {
        suiteRunner.runTest(TESTROOT + "input-012.xml");
    }

    @Test
    public void testInsert001() {
        suiteRunner.runTest(TESTROOT + "insert-001.xml");
    }

    @Test
    public void testInsert002() {
        suiteRunner.runTest(TESTROOT + "insert-002.xml");
    }

    @Test
    public void testInsert003() {
        suiteRunner.runTest(TESTROOT + "insert-003.xml");
    }

    @Test
    public void testInsert004() {
        suiteRunner.runTest(TESTROOT + "insert-004.xml");
    }

    @Test
    public void testInsert005() {
        suiteRunner.runTest(TESTROOT + "insert-005.xml");
    }

    @Test
    public void testInsert006() {
        suiteRunner.runTest(TESTROOT + "insert-006.xml");
    }

    @Test
    public void testInsert007() {
        suiteRunner.runTest(TESTROOT + "insert-007.xml");
    }

    @Test
    public void testInsert008() {
        suiteRunner.runTest(TESTROOT + "insert-008.xml");
    }

    @Test
    public void testInsert009() {
        suiteRunner.runTest(TESTROOT + "insert-009.xml");
    }

    @Test
    public void testInsert010() {
        suiteRunner.runTest(TESTROOT + "insert-010.xml");
    }

    @Test
    public void testIteration001() {
        suiteRunner.runTest(TESTROOT + "iteration-001.xml");
    }

    @Test
    public void testIteration002() {
        suiteRunner.runTest(TESTROOT + "iteration-002.xml");
    }

    @Test
    public void testLabelelements001() {
        suiteRunner.runTest(TESTROOT + "labelelements-001.xml");
    }

    @Test
    public void testLabelelements002() {
        suiteRunner.runTest(TESTROOT + "labelelements-002.xml");
    }

    @Test
    public void testLabelelements003() {
        suiteRunner.runTest(TESTROOT + "labelelements-003.xml");
    }

    @Test
    public void testLabelelements004() {
        suiteRunner.runTest(TESTROOT + "labelelements-004.xml");
    }

    @Test
    public void testLabelelements005() {
        suiteRunner.runTest(TESTROOT + "labelelements-005.xml");
    }

    @Test
    public void testLabelelements006() {
        suiteRunner.runTest(TESTROOT + "labelelements-006.xml");
    }

    @Test
    public void testLabelelements007() {
        suiteRunner.runTest(TESTROOT + "labelelements-007.xml");
    }

    @Test
    public void testLabelelements008() {
        suiteRunner.runTest(TESTROOT + "labelelements-008.xml");
    }

    @Test
    public void testLabelelements009() {
        suiteRunner.runTest(TESTROOT + "labelelements-009.xml");
    }

    @Test
    public void testLabelelements010() {
        suiteRunner.runTest(TESTROOT + "labelelements-010.xml");
    }

    @Test
    public void testLabelelements011() {
        suiteRunner.runTest(TESTROOT + "labelelements-011.xml");
    }

    @Test
    public void testLoad001() {
        suiteRunner.runTest(TESTROOT + "load-001.xml");
    }

    @Test
    public void testLoad002() {
        suiteRunner.runTest(TESTROOT + "load-002.xml");
    }

    @Test
    public void testLoad003() {
        suiteRunner.runTest(TESTROOT + "load-003.xml");
    }

    @Test
    public void testLoad004() {
        suiteRunner.runTest(TESTROOT + "load-004.xml");
    }

    @Test
    public void testLog001() {
        suiteRunner.runTest(TESTROOT + "log-001.xml");
    }

    @Test
    public void testLog002() {
        suiteRunner.runTest(TESTROOT + "log-002.xml");
    }

    @Ignore
    public void testMakeAbsoluteUris001() {
        suiteRunner.runTest(TESTROOT + "make-absolute-uris-001.xml");
    }

    @Test
    public void testMakeAbsoluteUris002() {
        suiteRunner.runTest(TESTROOT + "make-absolute-uris-002.xml");
    }

    @Test
    public void testMakeAbsoluteUris003() {
        suiteRunner.runTest(TESTROOT + "make-absolute-uris-003.xml");
    }

    @Test
    public void testMakeSequence() {
        suiteRunner.runTest(TESTROOT + "make-sequence.xml");
    }

    @Test
    public void testMultipart001() {
        suiteRunner.runTest(TESTROOT + "multipart-001.xml");
    }

    @Test
    public void testMultipart002() {
        suiteRunner.runTest(TESTROOT + "multipart-002.xml");
    }

    @Test
    public void testMultipart003() {
        suiteRunner.runTest(TESTROOT + "multipart-003.xml");
    }

    @Test
    public void testMultipart004() {
        suiteRunner.runTest(TESTROOT + "multipart-004.xml");
    }

    @Test
    public void testMultipart005() {
        suiteRunner.runTest(TESTROOT + "multipart-005.xml");
    }

    @Test
    public void testNamespaceRename001() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-001.xml");
    }

    @Test
    public void testNamespaceRename002() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-002.xml");
    }

    @Test
    public void testNamespaceRename003() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-003.xml");
    }

    @Test
    public void testNamespaceRename004() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-004.xml");
    }

    @Test
    public void testNamespaceRename005() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-005.xml");
    }

    @Test
    public void testNamespaceRename006() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-006.xml");
    }

    @Test
    public void testNamespaceRename007() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-007.xml");
    }

    @Test
    public void testNamespaceRename008() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-008.xml");
    }

    @Test
    public void testNamespaceRename009() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-009.xml");
    }

    @Test
    public void testNamespaceRename010() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-010.xml");
    }

    @Test
    public void testNamespaceRename011() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-011.xml");
    }

    @Test
    public void testNamespaceRename012() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-012.xml");
    }

    @Test
    public void testNamespaceRename013() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-013.xml");
    }

    @Test
    public void testNamespaceRename014() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-014.xml");
    }

    @Test
    public void testNamespaceRename015() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-015.xml");
    }

    @Test
    public void testNamespaceRename016() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-016.xml");
    }

    @Test
    public void testNamespaceRename017() {
        suiteRunner.runTest(TESTROOT + "namespace-rename-017.xml");
    }

    @Test
    public void testNamespaces001() {
        suiteRunner.runTest(TESTROOT + "namespaces-001.xml");
    }

    @Test
    public void testNamespaces002() {
        suiteRunner.runTest(TESTROOT + "namespaces-002.xml");
    }

    @Test
    public void testNamespaces003() {
        suiteRunner.runTest(TESTROOT + "namespaces-003.xml");
    }

    @Test
    public void testNamespaces004() {
        suiteRunner.runTest(TESTROOT + "namespaces-004.xml");
    }

    @Test
    public void testNamespaces005() {
        suiteRunner.runTest(TESTROOT + "namespaces-005.xml");
    }

    @Test
    public void testNamespaces006() {
        suiteRunner.runTest(TESTROOT + "namespaces-006.xml");
    }

    @Test
    public void testNamespaces007() {
        suiteRunner.runTest(TESTROOT + "namespaces-007.xml");
    }

    @Test
    public void testNamespaces008() {
        suiteRunner.runTest(TESTROOT + "namespaces-008.xml");
    }

    @Test
    public void testNamespaces009() {
        suiteRunner.runTest(TESTROOT + "namespaces-009.xml");
    }

    @Test
    public void testNamespaces010() {
        suiteRunner.runTest(TESTROOT + "namespaces-010.xml");
    }

    @Test
    public void testNestedPipeline001() {
        suiteRunner.runTest(TESTROOT + "nested-pipeline-001.xml");
    }

    @Test
    public void testOption001() {
        suiteRunner.runTest(TESTROOT + "option-001.xml");
    }

    @Test
    public void testOption002() {
        suiteRunner.runTest(TESTROOT + "option-002.xml");
    }

    @Test
    public void testOption004() {
        suiteRunner.runTest(TESTROOT + "option-004.xml");
    }

    @Test
    public void testOutput001() {
        suiteRunner.runTest(TESTROOT + "output-001.xml");
    }

    @Test
    public void testOutput002() {
        suiteRunner.runTest(TESTROOT + "output-002.xml");
    }

    @Test
    public void testPack001() {
        suiteRunner.runTest(TESTROOT + "pack-001.xml");
    }

    @Test
    public void testPack002() {
        suiteRunner.runTest(TESTROOT + "pack-002.xml");
    }

    @Test
    public void testPack003() {
        suiteRunner.runTest(TESTROOT + "pack-003.xml");
    }

    @Test
    public void testPack004() {
        suiteRunner.runTest(TESTROOT + "pack-004.xml");
    }

    @Test
    public void testPack005() {
        suiteRunner.runTest(TESTROOT + "pack-005.xml");
    }

    @Test
    public void testPack006() {
        suiteRunner.runTest(TESTROOT + "pack-006.xml");
    }

    @Test
    public void testParam001() {
        suiteRunner.runTest(TESTROOT + "param-001.xml");
    }

    @Test
    public void testParam002() {
        suiteRunner.runTest(TESTROOT + "param-002.xml");
    }

    @Test
    public void testParam003() {
        suiteRunner.runTest(TESTROOT + "param-003.xml");
    }

    @Test
    public void testParam004() {
        suiteRunner.runTest(TESTROOT + "param-004.xml");
    }

    @Test
    public void testParam005() {
        suiteRunner.runTest(TESTROOT + "param-005.xml");
    }

    @Test
    public void testParameters001() {
        suiteRunner.runTest(TESTROOT + "parameters-001.xml");
    }

    @Test
    public void testParameters002() {
        suiteRunner.runTest(TESTROOT + "parameters-002.xml");
    }

    @Test
    public void testParameters003() {
        suiteRunner.runTest(TESTROOT + "parameters-003.xml");
    }

    @Test
    public void testPipe001() {
        suiteRunner.runTest(TESTROOT + "pipe-001.xml");
    }

    @Test
    public void testPipeinfo001() {
        suiteRunner.runTest(TESTROOT + "pipeinfo-001.xml");
    }

    @Test
    public void testPreserveBaseUri001() {
        suiteRunner.runTest(TESTROOT + "preserve-base-uri-001.xml");
    }

    @Test
    public void testPreserveBaseUri002() {
        suiteRunner.runTest(TESTROOT + "preserve-base-uri-002.xml");
    }

    @Test
    public void testRename001() {
        suiteRunner.runTest(TESTROOT + "rename-001.xml");
    }

    @Test
    public void testRename002() {
        suiteRunner.runTest(TESTROOT + "rename-002.xml");
    }

    @Test
    public void testRename003() {
        suiteRunner.runTest(TESTROOT + "rename-003.xml");
    }

    @Test
    public void testRename004() {
        suiteRunner.runTest(TESTROOT + "rename-004.xml");
    }

    @Test
    public void testRename005() {
        suiteRunner.runTest(TESTROOT + "rename-005.xml");
    }

    @Test
    public void testRename006() {
        suiteRunner.runTest(TESTROOT + "rename-006.xml");
    }

    @Test
    public void testRename007() {
        suiteRunner.runTest(TESTROOT + "rename-007.xml");
    }

    @Test
    public void testReplace001() {
        suiteRunner.runTest(TESTROOT + "replace-001.xml");
    }

    @Test
    public void testResolveUri001() {
        suiteRunner.runTest(TESTROOT + "resolve-uri-001.xml");
    }

    @Test
    public void testResolveUri002() {
        suiteRunner.runTest(TESTROOT + "resolve-uri-002.xml");
    }

    @Test
    public void testSetAttributes001() {
        suiteRunner.runTest(TESTROOT + "set-attributes-001.xml");
    }

    @Test
    public void testSetAttributes002() {
        suiteRunner.runTest(TESTROOT + "set-attributes-002.xml");
    }

    @Test
    public void testSink001() {
        suiteRunner.runTest(TESTROOT + "sink-001.xml");
    }

    @Test
    public void testSink002() {
        suiteRunner.runTest(TESTROOT + "sink-002.xml");
    }

    @Test
    public void testSink003() {
        suiteRunner.runTest(TESTROOT + "sink-003.xml");
    }

    @Test
    public void testSplitSequence001() {
        suiteRunner.runTest(TESTROOT + "split-sequence-001.xml");
    }

    @Test
    public void testSplitSequence002() {
        suiteRunner.runTest(TESTROOT + "split-sequence-002.xml");
    }

    @Test
    public void testSplitSequence003() {
        suiteRunner.runTest(TESTROOT + "split-sequence-003.xml");
    }

    @Test
    public void testSplitSequence004() {
        suiteRunner.runTest(TESTROOT + "split-sequence-004.xml");
    }

    @Test
    public void testSplitSequence005() {
        suiteRunner.runTest(TESTROOT + "split-sequence-005.xml");
    }

    @Test
    public void testSplitSequence006() {
        suiteRunner.runTest(TESTROOT + "split-sequence-006.xml");
    }

    @Test
    public void testSplitSequence007() {
        suiteRunner.runTest(TESTROOT + "split-sequence-007.xml");
    }

    @Test
    public void testStepAvailable001() {
        suiteRunner.runTest(TESTROOT + "step-available-001.xml");
    }

    @Test
    public void testStepAvailable002() {
        suiteRunner.runTest(TESTROOT + "step-available-002.xml");
    }

    @Test
    public void testStepAvailable003() {
        suiteRunner.runTest(TESTROOT + "step-available-003.xml");
    }

    @Test
    public void testStepAvailable004() {
        suiteRunner.runTest(TESTROOT + "step-available-004.xml");
    }

    @Test
    public void testStepAvailable005() {
        suiteRunner.runTest(TESTROOT + "step-available-005.xml");
    }

    @Test
    public void testStepAvailable006() {
        suiteRunner.runTest(TESTROOT + "step-available-006.xml");
    }

    @Test
    public void testStore001() {
        suiteRunner.runTest(TESTROOT + "store-001.xml");
    }

    @Test
    public void testStringReplace001() {
        suiteRunner.runTest(TESTROOT + "string-replace-001.xml");
    }

    @Test
    public void testStringReplace002() {
        suiteRunner.runTest(TESTROOT + "string-replace-002.xml");
    }

    @Test
    public void testStringReplace003() {
        suiteRunner.runTest(TESTROOT + "string-replace-003.xml");
    }

    @Test
    public void testStringReplace004() {
        suiteRunner.runTest(TESTROOT + "string-replace-004.xml");
    }

    @Test
    public void testStringReplace005() {
        suiteRunner.runTest(TESTROOT + "string-replace-005.xml");
    }

    @Test
    public void testSystemProperty001() {
        suiteRunner.runTest(TESTROOT + "system-property-001.xml");
    }

    @Test
    public void testSystemProperty002() {
        suiteRunner.runTest(TESTROOT + "system-property-002.xml");
    }

    @Test
    public void testTry001() {
        suiteRunner.runTest(TESTROOT + "try-001.xml");
    }

    @Test
    public void testTry002() {
        suiteRunner.runTest(TESTROOT + "try-002.xml");
    }

    @Test
    public void testTry003() {
        suiteRunner.runTest(TESTROOT + "try-003.xml");
    }

    @Test
    public void testTry004() {
        suiteRunner.runTest(TESTROOT + "try-004.xml");
    }

    @Test
    public void testTry005() {
        suiteRunner.runTest(TESTROOT + "try-005.xml");
    }

    @Test
    public void testTry006() {
        suiteRunner.runTest(TESTROOT + "try-006.xml");
    }

    @Test
    public void testUnescapemarkup001() {
        suiteRunner.runTest(TESTROOT + "unescapemarkup-001.xml");
    }

    @Test
    public void testUnescapemarkup002() {
        suiteRunner.runTest(TESTROOT + "unescapemarkup-002.xml");
    }

    @Test
    public void testUnescapemarkup003() {
        suiteRunner.runTest(TESTROOT + "unescapemarkup-003.xml");
    }

    @Ignore
    public void testUnescapemarkup004() {
        suiteRunner.runTest(TESTROOT + "unescapemarkup-004.xml");
    }

    @Test
    public void testUnescapemarkup005() {
        suiteRunner.runTest(TESTROOT + "unescapemarkup-005.xml");
    }

    @Test
    public void testUnescapemarkup006() {
        suiteRunner.runTest(TESTROOT + "unescapemarkup-006.xml");
    }

    @Test
    public void testUnescapemarkup007() {
        suiteRunner.runTest(TESTROOT + "unescapemarkup-007.xml");
    }

    @Test
    public void testUnwrap001() {
        suiteRunner.runTest(TESTROOT + "unwrap-001.xml");
    }

    @Test
    public void testUnwrap002() {
        suiteRunner.runTest(TESTROOT + "unwrap-002.xml");
    }

    @Test
    public void testUseWhen001() {
        suiteRunner.runTest(TESTROOT + "use-when-001.xml");
    }

    @Test
    public void testUseWhen002() {
        suiteRunner.runTest(TESTROOT + "use-when-002.xml");
    }

    @Test
    public void testUseWhen003() {
        suiteRunner.runTest(TESTROOT + "use-when-003.xml");
    }

    @Test
    public void testUseWhen004() {
        suiteRunner.runTest(TESTROOT + "use-when-004.xml");
    }

    @Test
    public void testValueAvailable001() {
        suiteRunner.runTest(TESTROOT + "value-available-001.xml");
    }

    @Test
    public void testValueAvailable002() {
        suiteRunner.runTest(TESTROOT + "value-available-002.xml");
    }

    @Test
    public void testValueAvailable003() {
        suiteRunner.runTest(TESTROOT + "value-available-003.xml");
    }

    @Test
    public void testValueAvailable004() {
        suiteRunner.runTest(TESTROOT + "value-available-004.xml");
    }

    @Test
    public void testValueAvailable005() {
        suiteRunner.runTest(TESTROOT + "value-available-005.xml");
    }

    @Test
    public void testValueAvailable006() {
        suiteRunner.runTest(TESTROOT + "value-available-006.xml");
    }

    @Test
    public void testVariable001() {
        suiteRunner.runTest(TESTROOT + "variable-001.xml");
    }

    @Test
    public void testVariable002() {
        suiteRunner.runTest(TESTROOT + "variable-002.xml");
    }

    @Test
    public void testVariable003() {
        suiteRunner.runTest(TESTROOT + "variable-003.xml");
    }

    @Test
    public void testVariable004() {
        suiteRunner.runTest(TESTROOT + "variable-004.xml");
    }

    @Test
    public void testVariable005() {
        suiteRunner.runTest(TESTROOT + "variable-005.xml");
    }

    @Test
    public void testVariable006() {
        suiteRunner.runTest(TESTROOT + "variable-006.xml");
    }

    @Test
    public void testVariable007() {
        suiteRunner.runTest(TESTROOT + "variable-007.xml");
    }

    @Test
    public void testVariable008() {
        suiteRunner.runTest(TESTROOT + "variable-008.xml");
    }

    @Test
    public void testVersionAvailable001() {
        suiteRunner.runTest(TESTROOT + "version-available-001.xml");
    }

    @Test
    public void testVersionAvailable002() {
        suiteRunner.runTest(TESTROOT + "version-available-002.xml");
    }

    @Test
    public void testVersioning001() {
        suiteRunner.runTest(TESTROOT + "versioning-001.xml");
    }

    @Test
    public void testVersioning002() {
        suiteRunner.runTest(TESTROOT + "versioning-002.xml");
    }

    @Test
    public void testVersioning003() {
        suiteRunner.runTest(TESTROOT + "versioning-003.xml");
    }

    @Test
    public void testVersioning004() {
        suiteRunner.runTest(TESTROOT + "versioning-004.xml");
    }

    @Test
    public void testVersioning005() {
        suiteRunner.runTest(TESTROOT + "versioning-005.xml");
    }

    @Test
    public void testVersioning006() {
        suiteRunner.runTest(TESTROOT + "versioning-006.xml");
    }

    @Test
    public void testVersioning007() {
        suiteRunner.runTest(TESTROOT + "versioning-007.xml");
    }

    @Test
    public void testViewport001() {
        suiteRunner.runTest(TESTROOT + "viewport-001.xml");
    }

    @Test
    public void testViewport002() {
        suiteRunner.runTest(TESTROOT + "viewport-002.xml");
    }

    @Test
    public void testViewport003() {
        suiteRunner.runTest(TESTROOT + "viewport-003.xml");
    }

    @Test
    public void testViewport004() {
        suiteRunner.runTest(TESTROOT + "viewport-004.xml");
    }

    @Test
    public void testViewport005() {
        suiteRunner.runTest(TESTROOT + "viewport-005.xml");
    }

    @Test
    public void testViewport006() {
        suiteRunner.runTest(TESTROOT + "viewport-006.xml");
    }

    @Test
    public void testViewport007() {
        suiteRunner.runTest(TESTROOT + "viewport-007.xml");
    }

    @Test
    public void testViewport008() {
        suiteRunner.runTest(TESTROOT + "viewport-008.xml");
    }

    @Test
    public void testViewport009() {
        suiteRunner.runTest(TESTROOT + "viewport-009.xml");
    }

    @Test
    public void testViewport010() {
        suiteRunner.runTest(TESTROOT + "viewport-010.xml");
    }

    @Test
    public void testViewport011() {
        suiteRunner.runTest(TESTROOT + "viewport-011.xml");
    }

    @Test
    public void testWrap001() {
        suiteRunner.runTest(TESTROOT + "wrap-001.xml");
    }

    @Test
    public void testWrap002() {
        suiteRunner.runTest(TESTROOT + "wrap-002.xml");
    }

    @Test
    public void testWrap003() {
        suiteRunner.runTest(TESTROOT + "wrap-003.xml");
    }

    @Test
    public void testWrap004() {
        suiteRunner.runTest(TESTROOT + "wrap-004.xml");
    }

    @Test
    public void testWrap005() {
        suiteRunner.runTest(TESTROOT + "wrap-005.xml");
    }

    @Test
    public void testWrap006() {
        suiteRunner.runTest(TESTROOT + "wrap-006.xml");
    }

    @Test
    public void testWrap007() {
        suiteRunner.runTest(TESTROOT + "wrap-007.xml");
    }

    @Test
    public void testWrap008() {
        suiteRunner.runTest(TESTROOT + "wrap-008.xml");
    }

    @Test
    public void testWrap009() {
        suiteRunner.runTest(TESTROOT + "wrap-009.xml");
    }

    @Test
    public void testWrap010() {
        suiteRunner.runTest(TESTROOT + "wrap-010.xml");
    }

    @Test
    public void testWrap011() {
        suiteRunner.runTest(TESTROOT + "wrap-011.xml");
    }

    @Test
    public void testWrapSequence001() {
        suiteRunner.runTest(TESTROOT + "wrap-sequence-001.xml");
    }

    @Test
    public void testWrapSequence002() {
        suiteRunner.runTest(TESTROOT + "wrap-sequence-002.xml");
    }

    @Test
    public void testWrapSequence003() {
        suiteRunner.runTest(TESTROOT + "wrap-sequence-003.xml");
    }

    @Test
    public void testWrapSequence004() {
        suiteRunner.runTest(TESTROOT + "wrap-sequence-004.xml");
    }

    @Test
    public void testWrapSequence005() {
        suiteRunner.runTest(TESTROOT + "wrap-sequence-005.xml");
    }

    @Test
    public void testXinclude001() {
        suiteRunner.runTest(TESTROOT + "xinclude-001.xml");
    }

    @Test
    public void testXinclude002() {
        suiteRunner.runTest(TESTROOT + "xinclude-002.xml");
    }

    @Test
    public void testXinclude003() {
        suiteRunner.runTest(TESTROOT + "xinclude-003.xml");
    }

    @Test
    public void testXinclude004() {
        suiteRunner.runTest(TESTROOT + "xinclude-004.xml");
    }

    @Test
    public void testXinclude005() {
        suiteRunner.runTest(TESTROOT + "xinclude-005.xml");
    }

    @Test
    public void testXmlId001() {
        suiteRunner.runTest(TESTROOT + "xml-id-001.xml");
    }

    @Test
    public void testXmlId002() {
        suiteRunner.runTest(TESTROOT + "xml-id-002.xml");
    }

    @Test
    public void testXpathVersionAvailable001() {
        suiteRunner.runTest(TESTROOT + "xpath-version-available-001.xml");
    }

    @Test
    public void testXpathVersionAvailable002() {
        suiteRunner.runTest(TESTROOT + "xpath-version-available-002.xml");
    }

    @Test
    public void testXslt001() {
        suiteRunner.runTest(TESTROOT + "xslt-001.xml");
    }

    @Test
    public void testXslt002() {
        suiteRunner.runTest(TESTROOT + "xslt-002.xml");
    }

    @Test
    public void testXslt003() {
        suiteRunner.runTest(TESTROOT + "xslt-003.xml");
    }

    @Test
    public void testXslt004() {
        suiteRunner.runTest(TESTROOT + "xslt-004.xml");
    }

    @Test
    public void testXslt005() {
        suiteRunner.runTest(TESTROOT + "xslt-005.xml");
    }

    @Test
    public void testXslt006() {
        suiteRunner.runTest(TESTROOT + "xslt-006.xml");
    }
}
