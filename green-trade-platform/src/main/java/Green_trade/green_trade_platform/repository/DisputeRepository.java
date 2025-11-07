package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.enumerate.DisputeStatus;
import Green_trade.green_trade_platform.model.Dispute;
import Green_trade.green_trade_platform.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    Page<Dispute> findAllByStatus(DisputeStatus disputeStatus, Pageable pageable);

    Optional<Order> findOrderById(Long disputeId);
}
