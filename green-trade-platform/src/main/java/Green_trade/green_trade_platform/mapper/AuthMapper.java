package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.response.AdminResponse;
import Green_trade.green_trade_platform.response.AuthResponse;
import Green_trade.green_trade_platform.response.BuyerResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {
    public AuthResponse toDto(Buyer buyer, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .username(buyer.getUsername())
                .email(buyer.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AdminResponse toDto(Admin admin, String accessToken, String refreshToken) {
        return AdminResponse.builder()
                .employeeNumber(admin.getEmployeeNumber())
                .fullName(admin.getFullName())
                .phoneNumber(admin.getPhoneNumber())
                .email(admin.getEmail())
                .gender(admin.getGender())
                .id(admin.getId())
                .avatarUrl(admin.getAvatarUrl())
                .isSuperAdmin(admin.isSuperAdmin())
                .status(admin.getStatus())
                .build();
    }

    public AuthResponse toDto(String username, String email, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .username(username)
                .email(email)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
