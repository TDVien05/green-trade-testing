package Green_trade.green_trade_platform.exception;

public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException() {
        super("Password and Confirm password do not match");
    }
}
