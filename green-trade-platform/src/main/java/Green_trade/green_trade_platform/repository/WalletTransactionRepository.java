package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.model.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    boolean existsByExternalTransactionReference(String txnRef);

    Optional<WalletTransaction> findByExternalTransactionReference(String txnRef);

    Page<WalletTransaction> findByWallet(Wallet wallet, Pageable pageable);
}
