/*
 * TypeUtils.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.xmlcalabash.util;


import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;

import java.util.Hashtable;

import net.sf.saxon.s9api.*;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Jul 17, 2008
 * Time: 9:39:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class TypeUtils {
    private static final QName _XSLTMatchPattern = new QName("XSLTMatchPattern");
    private static final QName _RegularExpression = new QName("RegularExpression");
    private static final QName _ListOfQNames = new QName("ListOfQNames");
    private static final QName _XPathExpression = new QName("XPathExpression");
    private static final QName _NormalizationForm = new QName("NormalizationForm");

    private static int anonTypeCount = 0;
    private static ItemTypeFactory typeFactory = null;
    private static Hashtable<QName, ItemType> types = null;

    public static QName generateUniqueType(String baseName) {
        anonTypeCount++;
        String localName = baseName + "_" + anonTypeCount;
        return new QName(XProcConstants.NS_CALABASH_EX, localName);
    }

    public static QName generateUniqueType() {
        return generateUniqueType("anonymousType");
    }

    public static void checkLiteral(String value, String literals) {
        String[] values = literals.split("\\|");
        for (String v : values) {
            if (v.equals(value)) {
                return;
            }
        }

        throw XProcException.dynamicError(45, "Invalid value: \"" + value + "\" must be one of \"" + literals + "\".");
    }

    public static void checkType(XProcRuntime runtime, String value, QName type, XdmNode node) {
        if (XProcConstants.xs_string.equals(type) || XProcConstants.xs_untypedAtomic.equals(type)) {
            return;
        }

        if (_XSLTMatchPattern.equals(type) || _RegularExpression.equals(type) || _ListOfQNames.equals(type)
                || _XPathExpression.equals(type) || _NormalizationForm.equals(type)) {
            // FIXME: Check these!
            return;
        }

        if (XProcConstants.xs_QName.equals(type)) {
            try {
                QName name = new QName(value, node);
            } catch (Exception e) {
                throw XProcException.dynamicError(45, e);
            }
            return;
        }

        if (typeFactory == null) {
            typeFactory = new ItemTypeFactory(runtime.getProcessor());
            types = new Hashtable<QName,ItemType> ();
        }

        ItemType itype = null;

        if (types.containsKey(type)) {
            itype = types.get(type);
        } else {
            try {
                itype = typeFactory.getAtomicType(type);
            } catch (SaxonApiException sae) {
                throw new XProcException("Unexpected type: " + type);
            }
            types.put(type,itype);
        }

        // FIXME: There's probably a less expensive expensive way to do this
        try {
            XdmAtomicValue avalue = new XdmAtomicValue(value, itype);
        } catch (SaxonApiException sae) {
            throw XProcException.dynamicError(45, sae);
        }
    }    
}
