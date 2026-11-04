package orderbooksimulator;

import java.util.ArrayList;

public class Main {

    // Prints the trades produced by one order.
    private static void printTrades(ArrayList<Trade> trades) {
        if (trades.isEmpty()) {
            System.out.println("  (no trades)");
        }
        for (Trade trade : trades) {
            System.out.println("  TRADE EXECUTED: " + trade);
        }
    }

    public static void main(String[] args) {

        // ============================================================
        // Scenario 1: partial fills
        // One sell of 50 is hit by two buys at the same price.
        // Note: the two buys never wait in the book together (each one trades
        // as soon as it arrives), so time priority is NOT tested here.
        // See scenario 5 for that.
        // ============================================================
        System.out.println("=== SCENARIO 1: partial fills ===");
        OrderBook book1 = new OrderBook();
        MatchingEngine engine1 = new MatchingEngine(book1);

        engine1.processOrder(new Order(1, "SELL", 100.0, 50));
        book1.display();

        System.out.println("BUY 40 @ 100 arrives:");
        printTrades(engine1.processOrder(new Order(2, "BUY", 100.0, 40)));

        System.out.println("BUY 30 @ 100 arrives (only 10 are left to sell):");
        printTrades(engine1.processOrder(new Order(3, "BUY", 100.0, 30)));

        book1.display();   // 20 of order 3 should be waiting in the book

        // ============================================================
        // Scenario 2: price priority
        // One big buy sweeps several sell orders, cheapest first.
        // ============================================================
        System.out.println("=== SCENARIO 2: price priority ===");
        OrderBook book2 = new OrderBook();
        MatchingEngine engine2 = new MatchingEngine(book2);

        engine2.processOrder(new Order(1, "SELL", 101.0, 10));
        engine2.processOrder(new Order(2, "SELL", 100.0, 10));
        engine2.processOrder(new Order(3, "SELL", 100.0, 10));
        book2.display();

        System.out.println("BUY 25 @ 102 arrives (trades at 100, 100 and then 101):");
        printTrades(engine2.processOrder(new Order(4, "BUY", 102.0, 25)));

        book2.display();

        // ============================================================
        // Scenario 3: no match (the spread) and a better price
        // The trade happens at the price of the order already in the book.
        // ============================================================
        System.out.println("=== SCENARIO 3: no match, then a trade at the resting price ===");
        OrderBook book3 = new OrderBook();
        MatchingEngine engine3 = new MatchingEngine(book3);

        engine3.processOrder(new Order(1, "BUY", 99.0, 10));
        engine3.processOrder(new Order(2, "SELL", 101.0, 10));
        book3.display();   // buy 99, sell 101 -> spread 2, nothing matches

        System.out.println("BUY 10 @ 105 arrives (willing to pay 105, but the seller asks 101):");
        printTrades(engine3.processOrder(new Order(3, "BUY", 105.0, 10)));

        book3.display();

        // ============================================================
        // Scenario 4: cancelling and rejected orders
        // ============================================================
        System.out.println("=== SCENARIO 4: cancel and invalid orders ===");
        OrderBook book4 = new OrderBook();
        MatchingEngine engine4 = new MatchingEngine(book4);

        engine4.processOrder(new Order(1, "BUY", 100.0, 10));
        System.out.println("Cancel order 1: " + engine4.cancelOrder(1));
        System.out.println("Cancel order 1 again: " + engine4.cancelOrder(1));

        engine4.processOrder(new Order(2, "buy", 100.0, 10));    // wrong type text
        engine4.processOrder(new Order(3, "BUY", 100.0, 0));     // quantity 0
        engine4.processOrder(new Order(4, "SELL", -5.0, 10));    // negative price
        engine4.processOrder(new Order(5, "SELL", 100.0, 10));   // valid
        engine4.processOrder(new Order(5, "SELL", 101.0, 10));   // same id as order 5

        book4.display();

        // ============================================================
        // Scenario 5: time priority
        // Two buys wait in the book at the same price. The incoming SELL must
        // choose one of them: it chooses the one that arrived first.
        // ============================================================
        System.out.println("=== SCENARIO 5: time priority ===");
        OrderBook book5 = new OrderBook();
        MatchingEngine engine5 = new MatchingEngine(book5);

        engine5.processOrder(new Order(1, "BUY", 100.0, 10));   // arrives first
        engine5.processOrder(new Order(2, "BUY", 100.0, 10));   // arrives second, same price
        book5.display();   // order 1 is listed before order 2

        System.out.println("SELL 10 @ 100 arrives (it could trade with order 1 or with order 2):");
        printTrades(engine5.processOrder(new Order(3, "SELL", 100.0, 10)));

        book5.display();   // order 2 is still waiting; order 1 was served first

        System.out.println("Trades per scenario (1, 2, 3, 4, 5): "
                + engine1.getTradeHistory().size() + ", "
                + engine2.getTradeHistory().size() + ", "
                + engine3.getTradeHistory().size() + ", "
                + engine4.getTradeHistory().size() + ", "
                + engine5.getTradeHistory().size());
    }
}
