package Green_trade.green_trade_platform.exception;

public class PostProductNotFound extends RuntimeException {
    public PostProductNotFound() {
        super("PostProduct is not existed");
    }

    public PostProductNotFound(String message) {
        super(message);
    }
}
