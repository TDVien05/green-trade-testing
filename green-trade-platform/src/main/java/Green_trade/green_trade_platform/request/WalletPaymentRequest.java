package Green_trade.green_trade_platform.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPaymentRequest {
    private String username;
    private Long postProductId;
    private Long paymentId;
}
