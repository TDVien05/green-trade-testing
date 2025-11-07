package Green_trade.green_trade_platform.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long buyerId;
    private String username;
    private String email;
    private String accessToken;
    private String refreshToken;
    private Object role;
    private String employeeId;
}
