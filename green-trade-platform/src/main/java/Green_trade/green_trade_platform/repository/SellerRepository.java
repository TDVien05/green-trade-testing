package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.enumerate.SellerStatus;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Seller;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    Page<Seller> findAllByStatus(SellerStatus status, Pageable pageable);

    @Transactional
    @Modifying
    @Query("UPDATE Seller s SET s.status = :status WHERE s.sellerId = :sellerId")
    int updatePendingSeller(@Param("status") SellerStatus status, @Param("sellerId") Long sellerId);

    @Query("SELECT s FROM Seller s WHERE s.buyer = :buyer")
    Optional<Seller> findByBuyer(Buyer buyer);
}
