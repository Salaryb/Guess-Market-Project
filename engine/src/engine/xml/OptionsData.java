package engine.xml;
import jakarta.xml.bind.annotation.*;
import java.util.List;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class OptionsData {
    @XmlElement(name = "GM-option")
    private List<String> options;

    public List<String> getOptions() { return options; }
}