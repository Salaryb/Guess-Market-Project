package engine;

import dto.*;

import java.util.List;

public interface IGuessMarketEngine {

    void loadDataFromXml(String filePath);

    List<EventDTO> getAllEvents();
    List<String> getAllUsernames();
    double getUserBalance(String username);
    boolean isUserBlocked(String username);
    List<UserEventSummaryDTO> getUserActiveEvents(String username);
    List<HoldingDTO> getUserHoldingsForEvent(String username, int eventId);

    void activateEvent(int eventId, String initiatorUsername);
    void closeEvent(int eventId, String initiatorUsername, String winningOption);

    void placeOrderBookOrder(int eventId, String username, String option, int shares, double price, boolean isBuy);

    // For Order Book Events
    List<OrderDTO> getBuyOrders(int eventId);
    List<OrderDTO> getSellOrders(int eventId);

    // For LMSR Events
    List<TradeDTO> getLmsrTradeHistory(int eventId);
    List<OptionDTO> getLmsrOptionStatus(int eventId);

    // General Event Info (Commission, Winner, etc.)
    double getEventTotalCommission(int eventId);
    String getEventWinningOption(int eventId);

    void placeLmsrOrder(int eventId, String username, String option, int shares);

    List<String> getEventOptions(int eventId);

    List<ParticipantDTO> getEventParticipants(int eventId);

    double getEventAccountBalance(int eventId);

    double getLastTradePrice(int eventId, String option);
}