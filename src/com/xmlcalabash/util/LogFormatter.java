package com.xmlcalabash.util;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;

import java.util.logging.Logger;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Jul 30, 2009
 * Time: 7:28:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class LogFormatter extends Formatter {

    public String format(LogRecord record) {
        return record.getLevel().getName() + ": " + record.getMessage() + "\n";
    }
}
