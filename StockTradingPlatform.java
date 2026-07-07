import java.io.*;
import java.util.*;

/**
 * CodeAlpha Java Programming Internship - Task 2
 * Stock Trading Platform (Simulation)
 *
 * Console-based simulation of a basic stock trading environment.
 * Uses OOP to model Stocks, a User, a Portfolio and Transactions.
 * Market prices fluctuate each "tick" (simulated). Portfolio data
 * is persisted to a file (portfolio.txt) via file I/O.
 */
public class StockTradingPlatform {

    private static final String PORTFOLIO_FILE = "portfolio.txt";
    private final Market market = new Market();
    private final User user = new User("Trader", 10000.00); // starting cash
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        StockTradingPlatform app = new StockTradingPlatform();
        app.loadPortfolio();
        app.run();
        app.savePortfolio();
        System.out.println("\nSession ended. Portfolio saved to " + PORTFOLIO_FILE);
    }

    private void run() {
        printBanner();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> market.displayMarket();
                case "2" -> buyStock();
                case "3" -> sellStock();
                case "4" -> user.getPortfolio().displayHoldings(market);
                case "5" -> user.getPortfolio().displayTransactionHistory();
                case "6" -> market.simulateTick();
                case "7" -> {
                    savePortfolio();
                    System.out.println("Portfolio saved.");
                }
                case "0" -> running = false;
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void printBanner() {
        System.out.println("=========================================");
        System.out.println("   STOCK TRADING PLATFORM - CodeAlpha     ");
        System.out.println("=========================================");
        System.out.printf("Welcome, %s! Starting cash: $%.2f%n", user.getName(), user.getCash());
    }

    private void printMenu() {
        System.out.printf("%n--- MENU (Cash: $%.2f) ---%n", user.getCash());
        System.out.println("1. View Market Data");
        System.out.println("2. Buy Stock");
        System.out.println("3. Sell Stock");
        System.out.println("4. View My Portfolio");
        System.out.println("5. View Transaction History");
        System.out.println("6. Advance Market (simulate price changes)");
        System.out.println("7. Save Portfolio Now");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    private void buyStock() {
        market.displayMarket();
        System.out.print("Enter stock symbol to buy: ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        Stock stock = market.getStock(symbol);
        if (stock == null) {
            System.out.println("Unknown stock symbol.");
            return;
        }
        System.out.print("Enter quantity to buy: ");
        int qty = readPositiveInt();
        double cost = stock.getPrice() * qty;
        if (cost > user.getCash()) {
            System.out.printf("Insufficient funds. Cost $%.2f exceeds cash $%.2f%n", cost, user.getCash());
            return;
        }
        user.setCash(user.getCash() - cost);
        user.getPortfolio().addHolding(symbol, qty, stock.getPrice());
        System.out.printf("Bought %d share(s) of %s at $%.2f each (total $%.2f)%n", qty, symbol, stock.getPrice(), cost);
    }

    private void sellStock() {
        System.out.print("Enter stock symbol to sell: ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        Stock stock = market.getStock(symbol);
        if (stock == null) {
            System.out.println("Unknown stock symbol.");
            return;
        }
        int owned = user.getPortfolio().getQuantity(symbol);
        if (owned <= 0) {
            System.out.println("You do not own any shares of " + symbol);
            return;
        }
        System.out.println("You own " + owned + " share(s).");
        System.out.print("Enter quantity to sell: ");
        int qty = readPositiveInt();
        if (qty > owned) {
            System.out.println("You cannot sell more than you own.");
            return;
        }
        double proceeds = stock.getPrice() * qty;
        user.setCash(user.getCash() + proceeds);
        user.getPortfolio().removeHolding(symbol, qty, stock.getPrice());
        System.out.printf("Sold %d share(s) of %s at $%.2f each (total $%.2f)%n", qty, symbol, stock.getPrice(), proceeds);
    }

    private int readPositiveInt() {
        while (true) {
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value > 0) return value;
                System.out.print("Please enter a positive number: ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid number, try again: ");
            }
        }
    }

    private void savePortfolio() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(PORTFOLIO_FILE))) {
            writer.println("CASH," + user.getCash());
            for (Map.Entry<String, Integer> entry : user.getPortfolio().getHoldings().entrySet()) {
                writer.println("HOLDING," + entry.getKey() + "," + entry.getValue());
            }
            for (Transaction t : user.getPortfolio().getHistory()) {
                writer.println("TXN," + t.toCsv());
            }
        } catch (IOException e) {
            System.out.println("Error saving portfolio: " + e.getMessage());
        }
    }

    private void loadPortfolio() {
        File file = new File(PORTFOLIO_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                switch (parts[0]) {
                    case "CASH" -> user.setCash(Double.parseDouble(parts[1]));
                    case "HOLDING" -> user.getPortfolio().setHoldingDirect(parts[1], Integer.parseInt(parts[2]));
                    case "TXN" -> user.getPortfolio().getHistory().add(Transaction.fromCsv(parts));
                    default -> { /* ignore unknown lines */ }
                }
            }
            System.out.println("Loaded existing portfolio from " + PORTFOLIO_FILE);
        } catch (IOException e) {
            System.out.println("Error loading portfolio: " + e.getMessage());
        }
    }

    // ===================== Domain Classes =====================

    /** A tradable stock with a symbol, company name and current price. */
    static class Stock {
        private final String symbol;
        private final String companyName;
        private double price;

        Stock(String symbol, String companyName, double price) {
            this.symbol = symbol;
            this.companyName = companyName;
            this.price = price;
        }

        String getSymbol() { return symbol; }
        String getCompanyName() { return companyName; }
        double getPrice() { return price; }
        void setPrice(double price) { this.price = price; }
    }

    /** Holds the list of available stocks and simulates price changes. */
    static class Market {
        private final Map<String, Stock> stocks = new LinkedHashMap<>();
        private final Random random = new Random();

        Market() {
            stocks.put("AAPL", new Stock("AAPL", "Apple Inc.", 190.00));
            stocks.put("GOOG", new Stock("GOOG", "Alphabet Inc.", 140.00));
            stocks.put("AMZN", new Stock("AMZN", "Amazon.com Inc.", 145.00));
            stocks.put("TSLA", new Stock("TSLA", "Tesla Inc.", 250.00));
            stocks.put("MSFT", new Stock("MSFT", "Microsoft Corp.", 330.00));
            stocks.put("INFY", new Stock("INFY", "Infosys Ltd.", 18.50));
        }

        Stock getStock(String symbol) { return stocks.get(symbol); }

        void displayMarket() {
            System.out.println("\n--- MARKET DATA ---");
            System.out.printf("%-8s %-20s %-10s%n", "Symbol", "Company", "Price");
            System.out.println("-".repeat(42));
            for (Stock s : stocks.values()) {
                System.out.printf("%-8s %-20s $%-9.2f%n", s.getSymbol(), s.getCompanyName(), s.getPrice());
            }
        }

        void simulateTick() {
            System.out.println("\nMarket prices updating...");
            for (Stock s : stocks.values()) {
                // Random walk: price moves by -5% to +5%
                double changePercent = (random.nextDouble() * 10 - 5) / 100.0;
                double newPrice = Math.max(0.50, s.getPrice() * (1 + changePercent));
                s.setPrice(Math.round(newPrice * 100.0) / 100.0);
            }
            displayMarket();
        }
    }

    /** Represents the trading user (name + cash balance + portfolio). */
    static class User {
        private final String name;
        private double cash;
        private final Portfolio portfolio = new Portfolio();

        User(String name, double cash) {
            this.name = name;
            this.cash = cash;
        }

        String getName() { return name; }
        double getCash() { return cash; }
        void setCash(double cash) { this.cash = cash; }
        Portfolio getPortfolio() { return portfolio; }
    }

    /** Tracks a user's stock holdings and transaction history over time. */
    static class Portfolio {
        private final Map<String, Integer> holdings = new LinkedHashMap<>();
        private final List<Transaction> history = new ArrayList<>();

        Map<String, Integer> getHoldings() { return holdings; }
        List<Transaction> getHistory() { return history; }

        int getQuantity(String symbol) { return holdings.getOrDefault(symbol, 0); }

        void addHolding(String symbol, int qty, double price) {
            holdings.merge(symbol, qty, Integer::sum);
            history.add(new Transaction("BUY", symbol, qty, price));
        }

        void removeHolding(String symbol, int qty, double price) {
            int remaining = getQuantity(symbol) - qty;
            if (remaining <= 0) holdings.remove(symbol);
            else holdings.put(symbol, remaining);
            history.add(new Transaction("SELL", symbol, qty, price));
        }

        void setHoldingDirect(String symbol, int qty) {
            if (qty > 0) holdings.put(symbol, qty);
        }

        void displayHoldings(Market market) {
            if (holdings.isEmpty()) {
                System.out.println("You do not own any stocks yet.");
                return;
            }
            System.out.println("\n--- MY PORTFOLIO ---");
            System.out.printf("%-8s %-10s %-12s %-12s%n", "Symbol", "Qty", "Price", "Value");
            System.out.println("-".repeat(46));
            double totalValue = 0;
            for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
                Stock stock = market.getStock(entry.getKey());
                double price = stock != null ? stock.getPrice() : 0;
                double value = price * entry.getValue();
                totalValue += value;
                System.out.printf("%-8s %-10d $%-11.2f $%-11.2f%n", entry.getKey(), entry.getValue(), price, value);
            }
            System.out.printf("Total portfolio value: $%.2f%n", totalValue);
        }

        void displayTransactionHistory() {
            if (history.isEmpty()) {
                System.out.println("No transactions yet.");
                return;
            }
            System.out.println("\n--- TRANSACTION HISTORY ---");
            for (Transaction t : history) {
                System.out.println(t);
            }
        }
    }

    /** Immutable record of a single buy/sell transaction. */
    static class Transaction {
        private final String type;
        private final String symbol;
        private final int quantity;
        private final double price;

        Transaction(String type, String symbol, int quantity, double price) {
            this.type = type;
            this.symbol = symbol;
            this.quantity = quantity;
            this.price = price;
        }

        String toCsv() {
            return type + "," + symbol + "," + quantity + "," + price;
        }

        static Transaction fromCsv(String[] parts) {
            // parts[0] == "TXN", parts[1]=type, parts[2]=symbol, parts[3]=qty, parts[4]=price
            return new Transaction(parts[1], parts[2], Integer.parseInt(parts[3]), Double.parseDouble(parts[4]));
        }

        @Override
        public String toString() {
            return String.format("%-4s %-4d share(s) of %-6s @ $%.2f", type, quantity, symbol, price);
        }
    }
}
