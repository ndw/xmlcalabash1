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
    public static final String NS_XPROC = "http://www.w3.org/ns/xproc";
    public static final String NS_XPROC_ERROR = "http://www.w3.org/ns/xproc-error";
    public static final String NS_XPROC_ERROR_EX = "http://xproc.org/ns/errors";
    public static final String NS_XPROC_STEP = "http://www.w3.org/ns/xproc-step";
    public static final String NS_CALABASH_EX = "http://xmlcalabash.com/ns/extensions";
    public static final String NS_XMLSCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String NS_CALABASH_CONFIG = "http://xmlcalabash.com/ns/configuration";
    public static final String NS_EXPROC_CONFIG = "http://exproc.org/ns/configuration";
    public static final String NS_EXPROC_FUNCTIONS = "http://exproc.org/standard/functions";

    public static final String CALABASH_EXTENSION_LIBRARY_1_0 = "http://xmlcalabash.com/extension/steps/library-1.0.xpl";

    public static final QName p_pipeline = new QName(NS_XPROC,"pipeline");
    public static final QName p_library = new QName(NS_XPROC,"library");
    public static final QName p_declare_step = new QName(NS_XPROC,"declare-step");
    public static final QName p_for_each = new QName(NS_XPROC,"for-each");
    public static final QName p_viewport = new QName(NS_XPROC,"viewport");
    public static final QName p_viewport_source = new QName(NS_XPROC,"viewport-source");
    public static final QName p_choose = new QName(NS_XPROC,"choose");
    public static final QName p_otherwise = new QName(NS_XPROC,"otherwise");
    public static final QName p_xpath_context = new QName(NS_XPROC,"xpath-context");
    public static final QName p_when = new QName(NS_XPROC,"when");
    public static final QName p_group = new QName(NS_XPROC,"group");
    public static final QName p_try = new QName(NS_XPROC,"try");
    public static final QName p_catch = new QName(NS_XPROC,"catch");
    public static final QName p_iteration_source = new QName(NS_XPROC,"iteration-source");
    public static final QName p_import = new QName(NS_XPROC,"import");
    public static final QName p_log = new QName(NS_XPROC,"log");
    public static final QName p_input = new QName(NS_XPROC,"input");
    public static final QName p_pipe = new QName(NS_XPROC,"pipe");
    public static final QName p_document = new QName(NS_XPROC,"document");
    public static final QName p_data = new QName(NS_XPROC,"data");
    public static final QName p_inline = new QName(NS_XPROC,"inline");
    public static final QName p_empty = new QName(NS_XPROC,"empty");
    public static final QName p_output = new QName(NS_XPROC,"output");
    public static final QName p_option = new QName(NS_XPROC,"option");
    public static final QName p_with_option = new QName(NS_XPROC,"with-option");
    public static final QName p_parameter = new QName(NS_XPROC,"parameter");
    public static final QName p_with_param = new QName(NS_XPROC,"with-param");
    public static final QName p_variable = new QName(NS_XPROC,"variable");
    public static final QName p_namespaces = new QName(NS_XPROC,"namespaces");
    public static final QName p_serialization = new QName(NS_XPROC,"serialization");
    public static final QName p_documentation = new QName(NS_XPROC, "documentation");
    public static final QName p_pipeinfo = new QName(NS_XPROC, "pipeinfo");
    public static final QName p_in_scope_names = new QName(NS_XPROC, "in-scope-names");
    public static final QName p_template = new QName(NS_XPROC, "template");
    public static final QName p_document_template = new QName(NS_XPROC, "document-template"); // DEPRECATED

    public static final QName cx_until_unchanged = new QName(NS_CALABASH_EX, "until-unchanged");

    public static final QName p_iteration_position = new QName(XProcConstants.NS_XPROC, "iteration-position");

    public static final QName p_episode = new QName(XProcConstants.NS_XPROC, "episode");
    public static final QName p_language = new QName(XProcConstants.NS_XPROC, "language");
    public static final QName p_product_name = new QName(XProcConstants.NS_XPROC, "product-name");
    public static final QName p_product_version = new QName(XProcConstants.NS_XPROC, "product-version");
    public static final QName p_vendor = new QName(XProcConstants.NS_XPROC, "vendor");
    public static final QName p_vendor_uri = new QName(XProcConstants.NS_XPROC, "vendor-uri");
    public static final QName p_version = new QName(XProcConstants.NS_XPROC, "version");
    public static final QName p_xpath_version = new QName(XProcConstants.NS_XPROC, "xpath-version");
    public static final QName p_psvi_supported = new QName(XProcConstants.NS_XPROC, "psvi-supported");

    public static final QName c_body = new QName("c",XProcConstants.NS_XPROC_STEP, "body");
    public static final QName c_multipart = new QName("c",XProcConstants.NS_XPROC_STEP, "multipart");
    public static final QName c_header = new QName("c",XProcConstants.NS_XPROC_STEP, "header");
    public static final QName c_data = new QName("c",XProcConstants.NS_XPROC_STEP, "data");
    public static final QName c_content_type = new QName("c",XProcConstants.NS_XPROC_STEP, "content-type");
    public static final QName c_result = new QName("c",XProcConstants.NS_XPROC_STEP, "result");
    public static final QName c_request = new QName("c",XProcConstants.NS_XPROC_STEP, "request");
    public static final QName c_response = new QName("c",XProcConstants.NS_XPROC_STEP, "response");
    public static final QName c_param = new QName("c",NS_XPROC_STEP, "param");
    public static final QName c_param_set = new QName("c",NS_XPROC_STEP, "param-set");
    public static final QName c_errors = new QName("c",NS_XPROC_STEP, "errors");
    public static final QName c_error = new QName("c",NS_XPROC_STEP, "error");

    public static final QName xml_base = new QName("xml", XMLConstants.XML_NS_URI, "base");
    public static final QName xml_lang = new QName("xml", XMLConstants.XML_NS_URI, "lang");
    public static final QName xml_id = new QName("xml", XMLConstants.XML_NS_URI, "id");

    public static final QName cx_depends_on = new QName("cx",NS_CALABASH_EX,"depends-on");
    public static final QName cx_cache = new QName("cx",NS_CALABASH_EX,"cache");
    public static final QName cx_type = new QName("cx",NS_CALABASH_EX,"type");

    public static final QName xs_QName = new QName("xs", NS_XMLSCHEMA, "QName");
    public static final QName xs_untypedAtomic = new QName("xs", NS_XMLSCHEMA, "untypedAtomic");
    public static final QName xs_string = new QName("xs", NS_XMLSCHEMA, "string");
    public static final QName xs_anyURI = new QName("xs", NS_XMLSCHEMA, "anyURI");
    public static final QName xs_NCName = new QName("xs", NS_XMLSCHEMA, "NCName");
    public static final QName xs_boolean = new QName("xs", NS_XMLSCHEMA, "boolean");
    public static final QName xs_decimal = new QName("xs", NS_XMLSCHEMA, "decimal");
    public static final QName xs_double = new QName("xs", NS_XMLSCHEMA, "double");
    public static final QName xs_integer = new QName("xs", NS_XMLSCHEMA, "integer");
    public static final QName xs_float = new QName("xs", NS_XMLSCHEMA, "float");


    /** Creates a new instance of XProcConstants */
    protected XProcConstants() {
    }

    private static String initializeVersion() {
        String sver = "(for Saxon 9.4.x)";
        Properties config = new Properties();
        InputStream stream = null;
        try {
            stream = XProcConstants.class.getResourceAsStream("/etc/version.properties");
            if (stream == null) {
                throw new UnsupportedOperationException("JAR file doesn't contain version.properties file!?");
            }
            config.load(stream);
            String major = config.getProperty("version.major");
            String minor = config.getProperty("version.minor");
            String release = config.getProperty("version.release");
            if (major == null || minor == null || release == null) {
                throw new UnsupportedOperationException("Invalid version.properties in JAR file!?");
            }
            return major + "." + minor + "." + release + " " + sver;
        } catch (IOException ioe) {
            throw new UnsupportedOperationException("No version.properties in JAR file!?");
        }
    }

    public static QName staticError(int errno) {
        String localName = String.format("XS%04d", errno);
        return new QName("err", NS_XPROC_ERROR, localName);
    }

    public static QName dynamicError(int errno) {
        String localName = String.format("XD%04d", errno);
        return new QName("err", NS_XPROC_ERROR, localName);

    }

    public static QName stepError(int errno) {
        String localName = String.format("XC%04d", errno);
        return new QName("err", NS_XPROC_ERROR, localName);
    }
}
