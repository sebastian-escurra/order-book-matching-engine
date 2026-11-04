package orderbooksimulator;

public class Order {
    private int id;
    private String type;       // "BUY" or "SELL"
    private double price;
    private int quantity;      // quantity still unfilled (it decreases as the order is executed)
    private long timestamp;    // arrival number: given by the MatchingEngine, NOT by the user

    public Order(int id, String type, double price, int quantity) {
        this.id = id;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = 0;    // the MatchingEngine sets the real value when the order arrives
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", timestamp=" + timestamp +
                '}';
    }
}
