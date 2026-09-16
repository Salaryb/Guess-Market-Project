package engine;

import dto.*;
import engine.models.*;
import engine.xml.*;
import exception.GuessMarketException;

import java.util.*;
import java.util.stream.Collectors;

public class GuessMarketEngineImpl implements IGuessMarketEngine {

    private final Map<Integer, Event> events = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();

    @Override
    public void loadDataFromXml(String filePath) {
        try {
            GuessMarketData xmlData = XmlParser.parseXml(filePath);
            validateAndLoadData(xmlData);
        } catch (Exception e) {
            throw new GuessMarketException("Error loading XML file: " + e.getMessage());
        }
    }

    private void validateAndLoadData(GuessMarketData xmlData) {
        Map<Integer, Event> tempEvents = new HashMap<>();
        Map<String, User> tempUsers = new HashMap<>();

        // 1. Load Events
        for (EventData eData : xmlData.getEvents().getEvents()) {
            if (tempEvents.containsKey(eData.getId())) {
                throw new GuessMarketException("Duplicate event ID found: " + eData.getId());
            }

            Event event;
            List<String> options = eData.getOptions().getOptions();
            if (eData.getMethod().getOrderBook() != null) {
                OrderBookData obData = eData.getMethod().getOrderBook();
                event = new OrderBookEvent(eData.getId(), eData.getName(), eData.getDescription(),
                        eData.getCommission().getValue(), eData.getCommission().getType(),
                        options, obData.isAllowMint(), obData.getD(), obData.getInitial());
            } else {
                event = new LmsrEvent(eData.getId(), eData.getName(), eData.getDescription(),
                        eData.getCommission().getValue(), eData.getCommission().getType(),
                        options, eData.getMethod().getLmsr().getB());
            }
            tempEvents.put(event.getId(), event);
        }

        // 2. Load Users and Validate EX2 Constraints
        Map<Integer, String> eventToMM = new HashMap<>();

        for (UserData uData : xmlData.getUsers().getUsers()) {
            if (tempUsers.containsKey(uData.getName())) {
                throw new GuessMarketException("Duplicate username found: " + uData.getName());
            }
            if (uData.getInitialCash() <= 0) {
                throw new GuessMarketException("Initial balance must be greater than 0 for user: " + uData.getName());
            }

            Set<Integer> mmEvents = new HashSet<>();
            if (uData.getMarketMakerEvents() != null && uData.getMarketMakerEvents().getEvents() != null) {
                for (EventIdData eid : uData.getMarketMakerEvents().getEvents()) {
                    int eId = eid.getId();
                    if (!tempEvents.containsKey(eId)) {
                        throw new GuessMarketException("User " + uData.getName() + " is defined as Market Maker for a non-existent event ID: " + eId);
                    }
                    if (eventToMM.containsKey(eId)) {
                        throw new GuessMarketException("Event ID " + eId + " has more than one Market Maker defined.");
                    }
                    mmEvents.add(eId);
                    eventToMM.put(eId, uData.getName());
                }
            }
            tempUsers.put(uData.getName(), new User(uData.getName(), uData.getInitialCash(), mmEvents));
        }

        for (Integer eventId : tempEvents.keySet()) {
            if (!eventToMM.containsKey(eventId)) {
                throw new GuessMarketException("Event ID " + eventId + " is missing a Market Maker.");
            }
        }

        // Apply to actual system maps only if fully validated
        this.events.clear();
        this.events.putAll(tempEvents);
        this.users.clear();
        this.users.putAll(tempUsers);
    }

    @Override
    public List<EventDTO> getAllEvents() {
        return events.values().stream()
                .map(e -> new EventDTO(e.getId(), e.getName(),
                        e.isClosed() ? "Closed" : (e.isActive() ? "Active" : "Pending"),
                        e.isOrderBook() ? "Order Book" : "LMSR",
                        e.getCommission(), e.getCommissionType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllUsernames() {
        return new ArrayList<>(users.keySet());
    }

    @Override
    public double getUserBalance(String username) {
        return users.get(username).getBalance();
    }

    @Override
    public boolean isUserBlocked(String username) {
        return users.get(username).isBlocked();
    }

    @Override
    public List<UserEventSummaryDTO> getUserActiveEvents(String username) {
        // For now, this returns all events so the user can select them in the UI to trade or activate
        return events.values().stream()
                .map(e -> new UserEventSummaryDTO(e.getId(), e.getName(), 0.0, e.isClosed(), 0.0))
                .collect(Collectors.toList());
    }

    @Override
    public List<HoldingDTO> getUserHoldingsForEvent(String username, int eventId) {
        return new ArrayList<>();
    }

    @Override
    public void activateEvent(int eventId, String initiatorUsername) {
        Event event = events.get(eventId);
        User user = users.get(initiatorUsername);

        if (event.isActive() || event.isClosed()) {
            throw new GuessMarketException("The event is already active or closed.");
        }
        if (!user.isMMFor(eventId)) {
            throw new GuessMarketException("Only the Market Maker can activate this event.");
        }

        if (event.isOrderBook()) {
            OrderBookEvent obEvent = (OrderBookEvent) event;
            double cost = obEvent.getInitialInvestment();
            if (user.getBalance() < cost) {
                throw new GuessMarketException("Insufficient funds to activate the event. Required: " + cost);
            }
            user.updateBalance(-cost);
            user.addEventCashFlow(eventId, -cost);
            obEvent.addToAccountBalance(cost);
            int sharesToIssue = (int) (cost / obEvent.getBaseValueD());
            for (String opt : obEvent.getOptionNames()) {
                user.addHoldings(eventId, opt, sharesToIssue);
            }
        }
        event.setActive(true);
    }

    @Override
    public void closeEvent(int eventId, String initiatorUsername, String winningOption) {
        Event event = events.get(eventId);
        User mmUser = users.get(initiatorUsername);

        if (!event.isActive()) {
            throw new GuessMarketException("The event is not currently active.");
        }
        if (!mmUser.isMMFor(eventId)) {
            throw new GuessMarketException("Only the Market Maker can close this event.");
        }

        double totalPayouts = 0.0;
        double totalCommissions = 0.0;
        boolean isOnCloseCommission = event.getCommissionType().equals("on-close");

        // 1. Distribute funds to all users who hold the winning option
        for (User user : users.values()) {
            Map<String, Integer> holdings = user.getEventHoldings(eventId);
            if (holdings != null && holdings.containsKey(winningOption)) {
                int winningShares = holdings.get(winningOption);

                if (winningShares > 0) {
                    // Order book pays 'd' per share. LMSR pays 1 per share.
                    double baseValue = event.isOrderBook() ? ((OrderBookEvent) event).getBaseValueD() : 1.0;
                    double grossWinnings = winningShares * baseValue;

                    double commissionToPay = 0.0;
                    if (isOnCloseCommission) {
                        commissionToPay = grossWinnings * (event.getCommission() / 100.0);
                        totalCommissions += commissionToPay;
                        event.addCommission(commissionToPay); // Track for the UI
                    }

                    double netWinnings = grossWinnings - commissionToPay;
                    user.updateBalance(netWinnings);
                    totalPayouts += grossWinnings;
                }
            }
        }

        // 2. Give the collected commissions to the Market Maker
        if (totalCommissions > 0) {
            mmUser.updateBalance(totalCommissions);
        }

        // 3. For LMSR, return any leftover subsidy to the Market Maker
        if (!event.isOrderBook()) {
            double remainingBalance = event.getAccountBalance() - totalPayouts;
            if (remainingBalance > 0) {
                mmUser.updateBalance(remainingBalance);
            }
        }

        // 4. Close the event permanently
        event.setActive(false);
        event.setClosed(true);
        event.setWinningOption(winningOption);
        event.emptyAccount();
    }

    @Override
    public void placeOrderBookOrder(int eventId, String username, String option, int shares, double price, boolean isBuy) {
        User user = users.get(username);
        Event event = events.get(eventId);

        if (user.isBlocked()) {
            throw new GuessMarketException("This user is blocked due to a negative balance.");
        }
        if (!event.isActive()) {
            throw new GuessMarketException("Cannot place order. The event is not active.");
        }
        if (!event.isOrderBook()) {
            throw new GuessMarketException("Cannot place order. This is not an Order Book event.");
        }

        OrderBookEvent obEvent = (OrderBookEvent) event;
        if (price >= obEvent.getBaseValueD()) {
            throw new GuessMarketException("Order price must be lower than the event's base value (" + obEvent.getBaseValueD() + ").");
        }

        double totalCost = shares * price;
        double comm = 0.0;

        if (isBuy && event.getCommissionType().equals("on-purchase")) {
            comm = totalCost * (event.getCommission() / 100.0);
        }

        event.addCommission(comm);

        if (isBuy && user.getBalance() < (totalCost + comm)) {
            throw new GuessMarketException("Insufficient balance to place this order.");
        }

        Order order = new Order(username, isBuy, shares, price);
        obEvent.handleNewOrder(option, order, users, eventId);
    }

    // ==========================================
    // UI FETCH METHODS FOR EVENT DETAILS
    // ==========================================

    @Override
    public List<OrderDTO> getBuyOrders(int eventId) {
        Event event = events.get(eventId);
        if (event == null || !event.isOrderBook()) return new ArrayList<>();

        OrderBookEvent obEvent = (OrderBookEvent) event;
        List<OrderDTO> buyOrders = new ArrayList<>();

        for (String option : obEvent.getOptionNames()) {
            OrderBook book = obEvent.getOrderBook(option);
            for (Order order : book.getBids()) {
                // Pass the 'option' into the new DTO here
                buyOrders.add(new OrderDTO(order.getUserName(), option, order.getShares(), order.getPrice(), true));
            }
        }
        return buyOrders;
    }

    @Override
    public List<OrderDTO> getSellOrders(int eventId) {
        Event event = events.get(eventId);
        if (event == null || !event.isOrderBook()) return new ArrayList<>();

        OrderBookEvent obEvent = (OrderBookEvent) event;
        List<OrderDTO> sellOrders = new ArrayList<>();

        for (String option : obEvent.getOptionNames()) {
            OrderBook book = obEvent.getOrderBook(option);
            for (Order order : book.getAsks()) {
                // Pass the 'option' into the new DTO here
                sellOrders.add(new OrderDTO(order.getUserName(), option, order.getShares(), order.getPrice(), false));
            }
        }
        return sellOrders;
    }

    @Override
    public List<TradeDTO> getLmsrTradeHistory(int eventId) {
        Event event = events.get(eventId);
        if (event == null || event.isOrderBook()) return new ArrayList<>();

        LmsrEvent lmsrEvent = (LmsrEvent) event;

        // Maps your Trade record to the UI's TradeDTO[cite: 16]
        // NOTE: Requires adding getTrades() to LmsrEvent (See step 2)
        return lmsrEvent.getTrades().stream()
                .map(trade -> new TradeDTO(trade.optionName(), trade.shares(), trade.pricePaid()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OptionDTO> getLmsrOptionStatus(int eventId) {
        Event event = events.get(eventId);
        if (event == null || event.isOrderBook()) return new ArrayList<>();

        LmsrEvent lmsrEvent = (LmsrEvent) event;

        // NOTE: Requires adding getOptionShares() to LmsrEvent (See step 2)
        return lmsrEvent.getOptionShares().entrySet().stream()
                .map(entry -> new OptionDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public double getEventTotalCommission(int eventId) {
        Event event = events.get(eventId);
        // NOTE: Requires adding getTotalCommissionCollected() to Event (See step 2)
        return (event != null) ? event.getTotalCommissionCollected() : 0.0;
    }

    @Override
    public String getEventWinningOption(int eventId) {
        Event event = events.get(eventId);
        if (event == null || !event.isClosed()) return "N/A";

        // NOTE: Requires adding getWinningOption() to Event (See step 2)
        return event.getWinningOption();
    }

    @Override
    public void placeLmsrOrder(int eventId, String username, String option, int shares) {
        User user = users.get(username);
        Event event = events.get(eventId);

        if (user.isBlocked()) {
            throw new GuessMarketException("This user is blocked due to a negative balance.");
        }
        if (!event.isActive()) {
            throw new GuessMarketException("Cannot place order. The event is not active.");
        }
        if (event.isOrderBook()) {
            throw new GuessMarketException("Cannot place LMSR order. This is an Order Book event.");
        }

        LmsrEvent lmsrEvent = (LmsrEvent) event;
        Map<String, Integer> currentShares = lmsrEvent.getOptionShares();
        int b = lmsrEvent.getB();

        // 1. Calculate LMSR Cost using the Ex1 Formula
        double sumBefore = 0.0;
        double sumAfter = 0.0;

        // Initialize shares if they don't exist yet
        for (String optName : lmsrEvent.getOptionNames()) {
            currentShares.putIfAbsent(optName, 0);
        }

        // Calculate equations
        for (Map.Entry<String, Integer> entry : currentShares.entrySet()) {
            int currentQty = entry.getValue();
            int futureQty = currentQty + (entry.getKey().equals(option) ? shares : 0);

            sumBefore += Math.exp((double) currentQty / b);
            sumAfter += Math.exp((double) futureQty / b);
        }

        double costBefore = b * Math.log(sumBefore);
        double costAfter = b * Math.log(sumAfter);
        double totalCost = costAfter - costBefore;

        // 2. Calculate Commission
        double comm = 0.0;
        if (event.getCommissionType().equals("on-purchase")) {
            comm = totalCost * (event.getCommission() / 100.0);
        }

        // 3. Check Balance
        if (user.getBalance() < (totalCost + comm)) {
            throw new GuessMarketException("Insufficient balance to place this LMSR order. Required: " + String.format("%.2f", totalCost + comm));
        }

        // 4. Execute Trade
        event.addCommission(comm);
        user.recordTrade(eventId, option, shares, -totalCost, comm);
        lmsrEvent.addToAccountBalance(totalCost);

        // Update Event Data
        currentShares.put(option, currentShares.get(option) + shares);
        lmsrEvent.getTrades().add(new Trade(option, shares, totalCost));
    }

    @Override
    public List<String> getEventOptions(int eventId) {
        Event event = events.get(eventId);
        return (event != null) ? event.getOptionNames() : new ArrayList<>();
    }

    @Override
    public List<ParticipantDTO> getEventParticipants(int eventId) {
        List<ParticipantDTO> list = new ArrayList<>();

        for (User user : users.values()) {
            Map<String, Integer> userHoldings = user.getEventHoldings(eventId);

            // Only include users who actually participated in this event
            if (!userHoldings.isEmpty()) {
                int totalQty = userHoldings.values().stream().mapToInt(Integer::intValue).sum();
                // Cash flow is negative when spending money, so we use absolute value for "Value"
                double totalValue = Math.abs(user.getEventCashFlow(eventId));

                list.add(new ParticipantDTO(user.getName(), totalQty, totalValue));
            }
        }
        return list;
    }

    @Override
    public double getEventAccountBalance(int eventId) {
        Event event = events.get(eventId);
        return (event != null) ? event.getAccountBalance() : 0.0;
    }

    @Override
    public double getLastTradePrice(int eventId, String option) {
        Event event = events.get(eventId);
        if (event != null && event.isOrderBook()) {
            return ((OrderBookEvent) event).getLastPrice(option);
        }
        return 0.0;
    }
}