package com.xmlcalabash.util;

import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.s9api.XdmNode;

import java.util.Vector;
import java.util.logging.Logger;
import java.net.URI;
import java.net.URISyntaxException;

import com.xmlcalabash.core.XProcRuntime;
import net.sf.saxon.tree.iter.ArrayIterator;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 3, 2008
 * Time: 5:44:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class CollectionResolver implements CollectionURIResolver {
    XProcRuntime runtime = null;
    Vector<XdmNode> docs = null;
    CollectionURIResolver chainedResolver = null;
    protected Logger logger = Logger.getLogger(this.getClass().getName());

    public CollectionResolver(XProcRuntime runtime, Vector<XdmNode> docs, CollectionURIResolver chainedResolver) {
        this.runtime = runtime;
        this.docs = docs;
        this.chainedResolver = chainedResolver;
    }

    public SequenceIterator<?> resolve(String href, String base, XPathContext context) throws XPathException {
        runtime.finest(null, null, "Collection: " + href + " (" + base + ")");
        if (href == null) {
            Item<?>[] array = new Item<?>[docs.size()];
            for (int pos = 0; pos < docs.size(); pos++) {
                array[pos] = docs.get(pos).getUnderlyingNode();
            }
            return new ArrayIterator<Item<?>>(array);
        } else {
            try {
                URI hrefuri;

                if (base == null) {
                    hrefuri = new URI(href);
                } else {
                    hrefuri = new URI(base).resolve(href);
                }
                Vector<XdmNode> docs = runtime.getCollection(hrefuri);
                if (docs != null) {
                    Item<?>[] items = new Item<?>[docs.size()];
                    for (int pos = 0; pos < docs.size(); pos++) {
                        items[pos] = docs.get(pos).getUnderlyingNode();
                    }
                    return new ArrayIterator<Item<?>>(items);
                }
            } catch (URISyntaxException use) {
                runtime.finest(null, null, "URI Syntax exception resolving collection URI: " + href + " (" + base + ")");
            }

            return chainedResolver.resolve(href,base,context);
        }
    }
}
