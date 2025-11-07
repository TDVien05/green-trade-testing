package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.service.implement.GhnServiceImpl;
import Green_trade.green_trade_platform.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RegisterShopShippingServiceMapper {
    private final GhnServiceImpl ghnService;
    private final StringUtils stringUtils;

    public RegisterShopShippingServiceMapper(GhnServiceImpl ghnService, StringUtils stringUtils) {
        this.ghnService = ghnService;
        this.stringUtils = stringUtils;
    }

    public Map<String, Object> toDto(Seller seller) throws JsonProcessingException {
        String provinceId = ghnService.findProvinceCodeByProvinceName(seller.getBuyer().getProvinceName());
        String districtId = ghnService.findDistrictCodeByDistrictName(Integer.parseInt(provinceId),
                seller.getBuyer().getDistrictName());
        try {
            Map<String, Object> result = Map.of(
                    "district_id", Integer.parseInt(districtId),
                    "ward_code",
                    ghnService.findWardCodeByWardName(Integer.parseInt(districtId), seller.getBuyer().getWardName()),
                    "name", seller.getStoreName(),
                    "phone", seller.getBuyer().getPhoneNumber(),
                    "address", stringUtils.fullAddress(seller.getBuyer().getStreet(), seller.getBuyer().getWardName(), seller.getBuyer().getDistrictName(), seller.getBuyer().getProvinceName()));
            return result;
        } catch (Exception e) {
            log.info(">>> [RegisterShopShippingServiceMapper] error occur when mapping: {}", e.getMessage());
        }
        return null;
    }
}
