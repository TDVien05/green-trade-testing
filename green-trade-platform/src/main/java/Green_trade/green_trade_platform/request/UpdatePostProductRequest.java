package Green_trade.green_trade_platform.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostProductRequest {
    private String title;
    private String brand;
    private String model;
    private Long manufactureYear;
    private String usedDuration;
    private String conditionLevel;
    private BigDecimal price;
    private String width;
    private String height;
    private String length;
    private String weight;
    private String description;
    private String locationTrading;
}
