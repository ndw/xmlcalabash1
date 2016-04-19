/*
 * FallbackDataStore.java
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
import java.net.MalformedURLException;
import java.net.URI;

/**
 * Throws an error with the scheme name in every method.
 * 
 * @author James Leigh &lt;james@3roundstones.com&gt;
 * 
 */
public class FallbackDataStore implements DataStore {

	public URI writeEntry(String href, String base, String media,
			DataWriter handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		String scheme = uri.getScheme();
		throw new IOException(scheme
				+ ": scheme URIs are not supported for writing");
	}

	public void readEntry(String href, String base, String accept,
			String overrideContentType, DataReader handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		String scheme = uri.getScheme();
		throw new IOException(scheme
				+ ": scheme URIs are not supported for reading");
	}

	public void infoEntry(String href, String base, String accept,
			DataInfo handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		String scheme = uri.getScheme();
		throw new IOException(scheme
				+ ": scheme URIs are not supported for metadata");
	}

	public void listEachEntry(String href, String base, String accept,
			DataInfo handler) throws MalformedURLException,
			FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		String scheme = uri.getScheme();
		throw new IOException(scheme
				+ ": scheme URIs are not supported for directory listing");
	}

	public URI createList(String href, String base)
			throws MalformedURLException, FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		String scheme = uri.getScheme();
		throw new IOException(scheme
				+ ": scheme URIs are not supported for creating directories");
	}

	public void deleteEntry(String href, String base)
			throws MalformedURLException, FileNotFoundException, IOException {
		URI baseURI = URI.create(base);
		URI uri = baseURI.resolve(href);
		String scheme = uri.getScheme();
		throw new IOException(scheme
				+ ": scheme URIs are not supported for deleting");
	}

}
