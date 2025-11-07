package Green_trade.green_trade_platform.request;

import Green_trade.green_trade_platform.model.ReviewImage;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequest {
    @NotBlank(message = "Order id is required.")
    private Long orderId;
    @NotBlank(message = "Rating is required.")
    private double rating;
    private String feedback;
}
