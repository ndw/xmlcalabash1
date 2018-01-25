package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcRuntime;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.CollectionFinder;
import net.sf.saxon.lib.Resource;
import net.sf.saxon.lib.ResourceCollection;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SpaceStrippingRule;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 3, 2008
 * Time: 5:44:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class XProcCollectionFinder implements CollectionFinder {
    public static final String DEFAULT = "http://xmlcalabash.com/saxon-default-collection";
    protected Logger logger = LoggerFactory.getLogger(XProcCollectionFinder.class);

    XProcRuntime runtime = null;
    Vector<XdmNode> docs = null;
    CollectionFinder chainedFinder = null;

    public XProcCollectionFinder(XProcRuntime runtime, Vector<XdmNode> docs, CollectionFinder chainedFinder) {
        this.runtime = runtime;
        this.docs = docs;
        this.chainedFinder = chainedFinder;
    }

    @Override
    public ResourceCollection findCollection(XPathContext context, String collectionURI) throws XPathException {
        logger.trace("Collection: " + collectionURI);

        if (collectionURI == null) {
            collectionURI = DEFAULT;
        }

        if (collectionURI.equals(DEFAULT)) {
            return new DocumentResourceCollection(collectionURI, docs);
        } else {
            try {
                URI cURI = new URI(collectionURI);
                Vector<XdmNode> docs = runtime.getCollection(cURI);
                if (docs != null) {
                    return new DocumentResourceCollection(collectionURI, docs);
                }
            } catch (URISyntaxException use) {
                logger.trace("URI Syntax exception resolving collection URI: " + collectionURI);
            }

            return chainedFinder.findCollection(context, collectionURI);
        }
    }

    private class DocumentResourceCollection implements ResourceCollection {
        private Vector<XdmNode> docs = null;
        private Vector<String> uris = null;
        private Vector<Resource> rsrcs = null;
        private String collectionURI = null;

        public DocumentResourceCollection(String collectionURI, Vector<XdmNode> docs) {
            this.collectionURI = collectionURI;
            this.docs = docs;
        }

        @Override
        public String getCollectionURI() {
            return collectionURI;
        }

        @Override
        public Iterator<String> getResourceURIs(XPathContext context) throws XPathException {
            if (uris == null) {
                uris = new Vector<String> ();
                for (XdmNode doc : docs) {
                    uris.add(doc.getBaseURI().toASCIIString());
                }
            }
            return uris.iterator();
        }

        @Override
        public Iterator<? extends Resource> getResources(XPathContext context) throws XPathException {
            if (rsrcs == null) {
                rsrcs = new Vector<Resource> ();
                for (XdmNode doc : docs) {
                    rsrcs.add(new DocumentResource(doc));
                }
            }

            return rsrcs.iterator();
        }

        @Override
        public boolean isStable(XPathContext context) {
            return true;
        }

        @Override
        public boolean stripWhitespace(SpaceStrippingRule rules) {
            return false;
        }
    }

    private class DocumentResource implements Resource {
        private XdmNode doc = null;

        public DocumentResource(XdmNode doc) {
            this.doc = doc;
        }

        @Override
        public String getResourceURI() {
            return doc.getBaseURI().toASCIIString();
        }

        @Override
        public Item getItem(XPathContext context) throws XPathException {
            return doc.getUnderlyingValue().head();
        }

        @Override
        public String getContentType() {
            return null;
        }
    }

}
