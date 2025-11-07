package Green_trade.green_trade_platform.request;

import Green_trade.green_trade_platform.model.Payment;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {
    @NotNull(message = "Product ID cannot be null")
    @Positive(message = "Product ID must be a positive number")
    private Long postProductId;

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    private String fullName;

    @NotBlank(message = "Street cannot be blank")
    @Size(max = 255, message = "Street must not exceed 255 characters")
    private String street;

    private String wardName;

    private String districtName;

    private String provinceName;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(
            regexp = "^(0|\\+84)[0-9]{9,10}$",
            message = "Phone number must be valid (starts with 0 or +84 and has 10–11 digits)"
    )
    private String phoneNumber;

    @NotNull(message = "Shipping partner ID cannot be null")
    @Positive(message = "Shipping partner ID must be a positive number")
    private Long shippingPartnerId;

    @NotNull(message = "Payment ID cannot be null")
    @Positive(message = "Payment ID must be a positive number")
    private Long paymentId;
}
