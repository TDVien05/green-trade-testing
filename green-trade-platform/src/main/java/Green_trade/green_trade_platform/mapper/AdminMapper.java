package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.request.CreateAdminRequest;
import Green_trade.green_trade_platform.response.AdminResponse;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {
    public Admin toEntity(CreateAdminRequest request) {
        return Admin.builder()
                .employeeNumber(request.getEmployeeNumber())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .gender(request.getGender())
                .build();
    }

    public AdminResponse toDto(Admin admin) {
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
}
