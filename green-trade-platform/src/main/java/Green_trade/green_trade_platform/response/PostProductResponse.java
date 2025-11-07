package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.enumerate.VerifiedDecisionStatus;
import Green_trade.green_trade_platform.model.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostProductResponse {
    private Long postId;
    private Long sellerId;
    private String sellerStoreName;
    private String title;
    private String brand;
    private String model;
    private Long manufactureYear;
    private String usedDuration;
    private String rejectedReason;
    private String conditionLevel;
    private VerifiedDecisionStatus verifiedDecisionStatus;
    private boolean active;
    private boolean verified;
    private String categoryName;
    private BigDecimal price;
    private String locationTrading;
    private List<ProductImageResponse> images;
}
