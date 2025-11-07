package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.model.CancelOrderReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancelOrderReasonRepository extends JpaRepository<CancelOrderReason, Long> {
    List<CancelOrderReason> findAll();
}
