package Green_trade.green_trade_platform.exception;

public class SystemWalletException extends RuntimeException {
    public SystemWalletException(String message) {
        super(message);
    }

    public SystemWalletException() {
        super("Escrow Service occurred error");
    }
}
