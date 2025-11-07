package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.enumerate.SellerStatus;
import Green_trade.green_trade_platform.enumerate.VerifiedDecisionStatus;
import Green_trade.green_trade_platform.model.Notification;
import Green_trade.green_trade_platform.model.Seller;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveSellerResponse {
    private Long sellerId;
    private VerifiedDecisionStatus decision;
    private String reason;
    private LocalDateTime decidedAt;
    private Notification notification;
}
