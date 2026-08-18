package engine.xml;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class MethodData {

    @XmlElement(name = "GM-LMSR")
    private LmsrData lmsr;

    // Getters
    public LmsrData getLmsr() {
        return lmsr;
    }
}