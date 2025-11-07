package Green_trade.green_trade_platform.exception;

public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(String message) {
        super(message);
    }

    public InvalidOrderStateException() {
        super("Order Status is not valid to cancel order");
    }
}
