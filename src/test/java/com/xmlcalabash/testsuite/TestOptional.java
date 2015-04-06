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
public class TestOptional {
    protected static SuiteRunner suiteRunner = null;
    protected static String TESTROOT = "test/testsuite/optional/";

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
    public void testErrC0033001() {
        suiteRunner.runTest(TESTROOT + "err-c0033-001.xml");
    }

    @Test
    public void testErrC0034001() {
        suiteRunner.runTest(TESTROOT + "err-c0034-001.xml");
    }

    @Test
    public void testErrC0035001() {
        suiteRunner.runTest(TESTROOT + "err-c0035-001.xml");
    }

    @Test
    public void testErrC0035002() {
        suiteRunner.runTest(TESTROOT + "err-c0035-002.xml");
    }

    @Test
    public void testErrC0036001() {
        suiteRunner.runTest(TESTROOT + "err-c0036-001.xml");
    }

    @Test
    public void testErrC0036002() {
        suiteRunner.runTest(TESTROOT + "err-c0036-002.xml");
    }

    @Test
    public void testErrC0036003() {
        suiteRunner.runTest(TESTROOT + "err-c0036-003.xml");
    }

    @Test
    public void testErrC0036004() {
        suiteRunner.runTest(TESTROOT + "err-c0036-004.xml");
    }

    @Test
    public void testErrC0036005() {
        suiteRunner.runTest(TESTROOT + "err-c0036-005.xml");
    }

    @Test
    public void testErrC0037001() {
        suiteRunner.runTest(TESTROOT + "err-c0037-001.xml");
    }

    @Test
    public void testErrC0037002() {
        suiteRunner.runTest(TESTROOT + "err-c0037-002.xml");
    }

    @Test
    public void testErrC0037003() {
        suiteRunner.runTest(TESTROOT + "err-c0037-003.xml");
    }

    @Test
    public void testErrC0053001() {
        suiteRunner.runTest(TESTROOT + "err-c0053-001.xml");
    }

    @Test
    public void testErrC0053002() {
        suiteRunner.runTest(TESTROOT + "err-c0053-002.xml");
    }

    @Test
    public void testErrC0053003() {
        suiteRunner.runTest(TESTROOT + "err-c0053-003.xml");
    }

    @Test
    public void testErrC0053004() {
        suiteRunner.runTest(TESTROOT + "err-c0053-004.xml");
    }

    @Test
    public void testErrC0054001() {
        suiteRunner.runTest(TESTROOT + "err-c0054-001.xml");
    }

    @Test
    public void testErrC0057001() {
        suiteRunner.runTest(TESTROOT + "err-c0057-001.xml");
    }

    @Test
    public void testErrC0060001() {
        suiteRunner.runTest(TESTROOT + "err-c0060-001.xml");
    }

    @Test
    public void testErrC0061001() {
        suiteRunner.runTest(TESTROOT + "err-c0061-001.xml");
    }

    @Test
    public void testErrC0063001() {
        suiteRunner.runTest(TESTROOT + "err-c0063-001.xml");
    }

    @Test
    public void testErrC0063002() {
        suiteRunner.runTest(TESTROOT + "err-c0063-002.xml");
    }

    @Test
    public void testErrC0064001() {
        suiteRunner.runTest(TESTROOT + "err-c0064-001.xml");
    }

    @Test
    public void testErrC0066001() {
        suiteRunner.runTest(TESTROOT + "err-c0066-001.xml");
    }

    @Test
    public void testErrC0066002() {
        suiteRunner.runTest(TESTROOT + "err-c0066-002.xml");
    }

    @Test
    public void testExec001() {
        suiteRunner.runTest(TESTROOT + "exec-001.xml");
    }

    @Test
    public void testExec002() {
        suiteRunner.runTest(TESTROOT + "exec-002.xml");
    }

    @Test
    public void testExec003() {
        suiteRunner.runTest(TESTROOT + "exec-003.xml");
    }

    @Test
    public void testExec004() {
        suiteRunner.runTest(TESTROOT + "exec-004.xml");
    }

    @Test
    public void testExec005() {
        suiteRunner.runTest(TESTROOT + "exec-005.xml");
    }

    @Test
    public void testExec006() {
        suiteRunner.runTest(TESTROOT + "exec-006.xml");
    }

    @Test
    public void testExec007() {
        suiteRunner.runTest(TESTROOT + "exec-007.xml");
    }

    @Test
    public void testExec008() {
        suiteRunner.runTest(TESTROOT + "exec-008.xml");
    }

    @Test
    public void testExec009() {
        suiteRunner.runTest(TESTROOT + "exec-009.xml");
    }

    @Test
    public void testExec010() {
        suiteRunner.runTest(TESTROOT + "exec-010.xml");
    }

    @Test
    public void testExec011() {
        suiteRunner.runTest(TESTROOT + "exec-011.xml");
    }

    @Test
    public void testExec012() {
        suiteRunner.runTest(TESTROOT + "exec-012.xml");
    }

    @Test
    public void testExec013() {
        suiteRunner.runTest(TESTROOT + "exec-013.xml");
    }

    @Test
    public void testExec014() {
        suiteRunner.runTest(TESTROOT + "exec-014.xml");
    }

    @Test
    public void testExec015() {
        suiteRunner.runTest(TESTROOT + "exec-015.xml");
    }

    @Test
    public void testExec016() {
        suiteRunner.runTest(TESTROOT + "exec-016.xml");
    }

    @Test
    public void testExec017() {
        suiteRunner.runTest(TESTROOT + "exec-017.xml");
    }

    @Test
    public void testHash001() {
        suiteRunner.runTest(TESTROOT + "hash-001.xml");
    }

    @Test
    public void testHash002() {
        suiteRunner.runTest(TESTROOT + "hash-002.xml");
    }

    @Test
    public void testHash003() {
        suiteRunner.runTest(TESTROOT + "hash-003.xml");
    }

    @Test
    public void testHash004() {
        suiteRunner.runTest(TESTROOT + "hash-004.xml");
    }

    @Test
    public void testHash005() {
        suiteRunner.runTest(TESTROOT + "hash-005.xml");
    }

    @Test
    public void testHash006() {
        suiteRunner.runTest(TESTROOT + "hash-006.xml");
    }

    @Test
    public void testInScopeNames001() {
        suiteRunner.runTest(TESTROOT + "in-scope-names-001.xml");
    }

    @Test
    public void testInScopeNames002() {
        suiteRunner.runTest(TESTROOT + "in-scope-names-002.xml");
    }

    @Ignore
    public void testPsviRequired001() {
        suiteRunner.runTest(TESTROOT + "psvi-required-001.xml");
    }

    @Test
    public void testTemplate001() {
        suiteRunner.runTest(TESTROOT + "template-001.xml");
    }

    @Test
    public void testTemplate002() {
        suiteRunner.runTest(TESTROOT + "template-002.xml");
    }

    @Test
    public void testTemplate003() {
        suiteRunner.runTest(TESTROOT + "template-003.xml");
    }

    @Test
    public void testTemplate004() {
        suiteRunner.runTest(TESTROOT + "template-004.xml");
    }

    @Test
    public void testTemplate005() {
        suiteRunner.runTest(TESTROOT + "template-005.xml");
    }

    @Test
    public void testTemplate006() {
        suiteRunner.runTest(TESTROOT + "template-006.xml");
    }

    @Test
    public void testTemplate007() {
        suiteRunner.runTest(TESTROOT + "template-007.xml");
    }

    @Test
    public void testTemplate008() {
        suiteRunner.runTest(TESTROOT + "template-008.xml");
    }

    @Test
    public void testTemplate009() {
        suiteRunner.runTest(TESTROOT + "template-009.xml");
    }

    @Test
    public void testTemplate010() {
        suiteRunner.runTest(TESTROOT + "template-010.xml");
    }

    @Test
    public void testTemplate011() {
        suiteRunner.runTest(TESTROOT + "template-011.xml");
    }

    @Test
    public void testTemplate012() {
        suiteRunner.runTest(TESTROOT + "template-012.xml");
    }

    @Test
    public void testTemplate013() {
        suiteRunner.runTest(TESTROOT + "template-013.xml");
    }

    @Test
    public void testTemplate014() {
        suiteRunner.runTest(TESTROOT + "template-014.xml");
    }

    @Test
    public void testTemplate015() {
        suiteRunner.runTest(TESTROOT + "template-015.xml");
    }

    @Test
    public void testTemplate016() {
        suiteRunner.runTest(TESTROOT + "template-016.xml");
    }

    @Test
    public void testTemplate017() {
        suiteRunner.runTest(TESTROOT + "template-017.xml");
    }

    @Test
    public void testTemplate018() {
        suiteRunner.runTest(TESTROOT + "template-018.xml");
    }

    @Test
    public void testUuid001() {
        suiteRunner.runTest(TESTROOT + "uuid-001.xml");
    }

    @Test
    public void testValidrng001() {
        suiteRunner.runTest(TESTROOT + "validrng-001.xml");
    }

    @Test
    public void testValidrng002() {
        suiteRunner.runTest(TESTROOT + "validrng-002.xml");
    }

    @Test
    public void testValidrng003() {
        suiteRunner.runTest(TESTROOT + "validrng-003.xml");
    }

    @Test
    public void testValidrng004() {
        suiteRunner.runTest(TESTROOT + "validrng-004.xml");
    }

    @Test
    public void testValidrng005() {
        suiteRunner.runTest(TESTROOT + "validrng-005.xml");
    }

    @Test
    public void testValidrng006() {
        suiteRunner.runTest(TESTROOT + "validrng-006.xml");
    }

    @Test
    public void testValidrng008() {
        suiteRunner.runTest(TESTROOT + "validrng-008.xml");
    }

    @Test
    public void testValidrng009() {
        suiteRunner.runTest(TESTROOT + "validrng-009.xml");
    }

    @Test
    public void testValidsch001() {
        suiteRunner.runTest(TESTROOT + "validsch-001.xml");
    }

    @Test
    public void testValidsch002() {
        suiteRunner.runTest(TESTROOT + "validsch-002.xml");
    }

    @Test
    public void testValidsch003() {
        suiteRunner.runTest(TESTROOT + "validsch-003.xml");
    }

    @Test
    public void testValidsch004() {
        suiteRunner.runTest(TESTROOT + "validsch-004.xml");
    }

    @Test
    public void testValidsch005() {
        suiteRunner.runTest(TESTROOT + "validsch-005.xml");
    }

    @Ignore
    public void testValidxsd001() {
        suiteRunner.runTest(TESTROOT + "validxsd-001.xml");
    }

    @Ignore
    public void testValidxsd002() {
        suiteRunner.runTest(TESTROOT + "validxsd-002.xml");
    }

    @Ignore
    public void testValidxsd003() {
        suiteRunner.runTest(TESTROOT + "validxsd-003.xml");
    }

    @Ignore
    public void testValidxsd004() {
        suiteRunner.runTest(TESTROOT + "validxsd-004.xml");
    }

    @Ignore
    public void testValidxsd005() {
        suiteRunner.runTest(TESTROOT + "validxsd-005.xml");
    }

    @Ignore
    public void testValidxsd006() {
        suiteRunner.runTest(TESTROOT + "validxsd-006.xml");
    }

    @Ignore
    public void testValidxsd007() {
        suiteRunner.runTest(TESTROOT + "validxsd-007.xml");
    }

    @Ignore
    public void testValidxsd008() {
        suiteRunner.runTest(TESTROOT + "validxsd-008.xml");
    }

    @Ignore
    public void testValidxsd009() {
        suiteRunner.runTest(TESTROOT + "validxsd-009.xml");
    }

    @Ignore
    public void testValidxsd010() {
        suiteRunner.runTest(TESTROOT + "validxsd-010.xml");
    }

    @Ignore
    public void testValidxsd011() {
        suiteRunner.runTest(TESTROOT + "validxsd-011.xml");
    }

    @Ignore
    public void testValidxsd012() {
        suiteRunner.runTest(TESTROOT + "validxsd-012.xml");
    }

    @Ignore
    public void testValidxsd013() {
        suiteRunner.runTest(TESTROOT + "validxsd-013.xml");
    }

    @Test
    public void testWwwFormUrldecode001() {
        suiteRunner.runTest(TESTROOT + "www-form-urldecode-001.xml");
    }

    @Test
    public void testWwwFormUrlencode001() {
        suiteRunner.runTest(TESTROOT + "www-form-urlencode-001.xml");
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
    public void testXinclude006() {
        suiteRunner.runTest(TESTROOT + "xinclude-006.xml");
    }

    @Test
    public void testXquery001() {
        suiteRunner.runTest(TESTROOT + "xquery-001.xml");
    }

    @Test
    public void testXquery002() {
        suiteRunner.runTest(TESTROOT + "xquery-002.xml");
    }

    @Test
    public void testXquery003() {
        suiteRunner.runTest(TESTROOT + "xquery-003.xml");
    }

    @Test
    public void testXquery004() {
        suiteRunner.runTest(TESTROOT + "xquery-004.xml");
    }

    @Test
    public void testXquery005() {
        suiteRunner.runTest(TESTROOT + "xquery-005.xml");
    }

    @Test
    public void testXquery006() {
        suiteRunner.runTest(TESTROOT + "xquery-006.xml");
    }

    @Ignore
    public void testXslFormatter001() {
        suiteRunner.runTest(TESTROOT + "xsl-formatter-001.xml");
    }

    @Test
    public void testXslt2001() {
        suiteRunner.runTest(TESTROOT + "xslt2-001.xml");
    }

    @Test
    public void testXslt2002() {
        suiteRunner.runTest(TESTROOT + "xslt2-002.xml");
    }

    @Test
    public void testXslt2003() {
        suiteRunner.runTest(TESTROOT + "xslt2-003.xml");
    }
}
