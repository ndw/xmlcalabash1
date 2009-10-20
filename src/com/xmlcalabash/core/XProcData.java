package com.xmlcalabash.core;

import com.xmlcalabash.runtime.XStep;

import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 20, 2009
 * Time: 9:25:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class XProcData {
    private Stack<StackFrame> stack = null;

    public XProcData() {
        stack = new Stack<StackFrame> ();
    }

    public void openFrame() {
        stack.push(new StackFrame());
    }


    public void closeFrame() {
        stack.pop();
    }

    public void setStep(XStep step) {
        stack.peek().step = step;
    }

    public XStep getStep() {
        return stack.peek().step;
    }

    public void setIterationPosition(int pos) {
        stack.peek().iterPos = pos;
    }

    public int getIterationPosition() {
        return stack.peek().iterPos;
    }

    public void setIterationSize(int size) {
        stack.peek().iterSize = size;
    }

    public int getIterationSize() {
        return stack.peek().iterSize;
    }

    private class StackFrame {
        public XStep step = null;
        public int iterPos = 1;
        public int iterSize = 1;

        public StackFrame() {
            // nop;
        }
    }
}
