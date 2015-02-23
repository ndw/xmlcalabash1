package com.xmlcalabash.util;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 18, 2008
 * Time: 2:19:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class XPointerTest extends TestCase {
    public XPointerTest(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        XPointerTest test = new XPointerTest("command line xpointer tests");
        test.testXmlns();
        test.testSchemes();
    }


    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
    public void testShortname() {
        XPointer xp = new XPointer("ncname");
        assertEquals(xp.xpathEquivalent(), "id(ncname)");
    }

    public void testElement() {
        XPointer xp = new XPointer("element(ncname)");
        assertEquals(xp.xpathEquivalent(), "id(ncname)");

        xp = new XPointer("element(/1/2)");
        assertEquals(xp.xpathEquivalent(), "/*[1]/*[2]");

        xp = new XPointer("element(ncname/1/2)");
        assertEquals(xp.xpathEquivalent(), "id(ncname)/*[1]/*[2]");
    }
    */

    public void testXmlns() {
        XPointer xp = new XPointer("xmlns(a=http://example.com/a) xmlns(b=http://example.com/b)", 1024000);
        assertNotNull(xp);

        xp = new XPointer("xmlns(a=http://example.com/a)xmlns(b=http://example.com/b)", 1024000);
        assertNotNull(xp);

        xp = new XPointer("xmlns(a=http://example.com/^(a^))xmlns(b=http://example.com/b)", 1024000);
        assertNotNull(xp);
    }

    public void testSchemes() {
        XPointer xp = new XPointer("xmlns(a=http://example.com/a) a:unk(a=^^b)", 1024000);
        assertNotNull(xp);

        xp = new XPointer("xmlns(a=http://example.com/a)xmlns(b=http://example.com/b) a:unk(a) b:unk(b)", 1024000);
        assertNotNull(xp);

        /*
        xp = new XPointer("xmlns(a=http://example.com/^(a^))xmlns(b=http://example.com/b) c:unk()", 1024000);
        assertNotNull(xp);
        */
    }
}

