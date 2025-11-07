package Green_trade.green_trade_platform.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostProductDecisionRequest {
    @NotNull(message = "Post product ID cannot be null")
    private Long postProductId;

    private Boolean passed;

    @Size(max = 255, message = "Rejected reason must be less than 255 characters")
    private String rejectedReason;
}
