package Green_trade.green_trade_platform.exception;

public class PaymentMethodNotSupportedException extends RuntimeException {
    public PaymentMethodNotSupportedException(String message) {
        super(message);
    }

    public PaymentMethodNotSupportedException() {
        super("Payment method is not supported");
    }
}
