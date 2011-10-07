package com.xmlcalabash.extensions;

import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.deltaxml.core.DXPConfiguration;
import com.deltaxml.core.PipelinedComparator;
import net.sf.saxon.s9api.*;

import javax.xml.transform.sax.SAXSource;
import java.io.StringWriter;
import java.io.StringReader;

import org.xml.sax.InputSource;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 21, 2008
 * Time: 8:51:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class DeltaXML extends DefaultStep {
    private ReadablePipe source = null;
    private ReadablePipe alternate = null;
    private ReadablePipe dxp = null;
    private WritablePipe result = null;

    /** Creates a new instance of Unzip */
    public DeltaXML(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else if ("alternate".equals(port)) {
            alternate = pipe;
        } else {
            dxp = pipe;
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        XdmNode docA = source.read();
        XdmNode docB = alternate.read();
        XdmNode dxpdoc = dxp.read();

        try {
            DXPConfiguration dxpconfig = new DXPConfiguration(S9apiUtils.xdmToInputSource(runtime, dxpdoc), null, false);
            PipelinedComparator comparator = dxpconfig.generate();

            // FIXME: Grotesque hackery!

            StringWriter sw = new StringWriter();
            Serializer serializer = new Serializer();
            serializer.setOutputWriter(sw);
            S9apiUtils.serialize(runtime, docA, serializer);

            String docAxml = sw.toString();

            sw = new StringWriter();
            serializer = new Serializer();
            serializer.setOutputWriter(sw);
            S9apiUtils.serialize(runtime, docB, serializer);

            String docBxml = sw.toString();

            StringBuffer buf = new StringBuffer();

            comparator.compare(docAxml, docBxml, buf);

            StringReader sr = new StringReader(buf.toString());
            XdmNode doc = runtime.parse(new InputSource(sr));
            result.write(doc);
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }
}
