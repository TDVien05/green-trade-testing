package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.DisputeCategory;
import Green_trade.green_trade_platform.response.DisputeCategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class DisputeCategoryMapper {
    public DisputeCategoryResponse toDto(DisputeCategory disputeCategory) {
        return DisputeCategoryResponse.builder()
                .disputeCategoryId(disputeCategory.getId())
                .title(disputeCategory.getTitle())
                .description(disputeCategory.getDescription())
                .reason(disputeCategory.getReason())
                .build();
    }
}
