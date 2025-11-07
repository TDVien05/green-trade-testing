package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.request.SignUpRequest;
import Green_trade.green_trade_platform.response.BuyerResponse;
import org.springframework.stereotype.Component;

@Component
public class BuyerMapper {
    public Buyer toEntity(SignUpRequest request) {
        return Buyer.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .build();
    }

    public BuyerResponse toDto(Buyer buyer) {
        return BuyerResponse.builder().buyerId(buyer.getBuyerId())
                .avatarUrl(buyer.getAvatarUrl())
                .username(buyer.getUsername())
                .fullName(buyer.getFullName())
                .street(buyer.getStreet())
                .phoneNumber(buyer.getPhoneNumber())
                .email(buyer.getEmail())
                .gender(buyer.getGender())
                .dob(buyer.getDob())
                .provinceName(buyer.getProvinceName())
                .districtName(buyer.getDistrictName())
                .wardName(buyer.getWardName())
                .createdAt(buyer.getCreatedAt())
                .updatedAt(buyer.getUpdatedAt())
                .build();
    }

}
