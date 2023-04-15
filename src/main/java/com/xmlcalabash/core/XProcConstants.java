/*
 * XProcConstants.java
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

import javax.xml.XMLConstants;

import net.sf.saxon.om.NamespaceUri;
import net.sf.saxon.s9api.QName;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author ndw
 */
public class XProcConstants {
    public static final String XPROC_VERSION = initializeVersion();

    public static final NamespaceUri NS_XML = NamespaceUri.of("http://www.w3.org/XML/1998/namespace");
    public static final NamespaceUri NS_XML_ATTR = NamespaceUri.of("http://www.w3.org/2000/xmlns/");
    public static final NamespaceUri NS_XPROC = NamespaceUri.of("http://www.w3.org/ns/xproc");
    public static final NamespaceUri NS_XQT_ERRORS = NamespaceUri.of("http://www.w3.org/2005/xqt-errors");
    public static final NamespaceUri NS_XPROC_ERROR = NamespaceUri.of("http://www.w3.org/ns/xproc-error");
    public static final NamespaceUri NS_XPROC_ERROR_EX = NamespaceUri.of("http://xproc.org/ns/errors");
    public static final NamespaceUri NS_XPROC_STEP = NamespaceUri.of("http://www.w3.org/ns/xproc-step");
    public static final NamespaceUri NS_CALABASH_EX = NamespaceUri.of("http://xmlcalabash.com/ns/extensions");
    public static final NamespaceUri NS_XMLSCHEMA = NamespaceUri.of("http://www.w3.org/2001/XMLSchema");
    public static final NamespaceUri NS_CALABASH_CONFIG = NamespaceUri.of("http://xmlcalabash.com/ns/configuration");
    public static final NamespaceUri NS_EXPROC_CONFIG = NamespaceUri.of("http://exproc.org/ns/configuration");
    public static final NamespaceUri NS_EXPROC_FUNCTIONS = NamespaceUri.of("http://exproc.org/standard/functions");

    public static final NamespaceUri CALABASH_EXTENSION_LIBRARY_1_0 = NamespaceUri.of("http://xmlcalabash.com/extension/steps/library-1.0.xpl");
    public static final QName p_pipeline = XProcConstants.qNameFor(NS_XPROC,"pipeline");
    public static final QName p_library = XProcConstants.qNameFor(NS_XPROC,"library");
    public static final QName p_declare_step = XProcConstants.qNameFor(NS_XPROC,"declare-step");
    public static final QName p_for_each = XProcConstants.qNameFor(NS_XPROC,"for-each");
    public static final QName p_viewport = XProcConstants.qNameFor(NS_XPROC,"viewport");
    public static final QName p_viewport_source = XProcConstants.qNameFor(NS_XPROC,"viewport-source");
    public static final QName p_choose = XProcConstants.qNameFor(NS_XPROC,"choose");
    public static final QName p_otherwise = XProcConstants.qNameFor(NS_XPROC,"otherwise");
    public static final QName p_xpath_context = XProcConstants.qNameFor(NS_XPROC,"xpath-context");
    public static final QName p_when = XProcConstants.qNameFor(NS_XPROC,"when");
    public static final QName p_group = XProcConstants.qNameFor(NS_XPROC,"group");
    public static final QName p_try = XProcConstants.qNameFor(NS_XPROC,"try");
    public static final QName p_catch = XProcConstants.qNameFor(NS_XPROC,"catch");
    public static final QName p_iteration_source = XProcConstants.qNameFor(NS_XPROC,"iteration-source");
    public static final QName p_import = XProcConstants.qNameFor(NS_XPROC,"import");
    public static final QName p_log = XProcConstants.qNameFor(NS_XPROC,"log");
    public static final QName p_input = XProcConstants.qNameFor(NS_XPROC,"input");
    public static final QName p_pipe = XProcConstants.qNameFor(NS_XPROC,"pipe");
    public static final QName p_document = XProcConstants.qNameFor(NS_XPROC,"document");
    public static final QName p_data = XProcConstants.qNameFor(NS_XPROC,"data");
    public static final QName p_inline = XProcConstants.qNameFor(NS_XPROC,"inline");
    public static final QName p_empty = XProcConstants.qNameFor(NS_XPROC,"empty");
    public static final QName p_output = XProcConstants.qNameFor(NS_XPROC,"output");
    public static final QName p_option = XProcConstants.qNameFor(NS_XPROC,"option");
    public static final QName p_with_option = XProcConstants.qNameFor(NS_XPROC,"with-option");
    public static final QName p_parameter = XProcConstants.qNameFor(NS_XPROC,"parameter");
    public static final QName p_with_param = XProcConstants.qNameFor(NS_XPROC,"with-param");
    public static final QName p_variable = XProcConstants.qNameFor(NS_XPROC,"variable");
    public static final QName p_namespaces = XProcConstants.qNameFor(NS_XPROC,"namespaces");
    public static final QName p_serialization = XProcConstants.qNameFor(NS_XPROC,"serialization");
    public static final QName p_documentation = XProcConstants.qNameFor(NS_XPROC, "documentation");
    public static final QName p_pipeinfo = XProcConstants.qNameFor(NS_XPROC, "pipeinfo");
    public static final QName p_in_scope_names = XProcConstants.qNameFor(NS_XPROC, "in-scope-names");
    public static final QName p_template = XProcConstants.qNameFor(NS_XPROC, "template");
    public static final QName p_document_template = XProcConstants.qNameFor(NS_XPROC, "document-template"); // DEPRECATED

    public static final QName cx_until_unchanged = XProcConstants.qNameFor(NS_CALABASH_EX, "until-unchanged");

    public static final QName p_iteration_position = XProcConstants.qNameFor(XProcConstants.NS_XPROC, "iteration-position");

    public static final QName p_episode = XProcConstants.qNameFor(XProcConstants.NS_XPROC, "episode");
    public static final QName p_language = XProcConstants.qNameFor(XProcConstants.NS_XPROC, "language");
    public static final QName p_product_name = XProcConstants.qNameFor(XProcConstants.NS_XPROC, "product-name");
    public static final QName p_product_version = XProcConstants.qNameFor(XProcConstants.NS_XPROC, "product-version");
    public static final QName p_vendor = XProcConstants.qNameFor(XProcConstants.NS_XPROC, "vendor");
    public static final QName p_vendor_uri = XProcConstants.qNameFor(XProcConstants.NS_XPROC, "vendor-uri");
    public static final QName p_version = XProcConstants.qNameFor(XProcConstants.NS_XPROC, "version");
    public static final QName p_xpath_version = XProcConstants.qNameFor(XProcConstants.NS_XPROC, "xpath-version");
    public static final QName p_psvi_supported = XProcConstants.qNameFor(XProcConstants.NS_XPROC, "psvi-supported");

    public static final QName c_body = XProcConstants.qNameFor("c",XProcConstants.NS_XPROC_STEP, "body");
    public static final QName c_multipart = XProcConstants.qNameFor("c",XProcConstants.NS_XPROC_STEP, "multipart");
    public static final QName c_header = XProcConstants.qNameFor("c",XProcConstants.NS_XPROC_STEP, "header");
    public static final QName c_data = XProcConstants.qNameFor("c",XProcConstants.NS_XPROC_STEP, "data");
    public static final QName c_content_type = XProcConstants.qNameFor("c",XProcConstants.NS_XPROC_STEP, "content-type");
    public static final QName c_result = XProcConstants.qNameFor("c",XProcConstants.NS_XPROC_STEP, "result");
    public static final QName c_request = XProcConstants.qNameFor("c",XProcConstants.NS_XPROC_STEP, "request");
    public static final QName c_response = XProcConstants.qNameFor("c",XProcConstants.NS_XPROC_STEP, "response");
    public static final QName c_param = XProcConstants.qNameFor("c",NS_XPROC_STEP, "param");
    public static final QName c_param_set = XProcConstants.qNameFor("c",NS_XPROC_STEP, "param-set");
    public static final QName c_errors = XProcConstants.qNameFor("c",NS_XPROC_STEP, "errors");
    public static final QName c_error = XProcConstants.qNameFor("c",NS_XPROC_STEP, "error");

    public static final QName xml_base = XProcConstants.qNameFor("xml", NamespaceUri.of(XMLConstants.XML_NS_URI), "base");
    public static final QName xml_lang = XProcConstants.qNameFor("xml", NamespaceUri.of(XMLConstants.XML_NS_URI), "lang");
    public static final QName xml_id = XProcConstants.qNameFor("xml", NamespaceUri.of(XMLConstants.XML_NS_URI), "id");

    public static final QName cx_depends_on = XProcConstants.qNameFor("cx",NS_CALABASH_EX,"depends-on");
    public static final QName cx_cache = XProcConstants.qNameFor("cx",NS_CALABASH_EX,"cache");
    public static final QName cx_type = XProcConstants.qNameFor("cx",NS_CALABASH_EX,"type");

    public static final QName xs_QName = XProcConstants.qNameFor("xs", NS_XMLSCHEMA, "QName");
    public static final QName xs_untypedAtomic = XProcConstants.qNameFor("xs", NS_XMLSCHEMA, "untypedAtomic");
    public static final QName xs_string = XProcConstants.qNameFor("xs", NS_XMLSCHEMA, "string");
    public static final QName xs_anyURI = XProcConstants.qNameFor("xs", NS_XMLSCHEMA, "anyURI");
    public static final QName xs_NCName = XProcConstants.qNameFor("xs", NS_XMLSCHEMA, "NCName");
    public static final QName xs_boolean = XProcConstants.qNameFor("xs", NS_XMLSCHEMA, "boolean");
    public static final QName xs_decimal = XProcConstants.qNameFor("xs", NS_XMLSCHEMA, "decimal");
    public static final QName xs_double = XProcConstants.qNameFor("xs", NS_XMLSCHEMA, "double");
    public static final QName xs_integer = XProcConstants.qNameFor("xs", NS_XMLSCHEMA, "integer");
    public static final QName xs_float = XProcConstants.qNameFor("xs", NS_XMLSCHEMA, "float");


    /** Creates a new instance of XProcConstants */
    protected XProcConstants() {
    }

    private static String initializeVersion() {
        Properties config = new Properties();
        InputStream stream = null;
        try {
            stream = XProcConstants.class.getResourceAsStream("/etc/version.properties");
            if (stream == null) {
                throw new UnsupportedOperationException("JAR file doesn't contain version.properties file!?");
            }
            config.load(stream);
            String version = config.getProperty("version");
            if (version == null) {
                throw new UnsupportedOperationException("Invalid version.properties in JAR file!?");
            }
            return version;
        } catch (IOException ioe) {
            throw new UnsupportedOperationException("No version.properties in JAR file!?");
        }
    }

    public static QName qNameFor(NamespaceUri uri, String localName) {
        if (uri == NS_XML) {
            return XProcConstants.qNameFor("xml", uri, localName);
        }
        if (uri == NS_XPROC) {
            return XProcConstants.qNameFor("p", uri, localName);
        }
        if (uri == NS_XMLSCHEMA) {
            return XProcConstants.qNameFor("xs", uri, localName);
        }
        if (uri == NS_XPROC_STEP) {
            return XProcConstants.qNameFor("c", uri, localName);
        }
        if (uri == NS_CALABASH_EX) {
            return XProcConstants.qNameFor("cx", uri, localName);
        }
        return new QName(uri.toString(), localName);
    }

    public static QName qNameFor(String prefix, NamespaceUri uri, String localName) {
        if (prefix == null || "".equals(prefix)) {
            return new QName(uri.toString(), localName);
        }
        return new QName(prefix, uri.toString(), localName);
    }

    public static QName staticError(int errno) {
        String localName = String.format("XS%04d", errno);
        return XProcConstants.qNameFor("err", NS_XPROC_ERROR, localName);
    }

    public static QName dynamicError(int errno) {
        String localName = String.format("XD%04d", errno);
        return XProcConstants.qNameFor("err", NS_XPROC_ERROR, localName);

    }

    public static QName stepError(int errno) {
        String localName = String.format("XC%04d", errno);
        return XProcConstants.qNameFor("err", NS_XPROC_ERROR, localName);
    }
}
