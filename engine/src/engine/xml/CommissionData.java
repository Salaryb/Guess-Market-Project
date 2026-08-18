package engine.xml;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class CommissionData {

    @XmlAttribute(name = "type")
    private String type;

    @XmlValue
    private int value;

    // Getters
    public String getType() {
        return type;
    }

    public int getValue() {
        return value;
    }
}