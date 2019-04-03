/*
 * XSLT.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * Portions Copyright 2007 Sun Microsystems, Inc.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * https://xproc.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 */

package com.xmlcalabash.library;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.RebasedDocument;
import com.xmlcalabash.util.RebasedNode;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.XProcCollectionFinder;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.CollectionFinder;
import net.sf.saxon.lib.OutputURIResolver;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.s9api.Action;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.MessageListener;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.RawDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.ValidationMode;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.xml.sax.InputSource;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.function.Function;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:xslt",
        type = "{http://www.w3.org/ns/xproc}xslt")

public class XSLT extends DefaultStep {
    private static final QName _initial_mode = new QName("", "initial-mode");
    private static final QName _template_name = new QName("", "template-name");
    private static final QName _output_base_uri = new QName("", "output-base-uri");
    private static final QName _version = new QName("", "version");
    private static final QName _content_type = new QName("content-type");
    private static final QName cx_decode = new QName("cx", XProcConstants.NS_CALABASH_EX, "decode");
    private ReadablePipe sourcePipe = null;
    private ReadablePipe stylesheetPipe = null;
    private WritablePipe resultPipe = null;
    private WritablePipe secondaryPipe = null;
    private Hashtable<QName, RuntimeValue> params = new Hashtable<QName, RuntimeValue>();

    /*
     * Creates a new instance of XSLT
     */
    public XSLT(XProcRuntime runtime, XAtomicStep step) {
        super(runtime, step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            sourcePipe = pipe;
        } else {
            stylesheetPipe = pipe;
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        if ("result".equals(port)) {
            resultPipe = pipe;
        } else {
            secondaryPipe = pipe;
        }
    }

    public void setParameter(QName name, RuntimeValue value) {
        params.put(name, value);
    }

    public void reset() {
        sourcePipe.resetReader();
        stylesheetPipe.resetReader();
        resultPipe.resetWriter();
        secondaryPipe.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        XdmNode stylesheet = stylesheetPipe.read();
        if (stylesheet == null) {
            throw XProcException.dynamicError(6, step.getNode(), "No stylesheet provided.");
        }

        Vector<XdmNode> defaultCollection = new Vector<XdmNode>();

        while (sourcePipe.moreDocuments()) {
            defaultCollection.add(sourcePipe.read());
        }

        XdmNode document = null;
        if (defaultCollection.size() > 0) {
            document = defaultCollection.firstElement();
        }

        String version = null;
        if (getOption(_version) == null) {
            XdmNode ssroot = S9apiUtils.getDocumentElement(stylesheet);
            if (ssroot != null) {
                version = ssroot.getAttributeValue(new QName("", "version"));
                if (version == null) {
                    version = ssroot.getAttributeValue(new QName("http://www.w3.org/1999/XSL/Transform", "version"));
                }
            }
            if (version == null) {
                version = "2.0"; // WTF?
            }
        } else {
            version = getOption(_version).getString();
        }

        // We used to check if the XSLT version was supported, but I've removed that check.
        // If it's not supported by Saxon, we'll get an error from Saxon. Otherwise, we'll
        // get the results we get.

        if ("1.0".equals(version) && defaultCollection.size() > 1) {
            throw XProcException.stepError(39);
        }

        if ("1.0".equals(version) && runtime.getUseXslt10Processor()) {
            run10(stylesheet, document);
            return;
        }

        QName initialMode = null;
        QName templateName = null;
        String outputBaseURI = null;

        RuntimeValue opt = getOption(_initial_mode);
        if (opt != null) {
            initialMode = opt.getQName();
        }

        opt = getOption(_template_name);
        if (opt != null) {
            templateName = opt.getQName();
        }

        opt = getOption(_output_base_uri);
        if (opt != null) {
            outputBaseURI = opt.getString();
        }

        Processor processor = runtime.getProcessor();
        Configuration config = processor.getUnderlyingConfiguration();

        runtime.getConfigurer().getSaxonConfigurer().configXSLT(config);

        OutputURIResolver uriResolver = config.getOutputURIResolver();
        CollectionFinder collectionFinder = config.getCollectionFinder();
        UnparsedTextURIResolver unparsedTextURIResolver = runtime.getResolver();

        config.setDefaultCollection(XProcCollectionFinder.DEFAULT);
        config.setCollectionFinder(new XProcCollectionFinder(runtime, defaultCollection, collectionFinder));

        RawDestination result = new RawDestination();
        try {
            XsltCompiler compiler = runtime.getProcessor().newXsltCompiler();
            compiler.setSchemaAware(processor.isSchemaAware());
            XsltExecutable exec = compiler.compile(stylesheet.asSource());
            XsltTransformer transformer = exec.load();
            transformer.setResultDocumentHandler(new DocumentHandler());

            for (QName name : params.keySet()) {
                RuntimeValue v = params.get(name);
                if (runtime.getAllowGeneralExpressions()) {
                    transformer.setParameter(name, v.getValue());
                } else {
                    transformer.setParameter(name, v.getUntypedAtomic(runtime));
                }
            }

            if (document != null) {
                transformer.setInitialContextNode(document);
            }
            transformer.setMessageListener(new CatchMessages());
            transformer.setDestination(result);

            if (initialMode != null) {
                transformer.setInitialMode(initialMode);
            }

            if (templateName != null) {
                transformer.setInitialTemplate(templateName);
            }

            if (outputBaseURI != null) {
                transformer.setBaseOutputURI(outputBaseURI);
            }

            transformer.setSchemaValidationMode(ValidationMode.DEFAULT);
            transformer.getUnderlyingController().setUnparsedTextURIResolver(unparsedTextURIResolver);
            transformer.transform();
        } finally {
            config.setCollectionFinder(collectionFinder);
        }

        XdmValue value = result.getXdmValue();
        XdmNode xformed = null;
        if (value != null && value != XdmEmptySequence.getInstance()) {
            // In XProc 1.0, the output from XSLT has to be a document. If we get a node
            // or a sequence of nodes, then make a document out of it. Otherwise, throw
            // an exception. Note: The RawDestination doesn't wrap nodes in a document,
            // so this is always necessary.
            TreeWriter docout = new TreeWriter(runtime);
            if (document == null) {
                docout.startDocument(null);
            } else {
                docout.startDocument(document.getBaseURI());
            }
            for (XdmValue v : value) {
                if (v instanceof XdmNode) {
                    docout.addSubtree((XdmNode) v);
                } else {
                    throw new XProcException(step.getStep(), "p:xslt returned non-XML result");
                }
            }

            xformed = docout.getResult();
        }

        // Can be null when nothing is written to the principle result tree...
        if (xformed != null) {
            if (getOption(_output_base_uri) == null && document != null) {
                // Before Saxon 9.8, it was possible to simply set the base uri of the
                // output document. That became impossible in Saxon 9.8, but I still
                // think there might be XProc pipelines that rely on the fact that the
                // base URI doesn't change when processed by XSLT. So we're doing it
                // the hard way.
                //
                // In Saxon 9.9, I switched to the RawDestination which doesn't have
                // a base URI setter, so this is still necessary.
                BaseURIMapper bmapper = new BaseURIMapper(document.getBaseURI().toASCIIString());
                SystemIdMapper smapper = new SystemIdMapper();
                TreeInfo tree = xformed.getUnderlyingNode().getTreeInfo();
                RebasedDocument rebaser = new RebasedDocument(tree, bmapper, smapper);
                RebasedNode xfixbase = rebaser.wrap(xformed.getUnderlyingNode());
                xformed = new XdmNode(xfixbase);
            }

            // If the document isn't well-formed XML, encode it as text
            try {
                S9apiUtils.assertDocument(xformed);
                resultPipe.write(xformed);
            } catch (XProcException e) {
                // If the document isn't well-formed XML, encode it as text
                if (runtime.getAllowTextResults()) {
                    // Document is apparently not well-formed XML.
                    TreeWriter tree = new TreeWriter(runtime);
                    tree.startDocument(xformed.getBaseURI());
                    tree.addStartElement(XProcConstants.c_result);
                    tree.addAttribute(_content_type, "text/plain");
                    tree.addAttribute(cx_decode, "true");
                    tree.startContent();

                    // Serialize the content as text so that we don't wind up with encoded XML characters
                    Serializer serializer = makeSerializer();
                    serializer.setOutputProperty(Serializer.Property.METHOD, "text");

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    serializer.setOutputStream(baos);
                    try {
                        S9apiUtils.serialize(runtime, xformed, serializer);
                    } catch (SaxonApiException e2) {
                        throw new XProcException(e2);
                    }

                    tree.addText(baos.toString());
                    tree.addEndElement();
                    tree.endDocument();
                    resultPipe.write(tree.getResult());
                } else {
                    throw new XProcException(step.getStep(), "p:xslt returned non-XML result", e.getCause());
                }
            }
        }
    }

    public void run10(XdmNode stylesheet, XdmNode document) {
        try {
            InputSource is = S9apiUtils.xdmToInputSource(runtime, stylesheet);

            TransformerFactory tfactory = TransformerFactory.newInstance();
            Transformer transformer = tfactory.newTransformer(new SAXSource(is));

            transformer.setURIResolver(runtime.getResolver());

            for (QName name : params.keySet()) {
                RuntimeValue v = params.get(name);
                transformer.setParameter(name.getClarkName(), v.getString());
            }

            DOMResult result = new DOMResult();
            is = S9apiUtils.xdmToInputSource(runtime, document);
            transformer.transform(new SAXSource(is), result);

            DocumentBuilder xdmBuilder = runtime.getConfiguration().getProcessor().newDocumentBuilder();
            XdmNode xformed = xdmBuilder.build(new DOMSource(result.getNode()));

            // Can be null when nothing is written to the principle result tree...
            if (xformed != null) {
                // There used to be an attempt to set the system identifier of the xformed
                // document, but that's not allowed in Saxon 9.8.
                resultPipe.write(xformed);
            }
        } catch (SaxonApiException | TransformerException sae) {
            throw new XProcException(sae);
        }
    }

    private class DocumentHandler implements Function<URI, Destination> {
        @Override
        public Destination apply(URI uri) {
            XdmDestination xdmResult = new XdmDestination();
            xdmResult.setBaseURI(uri);
            xdmResult.onClose(new DocumentCloseAction(uri, xdmResult));
            return xdmResult;
        }
    }

    private class BaseURIMapper implements Function<NodeInfo, String> {
        private String origBase = null;

        public BaseURIMapper(String origBase) {
            this.origBase = origBase;
        }

        @Override
        public String apply(NodeInfo node) {
            String base = node.getBaseURI();
            if (origBase != null && (base == null) || "".equals(base)) {
                base = origBase;
            }
            return base;
        }
    }

    private class SystemIdMapper implements Function<NodeInfo, String> {
        // This is a nop for now
        @Override
        public String apply(NodeInfo node) {
            return node.getSystemId();
        }
    }

    private class DocumentCloseAction implements Action {
        private URI uri = null;
        private XdmDestination destination = null;

        public DocumentCloseAction(URI uri, XdmDestination destination) {
            this.uri = uri;
            this.destination = destination;
        }

        @Override
        public void act() throws SaxonApiException {
            XdmNode doc = destination.getXdmNode();

            BaseURIMapper bmapper = new BaseURIMapper(doc.getBaseURI().toASCIIString());
            SystemIdMapper smapper = new SystemIdMapper();
            TreeInfo treeinfo = doc.getUnderlyingNode().getTreeInfo();
            RebasedDocument rebaser = new RebasedDocument(treeinfo, bmapper, smapper);
            RebasedNode xfixbase = rebaser.wrap(doc.getUnderlyingNode());
            doc = new XdmNode(xfixbase);

            try {
                S9apiUtils.assertDocument(doc);
                secondaryPipe.write(doc);
            } catch (XProcException e) {
                // If the document isn't well-formed XML, encode it as text
                if (runtime.getAllowTextResults()) {
                    // Document is apparently not well-formed XML.
                    TreeWriter tree = new TreeWriter(runtime);
                    tree.startDocument(doc.getBaseURI());
                    tree.addStartElement(XProcConstants.c_result);
                    tree.addAttribute(_content_type, "text/plain");
                    tree.addAttribute(cx_decode, "true");
                    tree.startContent();
                    tree.addText(doc.toString());
                    tree.addEndElement();
                    tree.endDocument();
                    secondaryPipe.write(tree.getResult());
                } else {
                    throw new XProcException(step.getStep(), "p:xslt returned non-XML secondary result", e.getCause());
                }
            }
        }
    }

    class CatchMessages implements MessageListener {
        public CatchMessages() {
        }

        public void message(XdmNode content, boolean terminate, javax.xml.transform.SourceLocator locator) {
            if (runtime.getShowMessages()) {
                System.err.println(content.toString());
            }

            TreeWriter treeWriter = new TreeWriter(runtime);
            treeWriter.startDocument(content.getBaseURI());
            treeWriter.addStartElement(XProcConstants.c_error);
            treeWriter.startContent();

            treeWriter.addSubtree(content);

            treeWriter.addEndElement();
            treeWriter.endDocument();

            step.reportError(treeWriter.getResult());
            step.info(step.getNode(), content.toString());
        }
    }
}

