package com.xmlcalabash.util;

import com.xmlcalabash.config.XMLCalabashConfigurer;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadableData;
import com.xmlcalabash.io.ReadableDocument;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.library.Load;
import com.xmlcalabash.model.DataBinding;
import com.xmlcalabash.model.DocumentBinding;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 9:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultXMLCalabashConfigurer implements XMLCalabashConfigurer {
    private static final QName _href = new QName("href");
    private static final QName _dtd_validate = new QName("dtd-validate");
    private final static QName cx_filemask = new QName("cx", XProcConstants.NS_CALABASH_EX,"filemask");
    protected XProcRuntime runtime = null;

    public DefaultXMLCalabashConfigurer(XProcRuntime runtime) {
        this.runtime = runtime;
    }

    public void configRuntime(XProcRuntime runtime) {
        // Do nothing
    }

    public XdmNode loadDocument(Load load) {
        boolean      validate = load.getOption(_dtd_validate, false);
        RuntimeValue href     = load.getOption(_href);
        String       base     = href.getBaseURI().toASCIIString();
        if (runtime.getSafeMode() && base.startsWith("file:")) {
            throw XProcException.dynamicError(21);
        }
        return runtime.parse(href.getString(), base, validate);
    }

    public ReadablePipe makeReadableData(XProcRuntime runtime, DataBinding binding) {
        return new ReadableData(runtime, binding.getWrapper(), binding.getHref(), binding.getContentType());
    }

    public ReadablePipe makeReadableDocument(XProcRuntime runtime, DocumentBinding binding) {
        String mask = binding.getExtensionAttribute(cx_filemask);
        String base = binding.getNode().getBaseURI().toASCIIString();
        return new ReadableDocument(runtime, binding.getNode(), binding.getHref(), base, mask);
    }
}
