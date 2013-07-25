/*
 * Exec.java
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

package com.xmlcalabash.library;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.XdmNode;

/**
 *
 * @author ndw
 */
public class Exec extends DefaultStep {
    private static final QName c_result = new QName("c", XProcConstants.NS_XPROC_STEP, "result");
    private static final QName c_line = new QName("c", XProcConstants.NS_XPROC_STEP, "line");
    private static final QName cx_show_stderr = new QName("cx", XProcConstants.NS_CALABASH_EX, "show-stderr");

    private static final QName _command = new QName("", "command");
    private static final QName _args = new QName("", "args");
    private static final QName _cwd = new QName("", "cwd");
    private static final QName _source_is_xml = new QName("", "source-is-xml");
    private static final QName _result_is_xml = new QName("", "result-is-xml");
    private static final QName _wrap_result_lines = new QName("", "wrap-result-lines");
    private static final QName _errors_is_xml = new QName("", "errors-is-xml");
    private static final QName _wrap_error_lines = new QName("", "wrap-error-lines");
    private static final QName _path_separator = new QName("", "path-separator");
    private static final QName _failure_threshold = new QName("", "failure-threshold");
    private static final QName _arg_separator = new QName("", "arg-separator");

    private ReadablePipe source = null;
    private WritablePipe result = null;
    private WritablePipe errors = null;
    private WritablePipe status = null;
    private String command = null;
    private String args = null;
    private String cwd = null;
    private boolean wrapResultLines = false;
    private boolean wrapErrorLines = false;
    private String pathSeparator = null;
    private boolean failureThreshold = false;
    private int failureThresholdValue = 0;
    private String argSeparator = null;
    private boolean sourceIsXML = false;
    private boolean resultIsXML = false;
    private boolean errorsIsXML = false;

    public Exec(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        if ("result".equals(port)) {
            result = pipe;
        } else if ("errors".equals(port)){
            errors = pipe;
        } else {
            status = pipe;
        }
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
        errors.resetWriter();
        status.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        if (runtime.getSafeMode()) {
            throw XProcException.dynamicError(21);
        }

        command = getOption(_command).getString();
        args = getOption(_args, (String) null);
        cwd = getOption(_cwd, (String) null);
        sourceIsXML = getOption(_source_is_xml, false);
        resultIsXML = getOption(_result_is_xml, false);
        errorsIsXML = getOption(_errors_is_xml, false);
        wrapResultLines = getOption(_wrap_result_lines, false);
        wrapErrorLines = getOption(_wrap_error_lines, false);
        if (getOption(_path_separator) != null) {
            pathSeparator = getOption(_path_separator).getString();
            if (pathSeparator.length() != 1) {
                throw XProcException.stepError(63);
            }
        }
        if (getOption(_failure_threshold) != null) {
            failureThreshold = true;
            failureThresholdValue = Integer.parseInt(getOption(_failure_threshold).getString());
        }
        if (getOption(_arg_separator) != null) {
            argSeparator = getOption(_arg_separator).getString();
            if (argSeparator.length() != 1) {
                throw XProcException.stepError(66);
            }
        }

        String slash = System.getProperty("file.separator");

        if (command == null || "".equals(command)) {
            throw XProcException.stepError(33);
        }

        if ((resultIsXML && wrapResultLines) || (errorsIsXML && wrapErrorLines)) {
            throw new XProcException(XProcException.stepError(34));
        }

        if (pathSeparator != null) {
            command = command.replaceAll(Pattern.quote(pathSeparator), slash);
        }

        String showCmd = "";
        try {
            List<String> command_line = new ArrayList<String>();
            command_line.add(command);
            showCmd += command;
            if (args != null && !"".equals(args)) {
                if (pathSeparator != null) {
                    args = args.replaceAll(Pattern.quote(pathSeparator), slash);
                }
                String[] arglist = args.split("\\" + argSeparator);
                for (String arg : arglist) {
                    command_line.add(arg);
                    showCmd += " " + arg;
                }
            }

            ProcessBuilder builder = new ProcessBuilder(command_line);
            if (cwd != null) {
                File dir = new File(cwd);
                if (!dir.isDirectory() || !dir.canRead()) {
                    throw XProcException.stepError(34, "Cannot change to requested directory: " + cwd);
                }
                builder.directory(new File(cwd));
            }

            fine(step.getNode(), "Exec: " + showCmd);

            Process process = builder.start();

            if (source.moreDocuments()) {
                XdmNode srcDoc = source.read();

                if (source.moreDocuments()) {
                    throw XProcException.dynamicError(6);
                }

                OutputStream os = process.getOutputStream();

                Serializer serializer = makeSerializer();

                // FIXME: there must be a better way to print text descendants
                String queryexpr = null;
                if (sourceIsXML) {
                    queryexpr = ".";
                } else {
                    queryexpr = "//text()";
                    serializer.setOutputProperty(Serializer.Property.METHOD, "text");
                    serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
                }

                Processor qtproc = runtime.getProcessor();
                XQueryCompiler xqcomp = qtproc.newXQueryCompiler();
                xqcomp.setModuleURIResolver(runtime.getResolver());
                XQueryExecutable xqexec = xqcomp.compile(queryexpr);
                XQueryEvaluator xqeval = xqexec.load();
                xqeval.setContextItem(srcDoc);

                serializer.setOutputStream(os);
                xqeval.setDestination(serializer);
                xqeval.run();

                os.close();
            } else {
                OutputStream os = process.getOutputStream();
                os.close();
            }

            boolean showStderr = !"false".equals(step.getExtensionAttribute(cx_show_stderr));
            ProcessOutputReader stdoutReader = new ProcessOutputReader(process.getInputStream(), resultIsXML, wrapResultLines, false);
            ProcessOutputReader stderrReader = new ProcessOutputReader(process.getErrorStream(), errorsIsXML, wrapErrorLines, showStderr);

            Thread stdoutThread = new Thread(stdoutReader);
            Thread stderrThread = new Thread(stderrReader);

            stdoutThread.start();
            stderrThread.start();

            int rc = 0;
            try {
                rc = process.waitFor();
                stdoutThread.join();
                stderrThread.join();
            } catch (InterruptedException tie) {
                throw new XProcException(tie);
            }

            if (failureThreshold && (rc > failureThresholdValue)) {
                throw XProcException.stepError(64);
            }

            TreeWriter tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(c_result);
            tree.startContent();
            tree.addText("" + rc);
            tree.addEndElement();
            tree.endDocument();
            XdmNode execResult = tree.getResult();
            status.write(execResult);

            execResult = stdoutReader.getResult();
            result.write(execResult);

            execResult = stderrReader.getResult();
            errors.write(execResult);
        } catch (IOException ex) {
            throw new XProcException(ex);
        }
    }

    private static class ParseArgs {
        private static final String dq = "\uE000";
        private static final String sq = "\uE001";
        private static final Pattern squoted = Pattern.compile("(^.*?)'(.*?)'(.*)$");
        private static final Pattern dquoted = Pattern.compile("(^.*?)\"(.*?)\"(.*)$");

        protected ParseArgs() {
        }

        public static String[] parse(String argstring) {
            Vector<String> args = new Vector<String> ();

            argstring = argstring.replaceAll("\"\"", dq).replaceAll("\'\'", sq);

            Matcher qs = dquoted.matcher(argstring);
            if (qs.matches()) {
                String[] pre = parse(qs.group(1).trim());
                String quoted = qs.group(2);
                String[] post = parse(qs.group(3).trim());
                for (String a : pre) {
                    args.add(fixup(a));
                }
                args.add(fixup(quoted));
                for (String a : post) {
                    args.add(fixup(a));
                }
                return args.toArray(new String[] { " " });
            }

            qs = squoted.matcher(argstring);
            if (qs.matches()) {
                String[] pre = parse(qs.group(1));
                String quoted = qs.group(2);
                String[] post = parse(qs.group(3));
                for (String a : pre) {
                    args.add(fixup(a));
                }
                args.add(fixup(quoted));
                for (String a : post) {
                    args.add(fixup(a));
                }
                return args.toArray(new String[] { " " });
            }

            return fixup(argstring).split("\\s+");
        }

        private static String fixup(String s) {
            return s.replaceAll(dq, "\"").replaceAll(sq,"\'");
        }
    }

    private class ProcessOutputReader implements Runnable {
        private InputStream is;
        private boolean asXML;
        private boolean showLines;
        private boolean wrapLines;
        private TreeWriter tree;

        public ProcessOutputReader(InputStream is, boolean asXML, boolean wrapLines, boolean showLines) {
            this.is = is;
            this.asXML = asXML;
            this.wrapLines = wrapLines;
            this.showLines = showLines;

            tree = new TreeWriter(runtime);
        }

        public XdmNode getResult() {
            return tree.getResult();
        }

        public void run() {
            tree.startDocument(step.getNode().getBaseURI());

            tree.addStartElement(c_result);
            tree.startContent();

            if (asXML) {
                XdmNode doc = runtime.parse(new InputSource(is));
                tree.addSubtree(doc);
            } else {
                // If we're not wrapping the lines, a buffered reader doesn't work. It can't
                // tell the difference between a file with a trailing EOL and one without.
                try {
                    if (wrapLines) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line = br.readLine();
                        while (line != null) {
                            if (showLines) {
                                System.err.println(line);
                            }
                            tree.addStartElement(c_line);
                            tree.startContent();
                            tree.addText(line);
                            tree.addEndElement();
                            tree.addText("\n");
                            line = br.readLine();
                        }
                    } else {
                        InputStreamReader r = new InputStreamReader(is);
                        char[] buf = new char[1000];
                        int len = r.read(buf,0,buf.length);
                        while (len >= 0) {
                            if (len == 0) {
                                Thread.sleep(1000);
                                continue;
                            }
                            String s = new String(buf,0,len);
                            if (showLines) {
                                System.err.print(s);
                            }
                            tree.addText(s);
                            len = r.read(buf,0,buf.length);
                        }
                    }
                } catch (IOException ioe) {
                    throw new XProcException(ioe);
                } catch (InterruptedException ie) {
                    // who cares?
                }
            }

            tree.addEndElement();
            tree.endDocument();
        }
    }
}
