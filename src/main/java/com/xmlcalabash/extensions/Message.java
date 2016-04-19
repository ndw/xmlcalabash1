package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;

import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.MessageFormatter;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.library.DefaultStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "cx:message",
        type = "{http://xmlcalabash.com/ns/extensions}message")

public class Message extends DefaultStep {
    private static final QName _message = new QName("","message");
    private static final QName _log = new QName("","log");
    private ReadablePipe source = null;
    private WritablePipe result = null;

    /*
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
        RuntimeValue loglevel = getOption(_log);

        if (loglevel == null) {
            System.err.println("Message: " + message);
        } else {
            String level = loglevel.getString();
            if ("error".equals(level)) {
                logger.error(message);
            } else if ("warn".equals(level)) {
                logger.warn(message);
            } else if ("info".equals(level)) {
                logger.info(message);
            } else if ("debug".equals(level)) {
                logger.debug(message);
            } else if ("trace".equals(level)) {
                logger.trace(message);
            } else {
                logger.warn("Unrecognized cx:message log level: " + level);
                logger.warn(message);
            }
        }

        while (source.moreDocuments()) {
            XdmNode doc = source.read();
            logger.trace(MessageFormatter.nodeMessage(step.getNode(),
                    "Message step " + step.getName() + " read " + doc.getDocumentURI()));
            result.write(doc);
        }
    }
}