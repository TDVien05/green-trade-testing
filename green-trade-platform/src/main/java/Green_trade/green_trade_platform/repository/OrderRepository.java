package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.enumerate.OrderStatus;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.model.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByBuyer(Buyer buyer, Pageable pageable);


    Optional<Order> findOrderById(Long id);

    Page<Order> findByPostProduct_SellerAndStatus(Seller seller, OrderStatus orderStatus, Pageable pageable);

    Page<Order> findAllByPostProduct_Seller(Seller seller, Pageable pageable);
}
