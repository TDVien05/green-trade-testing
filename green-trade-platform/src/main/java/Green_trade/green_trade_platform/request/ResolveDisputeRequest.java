package Green_trade.green_trade_platform.request;

import Green_trade.green_trade_platform.enumerate.DisputeDecision;
import Green_trade.green_trade_platform.enumerate.ResolutionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResolveDisputeRequest {
    private Long disputeId;
    private DisputeDecision decision;
    private String resolution;
    private ResolutionType resolutionType;
    private double refundPercent;
}
