package engine;

import dto.EventDTO;
import dto.EventDetailsDTO;
import dto.HoldingDTO;
import dto.OptionStatusDTO;
import dto.OrderBookDTO;
import dto.OrderDTO;
import dto.ParticipantDTO;
import dto.TradeDTO;
import dto.TransactionDTO;
import dto.UserDetailsDTO;
import dto.UserEventDetailsDTO;
import dto.UserEventSummaryDTO;
import dto.UserSummaryDTO;
import engine.models.AccountTransaction;
import engine.models.Event;
import engine.models.LmsrEvent;
import engine.models.LmsrTrade;
import engine.models.Order;
import engine.models.OrderBook;
import engine.models.OrderBookEvent;
import engine.models.User;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Translates the engine's internal objects into flat, logic-free DTOs
 * (the only objects that leave the engine and travel to the clients as JSON).
 */
final class DtoFactory {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String ROLE_MARKET_MAKER = "Market Maker";
    private static final String ROLE_PARTICIPANT = "Participant";
    private static final String ROLE_NONE = "-";

    private DtoFactory() {
    }

    static EventDTO toEventDTO(Event event) {
        return new EventDTO(event.getName(), event.getDescription(), event.getStatus().getDisplayName(),
                event.getTypeName(), event.getCommission(), event.getCommissionType(),
                event.getAccountBalance(), event.getMarketMaker(), event.getOptions());
    }

    static UserSummaryDTO toUserSummaryDTO(User user) {
        return new UserSummaryDTO(user.getName(), user.getBalance(), user.isMarketMaker(), user.isOnline());
    }

    // ----- Event details -----

    static EventDetailsDTO toEventDetailsDTO(Event event, Map<String, User> users) {
        List<OptionStatusDTO> lmsrOptions = List.of();
        List<TradeDTO> lmsrTrades = List.of();
        List<OrderBookDTO> orderBooks = List.of();

        if (event instanceof LmsrEvent lmsr) {
            lmsrOptions = toOptionStatuses(lmsr);
            lmsrTrades = lmsr.getTradesLatestFirst().stream().map(DtoFactory::toTradeDTO).toList();
        } else if (event instanceof OrderBookEvent ob) {
            orderBooks = ob.getOrderBooks().stream().map(DtoFactory::toOrderBookDTO).toList();
        }

        return new EventDetailsDTO(toEventDTO(event), event.getTotalCommissionCollected(), event.getWinningOption(),
                lmsrOptions, lmsrTrades, orderBooks, toParticipants(event, users));
    }

    private static List<OptionStatusDTO> toOptionStatuses(LmsrEvent lmsr) {
        return lmsr.getOptions().stream()
                .map(option -> new OptionStatusDTO(option, lmsr.getPrice(option), lmsr.getSharesBought(option)))
                .toList();
    }

    private static TradeDTO toTradeDTO(LmsrTrade trade) {
        return new TradeDTO(trade.userName(), trade.option(), trade.shares(), trade.pricePaid(), trade.commission());
    }

    private static OrderBookDTO toOrderBookDTO(OrderBook book) {
        return new OrderBookDTO(book.getOption(), toOrderDTOs(book.getBids()), toOrderDTOs(book.getAsks()),
                book.getLastTradePrice(), book.getHighestBid(), book.getLowestAsk(),
                book.getMidPrice(), book.getSpread());
    }

    private static List<OrderDTO> toOrderDTOs(Collection<Order> orders) {
        return orders.stream()
                .map(o -> new OrderDTO(o.getUserName(), o.getOption(), o.getShares(), o.getPrice(), o.isBuy()))
                .toList();
    }

    private static List<ParticipantDTO> toParticipants(Event event, Map<String, User> users) {
        List<ParticipantDTO> result = new ArrayList<>();
        for (String userName : event.getParticipants()) {
            User user = users.get(userName.toLowerCase(Locale.ROOT));
            List<HoldingDTO> holdings = toHoldings(event, user);
            int openOrders = event instanceof OrderBookEvent ob ? ob.getOpenOrdersOf(user.getName()).size() : 0;
            double totalValue = holdings.stream().mapToDouble(HoldingDTO::value).sum();
            result.add(new ParticipantDTO(user.getName(), holdings, openOrders, totalValue));
        }
        return result;
    }

    private static List<HoldingDTO> toHoldings(Event event, User user) {
        List<HoldingDTO> holdings = new ArrayList<>();
        for (String option : event.getOptions()) {
            int shares = user.getShares(event.getName(), option);
            double paid = user.getAmountPaid(event.getName(), option);
            holdings.add(new HoldingDTO(option, shares, paid, shares * currentShareValue(event, option)));
        }
        return holdings;
    }

    private static double currentShareValue(Event event, String option) {
        if (event.isEnded()) {
            return option.equals(event.getWinningOption()) ? event.getPayoutPerShare() : 0.0;
        }
        if (event instanceof LmsrEvent lmsr) {
            return lmsr.getPrice(option);
        }
        return ((OrderBookEvent) event).getCurrentShareValue(option);
    }

    // ----- User details -----

    static UserDetailsDTO toUserDetailsDTO(User user, Collection<Event> events) {
        List<UserEventSummaryDTO> userEvents = events.stream()
                .map(e -> new UserEventSummaryDTO(e.getName(), e.getTypeName(), e.getStatus().getDisplayName(),
                        roleOf(user, e)))
                .toList();
        List<TransactionDTO> transactions = user.getTransactions().stream().map(DtoFactory::toTransactionDTO).toList();
        return new UserDetailsDTO(user.getName(), user.getBalance(), user.isBlocked(), userEvents, transactions);
    }

    private static String roleOf(User user, Event event) {
        if (user.isMarketMakerOf(event.getName())) {
            return ROLE_MARKET_MAKER;
        }
        return event.getParticipants().contains(user.getName()) ? ROLE_PARTICIPANT : ROLE_NONE;
    }

    private static TransactionDTO toTransactionDTO(AccountTransaction t) {
        return new TransactionDTO(t.number(), t.time().format(TIME_FORMAT), t.type(), t.description(),
                t.amount(), t.balanceBefore(), t.balanceAfter());
    }

    static UserEventDetailsDTO toUserEventDetailsDTO(User user, Event event) {
        String eventName = event.getName();
        List<TradeDTO> trades = List.of();
        List<OrderDTO> openOrders = List.of();
        List<OptionStatusDTO> totals = List.of();

        if (event instanceof LmsrEvent lmsr) {
            trades = lmsr.getTradesLatestFirst().stream()
                    .filter(t -> t.userName().equals(user.getName()))
                    .map(DtoFactory::toTradeDTO).toList();
            totals = toOptionStatuses(lmsr);
        } else if (event instanceof OrderBookEvent ob) {
            openOrders = toOrderDTOs(ob.getOpenOrdersOf(user.getName()));
        }

        boolean involved = event.getParticipants().contains(user.getName()) || user.isMarketMakerOf(eventName);
        Double profitLoss = event.isEnded() && involved ? user.getEventCashFlow(eventName) : null;

        return new UserEventDetailsDTO(eventName, event.getTypeName(), event.getStatus().getDisplayName(),
                user.isMarketMakerOf(eventName), event.getOptions(), toHoldings(event, user), trades, openOrders,
                user.getCommissionPaid(eventName), profitLoss, event.getWinningOption(), totals);
    }
}
