package engine.xml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.SAXException;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

public class XmlParser {

    public static GuessMarketData parseXml(String filePath) throws JAXBException, SAXException {
        File file = new File(filePath);
        if (!file.exists() || !filePath.toLowerCase().endsWith(".xml")) {
            throw new IllegalArgumentException("The file does not exist or is not a valid XML format.");
        }

        JAXBContext context = JAXBContext.newInstance(GuessMarketData.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        File schemaFile = new File("GM-EX2-Schema.xsd");

        if (!schemaFile.exists()) {
            throw new IllegalArgumentException("The schema file (GM-EX2-Schema.xsd) was not found in the root directory.");
        }

        Schema schema = schemaFactory.newSchema(schemaFile);
        unmarshaller.setSchema(schema);

        return (GuessMarketData) unmarshaller.unmarshal(file);
    }
}