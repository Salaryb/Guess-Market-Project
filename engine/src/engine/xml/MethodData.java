package engine.xml;
import jakarta.xml.bind.annotation.*;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class MethodData {
    @XmlElement(name = "GM-LMSR")
    private LmsrData lmsr;

    @XmlElement(name = "GM-order-book")
    private OrderBookData orderBook;

    public LmsrData getLmsr() { return lmsr; }
    public OrderBookData getOrderBook() { return orderBook; }
}