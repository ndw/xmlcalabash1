package com.xmlcalabash.util;

import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Input;
import com.xmlcalabash.model.Output;
import com.xmlcalabash.model.Parameter;
import com.xmlcalabash.model.Option;
import com.xmlcalabash.model.Variable;
import com.xmlcalabash.model.Binding;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.model.DocumentBinding;
import com.xmlcalabash.model.PipeBinding;
import com.xmlcalabash.model.PipeNameBinding;
import com.xmlcalabash.model.DataBinding;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;

import java.io.PrintStream;

import net.sf.saxon.s9api.QName;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Dec 5, 2008
 * Time: 8:20:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class Reporter {
    private XProcRuntime runtime;
    private PrintStream pr;
    private boolean xmlReport;

    public Reporter(XProcRuntime runtime, PrintStream pr) {
        this.runtime = runtime;
        this.pr = pr;
        xmlReport = true;
    }

    public void report(Step step) {
        if (xmlReport) {
            pr.println("<pipeline-report xmlns='http://xmlcalabash.com/ns/pipeline-report'>");
            pr.println("<version>" + runtime.getXProcVersion() + "</version>");
            pr.println("<product-name>" + runtime.getProductName() + "</product-name>");
            pr.println("<product-version>" + runtime.getProductVersion() + "</product-version>");
            pr.println("<episode>" + runtime.getEpisode() + "</episode>");
            xmlReport(step);
            pr.println("</pipeline-report>");
        }
    }

    private void xmlReport(Step step) {
        pr.print("<step xml:base='" + step.getNode().getBaseURI() + "'");
        pr.print(" type='" + step.getType().getClarkName() + "'");

        if (step.getDeclaredType() != null) {
            pr.print(" declared-type='" + step.getDeclaredType().getClarkName() + "'");
        }
        
        pr.print(" name='" + step.getName() + "'");
        pr.println(">");

        for (Input input : step.inputs()) {
            xmlReport(input);
        }
        for (Output output : step.outputs()) {
            xmlReport(output);
        }
        for (Parameter param : step.parameters()) {
            xmlReport(param);
        }
        for (Option option : step.options()) {
            xmlReport(option);
        }
        for (Variable var : step.getVariables()) {
            xmlReport(var);
        }
        for (Step substep : step.subpipeline()) {
            xmlReport(substep);
        }

        pr.println("</step>");
    }

    private void xmlReport(Input input) {
        if (!input.getPort().startsWith("|")) {
            pr.print("  <input port='" + input.getPort() + "'");
            if (input.getParameterInput()) {
                pr.print(" kind='parameter'");
            }
            if (input.getSelect() != null) {
                pr.print(" select='" + input.getSelect() + "'");
            }
            pr.println(">");
            for (Binding binding : input.getBinding()) {
                xmlReport(binding);
            }
            pr.println("  </input>");
        }
    }

    private void xmlReport(Output output) {
        if (!output.getPort().endsWith("|")) {
            pr.println("  <output port='" + output.getPort() + "'>");
            Serialization ser = output.getSerialization();
            if (ser != null) {
                pr.println("    <serialization method='" + ser.getMethod() + "'/>");
            }
            for (Binding binding : output.getBinding()) {
                xmlReport(binding);
            }
            pr.println("  </output>");
        }
    }

    private void xmlReport(Binding binding) {
        switch (binding.getBindingType()) {
            case Binding.NO_BINDING:
                pr.println("    <no-binding/>");
                break;
            case Binding.PIPE_NAME_BINDING:
                xmlReport((PipeNameBinding) binding);
                break;
            case Binding.INLINE_BINDING:
                pr.println("    <inline-binding/>");
                break;
            case Binding.DOCUMENT_BINDING:
            case Binding.STDIO_BINDING:
                xmlReport((DocumentBinding) binding);
                break;
            case Binding.DATA_BINDING:
                xmlReport((DataBinding) binding);
                break;
            case Binding.PIPE_BINDING:
                pr.println("    <pipe-binding/>");
                break;
            case Binding.EMPTY_BINDING:
                pr.println("    <no-binding/>");
                break;
            case Binding.ERROR_BINDING:
                pr.println("    <error-binding/>");
                break;
            default:
                pr.println("    <unknown-binding type='" + binding.getBindingType() + "'/>");

        }
    }

    private void xmlReport(PipeNameBinding binding) {
        pr.println("    <pipe-name-binding step='" + binding.getStep() + "' port='" + binding.getPort() + "'/>");
    }

    private void xmlReport(DocumentBinding binding) {
        String href = binding.getHref();
        if (href == null) {
            pr.println("    <stdio-binding/>");
        } else {
            pr.println("    <document-binding href='" + href + "'/>");
        }
    }

    private void xmlReport(DataBinding binding) {
        String href = binding.getHref();
        QName wrapper = binding.getWrapper();
        pr.println("    <data-binding href='" + href + "' wrapper='" + wrapper.getClarkName() + "'/>");
    }

    private void xmlReport(Parameter param) {
        String select = param.getSelect();

        pr.print("  <parameter name='" + param.getName().getClarkName() + "'");
        if (select != null) {
            pr.print(" select=\"" + select.replaceAll("\"","&quot;") + "\"");
        }
        pr.println("/>");
    }

    private void xmlReport(Option option) {
        String type = option.getType();
        boolean req = option.getRequired();
        String select = option.getSelect();

        pr.print("  <option name='" + option.getName().getClarkName() + "' required='" + req + "'");
        if (type != null) {
            pr.print(" type='" + type + '"');
        }
        if (select != null) {
            pr.print(" select=\"" + select.replaceAll("\"","&quot;") + "\"");
        }
        pr.println("/>");
    }

    private void xmlReport(Variable var) {
        String select = var.getSelect();

        pr.print("  <variable name='" + var.getName().getClarkName() + "'");
        if (select != null) {
            pr.print(" select=\"" + select.replaceAll("\"","&quot;") + "\"");
        }
        pr.println("/>");
    }
}
