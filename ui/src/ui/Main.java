package ui;

import engine.GuessMarketEngineImpl;
import engine.IGuessMarketEngine;

public class Main {
    public static void main(String[] args) {
        IGuessMarketEngine engine = new GuessMarketEngineImpl();
        ConsoleUI app = new ConsoleUI(engine);
        app.start();
    }
}