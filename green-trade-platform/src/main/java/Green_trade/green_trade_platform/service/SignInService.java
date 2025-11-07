package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.request.SignInGoogleRequest;
import Green_trade.green_trade_platform.request.SignInRequest;

import java.util.Map;

public interface SignInService {
    Buyer startSignIn(SignInRequest request);

    Buyer startSignInWithGoogle(SignInGoogleRequest body) throws Exception;
}
