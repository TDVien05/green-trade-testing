package Green_trade.green_trade_platform.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyUsernameForgotPasswordRequest {
    @NotBlank(message = "Username is required.")
    @Pattern(regexp = "^[a-zA-Z]{8,}$",
            message = "Username must be at least 8 letters, with no spaces, numbers, or special characters.")
    private String username;
}
