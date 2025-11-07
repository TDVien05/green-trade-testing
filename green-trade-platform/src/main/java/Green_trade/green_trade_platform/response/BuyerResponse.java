package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.enumerate.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerResponse {
    private Long buyerId;
    private String avatarUrl;
    private String username;
    private String fullName;
    private String street;
    private String phoneNumber;
    private String email;
    private Gender gender;
    private LocalDate dob;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String provinceName;
    private String districtName;
    private String wardName;
}
