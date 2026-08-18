package engine.xml;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class EventData {
    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "id")
    private int id;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "comision")
    private CommissionData commission;

    @XmlElementWrapper(name = "GM-options")
    @XmlElement(name = "GM-option")
    private List<String> options;

    @XmlElement(name = "GM-method")
    private MethodData method;

    // Getters
    public String getName() { return name; }
    public int getId() { return id; }
    public String getDescription() { return description; }
    public CommissionData getCommission() { return commission; }
    public List<String> getOptions() { return options; }
    public MethodData getMethod() { return method; }
}