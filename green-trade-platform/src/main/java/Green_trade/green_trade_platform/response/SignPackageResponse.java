package Green_trade.green_trade_platform.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignPackageResponse {
    private String packageName;
    private String fullName;
    private double price;
    private Long durationByDay;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
