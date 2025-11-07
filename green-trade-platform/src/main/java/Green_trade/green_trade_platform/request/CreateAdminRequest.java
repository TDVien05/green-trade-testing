package Green_trade.green_trade_platform.request;

import Green_trade.green_trade_platform.enumerate.Gender;
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
public class CreateAdminRequest {
    @NotBlank(message = "Employee number is required.")
    @Pattern(
            regexp = "^\\d{10}$",
            message = "Employee number must be exactly 10 digits."
    )
    private String employeeNumber;

    @NotBlank(message = "Password is required.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])[^\\s]{8,}$",
            message = "Password must be at least 8 characters, include letters, numbers, and special characters, and contain no spaces."
    )
    private String password;

    @NotBlank(message = "Full name is required.")
    @Pattern(
            regexp = "^[\\p{L}\\s]+$",
            message = "Full name can only include letters and spaces."
    )
    private String fullName;

    @NotBlank(message = "Phone number is required.")
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Phone number must start with 0 and contain 10 digits."
    )
    private String phoneNumber;

    @NotBlank(message = "Email is required.")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
            message = "Invalid email format."
    )
    private String email;

    private Gender gender;
}
