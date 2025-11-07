package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.enumerate.AccountStatus;
import Green_trade.green_trade_platform.enumerate.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResponse {
    private Long id;
    private String avatarUrl;
    private boolean isSuperAdmin;
    private AccountStatus status;
    private String employeeNumber;
    private String fullName;
    private String phoneNumber;
    private String email;
    private Gender gender;
}

