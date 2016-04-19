/*
 * FileDataStore.java
 *
 * Copyright 2013 3 Round Stones Inc.
 * Some rights reserved.
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

package com.xmlcalabash.io;

import com.xmlcalabash.core.XProcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Uses {@link FileInputStream} and {@link FileOutputStream} to implement the
 * interface. The media type is stored as a File extension. The file extension
 * mapping is read from the file in the system property
 * "content.types.user.table" if present.
 * 
 * @author James Leigh &lt;james@3roundstones.com&gt;
 */
public class FileDataStore implements DataStore {
    private Logger logger = LoggerFactory.getLogger(FileDataStore.class);
	private final DataStore fallback;
	private final Properties contentTypes;
    private Hashtable<String,String> cachedMapping = null;

	public FileDataStore(DataStore fallback) {
		super();
		this.fallback = fallback;
		contentTypes = new Properties();
		loadDefaultContentTypes(contentTypes);
		loadContentTypes(contentTypes);
    }

	public URI writeEntry(String href, String base, String media,
			DataWriter handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		if ("file".equalsIgnoreCase(uri.getScheme())) {
			File file = new File(uri);
			String suffix = getFileSuffixFromType(media);
			if (file.isDirectory() || uri.getPath().endsWith("/")) {
				if (!file.isDirectory() && !file.mkdirs()) {
					throw new FileNotFoundException(file.getAbsolutePath());
				}
				File temp = File.createTempFile("calabash", suffix, file);
				OutputStream out = new FileOutputStream(temp);
				try {
					handler.store(out);
				} finally {
					out.close();
				}
				return temp.toURI();
			} else {
				File dir = file.getParentFile();
				if (!dir.isDirectory() && !dir.mkdirs()) {
					throw new FileNotFoundException(dir.getAbsolutePath());
				}
				File temp = File.createTempFile("calabash-temp", suffix, dir);
				try {
					OutputStream out = new FileOutputStream(temp);
					try {
						handler.store(out);
					} finally {
						out.close();
					}
					file.delete();
					temp.renameTo(file);
					return file.toURI();
				} finally {
					if (temp.exists()) {
						temp.delete();
					}
				}
			}
		} else {
			return fallback.writeEntry(href, base, media, handler);
		}
	}

	public void readEntry(String href, String base, String accept,
			String overrideContentType, DataReader handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		if ("file".equalsIgnoreCase(uri.getScheme())) {
			File file = new File(uri);
			String type = getContentTypeFromName(file.getName());
            if (overrideContentType != null) {
                type = overrideContentType;
            }
			InputStream in = new FileInputStream(file);
			try {
				handler.load(file.toURI(), type, in, file.length());
			} finally {
				in.close();
			}
		} else {
			fallback.readEntry(href, base, accept, overrideContentType, handler);
		}
	}

	public void infoEntry(String href, String base, String accept,
			DataInfo handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		if ("file".equalsIgnoreCase(uri.getScheme())) {
			File file = new File(uri);
			String type;
			if (file.isFile()) {
				type = getContentTypeFromName(file.getName());
			} else if (file.exists()) {
				type = null;
			} else {
				throw new FileNotFoundException(file.getAbsolutePath());
			}
			handler.list(file.toURI(), type, file.lastModified());
		} else {
			fallback.infoEntry(href, base, accept, handler);
		}
	}

	public void listEachEntry(String href, String base, String accept,
			DataInfo handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		if ("file".equalsIgnoreCase(uri.getScheme())) {
			File file = new File(uri);

            if (!file.canRead()) {
                throw XProcException.stepError(12);
            }

			if (file.isDirectory()) {
				for (File f : listAcceptableFiles(file, accept)) {
					String type;
					if (f.isDirectory()) {
						type = null;
					} else {
						type = getContentTypeFromName(f.getName());
					}
					handler.list(f.toURI(), type, f.lastModified());
				}
			} else {
                throw new FileNotFoundException(file.getAbsolutePath() + " is not a directory");
			}
		} else {
			fallback.listEachEntry(href, base, accept, handler);
		}
	}

	public URI createList(String href, String base)
			throws MalformedURLException, FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		if ("file".equalsIgnoreCase(uri.getScheme())) {
			File file = new File(uri);
			if (file.isDirectory()) {
				return file.toURI();
			} else if (file.exists()) {
				throw new FileNotFoundException(file.toURI().toASCIIString());
			} else {
				if (file.mkdirs()) {
					return file.toURI();
				} else {
					throw new IOException("Could not create directory: "
							+ file.getAbsolutePath());
				}
			}
		} else {
			return fallback.createList(href, base);
		}
	}

	public void deleteEntry(String href, String base)
			throws MalformedURLException, FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		if ("file".equalsIgnoreCase(uri.getScheme())) {
			File file = new File(uri);
			if (!file.exists()) {
				throw new FileNotFoundException(file.toURI().toASCIIString());
			} else if (!file.delete()) {
				throw new IOException("Could not delete "
						+ file.toURI().toASCIIString());
			}
		} else {
			fallback.deleteEntry(href, base);
		}
	}

	protected File[] listAcceptableFiles(File dir, final String accept) {
		if (accept.contains("*/*")) {
			File[] list = dir.listFiles();
			if (list == null) {
				return new File[0];
			} else {
				return list;
			}
		} else {
			File[] list = dir.listFiles(new FileFilter() {
				public boolean accept(File file) {
					if (!file.isFile()) {
						return false;
					}
					String type = getContentTypeFromName(file.getName());
					String primary = type.substring(0, type.indexOf('/'));
					return accept.contains(type)
							|| accept.contains(primary + "/*");
				}
			});
			if (list == null) {
				return new File[0];
			} else {
				return list;
			}
		}
	}

	protected String getContentTypeFromName(String name) {
		final String ext = getFileExtension(name);
		if (ext == null) {
			return "application/octet-stream";
		}

        if (cachedMapping == null) {
            // Let's do this only once...
            cachedMapping = new Hashtable<String,String> ();
            Enumeration<?> types = contentTypes.propertyNames();
            while (types.hasMoreElements()) {
                String type = (String) types.nextElement();
                String attrs = contentTypes.getProperty(type);

                String[] tokens = attrs.split(";");
                for (String tok : tokens) {
                    if (tok.startsWith("file_extensions=")) {
                        String extList = tok.substring(16);
                        String[] exts = extList.split(",");
                        for (String e : exts) {
                            cachedMapping.put(e, type);
                        }
                    }
                }
            }
        }

        if (cachedMapping.containsKey(ext)) {
            return cachedMapping.get(ext);
        }

		return "application/octet-stream";
	}

	protected String getFileSuffixFromType(String media) {
		int i = media.indexOf(';');
		if (i > 0) {
			media = media.substring(0, i);
		}
		i = media.indexOf(',');
		if (i > 0) {
			media = media.substring(0, i);
		}
		String attr = (String) contentTypes.get(media.trim());
		if (attr != null && attr.indexOf("file_extensions") >= 0) {
			int start = attr.indexOf('=', attr.indexOf("file_extensions")) + 1;
			int end = attr.indexOf(',', start);
			if (end < 0) {
				end = attr.indexOf(';', start);
			}
			if (end < 0) {
				end = attr.length();
			}
			return attr.substring(start, end).trim();
		} else {
			int plus = media.lastIndexOf('+');
			if (plus > 0) {
				String primary = media.substring(0, media.indexOf('/') + 1);
				String subtype = media.substring(plus + 1);
				return getFileSuffixFromType(primary + subtype);
			} else if (!media.startsWith("application/")) {
				String subtype = media.substring(media.indexOf('/') + 1);
				return getFileSuffixFromType("application/" + subtype);
			}
		}
		return "";
	}

	private String getFileExtension(String fname) {
		int end = fname.indexOf('#');
		if (end < 0) {
			end = fname.length();
		}

		int start = fname.lastIndexOf('.', end);
		if (start >= 0 && fname.charAt(start) == '.') {
			return fname.substring(start, end).toLowerCase();
		} else {
			return null;
		}
	}

	/**
	 * Load some basic types in case the system doesn't have them.
	 */
	private void loadDefaultContentTypes(Properties contentTypes) {
        contentTypes.put("application/json", "file_extensions=.json");
        contentTypes.put("application/javascript", "file_extensions=.js");
        contentTypes.put("text/css", "file_extensions=.css");
		contentTypes.put("application/xml", "file_extensions=.xml");
		contentTypes.put("application/zip", "file_extensions=.zip");
		contentTypes.put("text/plain", "file_extensions=.txt");
	}

	private void loadContentTypes(Properties contentTypes) {
		File file = null;
		try {
			// First try to load the user-specific table, if it exists
			String userTablePath = System
					.getProperty("content.types.user.table");
			if (userTablePath != null && new File(userTablePath).exists()) {
				file = new File(userTablePath);
			} else {
				// No user table, try to load the default built-in table.
				File lib = new File(System.getProperty("java.home"), "lib");
				file = new File(lib, "content-types.properties");
			}
	
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			try {
				contentTypes.load(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
            logger.warn("Failed to load content types: " + file.getAbsolutePath());
            logger.debug(e.getMessage(), e);
		}
	}

}
