package Green_trade.green_trade_platform.exception;

public class OrderNotFound extends RuntimeException {
    public OrderNotFound(String message) {
        super(message);
    }

    public OrderNotFound() {
        super("Order is not existed");
    }
}
