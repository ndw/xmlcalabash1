package com.xmlcalabash.extensions;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class MetadataExtractor extends DefaultStep {
    private static final QName _href = new QName("","href");
    private final static QName c_metadata = new QName("c", XProcConstants.NS_XPROC_STEP, "metadata");
    private final static QName c_tag = new QName("c", XProcConstants.NS_XPROC_STEP, "tag");
    private final static QName _dir = new QName("", "dir");
    private final static QName _type = new QName("", "type");
    private final static QName _name = new QName("", "name");

    private final static String[] controls = new String[] {
            "0000", "0001", "0002", "0003", "0004", "0005", "0006", "0007",
            "0008",                 "000b", "000c",         "000e", "000f",
            "0010", "0011", "0012", "0013", "0014", "0015", "0016", "0017",
            "0018", "0019", "001a", "001b", "001c", "001d", "001e", "001f",
            "007c" };

    private WritablePipe result = null;

    /**
     * Creates a new instance of Identity
     */
    public MetadataExtractor(XProcRuntime runtime, XAtomicStep step) {
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

        URI href = getOption(_href).getBaseURI().resolve(getOption(_href).getString());

        try {
            URL url = href.toURL();
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();
            Metadata metadata = JpegMetadataReader.readMetadata(stream);

            TreeWriter tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(c_metadata);
            tree.addAttribute(_href, href.toASCIIString());
            tree.startContent();

            // iterate through metadata directories
            Iterator<Directory> directories = metadata.getDirectories().iterator();
            while (directories.hasNext()) {
                Directory directory = directories.next();
                String dir = directory.getName();
                Iterator<Tag> tags = directory.getTags().iterator();
                while (tags.hasNext()) {
                    Tag tag = tags.next();

                    tree.addStartElement(c_tag);
                    tree.addAttribute(_dir, dir);
                    tree.addAttribute(_type, tag.getTagTypeHex());
                    tree.addAttribute(_name, tag.getTagName());

                    String value = tag.getDescription();

                    // Laboriously escape all the control characters with \\uxxxx, but first replace
                    // \\uxxxx with \\u005cuxxxx so we don't inadvertantly change the meaning of a string
                    value = value.replaceAll("\\\\u([0-9a-fA-F]{4}+)", "\\\\u005cu$1");
                    for (String control : controls) {
                        String match = "^.*\\u" + control + ".*$";
                        if (value.matches(match)) {
                            value = value.replaceAll("[\\u" + control + "]", "\\\\u" + control);
                        }
                    }

                    // Bah humbug...I don't see an easy way to tell if it's a date/time
                    if (value.matches("^\\d\\d\\d\\d:\\d\\d:\\d\\d \\d\\d:\\d\\d:\\d\\d$")) {
                        String iso = value.substring(0, 4) + "-" + value.substring(5, 7) + "-" + value.substring(8, 10)
                                + "T" + value.substring(11,19);
                        value = iso;
                    }

                    tree.startContent();
                    tree.addText(value);
                    tree.addEndElement();
                }
            }

            tree.addEndElement();
            tree.endDocument();
            result.write(tree.getResult());
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        } catch (JpegProcessingException e) {
            // Not a JPEG? Let's try to do at least the intrinsics...
            runIntrinsics(href);
        }
    }
    
    private void runIntrinsics(URI href) throws SaxonApiException {
        ImageIntrinsics intrinsics = new ImageIntrinsics();
        intrinsics.run(href.toASCIIString());
    }

    private class ImageIntrinsics implements ImageObserver {
        boolean imageLoaded = false;
        boolean imageFailed = false;
        Image image = null;
        int width = -1;
        int depth = -1;

        public void run(String imageFn) {
            imageLoaded = false;
            imageFailed = false;
            image = null;
            width = -1;
            depth = -1;

            System.setProperty("java.awt.headless","true");

            try {
                URL url = new URL(imageFn);
                image = Toolkit.getDefaultToolkit().getImage (url);
            } catch (MalformedURLException mue) {
                image = Toolkit.getDefaultToolkit().getImage (imageFn);
            }

            width = image.getWidth(this);
            depth = image.getHeight(this);

            while (!imageFailed && (width == -1 || depth == -1)) {
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                    // nop;
                }
                width = image.getWidth(this);
                depth = image.getHeight(this);
            }

            image.flush();

            if ((width == -1 || depth == -1) && imageFailed) {
                // Maybe it's an EPS or PDF?
                // FIXME: this code is crude
                BufferedReader ir = null;
                String line = null;
                int lineLimit = 100;

                try {
                    ir = new BufferedReader(new FileReader(new File(imageFn)));
                    line = ir.readLine();

                    if (line != null && line.startsWith("%PDF-")) {
                        // We've got a PDF!
                        while (lineLimit > 0 && line != null) {
                            lineLimit--;
                            if (line.startsWith("/CropBox [")) {
                                line = line.substring(10);
                                if (line.indexOf("]") >= 0) {
                                    line = line.substring(0, line.indexOf("]"));
                                }
                                parseBox(line);
                                lineLimit = 0;
                            }
                            line = ir.readLine();
                        }
                    } else if (line != null
                            && line.startsWith("%!")
                            && line.indexOf(" EPSF-") > 0) {
                        // We've got an EPS!
                        while (lineLimit > 0 && line != null) {
                            lineLimit--;
                            if (line.startsWith("%%BoundingBox: ")) {
                                line = line.substring(15);
                                parseBox(line);
                                lineLimit = 0;
                            }
                            line = ir.readLine();
                        }
                    } else {
                        System.err.println("Failed to interpret image: " + imageFn);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load image: " + imageFn);
                    // nop;
                }

                if (ir != null) {
                    try {
                        ir.close();
                    } catch (Exception e) {
                        // nop;
                    }
                }
            }

            if (width >= 0) {
                TreeWriter tree = new TreeWriter(runtime);
                tree.startDocument(step.getNode().getBaseURI());
                tree.addStartElement(c_metadata);
                tree.addAttribute(_href, imageFn);
                tree.startContent();

                tree.addStartElement(c_tag);
                tree.addAttribute(_dir, "Exif");
                tree.addAttribute(_type, "0x9000");
                tree.addAttribute(_name, "Exif Version");
                tree.startContent();
                tree.addText("0");
                tree.addEndElement();

                tree.addStartElement(c_tag);
                tree.addAttribute(_dir, "Jpeg");
                tree.addAttribute(_type, "0x0001");
                tree.addAttribute(_name, "Image Height");
                tree.startContent();
                tree.addText(""+depth+" pixels");
                tree.addEndElement();

                tree.addStartElement(c_tag);
                tree.addAttribute(_dir, "Jpeg");
                tree.addAttribute(_type, "0x0003");
                tree.addAttribute(_name, "Image Width");
                tree.startContent();
                tree.addText(""+width+" pixels");
                tree.addEndElement();

                tree.endDocument();
                result.write(tree.getResult());
            } else {
                throw new XProcException("Failed to read image intrinsics");
            }
        }

        private void parseBox(String line) {
            int [] corners = new int [4];
            int count = 0;

            StringTokenizer st = new StringTokenizer(line);
            while (count < 4 && st.hasMoreTokens()) {
                try {
                    corners[count++] = Integer.parseInt(st.nextToken());
                } catch (Exception e) {
                    // nop;
                }
            }

            width = corners[2] - corners[0];
            depth = corners[3] - corners[1];
        }

        public boolean imageUpdate(Image img, int infoflags,
                                   int x, int y, int width, int height) {
            if (((infoflags & ImageObserver.ERROR) == ImageObserver.ERROR)
                    || ((infoflags & ImageObserver.ABORT) == ImageObserver.ABORT)) {
                imageFailed = true;
                return false;
            }

            // I really only care about the width and height, but if I return false as
            // soon as those are available, the BufferedInputStream behind the loader
            // gets closed too early.
            if ((infoflags & ImageObserver.ALLBITS) == ImageObserver.ALLBITS) {
                return false;
            } else {
                return true;
            }
        }
    }
}