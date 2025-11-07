package Green_trade.green_trade_platform.exception;

public class ShippingPartnerNotSupported extends RuntimeException {
    public ShippingPartnerNotSupported(String message) {
        super(message);
    }

    public ShippingPartnerNotSupported() {
        super("Shipping Partner is not supported");
    }
}
