package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
//    Optional<Subscription> findBySeller_SellerIdOrderByEndDayDesc(Long sellerId);

    Optional<Subscription> findFirstBySeller_SellerIdOrderByEndDayDesc(Long sellerId);

    Subscription findBySeller_SellerId(Long sellerSellerId);
}
