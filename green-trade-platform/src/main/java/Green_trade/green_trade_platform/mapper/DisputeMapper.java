package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Dispute;
import Green_trade.green_trade_platform.model.Evidence;
import Green_trade.green_trade_platform.response.DisputeResponse;
import Green_trade.green_trade_platform.response.EvidenceResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisputeMapper {
    public DisputeResponse toDto(Dispute dispute) {
        List<Evidence> evidences = dispute.getEvidences();
        List<EvidenceResponse> evidenceResponses = evidences.stream().map(this::toEvidenDto).toList();
        return DisputeResponse.builder()
                .disputeId(dispute.getId())
                .disputeCategoryId(dispute.getDisputeCategory().getId())
                .disputeCategoryName(dispute.getDisputeCategory().getTitle())
                .description(dispute.getResolution())
                .resolution(dispute.getResolution())
                .decision(dispute.getDecision())
                .status(dispute.getStatus())
                .evidences(evidenceResponses)
                .build();
    }

    public EvidenceResponse toEvidenDto(Evidence evidence) {
        return EvidenceResponse.builder()
                .id(evidence.getId())
                .imageUrl(evidence.getImageUrl())
                .order(evidence.getOrderImage())
                .build();
    }
}
