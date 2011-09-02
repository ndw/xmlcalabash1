package com.xmlcalabash.core;

import com.xmlcalabash.runtime.XStep;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Dec 18, 2009
 * Time: 8:10:48 AM
 * To change this template use File | Settings | File Templates.
 */
public interface XProcMessageListener {
    public void error(XProcRunnable step, XdmNode node, String message, QName code);
    public void error(Throwable exception);
    public void warning(XProcRunnable step, XdmNode node, String message);
    public void warning(Throwable exception);
    public void info(XProcRunnable step, XdmNode node, String message);
    public void fine(XProcRunnable step, XdmNode node, String message);
    public void finer(XProcRunnable step, XdmNode node, String message);
    public void finest(XProcRunnable step, XdmNode node, String message);
}
