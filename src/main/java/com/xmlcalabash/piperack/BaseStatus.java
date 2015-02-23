package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.trans.XPathException;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.*;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.service.StatusService;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import java.net.URI;

/**
 * Created by ndw on 8/27/14.
 */
public class BaseStatus extends StatusService {
    XProcRuntime globalRuntime = null;

    public BaseStatus(XProcRuntime runtime) {
        globalRuntime = runtime;
    }

    @Override
    public Representation getRepresentation(org.restlet.data.Status status, Request request, Response response) {
        TreeWriter tree = new TreeWriter(globalRuntime);
        tree.startDocument(URI.create("http://example.com/"));
        tree.addStartElement(BaseResource.pr_error);
        tree.startContent();
        tree.addStartElement(BaseResource.pr_code);
        tree.startContent();
        tree.addText("" + status.getCode());
        tree.addEndElement();
        tree.addStartElement(BaseResource.pr_message);
        tree.startContent();
        tree.addText("Bad request: " + exceptionMessage(status.getThrowable()));
        tree.addEndElement();
        tree.addEndElement();
        tree.endDocument();
        return new StringRepresentation(tree.getResult().toString());
    }

    private String exceptionMessage(Throwable exception) {
        StructuredQName qCode = null;
        SourceLocator loc = null;
        String message = "";

        if (exception instanceof XPathException) {
            qCode = ((XPathException) exception).getErrorCodeQName();
        }

        if (exception instanceof TransformerException) {
            TransformerException tx = (TransformerException) exception;
            if (qCode == null && tx.getException() instanceof XPathException) {
                qCode = ((XPathException) tx.getException()).getErrorCodeQName();
            }

            if (tx.getLocator() != null) {
                loc = tx.getLocator();
                boolean done = false;
                while (!done && loc == null) {
                    if (tx.getException() instanceof TransformerException) {
                        tx = (TransformerException) tx.getException();
                        loc = tx.getLocator();
                    } else if (exception.getCause() instanceof TransformerException) {
                        tx = (TransformerException) exception.getCause();
                        loc = tx.getLocator();
                    } else {
                        done = true;
                    }
                }
            }
        }

        if (exception instanceof XProcException) {
            XProcException err = (XProcException) exception;
            loc = err.getLocator();
            if (err.getErrorCode() != null) {
                QName n = err.getErrorCode();
                qCode = new StructuredQName(n.getPrefix(),n.getNamespaceURI(),n.getLocalName());
            }
            if (err.getStep() != null) {
                message = message + err.getStep() + ":";
            }
        }

        if (loc != null) {
            if (loc.getSystemId() != null && !"".equals(loc.getSystemId())) {
                message = message + loc.getSystemId() + ":";
            }
            if (loc.getLineNumber() != -1) {
                message = message + loc.getLineNumber() + ":";
            }
            if (loc.getColumnNumber() != -1) {
                message = message + loc.getColumnNumber() + ":";
            }
        }

        if (qCode != null) {
            message = message + qCode.getDisplayName() + ":";
        }

        message += exception.getMessage();

        Throwable cause = exception.getCause();
        if (cause != null) {
            message += ": " + cause.getMessage();
            cause = cause.getCause();
            if (cause != null) {
                message += ": " + cause.getMessage();
            }
        }

        return message;
    }
}
