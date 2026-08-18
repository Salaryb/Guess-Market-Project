package engine;
import dto.EventDTO;
import dto.EventDetailsDTO;
import java.util.List;

public interface IGuessMarketEngine {
    void loadXml(String filePath);
    List<EventDTO> getAllEvents();
    EventDetailsDTO getEventDetails(int eventId);
    double buyShares(int eventId, int optionIndex, int amount);
    void closeEvent(int eventId, int winningOptionIndex);
    void saveSystemState(String pathWithoutExtension);
    void loadSystemState(String pathWithoutExtension);
}