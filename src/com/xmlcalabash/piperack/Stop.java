package com.xmlcalabash.piperack;

import com.xmlcalabash.core.XProcException;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Stop extends BaseResource {
    @Override
    protected Representation post(Representation entity, Variant variant) {
        try {
            ((PiperackApplication) getApplication()).stop();
        } catch (Exception e) {
            throw new XProcException(e);
        }

        return okResponse("Stopping", variant.getMediaType());
    }

}
