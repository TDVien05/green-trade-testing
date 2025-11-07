package Green_trade.green_trade_platform.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminAuthResponse {
    private AdminResponse adminResponse;
    private String accessToken;
    private String refreshToken;
    private String role;
}
