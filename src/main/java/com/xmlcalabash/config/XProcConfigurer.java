package com.xmlcalabash.config;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 8:45 AM
 * To change this template use File | Settings | File Templates.
 */
public interface XProcConfigurer {
    public XMLCalabashConfigurer getXMLCalabashConfigurer();
    public SaxonConfigurer getSaxonConfigurer();
    public JingConfigurer getJingConfigurer();
    public JaxpConfigurer getJaxpConfigurer();
}
