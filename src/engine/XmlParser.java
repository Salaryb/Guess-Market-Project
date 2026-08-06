package engine;

import engine.models.Event;
import engine.models.Option;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XmlParser {
    public static List<Event> parseEvents(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !filePath.toLowerCase().endsWith(".xml")) {
            throw new Exception("File does not exist or is not an XML file.");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);
        document.getDocumentElement().normalize();

        List<Event> events = new ArrayList<>();
        Set<Integer> existingIds = new HashSet<>();
        NodeList eventNodes = document.getElementsByTagName("GM-event");

        for (int i = 0; i < eventNodes.getLength(); i++) {
            Element eventElement = (Element) eventNodes.item(i);

            int id = Integer.parseInt(eventElement.getElementsByTagName("id").item(0).getTextContent());
            if (!existingIds.add(id)) {
                throw new Exception("Duplicate event ID found: " + id);
            }

            String name = eventElement.getAttribute("name");
            String description = eventElement.getElementsByTagName("description").item(0).getTextContent();

            Element commElement = (Element) eventElement.getElementsByTagName("comision").item(0);
            int commission = Integer.parseInt(commElement.getTextContent());
            if (commission < 0 || commission > 90) {
                throw new Exception("Commission must be between 0 and 90. Found: " + commission);
            }
            String commType = commElement.getAttribute("type").toLowerCase();

            List<Option> options = new ArrayList<>();
            NodeList optionNodes = eventElement.getElementsByTagName("GM-option");
            for (int j = 0; j < optionNodes.getLength(); j++) {
                options.add(new Option(optionNodes.item(j).getTextContent().trim()));
            }

            Element lmsrElement = (Element) eventElement.getElementsByTagName("GM-LMSR").item(0);
            int b = Integer.parseInt(lmsrElement.getElementsByTagName("b").item(0).getTextContent());

            events.add(new Event(id, name, description, commission, commType, options, b));
        }
        return events;
    }
}