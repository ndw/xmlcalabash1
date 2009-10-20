package com.xmlcalabash.functions;

import net.sf.saxon.trans.Err;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.functions.StandardFunction;
import net.sf.saxon.functions.SystemFunction;

import java.util.HashMap;
import java.lang.reflect.Constructor;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.*;

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

/**
 * The XProcFunctionLibrary is a hacked copy of Saxon's own VendorFunctionLibrary. This seemed to be
 * the easiest way to get a namespace context passed into the p:system-property extension function.
 * See also: {@link net.sf.saxon.functions.JavaExtensionLibrary}.                  
 */

public class EXProcFunctionLibrary implements FunctionLibrary {
    private XProcRuntime runtime;
    private HashMap<String,StandardFunction.Entry> functionTable;
    private XStep step;

    /**
     * Create the XProc Function Library for Calabash
     */

    public EXProcFunctionLibrary(XProcRuntime runtime, XStep forStep) {
        this.runtime = runtime;
        step = forStep;
        init();
    }

    /**
     * Register an extension function in the table of function details.
     * @param name the function name
     * @param implementationClass the class used to implement the function
     * @param opcode identifies the function when a single class implements several functions
     * @param minArguments the minimum number of arguments required
     * @param maxArguments the maximum number of arguments allowed
     * @param itemType the item type of the result of the function
     * @param cardinality the cardinality of the result of the function
     * @return the entry describing the function. The entry is incomplete, it does not yet contain information
     * about the function arguments.
    */

    protected StandardFunction.Entry register(String name,
                                              Class implementationClass,
                                              int opcode,
                                              int minArguments,
                                              int maxArguments,
                                              ItemType itemType,
                                              int cardinality ) {
        StandardFunction.Entry e = StandardFunction.makeEntry(
                name, implementationClass, opcode, minArguments, maxArguments, itemType, cardinality);
        functionTable.put(name, e);
        return e;
    }

    protected void init() {
        functionTable = new HashMap<String,StandardFunction.Entry>(2);
        register("cwd", Cwd.class, 0, 0, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO);
    }

    /**
     * Test whether a Saxon function with a given name and arity is available. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     * @param functionName the name of the function
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     */

    public boolean isAvailable(StructuredQName functionName, int arity) {
        if (functionName.getNamespaceURI().equals(XProcConstants.NS_EXPROC_PFUNCTIONS)
                || functionName.getNamespaceURI().equals(XProcConstants.NS_EXPROC_FUNCTIONS)) {
            StandardFunction.Entry entry = (StandardFunction.Entry)functionTable.get(functionName.getLocalName());
            return entry != null && (arity == -1 || (arity >= entry.minArguments && arity <= entry.maxArguments));
        } else {
            return false;
        }
    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName the name of the function
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @param env
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function; or if this function library "owns" the namespace containing
     * the function call, but no function was found.
     */

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env)
            throws XPathException {
        String uri = functionName.getNamespaceURI();
        String local = functionName.getLocalName();
        if (uri.equals(XProcConstants.NS_EXPROC_PFUNCTIONS)
            || uri.equals(XProcConstants.NS_EXPROC_FUNCTIONS)) {
            StandardFunction.Entry entry = (StandardFunction.Entry)functionTable.get(local);
            if (entry == null) {
                return null;
            }
            Class functionClass = entry.implementationClass;
            SystemFunction f;

            try {
                Constructor constructor = functionClass.getConstructor(new Class[] { runtime.getClass() });
                f = (SystemFunction) constructor.newInstance(runtime);
            } catch (Exception err) {
                throw new AssertionError("Failed to load XProc extension function: " + err.getMessage());
            }

            f.setDetails(entry);
            f.setFunctionName(functionName);
            f.setArguments(staticArgs);
            checkArgumentCount(staticArgs.length, entry.minArguments, entry.maxArguments, local);
            return f;
        } else {
            return null;
        }
    }

    /**
    * Check number of arguments. <BR>
    * A convenience routine for use in subclasses.
    * @param numArgs the actual number of arguments
    * @param min the minimum number of arguments allowed
    * @param max the maximum number of arguments allowed
    * @param local the local name of the function, used for diagnostics
    * @return the actual number of arguments
    * @throws net.sf.saxon.trans.XPathException if the number of arguments is out of range
    */

    private int checkArgumentCount(int numArgs, int min, int max, String local) throws XPathException {
        if (min==max && numArgs != min) {
            throw new XPathException("Function " + Err.wrap("p:"+local, Err.FUNCTION) + " must have "
                    + min + pluralArguments(min));
        }
        if (numArgs < min) {
            throw new XPathException("Function " + Err.wrap("p:"+local, Err.FUNCTION) + " must have at least "
                    + min + pluralArguments(min));
        }
        if (numArgs > max) {
            throw new XPathException("Function " + Err.wrap("p:"+local, Err.FUNCTION) + " must have no more than "
                    + max + pluralArguments(max));
        }
        return numArgs;
    }

    /**
    * Utility routine used in constructing error messages
     * @param num a number
     * @return the string " argument" or " arguments" if num is plural
    */

    public static String pluralArguments(int num) {
        if (num==1) return " argument";
        return " arguments";
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        return this;
    }
}