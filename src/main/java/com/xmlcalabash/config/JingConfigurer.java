package com.xmlcalabash.config;

import com.thaiopensource.util.PropertyMapBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 8:42 AM
 * To change this template use File | Settings | File Templates.
 */
public interface JingConfigurer {
    public void configRNC(PropertyMapBuilder properties);
    public void configRNG(PropertyMapBuilder properties);
}
