package com.xmlcalabash.util;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Feb 26, 2010
 * Time: 1:51:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpUtils {

    /** Creates a new instance of HttpUtils */
    protected HttpUtils() {
    }

    public static String getCharset(String contentType, String defaultCharset) {
        String charset = HttpUtils.getCharset(contentType);
        if (charset == null) {
            return defaultCharset;
        } else {
            return charset;
        }
    }

    public static String getCharset(String contentType) {
        String charset = null;

        if (contentType != null && contentType.matches("^.*;[ \t]*charset=([^ \t]+).*$")) {
            charset = contentType.replaceAll("^.*;[ \t]*charset=([^ \t]+).*$", "$1");
            if (charset.startsWith("\"") || charset.startsWith("'")) {
                charset = charset.substring(1,charset.length()-1);
            }
        }

        return charset;
    }
}
