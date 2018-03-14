package com.xmlcalabash.util;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ModuleURIResolver;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;

public class XProcURIResolverX implements URIResolver, EntityResolver, ModuleURIResolver, UnparsedTextURIResolver {
    private static XProcURIResolver realResolver = null;

    public XProcURIResolverX() {
        // no one can do this
    }

    public void setRealResolver(XProcURIResolver resolver) {
        realResolver = resolver;
    }

    public Source resolve(String href, String base) throws TransformerException {
        return realResolver.resolve(href, base);
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return realResolver.resolveEntity(publicId, systemId);
    }

    @Override
    public StreamSource[] resolve(String moduleURI, String baseURI, String[] locations)
            throws XPathException {
        return realResolver.resolve(moduleURI, baseURI, locations);
   }

    @Override
    public Reader resolve(URI uri, String encoding, Configuration configuration) throws XPathException {
        return realResolver.resolve(uri, encoding, configuration);
    }
}
