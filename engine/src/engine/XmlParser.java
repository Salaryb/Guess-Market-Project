package engine;

import engine.xml.GuessMarketData;
import exception.GuessMarketException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

public class XmlParser {

    public static GuessMarketData parse(String xmlFilePath) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarketData.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            File schemaFile = new File("GM-EX1-schema.xsd");

            if (schemaFile.exists()) {
                Schema schema = sf.newSchema(schemaFile);
                unmarshaller.setSchema(schema);
            } else {
                throw new GuessMarketException("Schema file 'GM-EX1-schema.xsd' not found in the root directory.");
            }

            return (GuessMarketData) unmarshaller.unmarshal(new File(xmlFilePath));

        } catch (Exception e) {
            throw new GuessMarketException("XML Error: " + e.getMessage());
        }
    }
}