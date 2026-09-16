package engine.xml;
import jakarta.xml.bind.annotation.*;
import java.util.List;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class MarketMakerData {
    @XmlElement(name = "event")
    private List<EventIdData> events;

    public List<EventIdData> getEvents() { return events; }
}