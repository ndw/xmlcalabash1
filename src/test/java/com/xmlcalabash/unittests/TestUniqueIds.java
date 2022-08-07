package com.xmlcalabash.unittests;

import com.xmlcalabash.util.URIUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestUniqueIds {
    public int nextId() {
        String test = URIUtils.makeUnique("http://example.com");
        if (test.contains("xmlcalabash_uniqueid=")) {
            int pos = test.indexOf("=");
            return Integer.parseInt(test.substring(pos+1), 10) + 1;
        } else {
            throw new RuntimeException("Failed to compute next id");
        }
    }

    @Test
    public void testNull() {
        String uri = null;
        int nextId = nextId();
        String unique = URIUtils.makeUnique(uri);
        Assert.assertNull(unique);
    }

    @Test
    public void testAddUniqueBare() {
        String uri = "http://example.com/";
        int nextId = nextId();
        String unique = URIUtils.makeUnique(uri);
        Assert.assertEquals("http://example.com/?xmlcalabash_uniqueid=" + nextId, unique);
    }

    @Test
    public void testAddUniqueHasQuery() {
        String uri = "http://example.com/?a=b&c=d";
        int nextId = nextId();
        String unique = URIUtils.makeUnique(uri);
        Assert.assertEquals("http://example.com/?a=b&c=d&xmlcalabash_uniqueid=" + nextId, unique);
    }

    @Test
    public void testReplaceUnique() {
        String uri = "http://example.com/?xmlcalabash_uniqueid=0";
        int nextId = nextId();
        String unique = URIUtils.makeUnique(uri);
        Assert.assertEquals("http://example.com/?xmlcalabash_uniqueid=" + nextId, unique);
    }

    @Test
    public void testReplaceUniqueHasQuery() {
        String uri = "http://example.com/?a=b&c=d&xmlcalabash_uniqueid=000";
        int nextId = nextId();
        String unique = URIUtils.makeUnique(uri);
        Assert.assertEquals("http://example.com/?a=b&c=d&xmlcalabash_uniqueid=" + nextId, unique);
    }

    @Test
    public void testReplaceUniqueMiddleQuery() {
        String uri = "http://example.com/?a=b&xmlcalabash_uniqueid=000&c=d";
        int nextId = nextId();
        String unique = URIUtils.makeUnique(uri);
        Assert.assertEquals("http://example.com/?a=b&xmlcalabash_uniqueid=" + nextId + "&c=d", unique);
    }

    @Test
    public void testFindIdNull() {
        String uri = null;
        Assert.assertEquals(0, URIUtils.uniqueId(uri));
    }

    @Test
    public void testFindIdMissing() {
        String uri = "http://example.com/?a=b&c=d";
        Assert.assertEquals(-1, URIUtils.uniqueId(uri));
    }

    @Test
    public void testFindId() {
        String uri = "http://example.com/?a=b&xmlcalabash_uniqueid=17&c=d";
        Assert.assertEquals(17, URIUtils.uniqueId(uri));
    }

}
