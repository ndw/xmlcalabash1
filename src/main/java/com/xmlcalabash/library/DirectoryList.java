/*
 * DirectoryList.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.util.MessageFormatter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DataStore;
import com.xmlcalabash.io.DataStore.DataInfo;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.URIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ndw
 */

@XMLCalabash(
        name = "p:directory-list",
        type = "{http://www.w3.org/ns/xproc}directory-list")

public class DirectoryList extends DefaultStep {
    private static final QName _name = new QName("", "name");
    private static final QName _path = new QName("", "path");
    private static final QName _include_filter = new QName("", "include-filter");
    private static final QName _exclude_filter = new QName("", "exclude-filter");
    private static final QName c_directory = new QName("c", XProcConstants.NS_XPROC_STEP, "directory");
    private static final QName c_file = new QName("c", XProcConstants.NS_XPROC_STEP, "file");
    private static final QName px_show_excluded = new QName(XProcConstants.NS_CALABASH_EX, "show-excluded");
    private WritablePipe result = null;
    private String path = ".";
    private String inclFilter = null;
    private String exclFilter = null;

    /** Creates a new instance of DirectoryList */
    public DirectoryList(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        if (runtime.getSafeMode()) {
            throw XProcException.dynamicError(21);
        }

        if (getOption(_path) != null) {
            URI pathbase = getOption(_path).getBaseURI();
            String pathstr = URIUtils.encode(getOption(_path).getString());
            path = pathbase.resolve(pathstr).toASCIIString();
        } else {
            path = step.getNode().getBaseURI().resolve(".").toASCIIString();
        }

        if (!path.endsWith("/")) {
            path = path + "/";
        }

        logger.trace(MessageFormatter.nodeMessage(step.getNode(), "path: " + path));

        RuntimeValue value = getOption(_include_filter);
        if (value != null) {
            inclFilter = value.getString();
            logger.trace(MessageFormatter.nodeMessage(step.getNode(), "include: " + inclFilter));
        }
        value = getOption(_exclude_filter);
        if (value != null) {
            exclFilter = value.getString();
            logger.trace(MessageFormatter.nodeMessage(step.getNode(), "exclude: " + exclFilter));
        }

        final boolean showExcluded = "true".equals(step.getExtensionAttribute(px_show_excluded));

        final URI uri = URI.create("file:///").resolve(path);
        final TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_directory);
        tree.addAttribute(_name, getName(uri));
        tree.addAttribute(XProcConstants.xml_base, uri.toASCIIString());
        tree.startContent();

        final DataStore store = runtime.getDataStore();
        try {
            store.listEachEntry(path, "file:///", "*/*", new DataInfo() {
                public void list(URI id, String media, long lastModified) throws IOException {
                    boolean use = true;
                    String filename = getName(id);

                    logger.trace(MessageFormatter.nodeMessage(step.getNode(), "name: " + filename));

                    if (inclFilter != null) {
                        use = filename.matches(inclFilter);
                        logger.trace(MessageFormatter.nodeMessage(step.getNode(), "include: " + use));
                    }

                    if (exclFilter != null) {
                        use = use && !filename.matches(exclFilter);
                        logger.trace(MessageFormatter.nodeMessage(step.getNode(), "exclude: " + !use));
                    }

                    if (use) {
                        if (!isFile(id)) {
                            tree.addStartElement(c_directory);
                            tree.addAttribute(_name, filename);
                            tree.addEndElement();
                            logger.trace(MessageFormatter.nodeMessage(step.getNode(), "Including directory: " + filename));
                        } else {
                            tree.addStartElement(c_file);
                            tree.addAttribute(_name, filename);
                            tree.addEndElement();
                            logger.trace(MessageFormatter.nodeMessage(step.getNode(), "Including file: " + filename));
                        }
                    } else if (showExcluded) {
                        tree.addComment(" excluded: " + filename + " ");
                        logger.trace(MessageFormatter.nodeMessage(step.getNode(), "Excluding: " + filename));
                    }
                }
            });
        } catch (FileNotFoundException e) {
            throw XProcException.stepError(17);
        } catch (IOException e) {
            throw XProcException.stepError(12);
        }

        tree.addEndElement();
        tree.endDocument();

        result.write(tree.getResult());
    }

    String getName(URI id) {
        String path = id.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.replaceAll("^.*/", "");
	}

	boolean isFile(URI id) throws IOException {
		final String entry = id.toASCIIString();
		final List<String> entries = new ArrayList<String>();
		DataStore store = runtime.getDataStore();
		store.infoEntry(entry, entry, "*/*", new DataInfo() {
			public void list(URI id, String media, long lastModified)
					throws IOException {
				if (media != null) {
					entries.add(media);
				}
			}
		});
		return !entries.isEmpty();
	}
}

