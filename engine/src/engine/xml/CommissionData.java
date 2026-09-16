package engine.xml;
import jakarta.xml.bind.annotation.*;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class CommissionData {
    @XmlValue
    private int value;

    @XmlAttribute(name = "type", required = true)
    private String type;

    public int getValue() { return value; }
    public String getType() { return type; }
}