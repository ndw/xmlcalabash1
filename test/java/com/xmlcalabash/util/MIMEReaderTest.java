package com.xmlcalabash.util;

import junit.framework.TestCase;

import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.apache.http.Header;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.trans.XPathException;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URLConnection;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Apr 21, 2008
 * Time: 1:11:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class MIMEReaderTest extends TestCase {
    Processor saxon = new Processor(false);

    public MIMEReaderTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHeaders() throws SaxonApiException, XPathException {
        String headers = "-=-=-=-\nHeader1: value\r\nHeader2: value\r\n extended\r\n\r\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(headers.getBytes());

        MIMEReader reader = new MIMEReader(stream, "-=-=-=-");
        reader.readHeaders();
        Header h = reader.getHeader("Header1");

        assertEquals(h.getName(), "Header1");
        assertEquals(h.getValue(), "value");

        h = reader.getHeader("Header2");

        assertEquals(h.getName(), "Header2");
        assertEquals(h.getValue(), "value extended");
    }

    public void testReader() throws SaxonApiException, XPathException, IOException {
        InputStream stream = null;
        try {
            URL url = new URL("http://localhost:8133/service/fixed-multipart");
            URLConnection conn = url.openConnection();
            stream = conn.getInputStream();
        } catch (MalformedURLException mue) {
            // nop;
        } catch(IOException ioe) {
            //nop;
        }

        MIMEReader reader = new MIMEReader(stream, "-=-=-=-");
        reader.readHeaders();
        Header headers[] = reader.getHeaders();
        for (int pos = 0; pos < headers.length; pos++) {
            Header h = headers[pos];
            System.err.println(h.getName() + ": " + h.getValue());
        }

        InputStream x = reader.readBodyPart(206);
        int i = x.read();
        while (i >= 0) {
            System.err.print((char) i);
            i = x.read();
        }

    }


}
