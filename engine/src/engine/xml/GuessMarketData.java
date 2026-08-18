package engine.xml;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class GuessMarketData {

    @XmlElementWrapper(name = "GM-events")
    @XmlElement(name = "GM-event")
    private List<EventData> events;

    public List<EventData> getEvents() { return events; }
}