package Green_trade.green_trade_platform.response;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubscriptionPackageResponse {
    private Long id;
    private String name;
    private String description;
    private boolean isActive;
    private Long maxProduct;
    private Long maxStoragePerImg;
    private Long maxImgPerPost;
    private List<PackagePriceResponse> prices;
}