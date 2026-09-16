package engine.xml;
import jakarta.xml.bind.annotation.*;

@SuppressWarnings("unused")
@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class GuessMarketData {
    @XmlElement(name = "GM-events")
    private EventsData events;

    @XmlElement(name = "GM-users")
    private UsersData users;

    public EventsData getEvents() { return events; }
    public UsersData getUsers() { return users; }
}