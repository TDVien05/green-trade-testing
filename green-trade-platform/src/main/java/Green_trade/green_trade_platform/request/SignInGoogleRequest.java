package Green_trade.green_trade_platform.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SignInGoogleRequest {
    @NotEmpty(message = "idToken is required")
    private String idToken;
}
