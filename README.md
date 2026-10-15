# Guess Market - Trading System

Welcome to the Guess Market application. This project implements a fully functional trading engine (LMSR & Order Book algorithms) wrapped in a custom JavaFX graphical user interface.

## 🚀 How to Launch
You do not need an IDE to run this application. 
1. Navigate to the **Releases** section on the right side of this repository.
2. Download the latest `Guess_Market_App.zip` file and extract it to a local folder.
3. Double-click the `run.bat` file. The application will launch instantly using Java's windowed environment.

## 📂 Loading Data
1. Click the **Load File** button located at the top-left of the main screen.
2. Select a valid `XML` data file to initialize the market events and users. 
*(Note: You can find ready-to-use sample XML files inside the `Tests` folder of this repository).*

## 📈 Using the App
* **Events Tab:** Browse all active, pending, and ended events. Use the filter buttons to sort them. Clicking an event displays its real-time Order Book statistics (LAST, BID, ASK, MID, SPREAD).
* **Users Tab (Trading):** To execute a trade, switch to the Users tab, select a specific participant, and then click on an event from their holdings table. This will reveal the trading window for that specific market.
* **Continuous Trading:** The application is designed to preserve your User and Event selections even after refreshing the data, allowing you to perform multiple consecutive transactions seamlessly.
