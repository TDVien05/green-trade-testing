package Green_trade.green_trade_platform.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeAccountRequest {
    @NotBlank(message = "Store name is required.")
    @Pattern(
            regexp = "^[A-Za-z0-9\\s\\p{L}]{2,50}$",
            message = "Store name must be 2–50 characters long and contain only letters, numbers, and spaces."
    )
    private String storeName;

    @NotBlank(message = "Tax number is required.")
    @Pattern(
            regexp = "^[0-9]{10,13}$",
            message = "Tax number must contain only digits and be 10–13 digits long."
    )
    private String taxNumber;

    @NotBlank(message = "Identity number is required.")
    @Pattern(
            regexp = "^[0-9]{9,12}$",
            message = "Identity number must contain only digits and be 9–12 digits long."
    )
    private String identityNumber;

    @NotBlank(message = "Seller name is required.")
    @Pattern(
            message = "Seller name just contains only characters.",
            regexp = "^[A-Za-z]+$"
    )
    private String sellerName;

    @NotBlank(message = "Nationality is required.")
    @Pattern(
            message = "Nationality just contains only characters.",
            regexp = "^[A-Za-z]+$"
    )
    private String nationality;

    @NotBlank(message = "Home(country) is required.")
    @Pattern(
            message = "Home just contains only characters.",
            regexp = "^[A-Za-z0-9]+$"
    )
    private String home;
}
