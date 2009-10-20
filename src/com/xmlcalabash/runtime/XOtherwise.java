package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.Step;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 13, 2008
 * Time: 4:58:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class XOtherwise extends XCompoundStep {
    public XOtherwise(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }
}
