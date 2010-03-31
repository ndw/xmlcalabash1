package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceIterator;
import net.sf.saxon.om.NodeInfo;
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

        HashSet excludeUris = readPrefixes(step.getNode(), getOption(_prefixes).getString());

        while (source.moreDocuments()) {
            XdmNode doc = source.read();
            runtime.finest(this, step.getNode(), "Namespace-delete step " + step.getName() + " read " + doc.getDocumentURI());
            doc = S9apiUtils.removeNamespaces(runtime, doc, excludeUris);
            result.write(doc);
        }
    }

    private HashSet<String> readPrefixes(XdmNode node, String prefixList) {
        HashSet<String> excludeURIs = new HashSet<String> ();
        excludeURIs.add(XProcConstants.NS_XPROC);

        if (prefixList != null) {
            // FIXME: Surely there's a better way to do this?
            NodeInfo inode = node.getUnderlyingNode();
            NamePool pool = inode.getNamePool();
            int inscopeNS[] = NamespaceIterator.getInScopeNamespaceCodes(inode);

            for (String pfx : prefixList.split("\\s+")) {
                boolean found = false;

                for (int pos = 0; pos < inscopeNS.length; pos++) {
                    int ns = inscopeNS[pos];
                    String nspfx = pool.getPrefixFromNamespaceCode(ns);
                    String nsuri = pool.getURIFromNamespaceCode(ns);

                    if (pfx.equals(nspfx) || ("#default".equals(pfx) && "".equals(nspfx)) || "#all".equals(pfx)) {
                        found = true;
                        excludeURIs.add(nsuri);
                    }
                }

                if (!found) {
                    throw new XProcException(XProcConstants.staticError(57), "No binding for '" + pfx + ":'");
                }
            }
        }

        return excludeURIs;
    }
}