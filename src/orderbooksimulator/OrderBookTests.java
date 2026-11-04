package orderbooksimulator;

import java.util.ArrayList;
import java.util.Random;

// Simple tests without any library. Run this class (Run File in NetBeans).
// Each check prints PASS or FAIL.
public class OrderBookTests {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }

    public static void main(String[] args) {

        // ---------- 1. basic matching ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "SELL", 100.0, 10));
            ArrayList<Trade> trades = engine.processOrder(new Order(2, "BUY", 100.0, 10));
            check("full match: one trade of 10 @ 100",
                    trades.size() == 1 && trades.get(0).getQuantity() == 10
                    && trades.get(0).getPrice() == 100.0);
            check("full match: book is empty afterwards",
                    book.getBuyCount() == 0 && book.getSellCount() == 0);
        }

        // ---------- 2. partial fill of the resting order ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "SELL", 100.0, 50));
            engine.processOrder(new Order(2, "BUY", 100.0, 40));
            check("partial fill: resting sell keeps 10",
                    book.getSellCount() == 1 && book.getBestSell().getQuantity() == 10);
            check("partial fill: nothing rests on the buy side", book.getBuyCount() == 0);
        }

        // ---------- 3. partial fill of the incoming order ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "SELL", 100.0, 10));
            engine.processOrder(new Order(2, "BUY", 100.0, 30));
            check("incoming partly filled: remaining 20 rests as a buy",
                    book.getBuyCount() == 1 && book.getBestBuy().getQuantity() == 20
                    && book.getSellCount() == 0);
        }

        // ---------- 4. no match ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "BUY", 99.0, 10));
            ArrayList<Trade> trades = engine.processOrder(new Order(2, "SELL", 100.0, 10));
            check("no cross (buy 99, sell 100): no trade, both rest",
                    trades.isEmpty() && book.getBuyCount() == 1 && book.getSellCount() == 1);
        }

        // ---------- 5. time priority ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "BUY", 100.0, 10));
            engine.processOrder(new Order(2, "BUY", 100.0, 10));
            ArrayList<Trade> trades = engine.processOrder(new Order(3, "SELL", 100.0, 10));
            check("time priority: the first buy (id 1) is filled, not id 2",
                    trades.size() == 1 && trades.get(0).getBuyOrderId() == 1
                    && book.getBestBuy().getId() == 2);
        }

        // ---------- 6. price priority on the buy side ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "BUY", 99.0, 5));
            engine.processOrder(new Order(2, "BUY", 101.0, 5));
            ArrayList<Trade> trades = engine.processOrder(new Order(3, "SELL", 98.0, 5));
            check("price priority (buys): the 101 buyer is served first, at 101",
                    trades.size() == 1 && trades.get(0).getBuyOrderId() == 2
                    && trades.get(0).getPrice() == 101.0);
        }

        // ---------- 7. trade at the price of the resting order ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "SELL", 100.0, 10));
            ArrayList<Trade> t1 = engine.processOrder(new Order(2, "BUY", 105.0, 10));
            check("resting sell 100, incoming buy 105 -> trade at 100",
                    t1.size() == 1 && t1.get(0).getPrice() == 100.0);

            engine.processOrder(new Order(3, "BUY", 105.0, 10));
            ArrayList<Trade> t2 = engine.processOrder(new Order(4, "SELL", 100.0, 10));
            check("resting buy 105, incoming sell 100 -> trade at 105",
                    t2.size() == 1 && t2.get(0).getPrice() == 105.0);
        }

        // ---------- 8. one order sweeps several levels ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "SELL", 101.0, 10));
            engine.processOrder(new Order(2, "SELL", 100.0, 10));
            engine.processOrder(new Order(3, "SELL", 100.0, 10));
            ArrayList<Trade> trades = engine.processOrder(new Order(4, "BUY", 102.0, 25));
            check("sweep: 3 trades at 100, 100, 101",
                    trades.size() == 3 && trades.get(0).getPrice() == 100.0
                    && trades.get(1).getPrice() == 100.0 && trades.get(2).getPrice() == 101.0);
            check("sweep: trades go to sells 2, 3, 1 in that order",
                    trades.get(0).getSellOrderId() == 2 && trades.get(1).getSellOrderId() == 3
                    && trades.get(2).getSellOrderId() == 1);
            check("sweep: 5 left of order 1 at 101, buy side empty",
                    book.getSellCount() == 1 && book.getBestSell().getQuantity() == 5
                    && book.getBuyCount() == 0);
        }

        // ---------- 9. a partly filled order keeps its place in the queue ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "SELL", 100.0, 10));
            engine.processOrder(new Order(2, "SELL", 100.0, 10));
            engine.processOrder(new Order(3, "BUY", 100.0, 5));        // takes 5 from order 1
            ArrayList<Trade> trades = engine.processOrder(new Order(4, "BUY", 100.0, 10));
            check("partly filled order 1 is still first: 5 to order 1, then 5 to order 2",
                    trades.size() == 2 && trades.get(0).getSellOrderId() == 1
                    && trades.get(0).getQuantity() == 5
                    && trades.get(1).getSellOrderId() == 2
                    && trades.get(1).getQuantity() == 5);
        }

        // ---------- 10. cancel ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "BUY", 100.0, 10));
            engine.processOrder(new Order(2, "SELL", 110.0, 10));
            check("cancel existing buy returns true", engine.cancelOrder(1));
            check("cancel existing sell returns true", engine.cancelOrder(2));
            check("cancel unknown id returns false", !engine.cancelOrder(99));
            check("book is empty after cancelling", book.getBuyCount() == 0 && book.getSellCount() == 0);
        }

        // ---------- 11. invalid orders are rejected and change nothing ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            engine.processOrder(new Order(1, "buy", 100.0, 10));
            engine.processOrder(new Order(2, "BUY", 100.0, 0));
            engine.processOrder(new Order(3, "BUY", 100.0, -5));
            engine.processOrder(new Order(4, "SELL", -50.0, 10));
            engine.processOrder(new Order(5, "SELL", 0.0, 10));
            engine.processOrder(new Order(6, "BUY", Double.NaN, 10));
            engine.processOrder(new Order(7, "OTHER", 100.0, 10));
            engine.processOrder(new Order(8, null, 100.0, 10));
            engine.processOrder(null);
            check("invalid orders (bad type, qty <= 0, price <= 0, NaN, null) are all rejected",
                    book.getBuyCount() == 0 && book.getSellCount() == 0
                    && engine.getTradeHistory().isEmpty());

            engine.processOrder(new Order(10, "BUY", 100.0, 10));
            engine.processOrder(new Order(10, "BUY", 99.0, 5));
            check("duplicate id in the book is rejected",
                    book.getBuyCount() == 1 && book.getBestBuy().getQuantity() == 10);
        }

        // ---------- 12. the engine, not the user, decides arrival order ----------
        {
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);
            Order first = new Order(1, "BUY", 100.0, 10);
            Order second = new Order(2, "BUY", 100.0, 10);
            second.setTimestamp(-1000);   // the user tries to look "older"
            engine.processOrder(first);
            engine.processOrder(second);
            check("arrival numbers are assigned by the engine (1 then 2)",
                    first.getTimestamp() == 1 && second.getTimestamp() == 2);
        }

        // ---------- 13. random test: invariants must hold after EVERY order ----------
        {
            Random random = new Random(42);   // fixed seed -> same test every time
            OrderBook book = new OrderBook();
            MatchingEngine engine = new MatchingEngine(book);

            long submittedBuy = 0;
            long submittedSell = 0;
            long tradedQuantity = 0;
            boolean neverCrossed = true;
            boolean alwaysSorted = true;
            boolean quantitiesBalance = true;

            for (int i = 1; i <= 5000; i++) {
                String type;
                if (random.nextBoolean()) {
                    type = "BUY";
                } else {
                    type = "SELL";
                }
                double price = 90 + random.nextInt(21);        // 90, 91, ..., 110
                int quantity = 1 + random.nextInt(100);        // 1, ..., 100

                if (type.equals("BUY")) {
                    submittedBuy = submittedBuy + quantity;
                } else {
                    submittedSell = submittedSell + quantity;
                }

                ArrayList<Trade> trades = engine.processOrder(new Order(i, type, price, quantity));
                for (Trade trade : trades) {
                    tradedQuantity = tradedQuantity + trade.getQuantity();
                }

                // (a) the book must never be crossed: best buy < best sell
                if (book.hasBuyOrders() && book.hasSellOrders()
                        && book.getBestBuy().getPrice() >= book.getBestSell().getPrice()) {
                    neverCrossed = false;
                }

                // (b) buys sorted by (price down, arrival up); sells by (price up, arrival up)
                for (int k = 1; k < book.getBuyCount(); k++) {
                    Order a = book.getBuyOrderAt(k - 1);
                    Order b = book.getBuyOrderAt(k);
                    if (a.getPrice() < b.getPrice()
                            || (a.getPrice() == b.getPrice() && a.getTimestamp() > b.getTimestamp())) {
                        alwaysSorted = false;
                    }
                }
                for (int k = 1; k < book.getSellCount(); k++) {
                    Order a = book.getSellOrderAt(k - 1);
                    Order b = book.getSellOrderAt(k);
                    if (a.getPrice() > b.getPrice()
                            || (a.getPrice() == b.getPrice() && a.getTimestamp() > b.getTimestamp())) {
                        alwaysSorted = false;
                    }
                }

                // (c) quantity is conserved: submitted = traded + still resting (per side)
                if (submittedBuy != tradedQuantity + book.getTotalBuyQuantity()
                        || submittedSell != tradedQuantity + book.getTotalSellQuantity()) {
                    quantitiesBalance = false;
                }
            }

            check("random 5000 orders: book never crossed", neverCrossed);
            check("random 5000 orders: price-time order always respected", alwaysSorted);
            check("random 5000 orders: submitted = traded + resting (buy and sell sides)", quantitiesBalance);
            check("random 5000 orders: some trades happened (test is meaningful)", tradedQuantity > 0);
        }

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
