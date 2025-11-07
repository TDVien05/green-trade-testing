package Green_trade.green_trade_platform.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

public interface GoogleVerifierService {
    GoogleIdToken.Payload verify(String idTokenString) throws Exception;
}
