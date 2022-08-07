/*
 * URIUtils.java
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

package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcException;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;

/**
 *
 * @author ndw
 */
public class URIUtils {
    private static final String uniqueQueryKey = "xmlcalabash_uniqueid";
    private static int uniqueId = 0;

    /** Creates a new instance of URIUtils */
    protected URIUtils() {
    }

    public static String encode(String src) {
        String genDelims = ":/?#[]@";
        String subDelims = "!$&'()*+,;=";
        String unreserved = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~";
        String okChars = genDelims + subDelims + unreserved + "%"; // don't double-escape %-escaped chars!

        // FIXME: This should be more general, but Windows seems to be the only problem
        // and I'm to lazy to look up how to dyanmically escape "\"
        String filesep = System.getProperty("file.separator");
        if ("\\".equals(filesep)) {
            src = src.replaceAll("\\\\", "/");
        }

        String encoded = "";
        
        try {
            byte[] bytes = src.getBytes("UTF-8");
            for (int pos = 0; pos < bytes.length; pos++) {
                if (okChars.indexOf(bytes[pos]) >= 0) {
                    encoded += (char) bytes[pos];
                } else {
                    encoded += String.format("%%%02X", bytes[pos]);
                }
            }
        } catch (UnsupportedEncodingException uee) {
            // This can't happen for UTF-8!
        }

        return encoded;
    }

    public static URI homeAsURI() {
        return dirAsURI(System.getProperty("user.home"));
    }
    
    public static URI cwdAsURI() {
        return dirAsURI(System.getProperty("user.dir"));
    }

    public static URI dirAsURI(String dir) {
        URI cwd = null;
        
        try {
            String path = encode(dir);
            if (!path.endsWith("/")) {
                path += "/";
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            cwd = new URI("file:" + path);
        } catch (URISyntaxException use) {
            throw new XProcException(use);
        }
        
        return cwd;
    }
    
    public static URI makeAbsolute(String localFn) {
        URI cwd = cwdAsURI();
        return cwd.resolve(encode(localFn));
    }

    public static File toFile(URI uri) {
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException("Expecting a file URI: " + uri.toASCIIString());
        }

        if (uri.getAuthority() != null && uri.getAuthority().length() > 0) {
            return new File("//"+uri.getAuthority()+uri.getPath());
        } else {
            return new File(uri.getPath());
        }
    }

    /**
     * Gets the unique ID associated with the URI.
     * <p>If the URI is null, returns 0. If the URI has no unique ID, returns -1. Otherwise, returns
     * the unique id value in the URI.</p>
     * @param uri The uri
     * @return The unique id
     */
    public static int uniqueId(String uri) {
        if (uri == null) {
            return 0;
        }

        int pos = uri.indexOf("?");
        if (pos >= 0) {
            String query = uri.substring(pos+1);
            pos = query.indexOf(uniqueQueryKey);
            if (pos >= 0) {
                String id = query.substring(pos).replaceAll(uniqueQueryKey + "=([0-9]+).*$", "$1");
                return Integer.parseInt(id, 10);
            } else {
                return -1;
            }
        } else {
            return -1;
        }

    }

    /**
     * Force the uri to be unique.
     * <p>This method adds a query parameter to the URI to ensure that it's unique. If the query parameter
     * already exists, it's value is changed so that it remains unique.</p>
     * @param uri The uri
     * @return The uri, made unique
     */
    public static String makeUnique(String uri) {
        if (uri == null) {
            return uri;
        }

        String id = uniqueQueryKey + "=" + (++uniqueId);
        int pos = uri.indexOf("?");

        if (pos >= 0) {
            String query = uri.substring(pos+1);
            if (query.contains(uniqueQueryKey)) {
                query = query.replaceAll(uniqueQueryKey + "=[0-9]+", id);
            } else {
                query += "&" + id;
            }
            return uri.substring(0, pos+1) + query;
        } else {
            return uri + "?" + id;
        }
    }
}
