package com.xmlcalabash.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import com.xmlcalabash.core.XProcException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Feb 1, 2009
 * Time: 2:06:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class MIMEReader {
    private static final int H_NAME = 1;
    private static final int H_VALUE = 2;
    private static final int H_DONE = 3;
    private static final int B_SOL = 4;
    private static final int B_CR = 5;
    private static final int B_MATCHSEP = 6;
    private static final int B_DATA = 7;
    private static final int B_MATCHLAST = 8;

    private InputStream stream = null;
    private String boundary = null;
    private String separator = null;
    private String lastsep = null;
    int peek = -1;
    private Header[] headers;
    private int headerPos = 0;

    public MIMEReader(InputStream stream, String boundary) {
        this.stream = stream;
        this.boundary = boundary;

        separator = "--" + boundary;
        lastsep = separator + "--";

        if (separator == null) {
            throw new NullPointerException();
        }

        String line = getLine();
        if (!separator.equals(line)) {
            throw new XProcException("MIME multipart doesn't start with separator?");
        }
    }

    public boolean readHeaders() {
        int headerSize = 5;
        int headergrow = 2;
        headers = new Header[headerSize];
        headerPos = 0;

        if (peekByte() < 0) {
            return false;
        }

        Header h = getHeader();
        while (h != null) {
            if (headerPos == headerSize) {
                Header newheaders[] = new Header[headerSize + headergrow];
                System.arraycopy(headers, 0, newheaders, 0, headerSize);
                headers = newheaders;
                headerSize += headergrow;
            }
            headers[headerPos++] = h;
            h = getHeader();
        }

        return true;
    }

    public Header[] getHeaders() {
        Header rh[] = new Header[headerPos];
        System.arraycopy(headers, 0, rh, 0, headerPos);
        return rh;
    }

    public Header getHeader(String name) {
        for (int pos = 0; pos < headerPos; pos++) {
            if (name.toLowerCase().equals(headers[pos].getName().toLowerCase())) {
                return headers[pos];
            }
        }
        return null;
    }

    // FIXME: RFC 822 suggests that headers must be separated by CRLF, but in practice
    // it seems that LF alone is sometimes used. I suppose CR alone is possible too.

    private Header getHeader() {
        String name = "";
        String value = "";
        
        int state = H_NAME;
        while (state != H_DONE) {
            int b = nextByte();
            int peek = peekByte();

            if (b == '\r') {
                if (peek == '\n') {
                    b = nextByte();
                    peek = peekByte();
                } else {
                    b = '\n';
                }
            }

            if (b < 0) {
                throw new XProcException("Got -1 reading stream...");
            }
            switch (state) {
                case H_NAME:
                    if (b == '\n') {
                        state = H_DONE;
                    } else {
                        if (b == ':') {
                            state = H_VALUE;
                        } else {
                            name += (char) b;
                        }
                    }
                    break;
                case H_VALUE:
                    if (b == '\n') {
                        if (peek == ' ' || peek == '\t') {
                            // nop, we'll catch this on the next loop
                        } else {
                            state = H_DONE;
                        }
                    } else {
                        value += (char) b;
                    }
                    break;
                default:
                    throw new XProcException("Default in getHeader?");
            }
        }

        if ("".equals(name)) {
            return null;
        } else {
            name = name.trim();
            value = value.trim();
            return new BasicHeader(name, value);
        }
    }

    public String getLine() {
        String line = "";
        boolean done = false;

        while (!done) {
            int b = nextByte();
            int peek = peekByte();

            if (b == '\r') {
                if (peek == '\n') {
                    b = nextByte();
                    peek = peekByte();
                } else {
                    b = '\n';
                }
            }

            if (b < 0) {
                throw new XProcException("Got -1 reading stream...");
            }

            if (b == '\n') {
                done = true;
            } else {
                line += (char) b;
            }
        }

        return line;
    }

    public InputStream readBodyPart(int contentLength) {
        byte bodybytes[] = new byte[contentLength];
        int pos = 0;

        if (peek >= 0) {
            bodybytes[0] = (byte) peek;
            peek = -1;
            pos++;
            contentLength--;
        }

        while (contentLength > 0) {
            try {
                int len = stream.read(bodybytes, pos, contentLength);
                if (len < 0) {
                    throw new XProcException("Read returned -1?");
                }
                pos += len;
                contentLength -= len;
            } catch (IOException ioe) {
                throw new XProcException(ioe);
            }
        }

        // The separator has to be at the start of a line...
        int peek = peekByte();
        if (peek == '\r') {
            nextByte();
            peek = peekByte();
        }

        if (peek == '\n') {
            nextByte();
        }

        String line = getLine();
        if (!separator.equals(line) && !lastsep.equals(line)) {
            throw new XProcException("MIME multipart missing separator?");
        }

        return new ByteArrayInputStream(bodybytes);
    }

    public InputStream readBodyPart() {
        int bodygrow = 4096;
        int bodysize = 16384;
        byte bodybytes[] = new byte[bodysize];
        boolean done = false;
        int bodyidx = 0;
        int sepidx = 0;
        int state = B_SOL;
        while (!done) {
            int b = nextByte();
            if (b < 0) {
                throw new XProcException("Got -1 in readBodyPart?");
            }

            if (bodyidx == bodysize) {
                byte newbytes[] = new byte[bodysize + bodygrow];
                System.arraycopy(bodybytes, 0, newbytes, 0, bodysize);
                bodybytes = newbytes;
                bodysize = bodysize + bodygrow;
            }

            char x = (char) b;
            bodybytes[bodyidx++] = (byte) b;

            switch (state) {
                case B_SOL:
                case B_MATCHSEP:
                    if (sepidx == separator.length()) {
                        if (b == '-') {
                            state = B_MATCHLAST;
                            nextByte();
                        } else if (b == '\r' || b == '\n') {
                            done = true;
                            bodyidx -= (separator.length() + 3); // The CR/LF is part of the separator!
                            if (b == '\r' && peekByte() == '\n') {
                                nextByte();
                            }
                        } else {
                            state = B_DATA;
                            sepidx = 0;
                        }
                    } else {
                        if ((char) b == separator.charAt(sepidx)) {
                            state = B_MATCHSEP;
                            sepidx++;
                        } else {
                            sepidx = 0;

                            if (b == '\n') {
                                state = B_SOL;
                            } else {
                                state = B_DATA;
                            }
                        }
                    }
                    break;
                case B_MATCHLAST:
                    if (b == '\r' || b == '\n') {
                        done = true;
                        // +4 = +1 for whatever reason, +1 for the "-" that got added, and +2 for the CR/LF
                        bodyidx -= (separator.length() + 4); // The CR/LF is part of the separator!
                        if (b == '\r' && peekByte() == '\n') {
                            nextByte();
                        }
                    } else {
                        state = B_DATA;
                        sepidx = 0;
                    }
                    break;
                case B_CR:
                case B_DATA:
                    if (b == '\n') {
                        state = B_SOL;
                    }
                    break;
            }

            if (b == '\r') {
                state = B_CR;
            }
        }

        return new ByteArrayInputStream(bodybytes, 0, bodyidx);
    }

    private int peekByte() {
        if (peek >=  0) {
            return peek;
        } else {
            try {
                peek = stream.read();
            } catch (IOException ioe) {
                throw new XProcException(ioe);
            }
            return peek;
        }
    }

    private int nextByte() {
        if (peek >=  0) {
            int v = peek;
            peek = -1;
            return v;
        } else {
            try {
                return stream.read();
            } catch (IOException ioe) {
                throw new XProcException(ioe);
            }
        }
    }

}
