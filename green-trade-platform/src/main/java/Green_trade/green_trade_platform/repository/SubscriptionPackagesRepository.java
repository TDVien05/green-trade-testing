package Green_trade.green_trade_platform.repository;


import Green_trade.green_trade_platform.model.SubscriptionPackages;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPackagesRepository extends JpaRepository<SubscriptionPackages, Long> {
    Page<SubscriptionPackages> findByIsActiveTrue(Pageable pageable);

}