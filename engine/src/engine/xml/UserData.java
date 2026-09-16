package engine.xml;
import jakarta.xml.bind.annotation.*;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserData {
    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElement(name = "initial-cash")
    private int initialCash;

    @XmlElement(name = "GM-market-maker")
    private MarketMakerData marketMakerEvents;

    public String getName() { return name; }
    public int getInitialCash() { return initialCash; }
    public MarketMakerData getMarketMakerEvents() { return marketMakerEvents; }
}