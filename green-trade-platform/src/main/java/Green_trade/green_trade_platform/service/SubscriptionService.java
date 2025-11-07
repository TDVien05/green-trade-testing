package Green_trade.green_trade_platform.service;

public interface SubscriptionService {
    boolean isServicePackageExpired(Long sellerId) throws Exception;
}
