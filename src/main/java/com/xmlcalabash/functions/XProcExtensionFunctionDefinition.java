package com.xmlcalabash.functions;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.RuntimeRegistry;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.SequenceType;

/**
 * Created with IntelliJ IDEA.
 * User: ndw
 * Date: 1/27/13
 * Time: 9:36 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class XProcExtensionFunctionDefinition extends ExtensionFunctionDefinition {
    protected RuntimeRegistry registry = RuntimeRegistry.getInstance();

    public void close() {
        registry.unregisterRuntime(this);
    }
}
