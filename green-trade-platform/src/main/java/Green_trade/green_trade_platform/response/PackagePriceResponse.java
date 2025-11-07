package Green_trade.green_trade_platform.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PackagePriceResponse {
    private Long id;
    private double price;
    private Long durationByDay;
    private String currency;
    private double discountPercent;
}