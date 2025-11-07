package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Subscription;
import Green_trade.green_trade_platform.response.CurrentSubscriptionResponse;
import Green_trade.green_trade_platform.response.SignPackageResponse;
import Green_trade.green_trade_platform.response.SubscriptionResponse;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SubscriptionMapper {
    public SubscriptionResponse toDto(boolean valid, LocalDateTime expiryDate, String packageName) {
        return SubscriptionResponse.builder()
                .valid(valid)
                .expiryDate(expiryDate)
                .packageName(packageName)
                .build();
    }

    public SignPackageResponse toSignPackageResponse(String packageName,
                                                     String fullName,
                                                     double price,
                                                     Long durationByDay,
                                                     LocalDateTime startDate,
                                                     LocalDateTime endDate) {
        return SignPackageResponse.builder()
                .packageName(packageName)
                .fullName(fullName)
                .price(price)
                .durationByDay(durationByDay)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    public CurrentSubscriptionResponse toDto(Subscription subscription) {
        return CurrentSubscriptionResponse.builder()
                .sellerId(subscription.getSeller().getSellerId())
                .sellerName(subscription.getSeller().getSellerName())
                .packageId(subscription.getSubscriptionPackage().getId())
                .start(subscription.getStartDay())
                .end(subscription.getEndDay())
                .build();
    }
}
