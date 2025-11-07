package Green_trade.green_trade_platform.exception;

public class ProductSoldOutException extends RuntimeException {
    public ProductSoldOutException(String message) {
        super(message);
    }

    public ProductSoldOutException() {
        super("Product has been sold out");
    }
}
