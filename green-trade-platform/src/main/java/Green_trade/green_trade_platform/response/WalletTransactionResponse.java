package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.enumerate.TransactionStatus;
import Green_trade.green_trade_platform.enumerate.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionResponse {
    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
}
