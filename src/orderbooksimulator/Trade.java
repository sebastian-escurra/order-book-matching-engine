package orderbooksimulator;

public class Trade {
    private double price;
    private int quantity;
    private int buyOrderId;
    private int sellOrderId;

    public Trade(double price, int quantity, int buyOrderId, int sellOrderId) {
        this.price = price;
        this.quantity = quantity;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getBuyOrderId() {
        return buyOrderId;
    }

    public int getSellOrderId() {
        return sellOrderId;
    }

    @Override
    public String toString() {
        return "Trade{" + "price=" + price + ", quantity=" + quantity
                + ", buyOrderId=" + buyOrderId + ", sellOrderId=" + sellOrderId + '}';
    }
}
