package engine;

import dto.EventDTO;
import dto.EventDetailsDTO;
import dto.OptionDTO;
import dto.TradeDTO;
import engine.models.Event;
import engine.models.Trade;
import engine.xml.EventData;
import engine.xml.GuessMarketData;
import exception.GuessMarketException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GuessMarketEngineImpl implements IGuessMarketEngine {

    private List<Event> events = new ArrayList<>();

    @Override
    public void loadXml(String filePath) {
        GuessMarketData jaxbData = XmlParser.parse(filePath);

        List<Event> newEvents = new ArrayList<>();
        Set<Integer> existingIds = new HashSet<>();

        for (EventData data : jaxbData.getEvents()) {
            int id = data.getId();

            if (!existingIds.add(id)) {
                throw new GuessMarketException("Invalid XML: Duplicate event ID found: " + id);
            }

            int commissionVal = data.getCommission().getValue();

            if (commissionVal < 0 || commissionVal > 90) {
                throw new GuessMarketException("Invalid XML: Commission must be between 0 and 90. Found: " + commissionVal);
            }

            String name = data.getName();
            String description = data.getDescription();
            String commissionType = data.getCommission().getType();
            List<String> options = data.getOptions();
            int b = data.getMethod().getLmsr().getB();

            newEvents.add(new Event(id, name, description, commissionVal, commissionType, options, b));
        }

        this.events = newEvents;

        for (Event event : events) {
            double initialSubsidy = LMSRMath.calculateCost(0, 0, event.getB());
            event.addFunds(initialSubsidy);
        }
    }

    @Override
    public List<EventDTO> getAllEvents() {
        return events.stream().map(this::mapToEventDTO).collect(Collectors.toList());
    }

    @Override
    public EventDetailsDTO getEventDetails(int eventId) {
        Event e = getEventInternal(eventId);

        List<Double> currentPrices = new ArrayList<>();
        currentPrices.add(LMSRMath.calculateProbability(e.getSharesBought(0), e.getSharesBought(1), e.getB()));
        currentPrices.add(LMSRMath.calculateProbability(e.getSharesBought(1), e.getSharesBought(0), e.getB()));

        List<TradeDTO> tradeDTOs = e.getTradeHistory().stream()
                .map(t -> new TradeDTO(t.optionName(), t.shares(), t.pricePaid()))
                .collect(Collectors.toList());

        return new EventDetailsDTO(mapToEventDTO(e), e.getAccountBalance(), e.getTotalCommission(), tradeDTOs, currentPrices);
    }

    @Override
    public double buyShares(int eventId, int optionIndex, int amount) {
        Event event = getEventInternal(eventId);
        if (!event.isActive()) throw new GuessMarketException("Event is closed.");
        if (amount <= 0) throw new GuessMarketException("Amount must be positive.");

        int otherIndex = 1 - optionIndex;
        double costBefore = LMSRMath.calculateCost(event.getSharesBought(optionIndex), event.getSharesBought(otherIndex), event.getB());
        double costAfter = LMSRMath.calculateCost(event.getSharesBought(optionIndex) + amount, event.getSharesBought(otherIndex), event.getB());
        double rawPrice = costAfter - costBefore;

        double totalPaid = rawPrice;
        if (event.getCommissionType().equals("on-purchase")) {
            double commission = rawPrice * (event.getCommission() / 100.0);
            totalPaid += commission;
            event.addCommission(commission);
        }

        event.addFunds(rawPrice);
        event.addShares(optionIndex, amount);
        event.recordTrade(new Trade(event.getOptionNames().get(optionIndex), amount, totalPaid));

        return totalPaid;
    }

    @Override
    public void closeEvent(int eventId, int winningOptionIndex) {
        Event event = getEventInternal(eventId);
        if (!event.isActive()) throw new GuessMarketException("Event is already closed.");

        event.setActive(false);
        event.setWinningOptionIndex(winningOptionIndex);

        double payout = event.getSharesBought(winningOptionIndex);

        if (event.getCommissionType().equals("on-close")) {
            double commission = payout * (event.getCommission() / 100.0);
            event.addCommission(commission);
            payout -= commission;
        }

        event.deductFunds(payout);
    }

    @Override
    public void saveSystemState(String pathWithoutExtension) {
        String fullPath = pathWithoutExtension + "Savedata.dat";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fullPath))) {
            oos.writeObject(this.events);
        } catch (IOException e) {
            throw new GuessMarketException("Failed to save system state: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadSystemState(String pathWithoutExtension) {
        String fullPath = pathWithoutExtension + "Savedata.dat";
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fullPath))) {
            this.events = (List<Event>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new GuessMarketException("Failed to load system state: " + e.getMessage());
        }
    }

    // מתודות עזר פרטיות
    private Event getEventInternal(int eventId) {
        return events.stream().filter(e -> e.getId() == eventId).findFirst()
                .orElseThrow(() -> new GuessMarketException("Event ID " + eventId + " not found."));
    }

    private EventDTO mapToEventDTO(Event e) {
        List<OptionDTO> optDTOs = new ArrayList<>();
        for (int i = 0; i < e.getOptionNames().size(); i++) {
            optDTOs.add(new OptionDTO(e.getOptionNames().get(i), e.getSharesBought(i)));
        }

        return new EventDTO(e.getId(), e.getName(), e.getDescription(), e.getCommission(), e.getCommissionType(), e.isActive(), optDTOs, e.getWinningOptionIndex());
    }
}