package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.model.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface BuyerRepository extends JpaRepository<Buyer, Long> {
    Optional<Buyer> findByUsername(String username);

    boolean existsByEmail(String email);

    Optional<Buyer> findByEmail(String email);

    boolean existsByUsername(String username);

    Optional<Buyer> findBySeller_SellerId(Long sellerId);

    @Query("SELECT w.balance FROM Wallet w WHERE w.buyer.buyerId = :buyerId")
    BigDecimal findBalanceByBuyerId(@Param("buyerId") Long buyerId);
}
