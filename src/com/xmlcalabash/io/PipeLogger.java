package com.xmlcalabash.io;

import com.xmlcalabash.model.Log;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.LogOptions;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcConstants;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.QName;

import java.io.File;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.util.GregorianCalendar;
import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Sep 28, 2008
 * Time: 3:15:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class PipeLogger {
    private static final QName cx_basename = new QName("cx", XProcConstants.NS_CALABASH_EX, "basename");
    private static final QName cx_logstyle = new QName("cx", XProcConstants.NS_CALABASH_EX, "logstyle");
    private Log log = null;
    private Serializer serializer = null;
    private PrintStream stream = null;
    private XProcRuntime runtime = null;
    private boolean logging = false;
    private XProcConfiguration config = null;
    private LogOptions logstyle = null;
    private String basename = null;
    private File baseDir = null;
    private int outputCount = 1;


    public PipeLogger(XProcRuntime xproc, Log log) {
        runtime = xproc;
        this.log = log;
        config = xproc.getConfiguration();

        basename = runtime.getEpisode();
        logstyle = config.logOpt;

        String ext = log.getExtensionAttribute(cx_basename);
        if (ext != null) {
            basename = ext;
        }

        ext = log.getExtensionAttribute(cx_logstyle);
        if (logstyle != LogOptions.OFF && ext != null) {
            if (ext.equals("off")) {
                logstyle = LogOptions.OFF;
            } else if (ext.equals("plain")) {
                logstyle = LogOptions.PLAIN;
            } else if (ext.equals("wrapped")) {
                logstyle = LogOptions.WRAPPED;
            } else if (ext.equals("directory")) {
                logstyle = LogOptions.DIRECTORY;
            } else {
                System.err.println("Invalid cx:logstyle ignored: " + ext);
            }
        }

        if (logstyle == LogOptions.OFF) {
            // there's nothing to do here...
            return;
        }

        serializer = new Serializer();
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        serializer.setOutputProperty(Serializer.Property.ENCODING, "utf-8");
        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");

        switch (logstyle) {
            case PLAIN:
            case WRAPPED:
                if (log.getHref() == null) {
                    stream = System.out;
                } else {
                    try {
                        String href = log.getHref().toASCIIString();
                        if (href.startsWith("file:///")) {
                            href = href.substring(7);
                        } else if (href.startsWith("file:/")) {
                            href = href.substring(5);
                        }
                        stream = new PrintStream(new File(href));
                    } catch (FileNotFoundException fnfe) {
                        System.err.println("Failed to create log: " + log.getHref());
                        stream = System.err;
                    }
                }
                break;
            case DIRECTORY:
                String dirstr = null;

                if (log.getHref() != null) {
                    if ((log.getHref().getScheme() == null) || log.getHref().getScheme().equals("file")) {
                        dirstr = log.getHref().getPath();
                    } else {
                        System.err.println("Only file: scheme URIs are supported for directory logging.");
                        logstyle = LogOptions.WRAPPED;
                        stream = System.err;
                        break;
                    }
                } else {
                    System.err.println("Directory logging requires a directory.");
                    logstyle = LogOptions.WRAPPED;
                    stream = System.out;
                    break;
                }

                baseDir = new File(dirstr);
                if (baseDir.isDirectory()) {
                    // ok
                } else if (baseDir.exists()) {
                    System.err.println("Log location is not a directory: " + log.getHref());
                    logstyle = LogOptions.WRAPPED;
                    stream = System.err;
                } else {
                    try {
                        baseDir.mkdirs();
                    } catch (Exception e) {
                        System.err.println("Could not create log directory: " + log.getHref());
                        logstyle = LogOptions.WRAPPED;
                        stream = System.err;
                    }
                }

                break;
            case OFF:
            default:
                break;
        }

        serializer.setOutputStream(stream);
    }

    private String dateTime() {
        GregorianCalendar cal = new GregorianCalendar();
        String rfc822tz = String.format("%1$tz", cal);
        // I assume it's either -0500 or +0100 or something like that...
        return String.format("%1$tFT%1$tT", cal) + rfc822tz.substring(0,3) + ":" + rfc822tz.substring(3);
    }

    public void startLogging() {
        String dt = dateTime();

        if (logstyle == LogOptions.OFF) {
            return;
        }

        if (logstyle == LogOptions.DIRECTORY) {
            // nop;
        } else {
            if (logstyle == LogOptions.WRAPPED) {
                stream.println("<px:document-sequence xmlns:px='http://xmlcalabash.com/ns/document-sequence'");
                stream.println("                      port='" + log.getPort() + "'");
                stream.println("                      xpl-file='" + log.xplFile() + "'");
                stream.println("                      xpl-line='" + log.xplLine() + "'");
                stream.println("                      dateTime='" + dt + "'>");
            } else {
                stream.println("<!-- Start of Calabash output " + log + " on " + dt + " -->");
            }
        }

        logging = true;
    }

    public void stopLogging() {
        if (logging) {
            if (logstyle == LogOptions.WRAPPED) {
                stream.print("</px:document-sequence>");
            }

            if (logstyle == LogOptions.PLAIN) {
                stream.print("\n");
                stream.println("<!-- End of Calabash output log -->");
            }
        }

        logging = false;
    }

    public void log(XdmNode node) {
        if (logstyle == LogOptions.OFF) {
            // there's nothing to do here...
            return;
        }

        if (!logging) {
            startLogging();
        }

        switch (logstyle) {
            case WRAPPED:
                stream.print("<px:document>");
                try {
                    S9apiUtils.serialize(runtime, node, serializer);
                } catch (SaxonApiException sae) {
                    System.err.println("Logging failed: " + sae);
                }
                stream.println("</px:document>");
                break;
            case PLAIN:
                try {
                    S9apiUtils.serialize(runtime, node, serializer);
                } catch (SaxonApiException sae) {
                    System.err.println("Logging failed: " + sae);
                }
                break;
            case DIRECTORY:
                String filename = String.format("%1$s-%2$04d.xml", basename, outputCount++);
                File output = new File(baseDir, filename);
                try {
                    stream = new PrintStream(output);
                } catch (FileNotFoundException fnfe) {
                    System.err.println("Failed to create log: " + log.getHref());
                    stream = System.err;
                }

                serializer.setOutputStream(stream);

                stream.println("<!-- Start of Calabash output " + log + " on " + dateTime() + " -->");

                try {
                    S9apiUtils.serialize(runtime, node, serializer);
                } catch (SaxonApiException sae) {
                    System.err.println("Logging failed: " + sae);
                }
                stream.close();
                break;
            default:
                break;
        }
    }
}
