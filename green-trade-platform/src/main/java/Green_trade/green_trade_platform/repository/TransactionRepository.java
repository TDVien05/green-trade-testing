package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.enumerate.TransactionStatus;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.model.Transaction;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByOrder(Order order);

    // Cách 2: dùng JPQL để tùy biến hơn
    @Query("SELECT t FROM Transaction t WHERE t.order = :order AND t.status <> :status")
    Optional<Transaction> findValidTransactionsByOrderId(@Param("order") Order order,
                                                         @Param("status") TransactionStatus status);

    Optional<Transaction> findFirstByOrderAndStatusNotOrderByCreatedAtDesc(Order order, TransactionStatus status);

}
