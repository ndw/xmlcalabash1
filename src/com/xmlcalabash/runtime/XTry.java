package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.*;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 13, 2008
 * Time: 7:40:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class XTry extends XCompoundStep {
    private static final QName c_errors = new QName("c", XProcConstants.NS_XPROC_STEP, "errors");
    private static final QName c_error = new QName("c", XProcConstants.NS_XPROC_STEP, "error");
    private static QName _href = new QName("", "href");
    private static QName _line = new QName("", "line");
    private static QName _column = new QName("", "column");
    private static QName _code = new QName("", "code");
    private Vector<XdmNode> errors = new Vector<XdmNode> ();

    public XTry(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }

    public void instantiate(Step step) {
        parent.addStep(this);

        DeclareStep decl = step.getDeclaration();

        for (Step substep : decl.subpipeline()) {
            if (XProcConstants.p_group.equals(substep.getType())) {
                XGroup newstep = new XGroup(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_catch.equals(substep.getType())) {
                XCatch newstep = new XCatch(runtime, substep, this);
                newstep.instantiate(substep);
            } else {
                throw new XProcException(step.getNode(), "This can't happen, can it? try contains something that isn't a group or a catch?");
            }
        }

        for (Output output : step.outputs()) {
            String port = output.getPort();
            if (port.endsWith("|")) {
                String rport = port.substring(0,port.length()-1);
                XInput xinput = getInput(rport);
                WritablePipe wpipe = xinput.getWriter();
                outputs.put(port, wpipe);
                finest(step.getNode(), " writes to " + wpipe + " for " + port);
            } else {
                XOutput xoutput = new XOutput(runtime, output);
                addOutput(xoutput);
                WritablePipe wpipe = xoutput.getWriter();
                outputs.put(port, wpipe);
                finest(step.getNode(), " writes to " + wpipe + " for " + port);
            }
        }
    }

    public void run() throws SaxonApiException {

        inScopeOptions = parent.getInScopeOptions();
        for (Variable var : step.getVariables()) {
            RuntimeValue value = computeValue(var);
            inScopeOptions.put(var.getName(), value);
        }

        XGroup xgroup = (XGroup) subpipeline.get(0);

        for (String port : inputs.keySet()) {
            if (!port.startsWith("|")) {
                xgroup.inputs.put(port, inputs.get(port));
            }
        }

        for (String port : outputs.keySet()) {
            if (!port.endsWith("|")) {
                xgroup.outputs.put(port, outputs.get(port));
            }
        }

        try {
            xgroup.run();
        } catch (Exception xe) {
            TreeWriter treeWriter = new TreeWriter(runtime);
            treeWriter.startDocument(step.getNode().getBaseURI());
            treeWriter.addStartElement(c_errors);
            treeWriter.startContent();

            boolean reported = false;
            for (XdmNode doc : runtime.getXProcData().errors()) {
                treeWriter.addSubtree(doc);
                reported = true;
            }

            for (XdmNode doc : errors) {
                treeWriter.addSubtree(doc);
                reported = true;
            }

            if (!reported) {
                // Hey, no one reported this exception. We better do it.
                treeWriter.addStartElement(c_error);

                String message = xe.getMessage();
                StructuredQName qCode = null;

                if (xe instanceof XPathException) {
                    XPathException xxx = (XPathException) xe;
                    qCode = xxx.getErrorCodeQName();

                    Throwable underlying = xe.getCause();
                    if (underlying != null) {
                        message = underlying.toString();
                    }
                }

                if (xe instanceof XProcException) {
                    XProcException xxx = (XProcException) xe;
                    QName code = xxx.getErrorCode();
                    message = xxx.getMessage();
                    Throwable underlying = xe.getCause();
                    if (underlying != null) {
                        message = underlying.getMessage();
                    }
                    if (code != null) {
                        qCode = new StructuredQName(code.getPrefix(), code.getNamespaceURI(), code.getLocalName());
                    }
                }

                if (qCode != null) {
                    treeWriter.addNamespace(qCode.getPrefix(), qCode.getNamespaceBinding().getURI());
                    treeWriter.addAttribute(_code, qCode.getDisplayName());
                }

                XStep step = runtime.runningStep();
                if (step != null && step.getNode() != null) {
                    XdmNode node = step.getNode();
                    if (node.getBaseURI() != null) {
                        treeWriter.addAttribute(_href, node.getBaseURI().toString());
                    }
                    if (node.getLineNumber() > 0) {
                        treeWriter.addAttribute(_line, ""+node.getLineNumber());
                    }
                    if (node.getColumnNumber() > 0) {
                        treeWriter.addAttribute(_column, ""+node.getColumnNumber());
                    }
                }

                treeWriter.startContent();
                treeWriter.addText(message);
                treeWriter.addEndElement();
            }

            treeWriter.addEndElement();
            treeWriter.endDocument();

            XCatch xcatch = (XCatch) subpipeline.get(1);

            xcatch.writeError(treeWriter.getResult());

            for (String port : inputs.keySet()) {
                if (!port.startsWith("|")) {
                    xcatch.inputs.put(port, inputs.get(port));
                }
            }

            for (String port : outputs.keySet()) {
                if (!port.endsWith("|")) {
                    xcatch.outputs.put(port, outputs.get(port));
                }
            }

            xcatch.run();
        }
    }

    public void reportError(XdmNode doc) {
        errors.add(doc);
    }
}
