package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.model.SystemWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemWalletRepossitory extends JpaRepository<SystemWallet, Long> {
    Optional<SystemWallet> findByOrder(Order order);
}
