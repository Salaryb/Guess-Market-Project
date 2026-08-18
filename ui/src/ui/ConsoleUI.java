package ui;

import dto.EventDTO;
import dto.EventDetailsDTO;
import dto.OptionDTO;
import dto.TradeDTO;
import engine.IGuessMarketEngine;
import exception.GuessMarketException;

import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final IGuessMarketEngine engine;
    private final Scanner scanner;

    public ConsoleUI(IGuessMarketEngine engine) {
        this.engine = engine;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- Guess Market ---");
            System.out.println("1. Load XML File");
            System.out.println("2. Show All Events");
            System.out.println("3. Show Event Status");
            System.out.println("4. Buy Shares");
            System.out.println("5. Close Event");
            System.out.println("6. Save System State");
            System.out.println("7. Load System State");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                switch (choice) {
                    case 1 -> loadXml();
                    case 2 -> showAllEvents();
                    case 3 -> showEventStatus();
                    case 4 -> buyShares();
                    case 5 -> closeEvent();
                    case 6 -> saveState();
                    case 7 -> loadState();
                    case 8 -> running = false;
                    default -> System.out.println("Invalid option. Choose 1-6.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            } catch (GuessMarketException e) {
                System.out.println("Logic Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected Error: " + e.getMessage());
            }
        }
    }

    private void loadXml() {
        System.out.print("Enter full path to XML file: ");
        String path = scanner.nextLine().trim();
        if (!path.toLowerCase().endsWith(".xml")) {
            System.out.println("File must end with .xml");
            return;
        }
        engine.loadXml(path);
        System.out.println("File loaded successfully!");
    }

    private void showAllEvents() {
        List<EventDTO> events = engine.getAllEvents();
        if (events.isEmpty()) {
            System.out.println("No events loaded.");
            return;
        }

        System.out.println("\n--- All Loaded Events ---");
        for (EventDTO e : events) {
            System.out.printf("Event ID: %d\n", e.id());
            System.out.printf("Name: %s\n", e.name());
            System.out.printf("Description: %s\n", e.description());
            System.out.printf("Commission: %d%% (%s)\n", e.commission(), e.commissionType());
            System.out.printf("Status: %s\n", e.isActive() ? "Active" : "Closed");
            System.out.print("Options: ");
            for (int i = 0; i < e.options().size(); i++) {
                System.out.print(e.options().get(i).name());
                if (i < e.options().size() - 1) {
                    System.out.print(" | ");
                }
            }
            System.out.println("\n-------------------------");
        }
    }

    private void showEventStatus() {
        showAllEvents();
        System.out.print("Enter Event ID to view detailed status: ");
        int id = Integer.parseInt(scanner.nextLine().trim());
        printEventStatus(id);
    }

    private void printEventStatus(int id) {
        EventDetailsDTO details = engine.getEventDetails(id);

        System.out.printf("\n=== Status for Event: %s ===\n", details.event().name());

        System.out.println("[ Current State ]");
        for (int i = 0; i < details.event().options().size(); i++) {
            OptionDTO opt = details.event().options().get(i);
            System.out.printf("- Option '%s': Current Value = %.2f, Total Shares Bought = %d\n",
                    opt.name(), details.currentPrices().get(i), opt.sharesBought());
        }

        System.out.printf("\n[ Financials ]\n");
        System.out.printf("Event Account Balance: %.2f\n", details.accountBalance());
        System.out.printf("Total Commission Collected: %.2f\n", details.totalCommission());

        if (!details.event().isActive()) {
            System.out.println("\n[ *** EVENT IS CLOSED *** ]");
            for (OptionDTO opt : details.event().options()) {
                System.out.printf("Final total shares for '%s': %d\n", opt.name(), opt.sharesBought());
            }
            if (details.event().winningOptionIndex() != null) {
                String winner = details.event().options().get(details.event().winningOptionIndex()).name();
                System.out.printf(">>> Winning Option: %s <<<\n", winner);
            }
        }

        System.out.println("\n[ Trade History ]");
        List<TradeDTO> trades = details.trades();
        if (trades.isEmpty()) {
            System.out.println("No trades recorded yet.");
        } else {
            for (int i = trades.size() - 1; i >= 0; i--) {
                TradeDTO t = trades.get(i);
                System.out.printf("Bought %d shares of '%s', Price paid: %.2f\n",
                        t.shares(), t.optionName(), t.pricePaid());
            }
        }
        System.out.println("===============================\n");
    }

    private void buyShares() {
        showAllEvents();
        System.out.print("Enter Event ID to participate: ");
        int eventId = Integer.parseInt(scanner.nextLine().trim());
        EventDetailsDTO details = engine.getEventDetails(eventId);

        System.out.println("Choose Option:");
        for (int i = 0; i < details.event().options().size(); i++) {
            System.out.printf("%d. %s\n", (i + 1), details.event().options().get(i).name());
        }
        int optChoice = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Amount of shares to buy: ");
        int shares = Integer.parseInt(scanner.nextLine().trim());

        double cost = engine.buyShares(eventId, optChoice - 1, shares);
        System.out.printf("Transaction successful! Total paid: %.2f$\n", cost);
    }

    private void closeEvent() {
        showAllEvents();
        System.out.print("Enter Event ID to close: ");
        int eventId = Integer.parseInt(scanner.nextLine().trim());
        EventDetailsDTO details = engine.getEventDetails(eventId);

        System.out.println("Which option won?");
        for (int i = 0; i < details.event().options().size(); i++) {
            System.out.printf("%d. %s\n", (i + 1), details.event().options().get(i).name());
        }
        int winningOpt = Integer.parseInt(scanner.nextLine().trim());

        engine.closeEvent(eventId, winningOpt - 1);
        System.out.println("Event closed successfully!");

        printEventStatus(eventId);
    }

    private void saveState() {
        System.out.print("Enter full path to save (WITHOUT extension): ");
        String path = scanner.nextLine().trim();
        engine.saveSystemState(path);
        System.out.println("System state saved successfully to " + path + ".dat!");
    }

    private void loadState() {
        System.out.print("Enter full path to load from (WITHOUT extension): ");
        String path = scanner.nextLine().trim();
        engine.loadSystemState(path);
        System.out.println("System state loaded successfully! All previous data restored.");
    }
}