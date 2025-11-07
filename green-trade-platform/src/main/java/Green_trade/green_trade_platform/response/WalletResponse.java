package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.enumerate.WalletConcurrency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WalletResponse {
    private BigDecimal balance;
    private WalletConcurrency concurrency;
}
