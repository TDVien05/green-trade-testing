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
public class SignInAdminRequest {
    @NotBlank(message = "employeeNo is required.")
    private String employeeNumber;
    @NotBlank(message = "Password is required.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])[^\\s]{8,}$",
            message = "Password must be at least 8 characters, include letters, numbers, and special characters, and contain no spaces."
    )
    private String password;
}
