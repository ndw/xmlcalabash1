/*
 * XProcException.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * Portions Copyright 2007 Sun Microsystems, Inc.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * https://xproc.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 */

package com.xmlcalabash.core;

import net.sf.saxon.s9api.QName;
import com.xmlcalabash.model.Step;

/**
 *
 * @author ndw
 */
public class XProcException extends RuntimeException {
    public static final QName err_E0001 = new QName(XProcConstants.NS_XPROC_ERROR_EX, "XE0001"); // invalid pipeline
    public static final QName err_E0002 = new QName(XProcConstants.NS_XPROC_ERROR_EX, "XE0002"); // invalid configuration

    private QName error = null;
    private Step step = null;
    
    /** Creates a new instance of XProcException */
    public XProcException() {
        super();
    }

    /** Creates a new instance of XProcException */
    public XProcException(String message) {
        super(message);
    }

    /** Creates a new instance of XProcException */
    public XProcException(Step step, String message) {
        super(message);
        this.step = step;
    }

    /** Creates a new instance of XProcException */
    public XProcException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Creates a new instance of XProcException */
    public XProcException(Step step, String message, Throwable cause) {
        super(message, cause);
        this.step = step;
    }

    /** Creates a new instance of XProcException */
    public XProcException(Throwable cause) {
        super(cause);
    }
    
    public XProcException(QName errorCode) {
    	super(errorCode.getLocalName());
        error = errorCode;
    }

    public XProcException(Step step, QName errorCode) {
    	super(errorCode.getLocalName());
        error = errorCode;
        this.step = step;
    }

    public XProcException(QName errorCode, String message) {
        super(message);
        error = errorCode;
    }

    public XProcException(Step step, QName errorCode, String message) {
        super(message);
        error = errorCode;
        this.step = step;
    }

    public XProcException(QName errorCode, Throwable cause) {
        super("XProc error err:" + errorCode.getLocalName(), cause);
        error = errorCode;
    }

    public static XProcException staticError(int errno) {
        return new XProcException(XProcConstants.staticError(errno));
    }

    public static XProcException staticError(int errno, String message) {
        return new XProcException(XProcConstants.staticError(errno), message);
    }

    public static XProcException staticError(int errno, Exception except) {
        return new XProcException(XProcConstants.staticError(errno), except);
    }

    public static XProcException dynamicError(int errno) {
        return new XProcException(XProcConstants.dynamicError(errno));
    }

    public static XProcException dynamicError(Step step, int errno) {
        return new XProcException(step, XProcConstants.dynamicError(errno));
    }

    public static XProcException dynamicError(int errno, String message) {
        return new XProcException(XProcConstants.dynamicError(errno), message);
    }

    public static XProcException dynamicError(Step step, int errno, String message) {
        return new XProcException(step, XProcConstants.dynamicError(errno), message);
    }

    public static XProcException dynamicError(int errno, Exception except) {
        return new XProcException(XProcConstants.dynamicError(errno), except);
    }

    public static XProcException stepError(int errno) {
        return new XProcException(XProcConstants.stepError(errno));
    }

    public static XProcException stepError(int errno, String message) {
        return new XProcException(XProcConstants.stepError(errno), message);
    }

    public static XProcException stepError(int errno, Exception except) {
        return new XProcException(XProcConstants.stepError(errno), except);
    }

    public QName getErrorCode() {
        return error;
    }

    public Step getStep() {
        return step;
    }
}
