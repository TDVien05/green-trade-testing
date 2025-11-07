package Green_trade.green_trade_platform.service;

public interface OtpService {
    void sendOtpEmail(String to, String otp);

    String generateOtpCode();
}
