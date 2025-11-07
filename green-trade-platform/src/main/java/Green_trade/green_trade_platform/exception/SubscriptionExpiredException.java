package Green_trade.green_trade_platform.exception;

public class SubscriptionExpiredException extends RuntimeException {
    public SubscriptionExpiredException() {
        super("Subscription of seller is expired");
    }

    public SubscriptionExpiredException(String message) {
        super(message);
    }
}
