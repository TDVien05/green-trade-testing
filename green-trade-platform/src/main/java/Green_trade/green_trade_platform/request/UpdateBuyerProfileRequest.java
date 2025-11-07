package Green_trade.green_trade_platform.request;

import Green_trade.green_trade_platform.enumerate.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBuyerProfileRequest {
    @NotBlank(message = "Full name is required.")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Full name can only include letters and spaces.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Invalid email format.")
    private String email;

    private Gender gender;

    private LocalDate dob;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^0\\d{9}$", message = "Phone number must start with 0 and contain 10 digits.")
    private String phoneNumber;

    @NotBlank(message = "Shipping address is required.")
    @Pattern(regexp = "^[\\p{L}0-9\\s,./-]+$", message = "Shipping address contains invalid characters.")
    private String street;

    private String wardName;

    private String districtName;

    private String provinceName;
}
