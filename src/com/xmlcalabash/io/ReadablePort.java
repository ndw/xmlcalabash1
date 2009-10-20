/*
 * ReadablePort.java
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

package com.xmlcalabash.io;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

/**
 *
 * @author ndw
 */
public interface ReadablePort {
    public void enableResetReader();
    
    public void resetReader();
    
    public boolean closed();
    
    public boolean moreDocuments();
    
    public int documentCount();
    
    public int documentsWritten();
    
    public int documentsRead();

    public XdmNode getDocument() throws SaxonApiException;
}
