package engine.xml;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class LmsrData {

    @XmlElement(name = "b")
    private int b;

    // Getters
    public int getB() {
        return b;
    }
}