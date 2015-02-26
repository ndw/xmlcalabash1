package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "cx:pretty-print",
        type = "{http://xmlcalabash.com/ns/extensions}pretty-print")

public class PrettyPrint extends DefaultStep {
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private static XdmNode prettyPrint = null;

    public PrettyPrint(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);

        if (prettyPrint == null) {
            try {
                InputStream instream = getClass().getResourceAsStream("/etc/prettyprint.xsl");
                if (instream == null) {
                    throw new UnsupportedOperationException("Failed to load prettyprint.xsl stylesheet from resources.");
                }
                XdmNode ppd = runtime.parse(new InputSource(instream));
                prettyPrint = S9apiUtils.getDocumentElement(ppd);
            } catch (Exception e) {
                throw new XProcException(e);
            }
        }
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        XsltCompiler compiler = runtime.getProcessor().newXsltCompiler();
        XsltExecutable exec = compiler.compile(prettyPrint.asSource());
        XsltTransformer transformer = exec.load();
        transformer.setInitialContextNode(source.read());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Serializer serializer = runtime.getProcessor().newSerializer();
        serializer.setOutputProperty(Serializer.Property.ENCODING, "utf-8");
        serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");

        serializer.setOutputStream(stream);
        transformer.setDestination(serializer);
        transformer.transform();

        XdmNode output = runtime.parse(new InputSource(new ByteArrayInputStream(stream.toByteArray())));
        result.write(output);
    }
}