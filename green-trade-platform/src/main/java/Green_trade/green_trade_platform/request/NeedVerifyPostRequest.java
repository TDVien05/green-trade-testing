package Green_trade.green_trade_platform.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NeedVerifyPostRequest {
    @NotNull(message = "Size is required")
    @Min(value = 1, message = "Size must be at least 1")
    private Integer size;

    @NotNull(message = "Page is required")
    @Min(value = 0, message = "Page must be 0 or greater")
    private Integer page;
}
