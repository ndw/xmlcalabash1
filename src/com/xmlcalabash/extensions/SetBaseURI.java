package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.RelevantNodes;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.*;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class SetBaseURI extends DefaultStep {
    private static final QName _uri = new QName("","uri");
    private XProcRuntime runtime = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of SetBaseURI
     */
    public SetBaseURI(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
        this.runtime = runtime;
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

        String uris = getOption(_uri, (String) null);
        if (uris == null) {
            throw new XProcException("URI is required");
        }

        URI uri = getOption(_uri).getBaseURI().resolve(uris);

        XdmNode doc = source.read();
        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(uri);

        for (XdmNode node : new RelevantNodes(doc, Axis.CHILD, true)) {
            tree.addSubtree(node);
        }

        tree.endDocument();

        result.write(tree.getResult());
    }
}
