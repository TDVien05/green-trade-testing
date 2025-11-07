package Green_trade.green_trade_platform.exception;

import org.springframework.security.core.AuthenticationException;

public class AuthException extends AuthenticationException {
    public AuthException(String message) {
        super(message);
    }
}
