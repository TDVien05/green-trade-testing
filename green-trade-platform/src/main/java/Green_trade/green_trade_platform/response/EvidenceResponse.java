package Green_trade.green_trade_platform.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvidenceResponse {
    private Long id;
    private String imageUrl;
    private long order;
}
