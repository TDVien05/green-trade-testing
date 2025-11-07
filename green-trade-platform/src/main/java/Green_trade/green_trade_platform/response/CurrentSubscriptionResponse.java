package Green_trade.green_trade_platform.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentSubscriptionResponse {
    private Long sellerId;
    private String sellerName;
    private Long packageId;
    private String packageName;
    private LocalDateTime start;
    private LocalDateTime end;
}
