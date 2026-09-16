package engine.xml;
import jakarta.xml.bind.annotation.*;
import java.util.List;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class UsersData {
    @XmlElement(name = "GM-user")
    private List<UserData> users;

    public List<UserData> getUsers() { return users; }
}