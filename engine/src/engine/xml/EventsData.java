package engine.xml;
import jakarta.xml.bind.annotation.*;
import java.util.List;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class EventsData {
    @XmlElement(name = "GM-event")
    private List<EventData> events;

    public List<EventData> getEvents() { return events; }
}