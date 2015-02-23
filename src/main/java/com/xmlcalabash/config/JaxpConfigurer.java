package com.xmlcalabash.config;

import javax.xml.validation.SchemaFactory;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 8:41 AM
 * To change this template use File | Settings | File Templates.
 */
public interface JaxpConfigurer {
    public void configSchemaFactory(SchemaFactory factory);
}
