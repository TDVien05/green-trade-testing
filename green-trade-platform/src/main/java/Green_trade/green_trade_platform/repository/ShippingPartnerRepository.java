package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.model.ShippingPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShippingPartnerRepository extends JpaRepository<ShippingPartner, Long> {
}
