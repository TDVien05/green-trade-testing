package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.enumerate.WishListPriority;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.WishListing;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WishListingRepository extends JpaRepository<WishListing, Long> {
    Page<WishListing> findByBuyer_BuyerId(Long buyerId, Pageable pageable);

    Page<WishListing> findByBuyer_BuyerIdAndPriority(Long buyerId, WishListPriority priority, Pageable pageable);

    @Query("SELECT w.postProduct FROM WishListing w WHERE w.id = :wishId")
    Optional<PostProduct> findPostProductByWishListId(@Param("wishId") Long wishId);
}
