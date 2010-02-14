package com.xmlcalabash.extensions;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.library.DefaultStep;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class Message extends DefaultStep {
    private static final QName _message = new QName("","message");
    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Identity
     */
    public Message(XProcRuntime runtime, XAtomicStep step) {
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

        String message = getOption(_message).getString();
        System.err.println("Message: " + message);

        while (source.moreDocuments()) {
            XdmNode doc = source.read();
            runtime.finest(this, step.getNode(), "Message step " + step.getName() + " read " + doc.getDocumentURI());
            result.write(doc);
        }
    }
}