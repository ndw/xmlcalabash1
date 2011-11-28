/*
 * WritablePipe.java
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

package com.xmlcalabash.io;

import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.model.Step;

/**
 *
 * @author ndw
 */
public interface WritablePipe {
    public void canWriteSequence(boolean sequence);
    public boolean writeSequence();
    public void write(XdmNode node);
    public void setWriter(Step step);
    public void resetWriter();
    public void close();
}
