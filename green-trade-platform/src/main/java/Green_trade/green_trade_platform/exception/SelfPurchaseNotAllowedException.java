package Green_trade.green_trade_platform.exception;

public class SelfPurchaseNotAllowedException extends RuntimeException {
    public SelfPurchaseNotAllowedException() {
        super("You cannot buy your own product.");
    }

    public SelfPurchaseNotAllowedException(String message) {
        super(message);
    }
}
