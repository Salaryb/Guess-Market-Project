package engine;

import dto.EventDTO;
import dto.EventDetailsDTO;
import dto.UserDetailsDTO;
import dto.UserEventDetailsDTO;
import dto.UserSummaryDTO;
import engine.models.Event;
import engine.models.EventStatus;
import engine.models.Fill;
import engine.models.LmsrEvent;
import engine.models.LmsrTrade;
import engine.models.Order;
import engine.models.OrderBookEvent;
import engine.models.User;
import engine.xml.XmlParser;
import exception.GuessMarketException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class GuessMarketEngineImpl implements IGuessMarketEngine {
    private static final double EPSILON = 1e-9;
    private static final double MIN_PRICE = 0.01;
    private static final int MAX_USER_NAME_LENGTH = 30;
    private static final Pattern USER_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 _.\\-]*");

    /** Keys are lower-cased names, so every lookup is case insensitive. Insertion order is kept for display. */
    private final Map<String, Event> events = new LinkedHashMap<>();
    private final Map<String, User> users = new LinkedHashMap<>();
    private long version = 0;

    // =====================================================================
    // Users
    // =====================================================================

    @Override
    public String loginUser(String userName) {
        String name = validateUserName(userName);
        User existing = users.get(key(name));
        if (existing != null && existing.isOnline()) {
            throw new GuessMarketException("The user name '" + existing.getName()
                    + "' is already taken by a logged in user. Please choose a different name.");
        }
        User user = existing != null ? existing : new User(name);
        user.setOnline(true);
        users.put(key(name), user);
        version++;
        return user.getName();
    }

    private String validateUserName(String userName) {
        String name = userName == null ? "" : userName.trim();
        if (name.isEmpty()) {
            throw new GuessMarketException("Please enter a user name.");
        }
        if (name.length() > MAX_USER_NAME_LENGTH) {
            throw new GuessMarketException("The user name is too long (max " + MAX_USER_NAME_LENGTH + " characters).");
        }
        if (!USER_NAME_PATTERN.matcher(name).matches()) {
            throw new GuessMarketException("The user name may contain only English letters, digits, spaces, '_', '.' or '-', "
                    + "and must start with a letter or a digit.");
        }
        return name;
    }

    @Override
    public void logoutUser(String userName) {
        User user = users.get(key(userName));
        if (user != null) {
            user.setOnline(false);
            version++;
        }
    }

    @Override
    public boolean isUserExists(String userName) {
        return userName != null && users.containsKey(key(userName));
    }

    @Override
    public void deposit(String userName, double amount) {
        User user = getUser(userName);
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new GuessMarketException("The deposit amount must be a positive number.");
        }
        user.changeBalance(amount, "Deposit", "Money loaded to the account", null);
        version++;
    }

    @Override
    public List<UserSummaryDTO> getAllUsers() {
        return users.values().stream().map(DtoFactory::toUserSummaryDTO).toList();
    }

    @Override
    public UserDetailsDTO getUserDetails(String userName) {
        return DtoFactory.toUserDetailsDTO(getUser(userName), events.values());
    }

    @Override
    public UserEventDetailsDTO getUserEventDetails(String userName, String eventName) {
        return DtoFactory.toUserEventDetailsDTO(getUser(userName), getEvent(eventName));
    }

    // =====================================================================
    // Events
    // =====================================================================

    @Override
    public List<String> loadEventsFromXml(InputStream xmlContent, String uploaderName) {
        User uploader = getUser(uploaderName);
        List<Event> newEvents = EventLoader.createEvents(XmlParser.parse(xmlContent), events.keySet(), uploader.getName());

        List<String> addedNames = new ArrayList<>();
        for (Event event : newEvents) {
            events.put(key(event.getName()), event);
            uploader.addMarketMakerEvent(event.getName());
            addedNames.add(event.getName());
        }
        version++;
        return addedNames;
    }

    @Override
    public List<EventDTO> getAllEvents() {
        return events.values().stream().map(DtoFactory::toEventDTO).toList();
    }

    @Override
    public EventDetailsDTO getEventDetails(String eventName) {
        return DtoFactory.toEventDetailsDTO(getEvent(eventName), users);
    }

    @Override
    public String activateEvent(String eventName, String userName) {
        Event event = getEvent(eventName);
        User marketMaker = getUser(userName);
        requireMarketMaker(event, marketMaker, "activate");
        if (event.getStatus() != EventStatus.NOT_STARTED) {
            throw new GuessMarketException("Event '" + event.getName() + "' is already "
                    + event.getStatus().getDisplayName().toLowerCase(Locale.ROOT) + " and cannot be activated again.");
        }

        double cost = event.getActivationCost();
        if (marketMaker.getBalance() + EPSILON < cost) {
            throw new GuessMarketException(String.format(
                    "Not enough money to activate '%s': it requires %.2f but your balance is %.2f. Please deposit money first.",
                    event.getName(), cost, marketMaker.getBalance()));
        }

        marketMaker.changeBalance(-cost, "Event Activation",
                (event instanceof LmsrEvent ? "LMSR subsidy for '" : "Initial shares for '") + event.getName() + "'",
                event.getName());
        event.addToAccount(cost);
        if (event instanceof OrderBookEvent ob) {
            issueInitialShares(ob, marketMaker, cost);
        }
        event.activate();
        version++;
        return String.format("Event '%s' is now active. %.2f were moved from your account to the event account.",
                event.getName(), cost);
    }

    private void issueInitialShares(OrderBookEvent event, User marketMaker, double cost) {
        int sharesPerOption = event.getInitialSharesPerOption();
        if (sharesPerOption == 0) {
            return;
        }
        double paidPerOption = cost / event.getOptions().size();
        for (String option : event.getOptions()) {
            marketMaker.addShares(event.getName(), option, sharesPerOption, paidPerOption);
        }
        event.addParticipant(marketMaker.getName());
    }

    @Override
    public String closeEvent(String eventName, String userName, String winningOption) {
        Event event = getEvent(eventName);
        User marketMaker = getUser(userName);
        requireMarketMaker(event, marketMaker, "close");
        if (!event.isActive()) {
            throw new GuessMarketException("Event '" + event.getName() + "' is not active ("
                    + event.getStatus().getDisplayName() + "), so it cannot be closed.");
        }
        String winner = event.resolveOption(winningOption);

        if (event instanceof OrderBookEvent ob) {
            ob.cancelAllOrders();
        }
        double totalPaid = payWinners(event, winner, marketMaker);
        double leftover = returnLeftoverToMarketMaker(event, marketMaker);
        event.end(winner);
        version++;

        return String.format("Event '%s' was closed. Winning option: '%s'. Paid %.2f to the winners, "
                        + "%.2f were returned to the market maker. Total commission collected: %.2f.",
                event.getName(), winner, totalPaid, leftover, event.getTotalCommissionCollected());
    }

    /** Pays every holder of the winning option, minus the on-close commission (if relevant). */
    private double payWinners(Event event, String winner, User marketMaker) {
        double totalPaid = 0.0;
        for (String participantName : event.getParticipants()) {
            User user = users.get(key(participantName));
            int shares = user.getShares(event.getName(), winner);
            if (shares <= 0) {
                continue;
            }
            double gross = shares * event.getPayoutPerShare();
            event.addToAccount(-gross);
            user.changeBalance(gross, "Event Payout",
                    String.format("%d winning '%s' shares in '%s'", shares, winner, event.getName()), event.getName());
            totalPaid += gross;
            if (event.isOnCloseCommission()) {
                chargeCommission(event, user, marketMaker, event.calculateCommission(gross));
            }
        }
        return totalPaid;
    }

    private double returnLeftoverToMarketMaker(Event event, User marketMaker) {
        double leftover = event.getAccountBalance();
        if (leftover > EPSILON) {
            marketMaker.changeBalance(leftover, "Event Account Return",
                    "Money left in the account of '" + event.getName() + "'", event.getName());
            event.addToAccount(-leftover);
            return leftover;
        }
        return 0.0;
    }

    // =====================================================================
    // Trading
    // =====================================================================

    @Override
    public String buyLmsrShares(String eventName, String userName, String option, int shares) {
        Event event = getEvent(eventName);
        User user = getUser(userName);
        if (!(event instanceof LmsrEvent lmsr)) {
            throw new GuessMarketException("Event '" + event.getName() + "' is an Order Book event - please place an order instead.");
        }
        requireCanTrade(event, user);
        requirePositiveShares(shares);
        String chosenOption = event.resolveOption(option);

        double cost = lmsr.calculatePurchaseCost(chosenOption, shares);
        if (Double.isNaN(cost) || Double.isInfinite(cost)) {
            throw new GuessMarketException("The requested amount of shares is too large for this event.");
        }
        double commission = event.isOnPurchaseCommission() ? event.calculateCommission(cost) : 0.0;
        requireAvailableMoney(user, cost + commission);

        user.changeBalance(-cost, "LMSR Purchase",
                String.format("Bought %d '%s' shares in '%s'", shares, chosenOption, event.getName()), event.getName());
        user.addShares(event.getName(), chosenOption, shares, cost);
        event.addToAccount(cost);
        if (commission > 0) {
            chargeCommission(event, user, getUser(event.getMarketMaker()), commission);
        }
        lmsr.recordPurchase(new LmsrTrade(user.getName(), chosenOption, shares, cost, commission));
        event.addParticipant(user.getName());
        version++;

        return String.format("Bought %d shares of '%s' for %.2f (%.2f per share) + commission %.2f = total %.2f. "
                        + "The new price of '%s' is %.2f.",
                shares, chosenOption, cost, cost / shares, commission, cost + commission,
                chosenOption, lmsr.getPrice(chosenOption));
    }

    @Override
    public String placeOrder(String eventName, String userName, String option, boolean buy, int shares, double price) {
        Event event = getEvent(eventName);
        User user = getUser(userName);
        if (!(event instanceof OrderBookEvent ob)) {
            throw new GuessMarketException("Event '" + event.getName() + "' is an LMSR event - please buy shares instead.");
        }
        requireCanTrade(event, user);
        requirePositiveShares(shares);
        validatePrice(ob, price);
        String chosenOption = event.resolveOption(option);

        if (buy) {
            double amount = shares * price;
            double commission = event.isOnPurchaseCommission() ? event.calculateCommission(amount) : 0.0;
            requireAvailableMoney(user, amount + commission);
        } else {
            requireSharesToSell(ob, user, chosenOption, shares);
        }

        Order order = ob.createOrder(user.getName(), chosenOption, buy, shares, price);
        List<Fill> fills = ob.submitOrder(order);
        event.addParticipant(user.getName());
        fills.forEach(fill -> settleFill(ob, fill));
        version++;

        return describeOrderResult(shares, order.getShares(), fills);
    }

    private void validatePrice(OrderBookEvent event, double price) {
        if (Double.isNaN(price) || price < MIN_PRICE - EPSILON || price > event.getMaxPrice() + EPSILON) {
            throw new GuessMarketException(String.format("The price per share must be between %.2f and %.2f (d - 0.01).",
                    MIN_PRICE, event.getMaxPrice()));
        }
        if (Math.abs(price * 100 - Math.round(price * 100)) > 1e-6) {
            throw new GuessMarketException("The price can have at most 2 digits after the decimal point.");
        }
    }

    private void requireSharesToSell(OrderBookEvent event, User user, String option, int shares) {
        int owned = user.getShares(event.getName(), option);
        int alreadyOffered = event.getPendingSellShares(user.getName(), option);
        if (owned - alreadyOffered < shares) {
            throw new GuessMarketException(String.format(
                    "You cannot sell %d '%s' shares: you hold %d, and %d of them are already offered for sale.",
                    shares, option, owned, alreadyOffered));
        }
    }

    /** Moves money and shares between the users (and the event account) for a single execution. */
    private void settleFill(OrderBookEvent event, Fill fill) {
        User buyer = getUser(fill.buyer());
        User counterparty = getUser(fill.counterparty());
        User marketMaker = getUser(event.getMarketMaker());
        String eventName = event.getName();
        double buyerAmount = fill.shares() * fill.buyerPrice();

        buyer.changeBalance(-buyerAmount, "Order Book Buy", String.format("Bought %d '%s' shares @ %.2f in '%s'",
                fill.shares(), fill.option(), fill.buyerPrice(), eventName), eventName);
        buyer.addShares(eventName, fill.option(), fill.shares(), buyerAmount);
        chargePurchaseCommission(event, buyer, marketMaker, buyerAmount);

        if (fill.type() == Fill.FillType.TRADE) {
            counterparty.changeBalance(buyerAmount, "Order Book Sell", String.format("Sold %d '%s' shares @ %.2f in '%s'",
                    fill.shares(), fill.option(), fill.buyerPrice(), eventName), eventName);
            counterparty.removeShares(eventName, fill.option(), fill.shares());
        } else {
            double otherAmount = fill.shares() * fill.otherPrice();
            counterparty.changeBalance(-otherAmount, "Order Book Buy (Mint)", String.format(
                    "Bought %d new '%s' shares @ %.2f in '%s'", fill.shares(), fill.otherOption(), fill.otherPrice(), eventName),
                    eventName);
            counterparty.addShares(eventName, fill.otherOption(), fill.shares(), otherAmount);
            chargePurchaseCommission(event, counterparty, marketMaker, otherAmount);
            event.addToAccount(buyerAmount + otherAmount);
        }
    }

    private void chargePurchaseCommission(Event event, User payer, User marketMaker, double purchaseAmount) {
        if (event.isOnPurchaseCommission()) {
            chargeCommission(event, payer, marketMaker, event.calculateCommission(purchaseAmount));
        }
    }

    /** Moves a commission from the payer to the event's market maker, recording it on both sides. */
    private void chargeCommission(Event event, User payer, User marketMaker, double amount) {
        if (amount <= 0) {
            return;
        }
        payer.changeBalance(-amount, "Commission Paid",
                String.format("%d%% commission in '%s'", event.getCommission(), event.getName()), event.getName());
        payer.addCommissionPaid(event.getName(), amount);
        marketMaker.changeBalance(amount, "Commission Received",
                String.format("From %s in '%s'", payer.getName(), event.getName()), event.getName());
        event.addCommission(amount);
    }

    private String describeOrderResult(int requested, int resting, List<Fill> fills) {
        int traded = fills.stream().filter(f -> f.type() == Fill.FillType.TRADE).mapToInt(Fill::shares).sum();
        int minted = fills.stream().filter(f -> f.type() == Fill.FillType.MINT).mapToInt(Fill::shares).sum();
        StringBuilder message = new StringBuilder("Order for " + requested + " shares received.");
        if (traded > 0) {
            message.append(' ').append(traded).append(" shares were traded.");
        }
        if (minted > 0) {
            message.append(' ').append(minted).append(" new shares were minted.");
        }
        if (resting > 0) {
            message.append(' ').append(resting).append(" shares are waiting in the order book.");
        }
        return message.toString();
    }

    // =====================================================================
    // Validation helpers
    // =====================================================================

    private Event getEvent(String eventName) {
        Event event = eventName == null ? null : events.get(key(eventName));
        if (event == null) {
            throw new GuessMarketException("The event '" + eventName + "' does not exist.");
        }
        return event;
    }

    private User getUser(String userName) {
        User user = userName == null ? null : users.get(key(userName));
        if (user == null) {
            throw new GuessMarketException("The user '" + userName + "' does not exist.");
        }
        return user;
    }

    private void requireMarketMaker(Event event, User user, String action) {
        if (!user.isMarketMakerOf(event.getName())) {
            throw new GuessMarketException("Only the market maker of '" + event.getName() + "' ("
                    + event.getMarketMaker() + ") can " + action + " it.");
        }
    }

    private void requireCanTrade(Event event, User user) {
        if (user.isBlocked()) {
            throw new GuessMarketException("Your account is blocked because its balance is negative. Please deposit money.");
        }
        if (!event.isActive()) {
            throw new GuessMarketException("Event '" + event.getName() + "' is not active ("
                    + event.getStatus().getDisplayName() + "), so trading is not possible.");
        }
    }

    private void requirePositiveShares(int shares) {
        if (shares <= 0) {
            throw new GuessMarketException("The number of shares must be a positive whole number.");
        }
    }

    /** Money the user can spend: his balance minus what is already committed in waiting buy orders. */
    private void requireAvailableMoney(User user, double required) {
        double committed = events.values().stream()
                .filter(e -> e instanceof OrderBookEvent)
                .mapToDouble(e -> ((OrderBookEvent) e).getPendingBuyAmount(user.getName()))
                .sum();
        double available = user.getBalance() - committed;
        if (available + EPSILON < required) {
            throw new GuessMarketException(String.format(
                    "Not enough money: this action requires %.2f but you have %.2f available "
                            + "(balance %.2f, %.2f reserved for your waiting buy orders). Please deposit money.",
                    required, available, user.getBalance(), committed));
        }
    }

    @Override
    public long getVersion() {
        return version;
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
