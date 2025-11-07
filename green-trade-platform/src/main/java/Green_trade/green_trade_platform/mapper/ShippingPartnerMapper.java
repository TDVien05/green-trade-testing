package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.ShippingPartner;
import Green_trade.green_trade_platform.response.ShippingPartnerResponse;
import org.springframework.stereotype.Component;

@Component
public class ShippingPartnerMapper {
    public ShippingPartnerResponse toDto(ShippingPartner shippingPartner) {
        return ShippingPartnerResponse.builder()
                .email(shippingPartner.getEmail())
                .address(shippingPartner.getAddress())
                .partnerName(shippingPartner.getPartnerName())
                .hotLine(shippingPartner.getHotline())
                .websiteUrl(shippingPartner.getWebsiteUrl())
                .updatedAt(shippingPartner.getUpdatedAt())
                .createdAt(shippingPartner.getCreatedAt())
                .build();
    }
}
