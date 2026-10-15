package engine;

import engine.models.Event;
import engine.models.LmsrEvent;
import engine.models.OrderBookEvent;
import engine.xml.EventData;
import engine.xml.GuessMarketData;
import engine.xml.MethodData;
import engine.xml.OrderBookData;
import exception.GuessMarketException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Converts a parsed XML into events, after validating it application-wise
 * (the schema validation itself is done by the XmlParser).
 */
final class EventLoader {
    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int MIN_OPTIONS = 2;

    private EventLoader() {
    }

    /**
     * @param existingEventKeys lower-cased names of events already in the system
     * @return the new events - only if the whole file is valid
     */
    static List<Event> createEvents(GuessMarketData data, Set<String> existingEventKeys, String marketMaker) {
        if (data.getEvents() == null || data.getEvents().getEvents() == null || data.getEvents().getEvents().isEmpty()) {
            throw new GuessMarketException("The file does not contain any event.");
        }

        Set<String> namesInFile = new HashSet<>();
        List<Event> events = new ArrayList<>();
        for (EventData eventData : data.getEvents().getEvents()) {
            String name = trim(eventData.getName());
            validateName(name, namesInFile, existingEventKeys);
            namesInFile.add(name.toLowerCase(Locale.ROOT));
            events.add(createEvent(eventData, name, marketMaker));
        }
        return events;
    }

    private static void validateName(String name, Set<String> namesInFile, Set<String> existingEventKeys) {
        if (name.isEmpty()) {
            throw new GuessMarketException("An event in the file has an empty name.");
        }
        String key = name.toLowerCase(Locale.ROOT);
        if (namesInFile.contains(key)) {
            throw new GuessMarketException("The event name '" + name + "' appears more than once in the file.");
        }
        if (existingEventKeys.contains(key)) {
            throw new GuessMarketException("An event named '" + name + "' already exists in the system "
                    + "(event names must be unique).");
        }
    }

    private static Event createEvent(EventData data, String name, String marketMaker) {
        int commission = data.getCommission().getValue();
        if (commission < MIN_COMMISSION || commission > MAX_COMMISSION) {
            throw new GuessMarketException("Event '" + name + "': the commission is " + commission
                    + "%, but it must be between " + MIN_COMMISSION + " and " + MAX_COMMISSION + ".");
        }
        String commissionType = trim(data.getCommission().getType());
        String description = trim(data.getDescription());
        List<String> options = readOptions(data, name);
        MethodData method = data.getMethod();

        if (method.getOrderBook() != null) {
            OrderBookData ob = method.getOrderBook();
            validateOrderBook(ob, name);
            return new OrderBookEvent(name, description, commission, commissionType, options, marketMaker,
                    ob.isAllowMint(), ob.getD(), ob.getInitial());
        }

        int b = method.getLmsr().getB();
        if (b <= 0) {
            throw new GuessMarketException("Event '" + name + "': the LMSR liquidity value b must be a positive number (got " + b + ").");
        }
        return new LmsrEvent(name, description, commission, commissionType, options, marketMaker, b);
    }

    private static List<String> readOptions(EventData data, String eventName) {
        List<String> options = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (String raw : data.getOptions().getOptions()) {
            String option = trim(raw);
            if (option.isEmpty()) {
                throw new GuessMarketException("Event '" + eventName + "' has an option with an empty name.");
            }
            if (!keys.add(option.toLowerCase(Locale.ROOT))) {
                throw new GuessMarketException("Event '" + eventName + "' has the option '" + option + "' more than once.");
            }
            options.add(option);
        }
        if (options.size() < MIN_OPTIONS) {
            throw new GuessMarketException("Event '" + eventName + "' must have at least " + MIN_OPTIONS + " options.");
        }
        return options;
    }

    private static void validateOrderBook(OrderBookData ob, String eventName) {
        if (ob.getD() <= 0) {
            throw new GuessMarketException("Event '" + eventName + "': the base value d must be a positive number (got " + ob.getD() + ").");
        }
        if (ob.getInitial() < 0) {
            throw new GuessMarketException("Event '" + eventName + "': the initial investment cannot be negative (got " + ob.getInitial() + ").");
        }
    }

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }
}
