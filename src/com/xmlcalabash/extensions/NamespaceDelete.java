package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class NamespaceDelete extends DefaultStep {
    private static final QName _prefixes = new QName("","prefixes");
    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of NamespaceDelete
     */
    public NamespaceDelete(XProcRuntime runtime, XAtomicStep step) {
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

        HashSet<String> excludeUris = S9apiUtils.excludeInlinePrefixes(step.getNode(), getOption(_prefixes).getString());

        while (source.moreDocuments()) {
            XdmNode doc = source.read();
            runtime.finest(this, step.getNode(), "Namespace-delete step " + step.getName() + " read " + doc.getDocumentURI());
            doc = S9apiUtils.removeNamespaces(runtime, doc, excludeUris, false);
            result.write(doc);
        }
    }
}