package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XMLCalabash;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.library.DefaultStep;
import com.thaiopensource.xml.sax.ErrorHandlerImpl;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.SchemaReader;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import com.thaiopensource.validate.rng.CompactSchemaReader;
import com.thaiopensource.validate.prop.rng.RngProperty;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.StringReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Apr 29, 2009
 * Time: 6:35:09 PM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "cx:nvdl",
        type = "{http://xmlcalabash.com/ns/extensions}nvdl " +
                "{http://exproc.org/proposed/steps}nvdl")

public class NVDL extends DefaultStep {
    private static final QName _assert_valid = new QName("", "assert-valid");

    private ReadablePipe source = null;
    private ReadablePipe nvdlSource = null;
    private ReadablePipe schemaSource = null;
    private WritablePipe result = null;

    /** Creates a new instance of Delete */
    public NVDL(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else if ("nvdl".equals(port)) {
            nvdlSource = pipe;
        } else if ("schemas".equals(port)) {
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

        boolean assertValid = getOption(_assert_valid,false);

        ErrorHandlerImpl eh = new ErrorHandlerImpl(System.out);
        PropertyMapBuilder properties = new PropertyMapBuilder();
        properties.put(ValidateProperty.ERROR_HANDLER, eh);
        RngProperty.CHECK_ID_IDREF.add(properties);

        properties.put(ValidateProperty.ENTITY_RESOLVER, runtime.getResolver());

        XdmNode srcdoc = source.read();
        XdmNode nvdldoc = nvdlSource.read();

        while (schemaSource.moreDocuments()) {
            XdmNode schema = schemaSource.read();
            runtime.getResolver().cache(schema, schema.getBaseURI());
        }

        ValidationDriver driver = new ValidationDriver(properties.toPropertyMap());

        InputSource nvdl = S9apiUtils.xdmToInputSource(runtime, nvdldoc);
        InputSource doc = S9apiUtils.xdmToInputSource(runtime, srcdoc);

        try {
            driver.loadSchema(nvdl);

            if (!driver.validate(doc)) {
                if (assertValid) {
                    throw XProcException.stepError(53);
                }
            }
        } catch (SAXException e) {
            throw new XProcException("SAX Exception", e);
        } catch (IOException e) {
            throw new XProcException("IO Exception", e);
        }

        result.write(srcdoc); // At the moment, we don't get any augmentation
    }
}