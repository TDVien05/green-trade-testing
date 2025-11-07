package Green_trade.green_trade_platform.service;

import java.util.Map;

public interface AuthService {
    Map<String, Object> verifyUsernameForgotPassword(String username) throws Exception;
}
