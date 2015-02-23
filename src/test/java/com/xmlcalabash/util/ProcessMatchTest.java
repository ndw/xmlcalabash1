/*
 * ProcessMatchTest.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.xmlcalabash.util;

import junit.framework.TestCase;

import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.trans.XPathException;

import java.io.StringReader;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Apr 21, 2008
 * Time: 1:11:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProcessMatchTest extends TestCase {
    Processor saxon = new Processor(false);

    public ProcessMatchTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMatch1() throws SaxonApiException, XPathException {
        //String uri = "file:///projects/src/runtime/testdocs/simple.xml";
        String xml = "<doc>\n" +
                "<p>Some document</p>\n" +
                "<p class=\"del\">Some deleted content</p>\n" +
                "<p>Some more content</p>\n" +
                "</doc>";

        SAXSource source = new SAXSource(new InputSource(new StringReader(xml)));
        // No resolver here, there isn't one.
        DocumentBuilder builder = saxon.newDocumentBuilder();
        XdmNode doc = builder.build(source);
        assertNotNull(doc);

        ProcessMatchingNodes pmn = new DebugProcessMatchingNodes();

        //ProcessMatch matcher = new ProcessMatch(saxon, pmn);
        //matcher.match(doc, new RuntimeValue("p[@class='del']", null)));
    }
}
