package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class WaitForUpdate extends DefaultStep {
    private static final QName _href = new QName("","href");
    private static final QName _pause_before = new QName("","pause-before");
    private static final QName _pause_after = new QName("","pause-after");
    private static final long FILESYSTEM_WAIT = 100;
    private static final long HTTP_WAIT = 1000;
    private WritablePipe result = null;
    private URI uri = null;
    private long pauseBefore = 0;
    private long pauseAfter = 0;

    /**
     * Creates a new instance of Identity
     */
    public WaitForUpdate(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        String href = getOption(_href).getString();
        URI baseURI = getOption(_href).getBaseURI();
        URI uri = baseURI.resolve(href);

        pauseBefore = getOption(_pause_before, (long) 0) * 1000;
        pauseAfter = getOption(_pause_after, (long) 0) * 1000;

        if (pauseBefore > 0) {
            try {
                Thread.sleep(pauseBefore);
            } catch (InterruptedException ie) {
                // I don't care
            }
        }

        String changed = "";
        if ("file".equals(uri.getScheme())) {
            changed = waitForFile(uri);
        } else if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
            changed = waitForHttp(uri);
        } else {
            throw new XProcException("Only http: and file: URIs are supported on cx:wait-for-update");
        }

        if (pauseAfter > 0) {
            try {
                Thread.sleep(pauseAfter);
            } catch (InterruptedException ie) {
                // I don't care
            }
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();
        tree.addText(changed);
        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }

    private String waitForFile(URI uri) {
        File f = new File(uri.getPath());
        boolean newFile = false;

        if (!f.exists()) {
            newFile = true;
            runtime.fine(this, step.getNode(), "Exist wait: " + f.getAbsolutePath());
            if (runtime.getDebug()) {
                System.err.println("Exist wait: " + f.getAbsolutePath());
            }
            while (!f.exists()) {
                try {
                    Thread.sleep(FILESYSTEM_WAIT);
                } catch (InterruptedException ie) {
                    // I don't care
                }
            }
        }

        long dt = f.lastModified();
        long cdt = dt;

        if (!newFile) {
            runtime.fine(this, step.getNode(), "Update wait: " + f.getAbsolutePath());
            if (runtime.getDebug()) {
                System.err.println("Update wait: " + f.getAbsolutePath());
            }
            while (cdt == dt) {
                try {
                    Thread.sleep(FILESYSTEM_WAIT);
                } catch (InterruptedException ie) {
                    // I don't care
                }
                cdt = f.lastModified();
            }
        }

        Calendar cal = GregorianCalendar.getInstance();
        TimeZone tz = TimeZone.getDefault();
        long gmt = cdt - tz.getRawOffset();
        if (tz.useDaylightTime() && tz.inDaylightTime(cal.getTime())) {
            gmt -= tz.getDSTSavings();
        }

        cal.setTimeInMillis(gmt);
        return String.format("%1$04d-%2$02d-%3$02dT%4$02d:%5$02d:%6$02dZ",
                             cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH),
                             cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
    }

    private String waitForHttp(URI uri) {
        SystemDefaultHttpClient client = new SystemDefaultHttpClient();
        client.setHttpRequestRetryHandler(new StandardHttpRequestRetryHandler(3, false));
        HttpContext localContext = new BasicHttpContext();
        HttpUriRequest httpRequest = new HttpHead(uri);
        HttpResponse httpResponse = null;

        httpResponse = head(client, httpRequest, localContext);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        String date = null;
        boolean newUri = false;

        if (statusCode == 404) {
            newUri = true;
            runtime.fine(this, step.getNode(), "Exist wait: " + uri.toASCIIString());
            if (runtime.getDebug()) {
                System.err.println("Exist wait: " + uri.toASCIIString());
            }
            while (statusCode == 404) {
                try {
                    Thread.sleep(HTTP_WAIT); // one second
                } catch (InterruptedException ie) {
                    // I don't care
                }

                httpResponse = head(client, httpRequest, localContext);
                statusCode = httpResponse.getStatusLine().getStatusCode();
            }
        }

        long dt = lastModified(httpResponse);
        long cdt = dt;

        if (statusCode == 200 && !newUri) {
            runtime.fine(this, step.getNode(), "Update wait: " + uri.toASCIIString());
            if (runtime.getDebug()) {
                System.err.println("Update wait: " + uri.toASCIIString());
            }
            while (statusCode == 200 && cdt == dt) {
                try {
                    Thread.sleep(HTTP_WAIT);
                } catch (InterruptedException ie) {
                    // I don't care
                }
                httpResponse = head(client, httpRequest, localContext);
                statusCode = httpResponse.getStatusLine().getStatusCode();
                cdt = lastModified(httpResponse);
            }
        }

        Calendar cal = GregorianCalendar.getInstance();
        TimeZone tz = TimeZone.getDefault();
        long gmt = cdt - tz.getRawOffset();
        if (tz.useDaylightTime() && tz.inDaylightTime(cal.getTime())) {
            gmt -= tz.getDSTSavings();
        }

        cal.setTimeInMillis(gmt);
        return String.format("%1$04d-%2$02d-%3$02dT%4$02d:%5$02d:%6$02dZ",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
    }

    private long lastModified(HttpResponse httpResponse) {
        String date = getHeader(httpResponse, "Last-modified", null);
        if (date == null) {
            date = getHeader(httpResponse, "Date", null);
        }

        try {
            Date dt = DateUtils.parseDate(date);
            return dt.getTime();
        } catch (DateParseException dpe) {
            // ignore
        }

        return GregorianCalendar.getInstance().getTimeInMillis();
    }

    private HttpResponse head(SystemDefaultHttpClient client, HttpUriRequest httpRequest, HttpContext localContext) {
        try {
            return client.execute(httpRequest, localContext);
        } catch (ClientProtocolException cpe) {
            throw new XProcException(cpe);
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }

    private String getHeader(HttpResponse resp, String name, String def) {
        Header[] headers = resp.getHeaders(name);

        if (headers == null) {
            return def;
        }

        if (headers == null || headers.length == 0) {
            // This should never happen
            return def;
        } else {
            return headers[0].getValue();
        }
    }
}
