package com.xmlcalabash.library;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */


@XMLCalabash(
        name = "p:identity",
        type = "{http://www.w3.org/ns/xproc}identity")

public class Identity extends DefaultStep {
    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Identity
     */
    public Identity(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
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

        while (source.moreDocuments()) {
            XdmNode doc = source.read();
            logger.trace("Identity step " + step.getName() + " read " + doc.getDocumentURI());
            result.write(doc);
        }
    }
}
