package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.enumerate.SellerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerResponse {
    private Long sellerId;
    private String storeName;
    private SellerStatus status;
    private String storePolicyUrl;
    private String taxNumber;

    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    // Optional for admin panel
    private String identityFrontImageUrl;
    private String identityBackImageUrl;
    private String businessLicenseUrl;
    private String selfieUrl;
    private String sellerName;
    private String nationality;
    private String home;
}
