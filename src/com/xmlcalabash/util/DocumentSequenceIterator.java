package com.xmlcalabash.util;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 20, 2008
 * Time: 4:35:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class DocumentSequenceIterator implements SequenceIterator<Item<NodeInfo>>, LastPositionFinder<Item<NodeInfo>> {
    int position = 0;
    int last = 0;
    Item<NodeInfo> item = null;

    public void setPosition(int position) {
        this.position = position;
    }

    public void setItem(Item<NodeInfo> item) {
        this.item = item;
    }

    public void setLast(int last) {
        this.last = last;
    }

    public Item<NodeInfo> next() throws XPathException {
        throw new UnsupportedOperationException("Don't know what to do for next() on DocumentSequenceIterator");
    }

    public Item<NodeInfo> current() {
        return item;
    }

    public int position() {
        return position;
    }

    public void close() {
        // ???
    }

    public SequenceIterator<Item<NodeInfo>> getAnother() throws XPathException {
        throw new UnsupportedOperationException("Don't know what to do for getAnother() on DocumentSequenceIterator");
    }

    public int getProperties() {
        return SequenceIterator.LAST_POSITION_FINDER;
    }

    @Override
    public int getLength() throws XPathException {
        return last;
    }
}
