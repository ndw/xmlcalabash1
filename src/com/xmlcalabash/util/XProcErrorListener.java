package com.xmlcalabash.util;

import net.sf.saxon.StandardErrorListener;
import net.sf.saxon.Configuration;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.KeyDefinition;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.instruct.Instruction;
import net.sf.saxon.instruct.Procedure;
import net.sf.saxon.instruct.UserFunction;
import net.sf.saxon.instruct.Template;
import net.sf.saxon.instruct.AttributeSet;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.type.ValidationException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.dom.DOMLocator;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Dec 9, 2009
 * Time: 7:13:02 AM
 *
 * This listener collects messages to send to the error port if applicable.
 */
public class XProcErrorListener implements ErrorListener {
    private static QName c_error = new QName(XProcConstants.NS_XPROC_STEP, "error");
    private static QName _name = new QName("", "name");
    private static QName _type = new QName("", "type");
    private static QName _href = new QName("", "href");
    private static QName _line = new QName("", "line");
    private static QName _column = new QName("", "column");
    private static QName _code = new QName("", "code");

    private ErrorListener parentListener = null;
    private XProcRuntime runtime = null;
    private URI baseURI = null;

    public XProcErrorListener(XProcRuntime runtime, ErrorListener parentListener) {
        super();
        this.runtime = runtime;
        this.parentListener = parentListener;
        try {
            baseURI = new URI("http://xproc.org/errors");
        } catch (URISyntaxException use) {
            // nop;
        }
    }

    public void error(TransformerException exception) throws TransformerException {
        report("error", exception);
        if (parentListener != null) {
            parentListener.error(exception);
        }
    }

    public void fatalError(TransformerException exception) throws TransformerException {
        report("fatal-error", exception);
        if (parentListener != null) {
            parentListener.fatalError(exception);
        }
    }

    public void warning(TransformerException exception) throws TransformerException {
        report("warning", exception);
        if (parentListener != null) {
            parentListener.warning(exception);
        }
    }

    private void report(String type, TransformerException exception) {
        TreeWriter writer = new TreeWriter(runtime);

        writer.startDocument(baseURI);
        writer.addStartElement(c_error);
        writer.addAttribute(_type, type);

        StructuredQName qCode = null;
        if (exception instanceof XPathException) {
            qCode = ((XPathException) exception).getErrorCodeQName();
        }
        if (qCode == null && exception.getException() instanceof XPathException) {
            qCode = ((XPathException) exception.getException()).getErrorCodeQName();
        }
        if (qCode != null) {
            writer.addAttribute(_code, qCode.getDisplayName());
        }

        if (exception.getLocator() != null) {
            SourceLocator loc = exception.getLocator();
            boolean done = false;
            while (!done && loc == null) {
                if (exception.getException() instanceof TransformerException) {
                    exception = (TransformerException) exception.getException();
                    loc = exception.getLocator();
                } else if (exception.getCause() instanceof TransformerException) {
                    exception = (TransformerException) exception.getCause();
                    loc = exception.getLocator();
                } else {
                    done = true;
                }
            }

            if (loc != null) {
                if (loc.getSystemId() != null && !"".equals(loc.getSystemId())) {
                    writer.addAttribute(_href, loc.getSystemId());
                }

                if (loc.getLineNumber() != -1) {
                    writer.addAttribute(_line, ""+loc.getLineNumber());
                }

                if (loc.getColumnNumber() != -1) {
                    writer.addAttribute(_column, ""+loc.getColumnNumber());
                }
            }
        }


        writer.startContent();
        writer.addText(exception.toString());
        writer.addEndElement();
        writer.endDocument();

        runtime.getXProcData().addError(writer.getResult());
    }
}