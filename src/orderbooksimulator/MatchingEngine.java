package orderbooksimulator;

import java.util.ArrayList;

// The MatchingEngine receives new orders and tries to match them with the orders
// that are already resting in the OrderBook.
//
// Rules:
//   1. A BUY matches a SELL if buy price >= sell price (and the reverse for a SELL).
//   2. The best price is served first; for equal price, the oldest order is first.
//   3. The trade price is the price of the order that was ALREADY in the book.
//   4. If an order is only partly filled, the rest keeps its place (or, if it is the
//      new order, the rest is added to the book).
public class MatchingEngine {

    private OrderBook orderBook;
    private ArrayList<Trade> tradeHistory;
    private long nextTimestamp;   // arrival counter: 1, 2, 3, ...

    public MatchingEngine(OrderBook orderBook) {
        this.orderBook = orderBook;
        this.tradeHistory = new ArrayList<>();
        this.nextTimestamp = 1;
    }

    // Processes one new order and returns the trades it produced
    // (an empty list if it did not match, or if it was rejected).
    public ArrayList<Trade> processOrder(Order newOrder) {

        ArrayList<Trade> newTrades = new ArrayList<>();

        String problem = checkOrder(newOrder);
        if (problem != null) {
            if (newOrder == null) {
                System.out.println("ORDER REJECTED: " + problem);   // no order, so no id to show
            } else {
                System.out.println("ORDER REJECTED (id=" + newOrder.getId() + "): " + problem);
            }
            return newTrades;
        }

        // The engine decides the arrival order, so the user cannot cheat the queue.
        newOrder.setTimestamp(nextTimestamp);
        nextTimestamp++;

        if (newOrder.getType().equals("BUY")) {
            processBuyOrder(newOrder, newTrades);
        } else {
            processSellOrder(newOrder, newTrades);
        }
        return newTrades;
    }

    // Returns a text describing what is wrong with the order, or null if it is valid.
    private String checkOrder(Order order) {
        if (order == null) {
            return "order is null";
        }
        // Writing "BUY" first avoids a crash if the type is null.
        if (!"BUY".equals(order.getType()) && !"SELL".equals(order.getType())) {
            return "type must be exactly BUY or SELL";
        }
        if (Double.isNaN(order.getPrice()) || order.getPrice() <= 0) {
            return "price must be positive";
        }
        if (order.getQuantity() <= 0) {
            return "quantity must be positive";
        }
        if (orderBook.hasOrder(order.getId())) {
            return "an order with this id is already in the book";
        }
        return null;
    }

    private void processBuyOrder(Order buyOrder, ArrayList<Trade> newTrades) {

        while (buyOrder.getQuantity() > 0 && orderBook.hasSellOrders()) {

            Order bestSell = orderBook.getBestSell();

            // The cheapest seller asks more than we offer: no more matches.
            if (buyOrder.getPrice() < bestSell.getPrice()) {
                break;
            }

            // Trade price = price of the order that was already resting (the sell).
            Trade trade = executeTrade(buyOrder, bestSell, bestSell.getPrice());
            newTrades.add(trade);

            if (bestSell.getQuantity() == 0) {
                orderBook.removeBestSell();
            }
        }

        // If something is left, it waits in the book.
        if (buyOrder.getQuantity() > 0) {
            orderBook.addOrder(buyOrder);
        }
    }

    private void processSellOrder(Order sellOrder, ArrayList<Trade> newTrades) {

        while (sellOrder.getQuantity() > 0 && orderBook.hasBuyOrders()) {

            Order bestBuy = orderBook.getBestBuy();

            // The best buyer offers less than we ask: no more matches.
            if (bestBuy.getPrice() < sellOrder.getPrice()) {
                break;
            }

            // Trade price = price of the order that was already resting (the buy).
            Trade trade = executeTrade(bestBuy, sellOrder, bestBuy.getPrice());
            newTrades.add(trade);

            if (bestBuy.getQuantity() == 0) {
                orderBook.removeBestBuy();
            }
        }

        if (sellOrder.getQuantity() > 0) {
            orderBook.addOrder(sellOrder);
        }
    }

    // Executes ONE trade between a buy and a sell order:
    // quantity traded = the smaller of the two quantities.
    private Trade executeTrade(Order buyOrder, Order sellOrder, double price) {

        int tradedQuantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());

        Trade trade = new Trade(price, tradedQuantity, buyOrder.getId(), sellOrder.getId());

        buyOrder.setQuantity(buyOrder.getQuantity() - tradedQuantity);
        sellOrder.setQuantity(sellOrder.getQuantity() - tradedQuantity);

        tradeHistory.add(trade);
        return trade;
    }

    // Cancels a resting order. Returns true if the order was found.
    public boolean cancelOrder(int id) {
        return orderBook.cancelOrder(id);
    }

    public ArrayList<Trade> getTradeHistory() {
        return tradeHistory;
    }
}
