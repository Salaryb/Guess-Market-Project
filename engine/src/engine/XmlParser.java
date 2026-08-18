package engine;

import engine.xml.GuessMarketData;
import exception.GuessMarketException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;

public class XmlParser {
    public static GuessMarketData parse(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new GuessMarketException("XML file not found: " + filePath);
            }

            JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarketData.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            GuessMarketData data = (GuessMarketData) unmarshaller.unmarshal(file);

            return data;

        } catch (Exception e) {
            throw new GuessMarketException("Failed to parse XML: " + e.getMessage());
        }
    }
}