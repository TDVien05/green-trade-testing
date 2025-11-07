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
public class VerifyOtpForgotPasswordRequest {
    @NotBlank(message = "OTP must not be blank.")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "OTP must be exactly 6 digits."
    )
    private String otp;

    @NotBlank(message = "Email must not be blank.")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
            message = "Invalid email format."
    )
    private String email;
}
