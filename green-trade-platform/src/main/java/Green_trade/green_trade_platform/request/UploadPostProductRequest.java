package Green_trade.green_trade_platform.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadPostProductRequest {
    @NotNull(message = "Seller Id is required")
    @Positive(message = "Seller Id must be positive")
    private Long sellerId;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @NotBlank(message = "Brand is required")
    @Size(max = 50, message = "Brand must not exceed 50 characters")
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(max = 50, message = "Model must not exceed 50 characters")
    private String model;

    @NotNull(message = "Manufacture Year is required")
    @Min(value = 1900, message = "Manufacture Year must be later than 1900")
    @Max(value = 2100, message = "Manufacture Year seems invalid")
    private Long manufactureYear;

    @NotBlank(message = "Used Duration is required")
    private String usedDuration;

    @NotBlank(message = "Condition Level is required")
    private String conditionLevel;

    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    private String length;

    private String width;

    private String height;

    private String weight;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Location Trading is required")
    private String locationTrading;

    @NotNull(message = "Category Id is required")
    @Positive(message = "Category Id must be positive")
    private Long categoryId;
}
