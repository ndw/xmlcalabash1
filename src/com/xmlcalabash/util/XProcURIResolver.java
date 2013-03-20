package com.xmlcalabash.util;

import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.URIResolver;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.Configuration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 29, 2008
 * Time: 4:04:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class XProcURIResolver implements URIResolver, EntityResolver, UnparsedTextURIResolver {
    private URIResolver uriResolver = null;
    private EntityResolver entityResolver = null;
    private UnparsedTextURIResolver unparsedTextResolver = null;
    private XProcRuntime runtime = null;
    private Hashtable<String,XdmNode> cache = new Hashtable<String,XdmNode> ();
    private static boolean useCache = true; // FIXME: this is supposed to be temporary!

    public XProcURIResolver(XProcRuntime runtime) {
        this.runtime = runtime;
    }

    public void setUnderlyingURIResolver(URIResolver resolver) {
        uriResolver = resolver;
    }

    public void setUnderlyingEntityResolver(EntityResolver resolver) {
        entityResolver = resolver;
    }

    public void setUnderlyingUnparsedTextURIResolver(UnparsedTextURIResolver resolver) {
        unparsedTextResolver = resolver;
    }

    public void cache(XdmNode doc, URI baseURI) {
        XdmNode root = S9apiUtils.getDocumentElement(doc);

        // We explicitly use the base URI of the root element so that if it has an xml:base
        // attribute, that becomes the base URI of the document.

        URI docURI = baseURI.resolve(root.getBaseURI());
        if (useCache) {
            cache.put(docURI.toASCIIString(), doc);
        }
    }

    public Source resolve(String href, String base) throws TransformerException {
        runtime.finest(null,null,"URIResolver(" + href + "," + base + ")");

        String uri = null;
        if (base == null) {
            try {
                URL url = new URL(href);
                uri = url.toURI().toASCIIString();
            } catch (MalformedURLException mue) {
                runtime.finest(null,null,"MalformedURLException on " + href);
            } catch (URISyntaxException use) {
                runtime.finest(null,null,"URISyntaxException on " + href);
            }
        } else {
            try {
                URI baseURI = new URI(base);
                uri = baseURI.resolve(href).toASCIIString();
            } catch (URISyntaxException use) {
                runtime.finest(null,null,"URISyntaxException resolving base and href: " + base + " : " + href);
            }
        }

        runtime.finest(null,null,"Resolved: " + uri);

        if (cache.containsKey(uri)) {
            runtime.finest(null ,null,"Returning cached document.");
            return cache.get(uri).asSource();
        }

        if (uriResolver != null) {
            runtime.finest(null,null,"uriResolver.resolve(" + href + "," + base + ")");
            Source resolved = uriResolver.resolve(href, base);

            // FIXME: This is a grotesque hack. This is wrong. Wrong. Wrong.
            // To support caching, XMLResolver (xmlresolver.org) returns a Source even when it hasn't
            // found the resource. Problem is, it doesn't setup the entity resolver correctly for that
            // resource. So we hack at it here...
            if (resolved != null && resolved instanceof SAXSource) {
                SAXSource ssource = (SAXSource) resolved;
                XMLReader reader = ssource.getXMLReader();
                if (reader == null) {
                    try {
                        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
                        saxParserFactory.setNamespaceAware(true); // Must be namespace aware
                        reader = saxParserFactory.newSAXParser().getXMLReader();
                        reader.setEntityResolver(this);
                        ssource.setXMLReader(reader);
                    } catch (SAXException se) {
                        // nop?
                    } catch (ParserConfigurationException pce) {
                        // nop?
                    }
                }
            }
            return resolved;
        }
        
        return null;
    }

    public XdmNode parse(String href, String base) {
        return parse(href, base, false);
    }

    public XdmNode parse(String href, String base, boolean dtdValidate) {
        Source source = null;
        href = URIUtils.encode(href);
        runtime.finest(null,null,"Attempting to parse: " + href + " (" + base + ")");

        try {
            source = resolve(href, base);
        } catch (TransformerException te) {
            throw new XProcException(XProcConstants.dynamicError(9), te);
        }

        if (source == null) {
            try {
                URI baseURI = new URI(base);
                URI resURI = baseURI.resolve(href);
                source = new SAXSource(new InputSource(resURI.toASCIIString()));

                XMLReader reader = ((SAXSource) source).getXMLReader();
                if (reader == null) {
                    try {
                        reader = XMLReaderFactory.createXMLReader();
                        ((SAXSource) source).setXMLReader(reader);
                        reader.setEntityResolver(this);
                    } catch (SAXException se) {
                        // nop?
                    }
                }
            } catch (URISyntaxException use) {
                throw new XProcException(use);
            }
        }

        DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
        builder.setDTDValidation(dtdValidate);
        builder.setLineNumbering(true);

        try {
            return builder.build(source);
        } catch (SaxonApiException sae) {
            String msg = sae.getMessage();
            if (msg.contains("validation")) {
                throw XProcException.stepError(27, sae);
            } else if (msg.contains("HTTP response code: 403 ")) {
                throw XProcException.dynamicError(21);
            } else {
                throw XProcException.dynamicError(11, sae);
            }
        }
    }

    public XdmNode parse(InputSource isource) {
        try {
            // Make sure the builder uses our entity resolver
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setEntityResolver(this);
            SAXSource source = new SAXSource(reader, isource);
            DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
            builder.setLineNumbering(true);
            builder.setDTDValidation(false);
            return builder.build(source);
        } catch (SaxonApiException sae) {
            String msg = sae.getMessage();
            if (msg.contains("validation")) {
                throw XProcException.stepError(27, sae);
            } else if (msg.contains("HTTP response code: 403 ")) {
                throw XProcException.dynamicError(21);
            } else {
                throw XProcException.dynamicError(11, sae);
            }
        } catch (SAXException e) {
            throw new XProcException(e);
        }
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        runtime.finest(null,null,"ResolveEntity(" + publicId + "," + systemId + ")");

        if (systemId == null) {
            return null;
        }

        try {
            URI baseURI = new URI(systemId);
            String uri = baseURI.toASCIIString();
            if (cache.containsKey(uri)) {
                runtime.finest(null,null,"Returning cached document.");
                return S9apiUtils.xdmToInputSource(runtime, cache.get(uri));
            }
        } catch (URISyntaxException use) {
            runtime.finest(null,null,"URISyntaxException resolving entityResolver systemId: " + systemId);
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        if (entityResolver != null) {
            InputSource r = entityResolver.resolveEntity(publicId, systemId);
            return r;
        } else {
            return null;
        }
    }

    @Override
    public Reader resolve(URI uri, String s, Configuration configuration) throws XPathException {
        if (unparsedTextResolver != null) {
            return unparsedTextResolver.resolve(uri, s, configuration);
        }

        // Ack. Apparently I have to do this if there isn't a resolver...
        try {
            URL url = uri.toURL();
            URLConnection conn = url.openConnection();
            InputStream stream = conn.getInputStream();
            return new InputStreamReader(stream);
        } catch (Exception e) {
            throw new XPathException(e);
        }
    }
}
