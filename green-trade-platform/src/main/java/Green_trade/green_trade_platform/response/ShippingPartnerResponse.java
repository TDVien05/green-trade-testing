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
public class ShippingPartnerResponse {
    private String email;
    private String partnerName;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private String address;
    private String websiteUrl;
    private String hotLine;
}
