package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcRuntime;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by ndw on 8/27/14.
 */
public class DefaultTestReporter implements TestReporter {
    private Logger logger = LoggerFactory.getLogger(DefaultTestReporter.class);
    XProcRuntime runtime = null;

    public DefaultTestReporter(XProcRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void runningTest(URI testURI) {
        System.err.println("Running test: " + testURI);
    }

    @Override
    public void startReport(Hashtable<String,String> props) {
        GregorianCalendar cal = new GregorianCalendar();

        System.out.println("<test-report xmlns='http://xproc.org/ns/testreport'>");
        System.out.println("<title>XProc Test Results for XML Calabash</title>");
        System.out.print("<date>");
        System.out.print(cal.get(Calendar.YEAR));
        System.out.print("-");
        if (cal.get(Calendar.MONTH) + 1 < 10) {
            System.out.print("0");
        }
        System.out.print(cal.get(Calendar.MONTH) + 1);
        System.out.print("-");
        if (cal.get(Calendar.DAY_OF_MONTH) < 10) {
            System.out.print("0");
        }
        System.out.print(cal.get(Calendar.DAY_OF_MONTH));
        System.out.print("T");
        if (cal.get(Calendar.HOUR_OF_DAY) < 10) {
            System.out.print("0");
        }
        System.out.print(cal.get(Calendar.HOUR_OF_DAY));
        System.out.print(":");
        if (cal.get(Calendar.MINUTE) < 10) {
            System.out.print("0");
        }
        System.out.print(cal.get(Calendar.MINUTE));
        System.out.print(":");
        if (cal.get(Calendar.SECOND) < 10) {
            System.out.print("0");
        }
        System.out.print(cal.get(Calendar.SECOND));
        System.out.println("</date>");

        System.out.println("<processor>");
        for (String key : props.keySet()) {
            String value = props.get(key);
            System.out.println("  <" + key + ">" + value + "</" + key + ">");
        }
        System.out.println("</processor>");
    }

    @Override
    public void endReport() {
        System.out.println("</test-report>");
    }

    @Override
    public void startTestSuite() {
        System.out.println("<test-suite>");
    }

    @Override
    public void endTestSuite() {
        System.out.println("</test-suite>");
    }

    @Override
    public void startTestResults(boolean pass, String testfile, String title) {
        String passOrFail = pass ? "pass" : "fail";

        System.out.println("<" + passOrFail + " uri='" + testfile + "'>");

        if (title != null) {
            System.out.println("<title>" + title + "</title>");
        }
    }

    @Override
    public void testError(QName expectedError, QName actualError) {
        System.out.print("<error");
        if (expectedError != null) {
            System.out.print(" expected='" + expectedError + "'");
        }
        System.out.println(">" + actualError + "</error>");
    }

    @Override
    public void testErrorMessages(Vector<String> errorMessages) {
        for (String message : errorMessages) {
            System.out.println("<message>" + xmlEscape(message) + "</message>");
        }
    }

    @Override
    public void testExpected(XdmNode expected) {
        if (expected != null) {
            System.out.print("<expected>");
            System.out.print(serialize(expected));
            System.out.println("</expected>");
        }
    }

    @Override
    public void testActual(XdmNode actual) {
        if (actual != null) {
            System.out.print("<actual>");
            System.out.print(serialize(actual));
            System.out.println("</actual>");
        }
    }

    @Override
    public void endTestResults(boolean pass) {
        String passOrFail = pass ? "pass" : "fail";

        System.out.println("</" + passOrFail + ">");
    }

    private String xmlEscape(String str) {
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll("<", "&lt;");
        str = str.replaceAll(">", "&gt;");
        return str;
    }

    public String serialize(XdmNode node) {
        String result = serializeAsXML(node);

        result = result.replace("&","&amp;");
        result = result.replace("<","&lt;");
        result = result.replace(">","&gt;");
        return result;
    }

    public String serializeAsXML(XdmNode node) {
        try {
            Serializer serializer = new Serializer();

            serializer.setOutputProperty(Serializer.Property.BYTE_ORDER_MARK, "no");
            serializer.setOutputProperty(Serializer.Property.ENCODING, "utf-8");
            serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
            serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
            serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            serializer.setOutputStream(os);

            S9apiUtils.serialize(runtime, node, serializer);
            String result = os.toString();

            return result;
        } catch (SaxonApiException sae) {
            logger.warn("Failed to serialize node: " + node);
            logger.debug(sae.getMessage(), sae);
            return "";
        }
    }
}
