package com.xmlcalabash.library;

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

import com.xmlcalabash.config.JingConfigurer;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.thaiopensource.validate.SchemaReader;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.prop.rng.RngProperty;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import com.thaiopensource.validate.rng.CompactSchemaReader;
import com.thaiopensource.xml.sax.ErrorHandlerImpl;
import com.thaiopensource.util.PropertyMapBuilder;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Apr 28, 2009
 * Time: 8:02:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class ValidateJing extends DefaultStep {
    private static final QName _assert_valid = new QName("", "assert-valid");
    private static final QName _dtd_attribute_values = new QName("", "dtd-attribute-values");
    private static final QName _dtd_id_idref_warnings = new QName("", "dtd-id-idref-warnings");
    private static final QName _encoding = new QName("encoding");
    private static final QName _line = new QName("line");
    private static final QName _column = new QName("column");

    private ReadablePipe source = null;
    private ReadablePipe schemaSource = null;
    private WritablePipe result = null;
    private URI docBaseURI = null;

    /** Creates a new instance of Delete */
    public ValidateJing(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else if ("schema".equals(port)) {
            schemaSource = pipe;
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

        boolean assertValid = getOption(_assert_valid,true);
        boolean checkIdRefs = getOption(_dtd_id_idref_warnings,false);
        boolean dtdAugment  = getOption(_dtd_attribute_values,false);

        ErrorHandler eh = new RNGErrorHandler();
        PropertyMapBuilder properties = new PropertyMapBuilder();
        properties.put(ValidateProperty.ERROR_HANDLER, eh);
        properties.put(ValidateProperty.URI_RESOLVER, runtime.getResolver());
        properties.put(ValidateProperty.ENTITY_RESOLVER, runtime.getResolver());


        if (checkIdRefs) {
            RngProperty.CHECK_ID_IDREF.add(properties);
        }
        
        XdmNode doc = source.read();
        XdmNode schema = schemaSource.read();
        XdmNode root = S9apiUtils.getDocumentElement(schema);

        docBaseURI = doc.getBaseURI();

        SchemaReader sr = null;

        boolean compact = XProcConstants.c_data.equals(root.getNodeName());

        String contentType = root.getAttributeValue(XProcConstants.c_content_type);
        if (contentType != null) {
            compact |= contentType.startsWith("text/") || contentType.equals("application/relax-ng-compact-syntax");
        }

        InputSource schemaInputSource = null;
        JingConfigurer configurer = runtime.getConfigurer().getJingConfigurer();

        if (compact) {
            // Compact syntax
            configurer.configRNC(properties);
            sr = CompactSchemaReader.getInstance();

            // Grotesque hack!
            StringReader srdr = new StringReader(compactSchema(root));
            schemaInputSource = new InputSource(srdr);
            schemaInputSource.setSystemId(root.getBaseURI().toASCIIString());
        } else {
            // XML syntax
            configurer.configRNG(properties);
            sr = new AutoSchemaReader();
            schemaInputSource = S9apiUtils.xdmToInputSource(runtime, schema);
        }

        ValidationDriver driver = new ValidationDriver(properties.toPropertyMap(), sr);
        try {
            if (driver.loadSchema(schemaInputSource)) {
                InputSource din = S9apiUtils.xdmToInputSource(runtime, doc);
                if (!driver.validate(din)) {
                    if (assertValid) {
                        throw XProcException.stepError(53);
                    }
                }
            } else {
                throw new XProcException(step.getNode(), "Error loading schema");
            }
        } catch (SAXParseException e) {
            if (assertValid) {
                throw XProcException.stepError(53);
            }
        } catch (SAXException e) {
            throw new XProcException("SAX Exception", e);
        } catch (IOException e) {
            throw new XProcException("IO Exception", e);
        }

        result.write(doc); // At the moment, we don't get any augmentation
    }

    private String compactSchema(XdmNode doc) {
        if ("base64".equals(doc.getAttributeValue(_encoding))) {
            byte[] decoded = Base64.decode(doc.getStringValue());
            String s = new String(decoded);
            return s;
        } else {
            return doc.getStringValue();
        }
    }

    class RNGErrorHandler implements ErrorHandler {
        SAXParseException err = null;

        public void fatalError(SAXParseException e) throws SAXException {
            error(e);
        }

        public void error(SAXParseException e) throws SAXException {
            TreeWriter treeWriter = new TreeWriter(runtime);
            treeWriter.startDocument(docBaseURI);
            treeWriter.addStartElement(XProcConstants.c_error);

            if (e.getLineNumber()!=-1) {
                treeWriter.addAttribute(_line, ""+e.getLineNumber());
            }

            if (e.getColumnNumber()!=-1) {
                treeWriter.addAttribute(_column, ""+e.getColumnNumber());
            }

            treeWriter.startContent();

            treeWriter.addText(e.toString());

            treeWriter.addEndElement();
            treeWriter.endDocument();

            step.reportError(treeWriter.getResult());
            if (err != null) {
                err = e;
            }
        }

        public void warning( SAXParseException e ) {
            // ignore warnings
        }
    }
}
