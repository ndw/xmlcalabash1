package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.stathissideris.ascii2image.core.ConversionOptions;
import org.stathissideris.ascii2image.graphics.BitmapRenderer;
import org.stathissideris.ascii2image.graphics.Diagram;
import org.stathissideris.ascii2image.text.TextGrid;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class DiTAA extends DefaultStep {
    private static final QName _shadows = new QName("", "shadows");
    private static final QName _antialias = new QName("", "antialias");
    private static final QName _corners = new QName("", "corners");
    private static final QName _separation = new QName("", "separation");
    private static final QName _scale = new QName("", "scale");
    private static final QName _html = new QName("", "html");
    private static final QName h_img = new QName("", "http://www.w3.org/1999/xhtml", "img");
    private static final QName _src = new QName("", "src");

    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ConversionOptions options = null;

    /**
     * Creates a new instance of Identity
     */
    public DiTAA(XProcRuntime runtime, XAtomicStep step) {
        super(runtime, step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        String base64 = null;
        boolean html = false;

        try {
            options = new ConversionOptions();

            boolean shadows = getOption(_shadows, true);
            boolean antialias = getOption(_antialias, true);
            boolean roundedCorners = "rounded".equals(getOption(_corners, "rounded"));
            boolean separation = getOption(_separation, true);
            html = getOption(_html, false);

            String scaleStr = getOption(_scale, (String) null);
            if (scaleStr != null) {
                float scale = Float.parseFloat(scaleStr);
                options.renderingOptions.setScale(scale);
            }

            options.processingOptions.setCharacterEncoding("utf-8");
            options.renderingOptions.setDropShadows(shadows);
            options.processingOptions.setAllCornersAreRound(roundedCorners);
            options.processingOptions.setPerformSeparationOfCommonEdges(separation);
            options.renderingOptions.setAntialias(antialias);


            TextGrid grid = new TextGrid();
            if(options.processingOptions.getCustomShapes() != null){
                grid.addToMarkupTags(options.processingOptions.getCustomShapes().keySet());
            }

            XdmNode doc = source.read();
            String text = doc.getStringValue();

            ArrayList<StringBuffer> lines = new ArrayList<StringBuffer>();
            String[] linesArray = text.split("\n");
            for (int i = 0; i  < linesArray.length; i++) {
                lines.add(new StringBuffer(linesArray[i]));
            }
            grid.initialiseWithLines(lines, options.processingOptions);
            Diagram diagram = new Diagram(grid, options);
            RenderedImage image = new BitmapRenderer().renderToImage(diagram, options.renderingOptions);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64 = Base64.encodeBytes(baos.toByteArray(), 0, baos.size());
        } catch (Exception e) {
            throw new XProcException(e);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());

        if (html) {
            tree.addStartElement(h_img);
            tree.addAttribute(_src, "data:image/png;base64," + base64);
            tree.startContent();
            tree.addEndElement();
        } else {
            tree.addStartElement(XProcConstants.c_data);
            tree.startContent();
            tree.addText("data:image/png;base64," + base64);
            tree.addEndElement();
        }

        tree.endDocument();
        result.write(tree.getResult());

    }
}
