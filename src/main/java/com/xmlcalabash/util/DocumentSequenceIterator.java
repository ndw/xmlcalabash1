package com.xmlcalabash.util;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.om.FocusIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 20, 2008
 * Time: 4:35:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class DocumentSequenceIterator implements FocusIterator, LastPositionFinder {
    int position = 0;
    int last = 0;
    Item item = null;

    public void setPosition(int position) {
        this.position = position;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setLast(int last) {
        this.last = last;
    }

    public Item next() {
        throw new UnsupportedOperationException("Don't know what to do for next() on DocumentSequenceIterator");
    }

    public Item current() {
        return item;
    }

    public int position() {
        return position;
    }

    public void close() {
        // ???
    }

    @Override
    public boolean supportsGetLength() {
        return false;
    }

    @Override
    public int getLength() {
        return last;
    }
}
