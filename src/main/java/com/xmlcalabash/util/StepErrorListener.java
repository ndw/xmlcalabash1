package com.xmlcalabash.util;

import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.om.*;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;

import javax.xml.transform.TransformerException;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.ErrorListener;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;

import java.net.URI;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Dec 9, 2009
 * Time: 7:13:02 AM
 *
 * This listener collects messages to send to the error port if applicable.
 */
public class StepErrorListener implements ErrorListener {
    private static QName c_error = new QName(XProcConstants.NS_XPROC_STEP, "error");
    private static StructuredQName err_sxxp0003 = new StructuredQName("err", "http://www.w3.org/2005/xqt-errors", "SXXP0003");
    private static QName _type = new QName("", "type");
    private static QName _href = new QName("", "href");
    private static QName _line = new QName("", "line");
    private static QName _column = new QName("", "column");
    private static QName _code = new QName("", "code");

    private XProcRuntime runtime = null;
    private URI baseURI = null;

    public StepErrorListener(XProcRuntime runtime) {
        super();
        this.runtime = runtime;
        baseURI = runtime.getStaticBaseURI();
    }

    public void error(TransformerException exception) throws TransformerException {
        if (!report("error", exception)) {
            runtime.error(exception);
        }
    }

    public void fatalError(TransformerException exception) throws TransformerException {
        if (!report("fatal-error", exception)) {
            runtime.error(exception);
        }
    }

    public void warning(TransformerException exception) throws TransformerException {
        if (!report("warning", exception)) {
            // XProc doesn't have recoverable exceptions...
            runtime.warning(exception);
        }
    }

    private boolean report(String type, TransformerException exception) {
        // HACK!!!
        if (runtime.transparentJSON() && exception instanceof XPathException) {
            XPathException e = (XPathException) exception;
            StructuredQName errqn = e.getErrorCodeQName();
            if (errqn != null && errqn.equals(err_sxxp0003)) {
                // We'll be trying again as JSON, so let it go this time
                return true;
            }
        }
        
        TreeWriter writer = new TreeWriter(runtime);

        writer.startDocument(baseURI);

        String message = exception.toString();
        StructuredQName qCode = null;
        if (exception instanceof XPathException) {
            XPathException xxx = (XPathException) exception;
            qCode = xxx.getErrorCodeQName();

            Throwable underlying = exception.getException();
            if (underlying == null) {
                underlying = exception.getCause();
            }

            if (underlying != null) {
                message = underlying.toString();
            }
        }

        if (qCode == null && exception.getException() instanceof XPathException) {
            qCode = ((XPathException) exception.getException()).getErrorCodeQName();
        }

        ArrayList<AttributeInfo> alist = new ArrayList<>();
        NamespaceMap nsmap = NamespaceMap.emptyMap();

        if (qCode != null) {
            nsmap = nsmap.put(qCode.getPrefix(), qCode.getNamespaceBinding().getURI());
            alist.add(new AttributeInfo(TypeUtils.fqName(_code), BuiltInAtomicType.ANY_ATOMIC, qCode.getDisplayName(), null, ReceiverOption.NONE));
        }

        alist.add(new AttributeInfo(TypeUtils.fqName(_type), BuiltInAtomicType.ANY_ATOMIC, type, null, ReceiverOption.NONE));

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
                    alist.add(new AttributeInfo(TypeUtils.fqName(_href), BuiltInAtomicType.ANY_ATOMIC, loc.getSystemId(), null, ReceiverOption.NONE));
                }

                if (loc.getLineNumber() != -1) {
                    alist.add(new AttributeInfo(TypeUtils.fqName(_line), BuiltInAtomicType.ANY_ATOMIC, ""+loc.getLineNumber(), null, ReceiverOption.NONE));
                }

                if (loc.getColumnNumber() != -1) {
                    alist.add(new AttributeInfo(TypeUtils.fqName(_type), BuiltInAtomicType.ANY_ATOMIC, ""+loc.getColumnNumber(), null, ReceiverOption.NONE));
                }
            }
        }

        writer.addStartElement(c_error, S9apiUtils.mapFromList(alist), nsmap);

        writer.addText(message);
        writer.addEndElement();
        writer.endDocument();

        XdmNode node = writer.getResult();

        return runtime.getXProcData().catchError(node);
    }
}