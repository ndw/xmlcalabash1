package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.*;
import com.xmlcalabash.util.MessageFormatter;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;

import java.math.BigDecimal;
import java.util.Vector;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 13, 2008
 * Time: 4:57:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class XWhen extends XCompoundStep {
    public XWhen(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }

    public boolean shouldRun() throws SaxonApiException {
        String testExpr = ((When) step).getTest();
        XdmNode doc = null;
        NamespaceBinding nsbinding = new NamespaceBinding(runtime, step.getNode());
        Hashtable<QName,RuntimeValue> globals = parent.getInScopeOptions();

        ReadablePipe reader = inputs.get("#xpath-context").firstElement();
        doc = reader.read();

        if (reader.moreDocuments() || inputs.get("#xpath-context").size() > 1) {
            throw XProcException.dynamicError(5);
        }

        // Surround testExpr with "boolean()" to force the EBV.
        Vector<XdmItem> results = evaluateXPath(doc, nsbinding.getNamespaceBindings(), "boolean(" + testExpr + ")", globals);

        if (results.size() != 1) {
            throw new XProcException("Attempt to compute EBV in p:when did not return a singleton!?");
        }

        XdmAtomicValue value = (XdmAtomicValue) results.get(0);
        return value.getBooleanValue();
    }

    protected void copyInputs() throws SaxonApiException {
        for (String port : inputs.keySet()) {
            if (!port.startsWith("|") && !"#xpath-context".equals(port)) {
            String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments()) {
                        XdmNode doc = reader.read();
                        pipe.write(doc);
                        logger.trace(MessageFormatter.nodeMessage(step.getNode(),
                                "Compound input copy from " + reader + " to " + pipe));
                    }
                }
            }
        }
    }    
}
