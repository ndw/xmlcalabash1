package com.xmlcalabash.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

import com.xmlcalabash.io.DataStore.DataInfo;
import com.xmlcalabash.io.DataStore.DataReader;
import com.xmlcalabash.io.DataStore.DataWriter;

public class FileDataStoreTest extends TestCase {
	private final String tmp = "file://" + System.getProperty("java.io.tmpdir").replace(File.separatorChar, '/') + '/';
	private FileDataStore store;

	public void setUp() throws Exception {
		store = new FileDataStore(new FallbackDataStore());
	}

	public void testWriteFile() throws IOException {
		store.writeEntry("file.txt", tmp, "text/plain", new DataWriter() {
			public void store(OutputStream content) throws IOException {
				content.write("content".getBytes());
			}
		});
		store.readEntry("file.txt", tmp, "text/plain", null, new DataReader() {
			public void load(URI id, String media, InputStream content, long len)
					throws IOException {
				byte[] buf = new byte[1024];
				assertEquals("content", new String(buf, 0, content.read(buf)));
			}
		});
		store.deleteEntry("file.txt", tmp);
	}

	public void testWriteDirectory() throws IOException {
		URI file = store.writeEntry("dir/", tmp, "text/plain", new DataWriter() {
			public void store(OutputStream content) throws IOException {
				content.write("content".getBytes());
			}
		});
		store.infoEntry(file.getPath(), tmp, "text/plain", new DataInfo() {
			public void list(URI id, String media, long lastModified)
					throws IOException {
				assertEquals("text/plain", media);
			}
		});
		store.readEntry(file.getPath(), tmp, "text/plain", null, new DataReader() {
			public void load(URI id, String media, InputStream content, long len)
					throws IOException {
				byte[] buf = new byte[1024];
				assertEquals("content", new String(buf, 0, content.read(buf)));
			}
		});
		store.deleteEntry(file.getPath(), tmp);
		store.deleteEntry("dir", tmp);
	}

	public void testReadEntry() throws IOException {
		File file = File.createTempFile("test", ".txt");
		FileWriter writer = new FileWriter(file);
		try {
			writer.write("read content");
		} finally {
			writer.close();
		}
		String uri = file.toURI().toASCIIString();
		store.readEntry(uri, uri, "*/*", null, new DataReader() {
			public void load(URI id, String media, InputStream content, long len)
					throws IOException {
				byte[] buf = new byte[1024];
				assertEquals("read content", new String(buf, 0, content.read(buf)));
			}
		});
		store.deleteEntry(file.getPath(), tmp);
	}

	public void testInfoEntry() throws IOException {
		File file = File.createTempFile("test", ".txt");
		FileWriter writer = new FileWriter(file);
		try {
			writer.write("read content");
		} finally {
			writer.close();
		}
		String uri = file.toURI().toASCIIString();
		store.readEntry(uri, uri, "*/*", null, new DataReader() {
			public void load(URI id, String media, InputStream content, long len)
					throws IOException {
				assertEquals("text/plain", media);
			}
		});
		store.deleteEntry(file.getPath(), tmp);
	}

	public void testListEachEntry() throws IOException {
		URI dir = store.createList("dir", tmp);
		final File text = File.createTempFile("test", ".txt", new File(dir));
		final File xml = File.createTempFile("test", ".xml", new File(dir));
		store.listEachEntry("dir", tmp, "text/plain", new DataInfo() {
			public void list(URI id, String media, long lastModified)
					throws IOException {
				assertEquals("text/plain", media);
				assertEquals(text.getAbsolutePath(), new File(id).getAbsolutePath());
			}
		});
		store.listEachEntry("dir", tmp, "application/xml", new DataInfo() {
			public void list(URI id, String media, long lastModified)
					throws IOException {
				assertEquals("application/xml", media);
				assertEquals(xml.getAbsolutePath(), new File(id).getAbsolutePath());
			}
		});
		store.deleteEntry(text.getPath(), tmp);
		store.deleteEntry(xml.getPath(), tmp);
		store.deleteEntry(dir.getPath(), tmp);
	}

	public void testCreateList() throws IOException {
		URI dir = store.createList("dir", tmp);
		assertTrue(new File(dir).isDirectory());
	}

	public void testDeleteEntry() throws IOException {
		URI file = store.writeEntry("dir/", tmp, "text/plain", new DataWriter() {
			public void store(OutputStream content) throws IOException {
				content.write("content".getBytes());
			}
		});
		assertTrue(new File(file).exists());
		store.deleteEntry(file.getPath(), tmp);
		assertFalse(new File(file).exists());
	}

	public void testListAcceptableFiles() throws IOException {
		URI dir = store.createList("dir", tmp);
		final File text = File.createTempFile("test", ".txt", new File(dir));
		final File xml = File.createTempFile("test", ".xml", new File(dir));
		final File json = File.createTempFile("test", ".json", new File(dir));
		final File zip = File.createTempFile("test", ".zip", new File(dir));
		assertEquals(4, store.listAcceptableFiles(new File(dir), "*/*").length);
		assertEquals(Collections.singletonList(text), Arrays.asList(store.listAcceptableFiles(new File(dir), "text/plain")));
		assertEquals(Collections.singletonList(xml), Arrays.asList(store.listAcceptableFiles(new File(dir), "application/xml")));
		assertEquals(Collections.singletonList(json), Arrays.asList(store.listAcceptableFiles(new File(dir), "application/json")));
		assertEquals(Collections.singletonList(zip), Arrays.asList(store.listAcceptableFiles(new File(dir), "application/zip")));
		zip.delete();
		json.delete();
		xml.delete();
		text.delete();
		new File(dir).delete();
	}

	public void testGetContentTypeFromName() throws IOException {
		assertEquals("text/plain", store.getContentTypeFromName(".txt"));
		assertEquals("application/xml", store.getContentTypeFromName(".xml"));
		assertEquals("application/json", store.getContentTypeFromName(".json"));
		assertEquals("application/zip", store.getContentTypeFromName(".zip"));
	}

	public void testGetFileSuffixFromType() throws IOException {
		assertEquals(".text", store.getFileSuffixFromType("text/plain"));
		assertEquals(".xml", store.getFileSuffixFromType("application/xml"));
		assertEquals(".xml", store.getFileSuffixFromType("text/xml"));
		assertEquals(".xml", store.getFileSuffixFromType("image/svg+xml"));
		assertEquals(".json", store.getFileSuffixFromType("application/json"));
		assertEquals(".zip", store.getFileSuffixFromType("application/zip"));
	}

}
