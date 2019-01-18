package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.Vector;

@XMLCalabash(
        name = "cx:commonmark",
        type = "{http://xmlcalabash.com/ns/extensions}commonmark")

public class CommonMark extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName h_body = new QName("http://www.w3.org/1999/xhtml", "body");

    private WritablePipe result = null;

    public CommonMark(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        if (runtime.getSafeMode()) {
            throw XProcException.dynamicError(21);
        }

        RuntimeValue href = getOption(_href);

        try {
            DataStore store = runtime.getDataStore();
            store.readEntry(href.getString(), href.getBaseURI().toASCIIString(), "text/*, */*", null, new DataStore.DataReader() {
                public void load(URI id, String media, InputStream content, long len) throws IOException {
                    Reader rdr = new InputStreamReader(content);
                    Parser parser = Parser.builder().build();
                    Node document = parser.parseReader(rdr);
                    HtmlRenderer renderer = HtmlRenderer.builder().build();

                    // We rely on the fact that the CommonMark parser returns well-formed markup consisting
                    // of the paragraphs and other bits that would occur inside a <body> element and
                    // that it returns them with no namespace declarations.
                    String markup = "<body xmlns='http://www.w3.org/1999/xhtml'>" + renderer.render(document) + "</body>";
                    XdmNode parsed = runtime.parse(new InputSource(new StringReader(markup)));

                    // Let's craft a baseURI for the document...
                    String path = id.getPath();
                    int lastSlash = path.lastIndexOf("/");
                    String base = path.substring(0, lastSlash+1);
                    String filename = path.substring(lastSlash+1);
                    int lastDot = filename.lastIndexOf(".");
                    if (lastDot > 0) {
                        base = base + filename.substring(0, lastDot) + ".html";
                    } else {
                        base = base + filename + ".html";
                    }
                    URI baseURI = id.resolve(base);

                    TreeWriter tree = new TreeWriter(runtime);
                    tree.startDocument(baseURI);
                    tree.addSubtree(parsed);
                    tree.endDocument();

                    result.write(tree.getResult());
                }
            });
        } catch (FileNotFoundException fnfe) {
            URI uri = href.getBaseURI().resolve(href.getString());
            throw new XProcException(href.getNode(), "Cannot read: file does not exist: " + uri.toASCIIString());
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }
}
