package com.xmlcalabash.config;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.library.Load;
import com.xmlcalabash.model.DataBinding;
import com.xmlcalabash.model.DocumentBinding;
import net.sf.saxon.s9api.XdmNode;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 8:36 AM
 * To change this template use File | Settings | File Templates.
 */
public interface XMLCalabashConfigurer {
    public void configRuntime(XProcRuntime runtime);
    public XdmNode loadDocument(Load load);
    public ReadablePipe makeReadableData(XProcRuntime runtime, DataBinding binding);
    public ReadablePipe makeReadableDocument(XProcRuntime runtime, DocumentBinding binding);
}
