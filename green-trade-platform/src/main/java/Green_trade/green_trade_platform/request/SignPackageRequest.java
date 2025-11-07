package Green_trade.green_trade_platform.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignPackageRequest {
    private Long packageId;
    private Long priceId;
    private double price;
    private Long durationByDay;
}
