package com.xmlcalabash.library;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SaxonApiUncheckedException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 22, 2008
 * Time: 11:06:11 AM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "p:validate-with-schematron",
        type = "{http://www.w3.org/ns/xproc}validate-with-schematron")

public class ValidateWithSCH extends DefaultStep {
    private static final StructuredQName _untyped = StructuredQName.fromClarkName("{http://www.w3.org/2001/XMLSchema}untyped");
    private static final QName _assert_valid = new QName("", "assert-valid");
    private static final QName _phase = new QName("", "phase");
    private InputStream  skeleton = null;
    private ReadablePipe source = null;
    private ReadablePipe schema = null;
    private WritablePipe resultPipe = null;
    private WritablePipe reportPipe = null;
    private HashMap<QName,RuntimeValue> params = new HashMap<QName,RuntimeValue> ();
    private boolean schemaAware = false;


    /* Creates a new instance of ValidateWithXSD */
    public ValidateWithSCH(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else if ("schema".equals(port)) {
            schema = pipe;
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        if ("result".equals(port)) {
            resultPipe = pipe;
        } else if ("report".equals(port)) {
            reportPipe = pipe;
        }
    }

    public void setParameter(QName name, RuntimeValue value) {
        params.put(name, value);
    }

    public void reset() {
        source.resetReader();
        schema.resetReader();
        resultPipe.resetWriter();
        reportPipe.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        XdmNode sourceXML = source.read();

        // If we're dealing with a typed document, we must compile the XSLT in schema-aware mode
        SchemaType type = sourceXML.getUnderlyingNode().getSchemaType();
        schemaAware = ! ( type == null || type.getStructuredQName().equals(_untyped) );

        XdmNode schemaXML = schema.read();

        XsltCompiler compiler;
        XsltExecutable exec;
        XdmDestination result;

        // From http://www.schematron.com/
        // ...
        // So the basic processing now looks like this:
        //
        // xslt -stylesheet iso_dsdl_include.xsl  theSchema.sch > theSchema1.sch
        // xslt -stylesheet iso_abstract_expand.xsl  theSchema1.sch > theSchema2.sch
        // xslt -stylesheet iso_svrl_for_xsltn.xsl  theSchema2.sch > theSchema.xsl
        // xslt -stylesheet theSchema.xsl  myDocument.xml > myResult.xml

        // It would be nice to load these stylesheets only once, but sometimes (i.e. from RunTest),
        // there are different controllers involved and you can't do that.
        XdmNode theSchema1_sch = transform(schemaXML, getSchematronXSLT("iso_dsdl_include.xsl"));
        XdmNode theSchema2_sch = transform(theSchema1_sch, getSchematronXSLT("iso_abstract_expand.xsl"));

        skeleton = getClass().getResourceAsStream("/etc/schematron/iso_schematron_skeleton_for_saxon.xsl");
        if (skeleton == null) {
            throw new UnsupportedOperationException("Failed to load iso_schematron_skeleton_for_saxon.xsl from JAR file.");
        }

        compiler = runtime.getProcessor().newXsltCompiler();
        compiler.setSchemaAware(schemaAware);
        compiler.setURIResolver(new UResolver());
        exec = compiler.compile(getSchematronXSLT("iso_svrl_for_xslt2.xsl"));
        XsltTransformer schemaCompiler = exec.load();

        if (getOption(_phase) != null) {
            String phase = getOption(_phase).getString();
            schemaCompiler.setParameter(new QName("","phase"), new XdmAtomicValue(phase));
        }

        for (QName name : params.keySet()) {
            RuntimeValue v = params.get(name);
            schemaCompiler.setParameter(name, new XdmAtomicValue(v.getString()));
        }

        schemaCompiler.setInitialContextNode(theSchema2_sch);
        result = new XdmDestination();
        schemaCompiler.setDestination(result);

        runtime.getConfigurer().getSaxonConfigurer().configSchematron(schemaCompiler.getUnderlyingController().getConfiguration());

        schemaCompiler.transform();

        XdmNode compiledSchema = result.getXdmNode();
        XdmNode compiledRoot = S9apiUtils.getDocumentElement(compiledSchema);
        
        if (compiledRoot == null) {
            XdmNode schemaRoot = S9apiUtils.getDocumentElement(schemaXML);
            String root = schemaRoot == null ? "null" : schemaRoot.getNodeName().toString();
            throw new XProcException("p:validate-with-schematron failed to compile provided schema: " + root);
        }

        XsltTransformer transformer;

        compiler = runtime.getProcessor().newXsltCompiler();
        compiler.setSchemaAware(schemaAware);
        exec = compiler.compile(compiledSchema.asSource());
        transformer = exec.load();
	    for (QName name : params.keySet()) {
             RuntimeValue v = params.get(name);
             transformer.setParameter(name, v.getUntypedAtomic(runtime));
        }        
        transformer.setInitialContextNode(sourceXML);
        result = new XdmDestination();
        transformer.setDestination(result);
        transformer.transform();

        XdmNode report = result.getXdmNode();

        // Write the report before throwing an exception so that p:log on the validate
        // step can do something useful.
        reportPipe.write(report);

        boolean failedAsserts = checkFailedAssert(report);

        if (failedAsserts && getOption(_assert_valid,false)) {
            throw XProcException.stepError(54);
        }

        resultPipe.write(sourceXML);
    }

    private boolean checkFailedAssert(XdmNode doc) {
        HashMap<String,String> nsBindings = new HashMap<> ();
        nsBindings.put("svrl", "http://purl.oclc.org/dsdl/svrl");
        String xpath = "//svrl:failed-assert|//svrl:successful-report";
        Vector<XdmItem> results = new Vector<XdmItem> ();

        Configuration config = runtime.getProcessor().getUnderlyingConfiguration();

        try {
            XPathCompiler xcomp = runtime.newXPathCompiler(step.getNode().getBaseURI(), nsBindings);
            XPathExecutable xexec = null;
            try {
                xexec = xcomp.compile(xpath);
            } catch (SaxonApiException sae) {
                throw sae;
            }

            XPathSelector selector = xexec.load();

            selector.setContextItem(doc);

            try {
                Iterator<XdmItem> values = selector.iterator();
                while (values.hasNext()) {
                    results.add(values.next());
                }
            } catch (SaxonApiUncheckedException saue) {
                Throwable sae = saue.getCause();
                if (sae instanceof XPathException) {
                    XPathException xe = (XPathException) sae;
                    if ("http://www.w3.org/2005/xqt-errors".equals(xe.getErrorCodeNamespace()) && "XPDY0002".equals(xe.getErrorCodeLocalPart())) {
                        throw XProcException.dynamicError(26, step.getNode(), "Expression refers to context when none is available: " + xpath);
                    } else {
                        throw saue;
                    }

                } else {
                    throw saue;
                }
            }
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        return results.size() != 0;
    }

    private class UResolver implements URIResolver {

        public Source resolve(String href, String base) throws TransformerException {
            if ("iso_schematron_skeleton_for_saxon.xsl".equals(href)) {
                return new SAXSource(new InputSource(skeleton));
            } else {
                throw new XProcException(step.getNode(), "Failed to resolve " + href + " from JAR file.");
            }
        }
    }

    private SAXSource getSchematronXSLT(String xslt) {
        InputStream instream = getClass().getResourceAsStream("/etc/schematron/" + xslt);
        if (instream == null) {
            throw new UnsupportedOperationException("Failed to load " + xslt + " from JAR file.");
        }

        return new SAXSource(new InputSource(instream));
    }

    private XdmNode transform(XdmNode source, SAXSource stylesheet) throws SaxonApiException {
        XsltCompiler compiler = runtime.getProcessor().newXsltCompiler();
        compiler.setSchemaAware(schemaAware);
        compiler.setURIResolver(new UResolver());
        XsltExecutable exec = compiler.compile(stylesheet);
        XsltTransformer schemaCompiler = exec.load();

        schemaCompiler.setInitialContextNode(source);
        XdmDestination result = new XdmDestination();
        schemaCompiler.setDestination(result);
        schemaCompiler.transform();

        return result.getXdmNode();
    }

}

