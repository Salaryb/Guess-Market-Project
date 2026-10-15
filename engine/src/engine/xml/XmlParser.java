package engine.xml;

import exception.GuessMarketException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.net.URL;

/**
 * Reads a Guess Market XML (exercise 3 format) from a stream.
 * The file is never saved to disk: the server parses the uploaded content directly.
 * The schema is packaged inside the engine jar, next to this class.
 */
public final class XmlParser {
    private static final String SCHEMA_RESOURCE = "GM-EX3-Schema.xsd";

    private XmlParser() {
    }

    public static GuessMarketData parse(InputStream xmlContent) {
        try {
            Unmarshaller unmarshaller = JAXBContext.newInstance(GuessMarketData.class).createUnmarshaller();
            unmarshaller.setSchema(loadSchema());
            return (GuessMarketData) unmarshaller.unmarshal(xmlContent);
        } catch (UnmarshalException e) {
            Throwable cause = e.getLinkedException() != null ? e.getLinkedException() : e;
            throw new GuessMarketException("The file is not a valid Guess Market XML (exercise 3 format): "
                    + cause.getMessage());
        } catch (JAXBException e) {
            throw new GuessMarketException("Failed reading the XML file: " + e.getMessage());
        }
    }

    private static Schema loadSchema() {
        URL schemaUrl = XmlParser.class.getResource(SCHEMA_RESOURCE);
        if (schemaUrl == null) {
            throw new GuessMarketException("Internal error: the schema " + SCHEMA_RESOURCE + " is missing from the engine.");
        }
        try {
            return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaUrl);
        } catch (SAXException e) {
            throw new GuessMarketException("Internal error: failed loading the schema: " + e.getMessage());
        }
    }
}
