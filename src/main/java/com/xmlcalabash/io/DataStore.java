/*
 * DataStore.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;

/**
 * An abstraction layer for many of the file operations in Calabash. The
 * following elements use this interface:
 * <ul>
 * <li>p:http-request when a non-http/https: href URI is provided</li>
 * <li>p:data</li>
 * <li>p:document when a file: href URI is provided</li>
 * <li>p:xinclude when reading text</li>
 * <li>p:xsl-formatter</li>
 * <li>p:store</li>
 * <li>pxf:delete</li>
 * <li>pxf:directory-list</li>
 * <li>pxf:copy</li>
 * <li>pxf:head</li>
 * <li>pxf:mkdir</li>
 * <li>pxf:move</li>
 * <li>pxf:tail</li>
 * <li>pxf:touch</li>
 * <li>cx:css-formatter</li>
 * <li>cx:java-properties</li>
 * <li>pxp:unzip</li>
 * <li>pxp:zip</li>
 * </ul>
 * 
 * @author James Leigh &lt;james@3roundstones.com&gt;
 * 
 */
public interface DataStore {
	interface DataWriter {
		/**
		 * Produces the document stream.
		 * 
		 * @param content
		 *            the document stream
         * @throws IOException
         *            if the document cannot be stored
		 */
		void store(OutputStream content) throws IOException;
	}

	interface DataReader {
		/**
		 * Consumes the document stream.
		 * 
		 * @param id
		 *            the document URI
		 * @param media
		 *            the mimetype of the document
		 * @param content
		 *            the document stream
		 * @param len
		 *            the length of the document stream or -1 if not known
         * @throws IOException
         *            If the document could not be loaded
		 */
		void load(URI id, String media, InputStream content, long len)
				throws IOException;
	}

	interface DataInfo {
		/**
		 * Provides information about a document or directory.
		 * 
		 * @param id
		 *            the document or directory URI
		 * @param media
		 *            the document mime-type or null if a directory or not a
		 *            document
		 * @param lastModified
		 *            the timestamp of the last time the document or directory
		 *            was modified.
         * @throws IOException
         *            if the the document or directory could not be listed
		 */
		void list(URI id, String media, long lastModified) throws IOException;
	}

	/**
	 * Creates or replaces (depending on implementation) the document in the
	 * store. The entry will appear unmodified in other readEntry operations in
	 * the current thread until this method returns successfully.
	 * 
	 * @param href
	 *            relative URI of the document, directory, or service to write
	 *            to
	 * @param base
	 *            the base URI used to resolve the document URI
	 * @param media
	 *            mimetype of the document to be stored
	 * @param handler
	 *            provided with a stream to write the document
	 * @return the created (or replaced) absolute document URI
	 * @throws MalformedURLException
	 *             if the href or base cannot be parsed as a URI
	 * @throws FileNotFoundException
	 *             if the directory or service does not exist
	 * @throws IOException
	 *             if the document could not written
	 */
	URI writeEntry(String href, String base, String media, DataWriter handler)
			throws MalformedURLException, FileNotFoundException, IOException;

	/**
	 * Read a binary document from the store.
	 * 
	 * @param href
	 *            relative URI of the document to read
	 * @param base
	 *            the base URI used to resolve the document URI
	 * @param accept
	 *            comma separated list of acceptable mimetypes
	 * @param overrideContentType
     *            the override content type
	 * @param handler
	 *            passed the document stream to read it
	 * @throws MalformedURLException
	 *             if the href or base cannot be parsed as a URI
	 * @throws FileNotFoundException
	 *             if the document does not exist or is a directory
	 * @throws IOException
	 *             if the document could not be read
	 */
	void readEntry(String href, String base, String accept, String overrideContentType, DataReader handler)
			throws MalformedURLException, FileNotFoundException, IOException;

	/**
	 * Provides metadata about this document or directory from within the store.
	 * 
	 * @param href
	 *            relative URI of the document or directory
	 * @param base
	 *            the base URI used to resolve the href URI
	 * @param accept
	 *            comma separated list of acceptable mimetypes
	 * @param handler
	 *            passed the document or directory metadata
	 * @throws MalformedURLException
	 *             if the href or base cannot be parsed as a URI
	 * @throws FileNotFoundException
	 *             if the document or directory does not exist
	 * @throws IOException
	 *             if the document or directory could not be read
	 */
	void infoEntry(String href, String base, String accept, DataInfo handler)
			throws MalformedURLException, FileNotFoundException, IOException;

	/**
	 * Lists each document or directory from the store within this directory.
	 * 
	 * @param href
	 *            relative URI of the directory
	 * @param base
	 *            the base URI used to resolve the directory URI
	 * @param accept
	 *            comma separated list of acceptable mimetypes, use *&#47;* to
	 *            include everything (including directories)
	 * @param handler
	 *            passed the document or directory URI
	 * @throws MalformedURLException
	 *             if the href or base cannot be parsed as a URI
	 * @throws FileNotFoundException
	 *             if the directory does not exist or is not a directory
	 * @throws IOException
	 *             if the document or directory could not be read
	 */
	void listEachEntry(String href, String base, String accept, DataInfo handler)
			throws MalformedURLException, FileNotFoundException, IOException;

	/**
	 * Creates a new directory in the store at the specified location.
	 * 
	 * @param href
	 *            relative URI of the new directory
	 * @param base
	 *            the base URI used to resolve the href URI
	 * @throws MalformedURLException
	 *             if the href or base cannot be parsed as a URI
	 * @throws FileNotFoundException
	 *             if this resource already exist and is not a directory
	 * @throws IOException
	 *             if the document or directory could not be read
	 * @return the URI of the created directory
	 */
	URI createList(String href, String base) throws MalformedURLException,
			FileNotFoundException, IOException;

	/**
	 * Deletes the given document or (empty) directory from the store.
	 * 
	 * @param href
	 *            relative URI of the document or directory to be deleted
	 * @param base
	 *            the base URI used to resolve the href URI
	 * @throws MalformedURLException
	 *             if the href or base cannot be parsed as a URI
	 * @throws FileNotFoundException
	 *             if the document or directory does not exist
	 * @throws IOException
	 *             if the document or directory could not be read
	 */
	void deleteEntry(String href, String base) throws MalformedURLException,
			FileNotFoundException, IOException;
}
