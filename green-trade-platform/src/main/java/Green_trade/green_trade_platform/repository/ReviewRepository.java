package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByOrder_Id(Long orderId);
}
