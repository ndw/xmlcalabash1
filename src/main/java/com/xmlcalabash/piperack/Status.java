package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.util.TreeWriter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;

import java.net.URI;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Status extends BaseResource {
    @Override
    protected Representation get(Variant variant) {
        TreeWriter tree = new TreeWriter(getGlobalRuntime());
        tree.startDocument(URI.create("http://example.com/"));

        tree.addStartElement(pr_status);

        tree.addStartElement(pr_version);
        tree.addText(XProcConstants.XPROC_VERSION);
        tree.addEndElement();

        tree.addStartElement(pr_saxon_version);
        tree.addText(getConfiguration().getProcessor().getSaxonProductVersion());
        tree.addEndElement();

        tree.addStartElement(pr_saxon_edition);
        tree.addText(getConfiguration().getProcessor().getUnderlyingConfiguration().getEditionCode());
        tree.addEndElement();

        tree.addStartElement(pr_copyright);
        tree.addText("© 2007-2020 Norman Walsh");
        tree.addEndElement();

        tree.addStartElement(pr_message);
        tree.addText("See docs/notices/NOTICES in the distribution for licensing.");
        tree.addEndElement();

        tree.addStartElement(pr_message);
        tree.addText("See also http://xmlcalabash.com/ for more information.");
        tree.addEndElement();

        tree.addEndElement();
        tree.endDocument();

        return new StringRepresentation(serialize(tree.getResult(), variant.getMediaType()), variant.getMediaType());
    }
}
