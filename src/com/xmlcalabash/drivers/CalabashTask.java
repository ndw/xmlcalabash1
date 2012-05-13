package com.xmlcalabash.drivers;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.S9apiUtils;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.InputSource;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import java.io.FileOutputStream;
import java.net.URI;
import java.util.*;
import javax.xml.transform.sax.SAXSource;

/**
 * The task of the tutorial.
 * Prints a message or let the build fail.
 * @author Jan Materne
 * @since 2003-08-19
 */
public class CalabashTask extends Task {

    /** The message to print. As attribute. */
    String message;
    public void setMessage(String msg) {
        message = msg;
    }

    /** Should the build fail? Defaults to <i>false</i>. As attribute. */
    boolean fail = false;
    public void setFail(boolean b) {
        fail = b;
    }

    /** Support for nested text. */
    public void addText(String text) {
        message = text;
    }


    /** Do the work. */
    public void execute() {
        XProcConfiguration config = new XProcConfiguration("he", false);
        XProcRuntime runtime = new XProcRuntime(config);

	try {
	    XPipeline pipeline = runtime.load("pipeline.xpl");
	    XdmNode doc = runtime.parse(new InputSource("in.xml"));
	    pipeline.writeTo("source", doc);
	    pipeline.run();

            for (String port : pipeline.getOutputs()) {
                String uri = null;
		/*
                if (cmd.outputs.containsKey(port)) {
                    uri = cmd.outputs.get(port);
                } else if (config.outputs.containsKey(port)) {
                    uri = config.outputs.get(port);
                }

                if (port.equals(stdio)) {
                    finest(logger, null, "Copy output from " + port + " to stdout");
                    uri = null;
                } else if (uri == null) {
                    // You didn't bind it, and it isn't going to stdout, so it's going into the bit bucket.
                    continue;
                } else {
                    finest(logger, null, "Copy output from " + port + " to " + uri);
                }
		*/
                Serialization serial = pipeline.getSerialization(port);

                if (serial == null) {
                    // Use the configuration options
                    // FIXME: should each of these be considered separately?
                    // FIXME: should there be command-line options to override these settings?
                    serial = new Serialization(runtime, pipeline.getNode()); // The node's a hack
                    for (String name : config.serializationOptions.keySet()) {
                        String value = config.serializationOptions.get(name);

                        if ("byte-order-mark".equals(name)) serial.setByteOrderMark("true".equals(value));
                        if ("escape-uri-attributes".equals(name)) serial.setEscapeURIAttributes("true".equals(value));
                        if ("include-content-type".equals(name)) serial.setIncludeContentType("true".equals(value));
                        if ("indent".equals(name)) serial.setIndent("true".equals(value));
                        if ("omit-xml-declaration".equals(name)) serial.setOmitXMLDeclaration("true".equals(value));
                        if ("undeclare-prefixes".equals(name)) serial.setUndeclarePrefixes("true".equals(value));
                        if ("method".equals(name)) serial.setMethod(new QName("", value));

                        // FIXME: if ("cdata-section-elements".equals(name)) serial.setCdataSectionElements();
                        if ("doctype-public".equals(name)) serial.setDoctypePublic(value);
                        if ("doctype-system".equals(name)) serial.setDoctypeSystem(value);
                        if ("encoding".equals(name)) serial.setEncoding(value);
                        if ("media-type".equals(name)) serial.setMediaType(value);
                        if ("normalization-form".equals(name)) serial.setNormalizationForm(value);
                        if ("standalone".equals(name)) serial.setStandalone(value);
                        if ("version".equals(name)) serial.setVersion(value);
                    }
                }

                // I wonder if there's a better way...
                WritableDocument wd = null;
                if (uri != null) {
                    URI furi = new URI(uri);
                    String filename = furi.getPath();
                    FileOutputStream outfile = new FileOutputStream(filename);
                    wd = new WritableDocument(runtime,filename,serial,outfile);
                } else {
                    wd = new WritableDocument(runtime,uri,serial);
                }

                ReadablePipe rpipe = pipeline.readFrom(port);
                while (rpipe.moreDocuments()) {
                    wd.write(rpipe.read());
                }

                if (uri!=null) {
		    wd.close();
                }
            }
	} catch (Exception err) {
	    throw new BuildException("Pipeline failed: " + err.toString(), err);
	}
        // handle attribute 'fail'
        if (fail) throw new BuildException("Fail requested.");

        // handle attribute 'message' and nested text
        if (message!=null) log(message);

        // handle nested elements
        for (Iterator it=messages.iterator(); it.hasNext(); ) {
            Message msg = (Message)it.next();
            log(msg.getMsg());
        }
    }


    /** Store nested 'message's. */
    Vector messages = new Vector();

    /** Factory method for creating nested 'message's. */
    public Message createMessage() {
        Message msg = new Message();
        messages.add(msg);
        return msg;
    }

    /** A nested 'message'. */
    public class Message {
        // Bean constructor
        public Message() {}

        /** Message to print. */
        String msg;
        public void setMsg(String msg) { this.msg = msg; }
        public String getMsg() { return msg; }
    }

}