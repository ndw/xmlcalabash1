package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.RebasedDocument;
import com.xmlcalabash.util.URIUtils;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import java.net.URI;
import java.util.HashSet;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "cx:collection-manager",
        type = "{http://xmlcalabash.com/ns/extensions}collection-manager")

public class CollectionManager extends DefaultStep {
    private static final QName _href = new QName("","href");
    private ReadablePipe source = null;
    private WritablePipe result = null;

    /*
     * Creates a new instance of CollectionManager
     */
    public CollectionManager(XProcRuntime runtime, XAtomicStep step) {
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

        URI href = step.getNode().getBaseURI().resolve(getOption(_href).getString());

        HashSet<Integer> seenIds = new HashSet<>();
        Vector<XdmNode> collection = new Vector<XdmNode> ();
        while (source.moreDocuments()) {
            XdmNode doc = source.read();
            int id = URIUtils.uniqueId(doc.getDocumentURI().toString());
            if (seenIds.contains(id)) {
                doc = RebasedDocument.makeUniqueDocumentId(doc);
                seenIds.add(URIUtils.uniqueId(doc.getDocumentURI().toString()));
            } else {
                seenIds.add(id);
            }

            collection.add(doc);
        }

        if (collection.size() == 0) {
            collection = runtime.getCollection(href);
        } else {
            runtime.setCollection(href, collection);
        }

        if (collection != null) {
            for (XdmNode doc : collection) {
                result.write(doc);
            }
        }
    }
}