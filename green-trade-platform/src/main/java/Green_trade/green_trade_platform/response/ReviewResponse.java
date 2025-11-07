package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.model.ReviewImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long orderId;
    private double rating;
    private String feedback;
    private List<ReviewImagesResponse> reviewImages;
}
