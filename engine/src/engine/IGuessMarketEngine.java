package engine;

import dto.EventDTO;
import dto.EventDetailsDTO;
import dto.UserDetailsDTO;
import dto.UserEventDetailsDTO;
import dto.UserSummaryDTO;

import java.io.InputStream;
import java.util.List;

/**
 * The Guess Market engine (exercise 3).
 * The engine is passive: it does not know who calls it (a servlet, a test, a console UI...).
 * It is NOT thread safe by itself - the server layer is responsible for synchronizing access.
 * Every invalid request is answered with a GuessMarketException holding a readable message.
 * Every text input (user / event / option names) is case insensitive.
 */
public interface IGuessMarketEngine {

    // ----- Users -----

    /**
     * Logs a user in. A new name creates a new user (balance 0). A name that is currently logged in is rejected.
     * @return the user's name as it is registered in the system (the casing of the first login)
     */
    String loginUser(String userName);

    void logoutUser(String userName);

    boolean isUserExists(String userName);

    void deposit(String userName, double amount);

    List<UserSummaryDTO> getAllUsers();

    UserDetailsDTO getUserDetails(String userName);

    UserEventDetailsDTO getUserEventDetails(String userName, String eventName);

    // ----- Events -----

    /**
     * Validates the XML and adds all its events to the system (accumulative).
     * The uploading user becomes the market maker of all the events in the file.
     * Either all events are added or none of them (if the file is invalid).
     * @return the names of the events that were added.
     */
    List<String> loadEventsFromXml(InputStream xmlContent, String uploaderName);

    List<EventDTO> getAllEvents();

    EventDetailsDTO getEventDetails(String eventName);

    /** Only the market maker can activate. Pays the LMSR subsidy / the Order Book initial shares. */
    String activateEvent(String eventName, String userName);

    /** Only the market maker can close. Pays the winners and the commissions. */
    String closeEvent(String eventName, String userName, String winningOption);

    // ----- Trading -----

    String buyLmsrShares(String eventName, String userName, String option, int shares);

    String placeOrder(String eventName, String userName, String option, boolean buy, int shares, double price);

    // ----- Change tracking -----

    /** A number that grows by one on every change in the system (used by clients to pull only when needed). */
    long getVersion();
}
