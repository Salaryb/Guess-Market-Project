package engine.xml;
import jakarta.xml.bind.annotation.*;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class OrderBookData {
    @XmlAttribute(name = "initial", required = true)
    private int initial;

    @XmlAttribute(name = "d", required = true)
    private int d;

    @XmlAttribute(name = "allow-mint", required = true)
    private boolean allowMint;

    public int getInitial() { return initial; }
    public int getD() { return d; }
    public boolean isAllowMint() { return allowMint; }
}