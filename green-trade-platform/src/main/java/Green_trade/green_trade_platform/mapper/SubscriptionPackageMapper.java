package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.PackagePrice;
import Green_trade.green_trade_platform.model.SubscriptionPackages;
import Green_trade.green_trade_platform.response.PackagePriceResponse;
import Green_trade.green_trade_platform.response.SubscriptionPackageResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubscriptionPackageMapper {

    public SubscriptionPackageResponse toResponse(SubscriptionPackages pkg) {
        if (pkg == null) return null;

        List<PackagePriceResponse> prices = pkg.getPackagePrices() == null
                ? List.of()
                : pkg.getPackagePrices().stream()
                .filter(price -> price.isActive() && price.getDeletedAt() == null)
                .map(this::toPriceResponse)
                .collect(Collectors.toList());

        return SubscriptionPackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .isActive(pkg.isActive())
                .maxProduct(pkg.getMaxProduct())
                .maxImgPerPost(pkg.getMaxImgPerPost())
                .prices(prices)
                .build();
    }

    private PackagePriceResponse toPriceResponse(PackagePrice price) {
        return PackagePriceResponse.builder()
                .id(price.getId())
                .price(price.getPrice())
                .durationByDay(price.getDurationByDay())
                .currency(price.getCurrency())
                .discountPercent(price.getDiscountPercent())
                .build();
    }
}