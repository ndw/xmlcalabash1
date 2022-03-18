/*
 * ProcessMatch.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.ComplexContentOutputter;
import net.sf.saxon.event.NamespaceReducer;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.NameOfNode;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.sxpath.XPathDynamicContext;
import net.sf.saxon.sxpath.XPathEvaluator;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.trans.XPathException;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author ndw
 */
public class ProcessMatch extends TreeWriter {
    public static final int SAW_ELEMENT = 1;
    public static final int SAW_WHITESPACE = 2;
    public static final int SAW_TEXT = 4;
    public static final int SAW_PI = 8;
    public static final int SAW_COMMENT = 16;
    private ProcessMatchingNodes processor = null;
    private int saw = 0;
    private XPathExpression matcher = null;
    private Configuration saxonConfig = null;
    private int count;

    /*
     * Creates a new instance of ProcessMatch
     */
    public ProcessMatch(XProcRuntime runtime, ProcessMatchingNodes processor) {
        super(runtime);
        this.runtime = runtime;
        this.processor = processor;
        saxonConfig = runtime.getProcessor().getUnderlyingConfiguration();
    }

    public void match(XdmNode doc, RuntimeValue match) {
        XdmNode node = match.getNode();

        try {
            XPathEvaluator xeval = new XPathEvaluator(saxonConfig);
            NamespaceResolver resolver = new MatchingNamespaceResolver(match.getNamespaceBindings());
            xeval.getStaticContext().setNamespaceResolver(resolver);

            matcher = xeval.createPattern(match.getString());

            destination = new XdmDestination();
            PipelineConfiguration pipe = controller.makePipelineConfiguration();
            receiver = destination.getReceiver(pipe, runtime.getDefaultSerializationProperties());

            receiver.setPipelineConfiguration(pipe);
            receiver.setSystemId(doc.getBaseURI().toASCIIString());

            receiver = new ComplexContentOutputter(new NamespaceReducer(receiver));

            receiver.open();

            // If we start a match at an element, fake a document wrapper so that
            // a sequence of nodes is returned correctly...
            if (doc.getNodeKind() != XdmNodeKind.DOCUMENT) {
                startDocument(doc.getBaseURI());
            }
            traverse(doc);
            if (doc.getNodeKind() != XdmNodeKind.DOCUMENT) {
                endDocument();
            }

            receiver.close();
        } catch (XProcException e) {
            throw e;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("syntax error")) {
                throw XProcException.dynamicError(23,node,e,"Syntax error in match pattern: \"" + match.getString() + "\"");
            } else {
                throw new XProcException(e);
            }
        }
    }

    // We've already done a bunch of setup, don't do it again!
    public void startDocument(URI baseURI) {
        inDocument = true;
        seenRoot = false;
        try {
            receiver.startDocument(0);
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }

    public int count(XdmNode doc, RuntimeValue match, boolean deep) {
        count = 0;

        try {
            XPathEvaluator xeval = new XPathEvaluator(saxonConfig);
            NamespaceResolver resolver = new MatchingNamespaceResolver(match.getNamespaceBindings());
            xeval.getStaticContext().setNamespaceResolver(resolver);

            matcher = xeval.createPattern(match.getString());

            traverse(doc, deep);
        } catch (XProcException e) {
            throw e;
        } catch (Exception e) {
            throw new XProcException(e);
        }

        return count;
    }

    public XdmNode getResult() {
        return destination.getXdmNode();
    }

    public boolean matches(XdmNode node) {
        try {
            XPathDynamicContext context = matcher.createDynamicContext(node.getUnderlyingNode());
            return matcher.effectiveBooleanValue(context);
        } catch (XPathException sae) {
            return false;
        }
    }

    private void traverse(XdmNode node) throws SaxonApiException, XPathException {
        boolean match = matches(node);
        boolean processChildren = false;

        if (!match) {
            if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                saw |= SAW_ELEMENT;
            } else if (node.getNodeKind() == XdmNodeKind.TEXT) {
                if ("".equals(node.getStringValue().trim())) {
                    saw |= SAW_WHITESPACE;
                } else {
                    saw |= SAW_TEXT;
                }
            } else if (node.getNodeKind() == XdmNodeKind.COMMENT) {
                saw |= SAW_COMMENT;
            } else if (node.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION) {
                saw |= SAW_PI;
            }
        }

        if (node.getNodeKind() == XdmNodeKind.DOCUMENT) {
            if (match) {
                processChildren = processor.processStartDocument(node);
                saw = 0;
            } else {
                startDocument(node.getBaseURI());
            }

            if (!match || processChildren) {
                XdmSequenceIterator<XdmNode> iter = node.axisIterator(Axis.CHILD);
                while (iter.hasNext()) {
                    XdmNode child = iter.next();
                    traverse(child);
                }
            }

            if (match) {
                processor.processEndDocument(node);
            } else {
                endDocument();
            }
        } else if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
            AttributeMap allAttributes = node.getUnderlyingNode().attributes();
            ArrayList<AttributeInfo> matchingAttributes = new ArrayList<>();
            ArrayList<AttributeInfo> nonMatchingAttributes = new ArrayList<>();

            XdmSequenceIterator<XdmNode> iter = node.axisIterator(Axis.ATTRIBUTE);
            while (iter.hasNext()) {
                XdmNode child = iter.next();
                NodeName name = NameOfNode.makeName(child.getUnderlyingNode());
                AttributeInfo attr = allAttributes.get(name);
                if (matches(child)) {
                    matchingAttributes.add(attr);
                } else {
                    nonMatchingAttributes.add(attr);
                }
            }

            if (!matchingAttributes.isEmpty()) {
                AttributeMap processed = processor.processAttributes(node,
                        AttributeMap.fromList(matchingAttributes),
                        AttributeMap.fromList(nonMatchingAttributes));
                if (processed != null) {
                    allAttributes = processed;
                }
            }

            if (match) {
                processChildren = processor.processStartElement(node, allAttributes);
                saw = 0;
            } else {
                addStartElement(node, allAttributes);
            }

            if (!match || processChildren) {
                iter = node.axisIterator(Axis.CHILD);
                while (iter.hasNext()) {
                    XdmNode child = iter.next();
                    traverse(child);
                }
            }

            if (match) {
                processor.processEndElement(node);
            } else {
                addEndElement();
            }
        } else if (node.getNodeKind() == XdmNodeKind.COMMENT) {
            if (match) {
                processor.processComment(node);
                saw = 0;
            } else {
                addComment(node.getStringValue());
            }
        } else if (node.getNodeKind() == XdmNodeKind.TEXT) {
            if (match) {
                processor.processText(node);
                saw = 0;
            } else {
                addText(node.getStringValue());
            }
        } else if (node.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION) {
            if (match) {
                processor.processPI(node);
                saw = 0;
            } else {
                addPI(node.getNodeName().getLocalName(), node.getStringValue());
            }
        } else {
            throw new UnsupportedOperationException("Unexpected node type");
        }
    }

    private void traverse(XdmNode node, boolean deep) {
        boolean match = matches(node);

        if (match) {
            count++;
        }

        if (node.getNodeKind() == XdmNodeKind.DOCUMENT) {
            if (!match || deep) {
                XdmSequenceIterator<XdmNode> iter = node.axisIterator(Axis.CHILD);
                while (iter.hasNext()) {
                    XdmNode child = iter.next();
                    traverse(child, deep);
                }
            }
        } else if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
            if (!match || deep) {
                XdmSequenceIterator<XdmNode> iter = node.axisIterator(Axis.ATTRIBUTE);
                while (iter.hasNext()) {
                    XdmNode child = iter.next();
                    traverse(child, deep);
                }
                iter = node.axisIterator(Axis.CHILD);
                while (iter.hasNext()) {
                    XdmNode child = iter.next();
                    traverse(child, deep);
                }
            }
        } else {
            //nop
        }
    }

    private static class MatchingNamespaceResolver implements NamespaceResolver {
        private HashMap<String,String> ns = new HashMap<String,String> ();

        public MatchingNamespaceResolver(HashMap<String,String> bindings) {
            ns = bindings;
        }

        public String getURIForPrefix(String prefix, boolean useDefault) {
            if ("".equals(prefix) && !useDefault) {
                return "";
            }

            return ns.get(prefix);
        }

        public Iterator<String> iteratePrefixes() {
            Vector<String> p = new Vector<String> ();
            p.addAll(ns.keySet());
            return p.iterator();
        }
    }
}
