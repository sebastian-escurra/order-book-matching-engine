package orderbooksimulator;

import java.util.ArrayList;

// The order book keeps two lists of resting (unexecuted) orders:
//   - buyOrders : best buy first  (highest price first)
//   - sellOrders: best sell first (lowest price first)
// Inside the same price, the order that arrived first stays first (time priority).
// The lists are kept sorted ALL THE TIME, because every new order is inserted
// at the right position. So the best order is always at index 0.
public class OrderBook {

    private ArrayList<Order> buyOrders;
    private ArrayList<Order> sellOrders;

    public OrderBook() {
        buyOrders = new ArrayList<>();
        sellOrders = new ArrayList<>();
    }

    // ---------- adding orders ----------

    public void addOrder(Order order) {
        if (order.getType().equals("BUY")) {
            addBuyOrder(order);
        } else {
            addSellOrder(order);
        }
    }

    private void addBuyOrder(Order order) {
        int position = 0;
        // Skip every order whose price is better than or equal to the new one.
        // Equal price -> the new order goes BEHIND the older ones (time priority).
        while (position < buyOrders.size()
                && buyOrders.get(position).getPrice() >= order.getPrice()) {
            position++;
        }
        buyOrders.add(position, order);
    }

    private void addSellOrder(Order order) {
        int position = 0;
        // For sells, a lower price is better.
        while (position < sellOrders.size()
                && sellOrders.get(position).getPrice() <= order.getPrice()) {
            position++;
        }
        sellOrders.add(position, order);
    }

    // ---------- best orders (used by the MatchingEngine) ----------

    public boolean hasBuyOrders() {
        return !buyOrders.isEmpty();
    }

    public boolean hasSellOrders() {
        return !sellOrders.isEmpty();
    }

    // Returns null if there is no buy order.
    public Order getBestBuy() {
        if (buyOrders.isEmpty()) {
            return null;
        }
        return buyOrders.get(0);
    }

    // Returns null if there is no sell order.
    public Order getBestSell() {
        if (sellOrders.isEmpty()) {
            return null;
        }
        return sellOrders.get(0);
    }

    public void removeBestBuy() {
        if (!buyOrders.isEmpty()) {
            buyOrders.remove(0);
        }
    }

    public void removeBestSell() {
        if (!sellOrders.isEmpty()) {
            sellOrders.remove(0);
        }
    }

    // ---------- cancel and search ----------

    // Returns true if an order with this id is resting in the book.
    public boolean hasOrder(int id) {
        for (int i = 0; i < buyOrders.size(); i++) {
            if (buyOrders.get(i).getId() == id) {
                return true;
            }
        }
        for (int i = 0; i < sellOrders.size(); i++) {
            if (sellOrders.get(i).getId() == id) {
                return true;
            }
        }
        return false;
    }

    // Removes the order with this id. Returns true if it was found.
    public boolean cancelOrder(int id) {
        for (int i = 0; i < buyOrders.size(); i++) {
            if (buyOrders.get(i).getId() == id) {
                buyOrders.remove(i);
                return true;
            }
        }
        for (int i = 0; i < sellOrders.size(); i++) {
            if (sellOrders.get(i).getId() == id) {
                sellOrders.remove(i);
                return true;
            }
        }
        return false;
    }

    // ---------- read-only information (the lists themselves stay private) ----------

    public int getBuyCount() {
        return buyOrders.size();
    }

    public int getSellCount() {
        return sellOrders.size();
    }

    public Order getBuyOrderAt(int index) {
        return buyOrders.get(index);
    }

    public Order getSellOrderAt(int index) {
        return sellOrders.get(index);
    }

    public int getTotalBuyQuantity() {
        int total = 0;
        for (int i = 0; i < buyOrders.size(); i++) {
            total = total + buyOrders.get(i).getQuantity();
        }
        return total;
    }

    public int getTotalSellQuantity() {
        int total = 0;
        for (int i = 0; i < sellOrders.size(); i++) {
            total = total + sellOrders.get(i).getQuantity();
        }
        return total;
    }

    public void display() {

        System.out.println("\n-------------- ORDER BOOK --------------");

        System.out.println("\nSELL ORDERS (best first):");
        for (Order order : sellOrders) {
            System.out.println(order);
        }

        System.out.println("\nBUY ORDERS (best first):");
        for (Order order : buyOrders) {
            System.out.println(order);
        }

        if (hasBuyOrders() && hasSellOrders()) {
            double spread = getBestSell().getPrice() - getBestBuy().getPrice();
            System.out.println("\nBest buy: " + getBestBuy().getPrice()
                    + " | Best sell: " + getBestSell().getPrice()
                    + " | Spread: " + spread);
        }

        System.out.println("----------------------------------------\n");
    }
}
