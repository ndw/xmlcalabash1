package com.xmlcalabash.core;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.QName;

import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:43:09 AM
 * To change this template use File | Settings | File Templates.
 */
public interface XProcStep extends XProcRunnable {
    public void setInput(String port, ReadablePipe pipe);
    public void setOutput(String port, WritablePipe pipe);
    public void setParameter(QName name, RuntimeValue value);
    public void setParameter(String port, QName name, RuntimeValue value);
    public void setOption(QName name, RuntimeValue value);
}
