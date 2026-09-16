package engine.xml;
import jakarta.xml.bind.annotation.*;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class EventData {
    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElement(name = "id")
    private int id;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "commission")
    private CommissionData commission;

    @XmlElement(name = "GM-options")
    private OptionsData options;

    @XmlElement(name = "GM-method")
    private MethodData method;

    public String getName() { return name; }
    public int getId() { return id; }
    public String getDescription() { return description; }
    public CommissionData getCommission() { return commission; }
    public OptionsData getOptions() { return options; }
    public MethodData getMethod() { return method; }
}