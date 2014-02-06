package com.xmlcalabash.util;

import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;
import jp.co.antenna.XfoJavaCtl.MessageListener;
import jp.co.antenna.XfoJavaCtl.XfoException;
import jp.co.antenna.XfoJavaCtl.XfoFormatPageListener;
import jp.co.antenna.XfoJavaCtl.XfoObj;
import net.sf.saxon.s9api.XdmNode;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class FoAH implements FoProcessor {
    XProcRuntime runtime = null;
    Properties options = null;
    XStep step = null;
    XfoObj ah = null;

    public void initialize(XProcRuntime runtime, XStep step, Properties options) {
        this.runtime = runtime;
        this.step = step;
        this.options = options;
        try {
            ah = new XfoObj();
            ah.setFormatterType(XfoObj.S_FORMATTERTYPE_XSLFO);
            FoMessages msgs = new FoMessages();
            ah.setMessageListener(msgs);

            String s = getStringProp("OptionsFileURI");
            if (s != null) {
                ah.setOptionFileURI(s);
            }

            ah.setExitLevel(4);
            Integer i = getIntProp("ExitLevel");
            if (i != null) {
                ah.setExitLevel(i);
            }

            i = getIntProp("EmbedAllFontsEx");
            if (i != null) {
                ah.setPdfEmbedAllFontsEx(i);
            }

            i = getIntProp("ImageCompression");
            if (i != null) {
                ah.setPdfImageCompression(i);
            }

            Boolean b = getBooleanProp("NoAccessibility");
            if (b != null) {
                ah.setPdfNoAccessibility(b);
            }

            b = getBooleanProp("NoAddingOrChangingComments");
            if (b != null) {
                ah.setPdfNoAddingOrChangingComments(b);
            }

            b = getBooleanProp("NoAssembleDoc");
            if (b != null) {
                ah.setPdfNoAssembleDoc(b);
            }

            b = getBooleanProp("NoChanging");
            if (b != null) {
                ah.setPdfNoChanging(b);
            }

            b = getBooleanProp("NoContentCopying");
            if (b != null) {
                ah.setPdfNoContentCopying(b);
            }

            b = getBooleanProp("NoFillForm");
            if (b != null) {
                ah.setPdfNoFillForm(b);
            }

            b = getBooleanProp("NoPrinting");
            if (b != null) {
                ah.setPdfNoPrinting(b);
            }

            s = getStringProp("OwnersPassword");
            if (s != null) {
                ah.setPdfOwnerPassword(s);
            }

            b = getBooleanProp("TwoPassFormatting");
            if (b != null) {
                ah.setTwoPassFormatting(b);
            }
        } catch (XfoException xfoe) {
            throw new XProcException(xfoe);
        }
    }
    public void format(XdmNode doc, OutputStream out, String contentType) {
        String outputFormat = null;
        if (contentType == null || "application/pdf".equals(contentType)) {
            outputFormat = "@PDF";
        } else if ("application/PostScript".equals(contentType)) {
            outputFormat = "@PS";
        } else if ("image/svg+xml".equals(contentType)) {
            outputFormat = "@SVG";
        } else if ("application/vnd.inx".equals(contentType)) {
            outputFormat = "@INX";
        } else if ("application/vnd.mif".equals(contentType)) {
            outputFormat = "@MIF";
        } else if ("text/plain".equals(contentType)) {
            outputFormat = "@TXT";
        } else {
            throw new XProcException(step.getNode(), "Unsupported content-type on p:xsl-formatter: " + contentType);
        }

        try {
            // Holy hack on a cracker!
            ByteArrayInputStream bis = new ByteArrayInputStream(doc.toString().getBytes("UTF-8"));
            ah.render(bis, out, outputFormat);
            ah.releaseObjectEx();
        } catch (XfoException e) {
            if (runtime.getDebug()) {
                System.out.println("ErrorLevel = " + e.getErrorLevel() + "\nErrorCode = " + e.getErrorCode() + "\n" + e.getErrorMessage());
                e.printStackTrace();
            }
            throw new XProcException(e);
        } catch (UnsupportedEncodingException e) {
            // won't happen
            throw new XProcException(e);
        }
    }

    private String getStringProp(String name) {
        return options.getProperty(name);
    }

    private Integer getIntProp(String name) {
        String s = getStringProp(name);
        if (s != null) {
            try {
                int i = Integer.parseInt(s);
                return new Integer(i);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    private Boolean getBooleanProp(String name) {
        String s = options.getProperty(name);
        if (s != null) {
            return "true".equals(s);
        }
        return null;
    }

    private class FoMessages implements MessageListener, XfoFormatPageListener {
	    public void onMessage(int errLevel, int errCode, String errMessage) {
            switch (errLevel) {
                case 1:
                    step.info(step.getNode(), errMessage);
                    return;
                case 2:
                    step.warning(step.getNode(), errMessage);
                    return;
                default:
                    step.error(step.getNode(), errMessage, XProcConstants.stepError(errCode));
                    return;
            }
	    }

        public void onFormatPage(int pageNo) {
            step.finest(step.getNode(), "Formatted PDF page " + pageNo);
        }
    }
}
