package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.model.ShippingPartner;
import Green_trade.green_trade_platform.repository.ShippingPartnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ShippingPartnerServiceImpl {

    private final ShippingPartnerRepository shippingPartnerRepository;

    public ShippingPartnerServiceImpl(ShippingPartnerRepository shippingPartnerRepository) {
        this.shippingPartnerRepository = shippingPartnerRepository;
    }

    public List<ShippingPartner> getShippingPartners() {
        List<ShippingPartner> partners = shippingPartnerRepository.findAll();
        return new ArrayList<>(partners);
    }
}
