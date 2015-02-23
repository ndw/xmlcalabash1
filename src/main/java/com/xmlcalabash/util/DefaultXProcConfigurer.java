package com.xmlcalabash.util;

import com.xmlcalabash.config.JaxpConfigurer;
import com.xmlcalabash.config.JingConfigurer;
import com.xmlcalabash.config.SaxonConfigurer;
import com.xmlcalabash.config.XMLCalabashConfigurer;
import com.xmlcalabash.config.XProcConfigurer;
import com.xmlcalabash.core.XProcRuntime;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 8:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultXProcConfigurer implements XProcConfigurer {
    private static final JaxpConfigurer defJaxpConfigurer = new DefaultJaxpConfigurer();
    private static final JingConfigurer defJingConfigurer = new DefaultJingConfigurer();
    private static final SaxonConfigurer defSaxonConfigurer = new DefaultSaxonConfigurer();
    private XMLCalabashConfigurer configurer = null;

    public DefaultXProcConfigurer(XProcRuntime runtime) {
        configurer = new DefaultXMLCalabashConfigurer(runtime);
    }

    public XMLCalabashConfigurer getXMLCalabashConfigurer() {
        return configurer;
    }

    public SaxonConfigurer getSaxonConfigurer() {
        return defSaxonConfigurer;
    }

    public JingConfigurer getJingConfigurer() {
        return defJingConfigurer;
    }

    public JaxpConfigurer getJaxpConfigurer() {
        return defJaxpConfigurer;
    }
}
