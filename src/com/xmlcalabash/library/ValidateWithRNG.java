/*
 * ValidateWithRNG.java
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

package com.xmlcalabash.library;

import java.io.IOException;
import java.net.URI;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import org.iso_relax.verifier.VerifierFactory;
import org.iso_relax.verifier.Verifier;
import org.iso_relax.verifier.Schema;
import org.iso_relax.verifier.VerifierConfigurationException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.runtime.XAtomicStep;
import org.xml.sax.SAXParseException;

/**
 *
 * @author ndw
 */
public class ValidateWithRNG extends DefaultStep {
    private static final QName _assert_valid = new QName("", "assert-valid");
    private static final QName _dtd_compatibility = new QName("", "dtd-compatibility");
    private static final String language = "http://relaxng.org/ns/structure/1.0";
    private ReadablePipe source = null;
    private ReadablePipe schema = null;
    private WritablePipe result = null;
    private URI docBaseURI = null;

    /** Creates a new instance of Delete */
    public ValidateWithRNG(XProcRuntime runtime, XAtomicStep step) {
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
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        XdmNode doc = null;

        try {
            VerifierFactory vfactory = new com.sun.msv.verifier.jarv.TheFactoryImpl();
            // FIXME: VerifierFactory.newInstance(language);

            Verifier verifier = null;
            XdmNode schemaNode = schema.read();
            InputSource schemaSource = S9apiUtils.xdmToInputSource(runtime, schemaNode);
            schemaSource.setSystemId(schemaNode.getBaseURI().toASCIIString());

            Schema docSchema = vfactory.compileSchema(schemaSource);
            verifier = docSchema.newVerifier();
            verifier.setErrorHandler(new RNGErrorHandler());

            doc = source.read();
            docBaseURI = doc.getBaseURI();

            if (verifier != null && !verifier.verify(S9apiUtils.xdmToInputSource(runtime, doc))) {
                throw new XProcException(XProcException.err_E0001, "Document is not valid");
            }

            result.write(doc);
        } catch (VerifierConfigurationException ex) {
            if (runtime.getDebug()) {
                ex.printStackTrace();
            }
            throw new XProcException(ex);
        } catch (SAXException sx) {
            // Assume the only error is validity failed?
            if (getOption(_assert_valid,false)) {
                throw XProcException.stepError(53);
            }
            result.write(doc);
        } catch (IOException ioe) {
            if (runtime.getDebug()) {
                ioe.printStackTrace();
            }
            throw new XProcException(ioe);
        }
    }

    class RNGErrorHandler implements ErrorHandler {
        public void fatalError(SAXParseException e) throws SAXException {
            error(e);
        }

        public void error(SAXParseException e) throws SAXException {
            TreeWriter treeWriter = new TreeWriter(runtime);
            treeWriter.startDocument(docBaseURI);
            treeWriter.addStartElement(XProcConstants.c_error);
            treeWriter.startContent();

            treeWriter.addText(e.toString());

            treeWriter.addEndElement();
            treeWriter.endDocument();

            step.reportError(treeWriter.getResult());

            throw e;
        }

        public void warning( SAXParseException e ) {
            // ignore warnings
        }
    }
}

