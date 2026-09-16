package engine.xml;

import jakarta.xml.bind.annotation.*;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class MarketMakerEventData {

    @XmlAttribute(name = "id", required = true)
    private int id;

    public int getId() { return id; }
}