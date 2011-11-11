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

    public static String baseContentType(String contentType) {
        if (contentType != null && contentType.matches("(^.*)[ \t]*;.*$")) {
            return contentType.replaceAll("(^.*)[ \t]*;.*$", "$1");
        } else {
            return contentType;
        }
    }

    public static boolean xmlContentType(String contentType) {
        String baseType = HttpUtils.baseContentType(contentType);
        return baseType != null
                && ("application/xml".equals(baseType)
                    || "text/xml".equals(baseType)
                    || baseType.endsWith("+xml"));
    }

    public static boolean jsonContentType(String contentType) {
        String baseType = HttpUtils.baseContentType(contentType);
        return baseType != null
                && ("application/json".equals(baseType)
                    || "text/json".equals(baseType));
    }

    public static boolean textContentType(String contentType) {
        return contentType != null && contentType.startsWith("text/");
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
