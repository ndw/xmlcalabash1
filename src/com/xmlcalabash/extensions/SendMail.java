package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.NodeToBytes;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 11/15/11
 * Time: 7:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class SendMail extends DefaultStep {
    public final static String NS_EMAIL = "URN:ietf:params:email-xml:";
    public final static String NS_RFC822 = "URN:ietf:params:rfc822:";
    public static final QName _content_type = new QName("content-type");
    public final static QName em_Message = new QName("em", NS_EMAIL, "Message");
    public final static QName em_Address = new QName("em", NS_EMAIL, "Address");
    public final static QName em_name = new QName("em", NS_EMAIL, "name");
    public final static QName em_adrs = new QName("em", NS_EMAIL, "adrs");
    public final static QName em_content = new QName("em", NS_EMAIL, "content");

    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Identity
     */
    public SendMail(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
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

        // Let's see if we like the message
        XdmNode email = S9apiUtils.getDocumentElement(source.read());
        if (!email.getNodeName().equals(em_Message)) {
            throw new XProcException("cx:send-mail source is not an em:Message");
        }

        Properties props = new Properties();
        if (runtime.getSendmailHost() != null) {
            props.put("mail.smtp.host", runtime.getSendmailHost());
        }
        if (runtime.getSendmailPort() != null) {
            props.put("mail.smtp.port", runtime.getSendmailPort());
        }
        if (runtime.getSendmailUsername() != null) {
            props.put("mail.smtp.auth", "true");
        }

        MimeMultipart mp = null;

        try {
            Authenticator auth = new SMTPAuthenticator();
            Session session = Session.getDefaultInstance(props, auth);
            session.setDebug(runtime.getDebug());

            MimeMessage msg = new MimeMessage(session);

            // Now parse the message...
            XdmSequenceIterator iter = email.axisIterator(Axis.CHILD);
            while (iter.hasNext()) {
                XdmNode field = (XdmNode) iter.next();
                if (!field.getNodeKind().equals(XdmNodeKind.ELEMENT)) {
                    continue;
                }

                if (NS_RFC822.equals(field.getNodeName().getNamespaceURI())) {
                    String name = field.getNodeName().getLocalName();
                    if ("to".equals(name)) {
                        InternetAddress[] addrs = parseAddresses(field);
                        msg.setRecipients(Message.RecipientType.TO, addrs);
                    } else if ("from".equals(name)) {
                        InternetAddress from = parseAddress(field);
                        msg.setFrom(from);
                    } else if ("sender".equals(name)) {
                        InternetAddress from = parseAddress(field);
                        msg.setSender(from);
                    } else if ("subject".equals(name)) {
                        msg.setSubject(field.getStringValue());
                    } else if ("cc".equals(name)) {
                        InternetAddress[] addrs = parseAddresses(field);
                        msg.setRecipients(Message.RecipientType.CC, addrs);
                    } else if ("bcc".equals(name)) {
                        InternetAddress[] addrs = parseAddresses(field);
                        msg.setRecipients(Message.RecipientType.BCC, addrs);
                    } else {
                        throw new XProcException("Unexpected RFC 822 element in email message: " + name);
                    }
                } else if (em_content.equals(field.getNodeName())) {
                    // What kind of content is this?
                    boolean text = false;
                    boolean html = false;

                    Vector<XdmNode> nodes = new Vector<XdmNode>();
                    XdmSequenceIterator citer = field.axisIterator(Axis.CHILD);
                    while (citer.hasNext()) {
                        XdmNode child = (XdmNode) citer.next();
                        nodes.add(child);
                        if (!html && !text) {
                            if (child.getNodeKind().equals(XdmNodeKind.ELEMENT)) {
                                html = "http://www.w3.org/1999/xhtml".equals(child.getNodeName().getNamespaceURI());
                            } else if (child.getNodeKind().equals(XdmNodeKind.TEXT)) {
                                text = !"".equals(child.getStringValue().trim());
                            }
                        }
                    }

                    String content = null;
                    String contentType = null;
                    if (html) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Serializer serializer = new Serializer();
                        serializer.setOutputProperty(Serializer.Property.ENCODING, "utf-8");
                        serializer.setOutputProperty(Serializer.Property.INDENT, "no");
                        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
                        serializer.setOutputProperty(Serializer.Property.METHOD, "html");
                        serializer.setOutputStream(stream);
                        S9apiUtils.serialize(runtime, nodes, serializer);
                        content = stream.toString();
                        contentType = "text/html; charset=utf-8";
                    } else {
                        content = field.getStringValue();
                        contentType = "text/plain; charset=utf-8";
                    }

                    if (source.moreDocuments()) {
                        mp = new MimeMultipart();
                        BodyPart bodyPart = new MimeBodyPart();
                        DataSource source = new PartDataSource(content.getBytes("utf-8"), contentType, "irrelevant");
                        bodyPart.setDataHandler(new DataHandler(source));
                        bodyPart.setDisposition(Part.INLINE);
                        mp.addBodyPart(bodyPart);
                    } else {
                        msg.setContent(content, contentType);
                    }
                } else {
                    throw new XProcException("Unexpected element in email message: " + field.getNodeName());
                }
            }

            while (source.moreDocuments()) {
                XdmNode xmlpart = S9apiUtils.getDocumentElement(source.read());
                String contentType = xmlpart.getAttributeValue(_content_type);
                String filename = xmlpart.getBaseURI().getPath();
                int pos = filename.lastIndexOf("/");
                if (pos >= 0) {
                    filename = filename.substring(pos+1);
                }
                if (contentType == null) { contentType = "application/octet-stream"; }
                BodyPart bodyPart = new MimeBodyPart();
                DataSource source = new PartDataSource(NodeToBytes.convert(runtime, xmlpart, true), contentType, "irrelevant");
                bodyPart.setDataHandler(new DataHandler(source));
                bodyPart.setFileName(filename);
                bodyPart.setDisposition(Part.ATTACHMENT);
                mp.addBodyPart(bodyPart);
            }
            msg.setContent(mp);

            Transport.send(msg);
        } catch (AddressException ex) {
            throw new XProcException(ex);
        } catch (MessagingException ex) {
            throw new XProcException(ex);
        } catch (UnsupportedEncodingException ex) {
            // I don't think this can ever actually happen
            throw new XProcException(ex);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();
        tree.addText("true");
        tree.addEndElement();
        tree.endDocument();
        result.write(tree.getResult());
    }

    private InternetAddress parseAddress(XdmNode field) {
        InternetAddress email = null;
        XdmSequenceIterator iter = field.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode addr = (XdmNode) iter.next();
            if (!addr.getNodeKind().equals(XdmNodeKind.ELEMENT)) {
                continue;
            }
            if (em_Address.equals(addr.getNodeName())) {
                if (email == null) {
                    email = parseEmail(addr);
                } else {
                    throw new XProcException("Expected only a single email address in " + field.getNodeName());
                }
            } else {
                throw new XProcException("Only <em:Address> is supported in " + field.getNodeName());
            }
        }
        return email;
    }

    private InternetAddress[] parseAddresses(XdmNode field) {
        Vector<InternetAddress> emails = new Vector<InternetAddress> ();
        XdmSequenceIterator iter = field.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode addr = (XdmNode) iter.next();
            if (!addr.getNodeKind().equals(XdmNodeKind.ELEMENT)) {
                continue;
            }
            if (em_Address.equals(addr.getNodeName())) {
                emails.add(parseEmail(addr));
            } else {
                throw new XProcException("Only <em:Address> is supported in " + field.getNodeName());
            }
        }

        InternetAddress[] addrs = new InternetAddress[emails.size()];
        for (int pos = 0; pos < emails.size(); pos++) {
            addrs[pos] = emails.get(pos);
        }
        return addrs;
    }

    private InternetAddress parseEmail(XdmNode address) {
        String email_name = null;
        String email_addr = null;

        XdmSequenceIterator iter = address.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode addr = (XdmNode) iter.next();
            if (!addr.getNodeKind().equals(XdmNodeKind.ELEMENT)) {
                continue;
            }
            if (em_name.equals(addr.getNodeName())) {
                email_name = addr.getStringValue();
            } else if (em_adrs.equals(addr.getNodeName())) {
                email_addr = addr.getStringValue();
            } else {
                throw new XProcException("Only <em:name> and <em:adrs> are supported in " + address.getNodeName());
            }
        }

        if (email_addr == null) {
            throw new XProcException("Email address specified without an <em:adrs>");
        }

        InternetAddress email = null;
        try {
            if (email_name == null) {
                email = new InternetAddress(email_addr);
            } else {
                email = new InternetAddress(email_addr, email_name);
            }
        } catch (UnsupportedEncodingException ex) {
            throw new XProcException(ex);
        } catch (AddressException ex) {
            throw new XProcException(ex);
        }

        return email;
    }

    private class SMTPAuthenticator extends javax.mail.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(runtime.getSendmailUsername(), runtime.getSendmailPassword());
        }
    }

    private class PartDataSource implements DataSource {
        private byte[] data = null;
        private String contentType = null;
        private String name = null;

        public PartDataSource(byte[] data, String contentType, String name) {
            this.data = data;
            this.contentType = contentType;
            this.name = name;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        public OutputStream getOutputStream() throws IOException {
            throw new XProcException("Called getOutputStream() on PartDataSource for cx:send-mail???");
        }

        public String getContentType() {
            return contentType;
        }

        public String getName() {
            return name;
        }
    }
}
