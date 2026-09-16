package engine.xml;
import jakarta.xml.bind.annotation.*;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class LmsrData {
    @XmlElement(name = "b")
    private int b;

    public int getB() { return b; }
}