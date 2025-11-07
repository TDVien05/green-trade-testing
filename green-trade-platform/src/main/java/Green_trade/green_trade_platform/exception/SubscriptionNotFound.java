package Green_trade.green_trade_platform.exception;

public class SubscriptionNotFound extends RuntimeException {
    public SubscriptionNotFound(String message) {
        super(message);
    }

    public SubscriptionNotFound() {
        super("The user has not subscribe the service");
    }
}
