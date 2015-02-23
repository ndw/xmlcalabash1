package com.xmlcalabash.extensions.marklogic;

import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.Configuration;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.model.RuntimeValue;
import com.marklogic.xcc.*;
import com.marklogic.xcc.types.*;
import com.marklogic.xcc.types.XdmItem;

import javax.xml.transform.sax.SAXSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;
import java.util.Hashtable;

import org.xml.sax.InputSource;


/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 4, 2008
 * Time: 11:24:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class XCCInvokeModule extends DefaultStep {
    private static final QName _pipeinfo = new QName("","pipeinfo");
    private static final QName _user = new QName("","user");
    private static final QName _password = new QName("","password");
    private static final QName _host = new QName("","host");
    private static final QName _port = new QName("","port");
    private static final QName _contentBase = new QName("","content-base");
    private static final QName _wrapper = new QName("","wrapper");
    private static final QName _module = new QName("","module");
    private static final QName _auth_method = new QName("auth-method");

    private WritablePipe result = null;
    private Hashtable<QName,String> params = new Hashtable<QName, String> ();

    /**
     * Creates a new instance of Identity
     */
    public XCCInvokeModule(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        params.put(name, value.getString());
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        String module = getOption(_module).getString();

        String host = getOption(_host, "");
        int port = getOption(_port, 0);
        String user = getOption(_user, "");
        String password = getOption(_password, "");
        String contentBase = getOption(_contentBase, "");
        QName wrapper = XProcConstants.c_result;

        if (getOption(_wrapper) != null) {
            wrapper = getOption(_wrapper).getQName();
        }

        ContentSource contentSource;

        try {
            if ("".equals(contentBase)) {
                contentSource = ContentSourceFactory.newContentSource(host, port, user, password);
            } else {
                contentSource = ContentSourceFactory.newContentSource(host, port, user, password, contentBase);
            }
        } catch (Exception e) {
            throw new XProcException(e);
        }

        if ("basic".equals(getOption(_auth_method, ""))) {
            contentSource.setAuthenticationPreemptive(true);
        }

        Session session;

        try {
            session = contentSource.newSession ();
            Request request = session.newModuleInvoke(module);

            for (QName name : params.keySet()) {
                XSString value = ValueFactory.newXSString (params.get(name));
                XName xname = new XName(name.getNamespaceURI(), name.getLocalName());
                XdmVariable myVariable = ValueFactory.newVariable (xname, value);
                request.setVariable (myVariable);
            }

            ResultSequence rs = session.submitRequest (request);

            while (rs.hasNext()) {
                ResultItem rsItem = rs.next();
                XdmItem item = rsItem.getItem();

                // FIXME: This needs work...
                if (item instanceof XdmDocument || item instanceof XdmElement) {
                    XdmNode xccXML = parseString(item.asString());
                    result.write(xccXML);
                } else {
                    String text = item.asString();
                    TreeWriter treeWriter = new TreeWriter(runtime);
                    treeWriter.startDocument(step.getNode().getBaseURI());
                    treeWriter.addStartElement(wrapper);
                    treeWriter.startContent();
                    treeWriter.addText(text);
                    treeWriter.addEndElement();
                    treeWriter.endDocument();
                    XdmNode node = treeWriter.getResult();
                    result.write(node);
                }
            }

            session.close();
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }

    private XdmNode parseString(String xml) {
        StringReader sr = new StringReader(xml);
        return runtime.parse(new InputSource(sr));
    }
}