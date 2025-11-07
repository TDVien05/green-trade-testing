package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.enumerate.WishListPriority;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.WishListing;
import Green_trade.green_trade_platform.repository.WishListingRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class WishListingServiceImpl {
    private final WishListingRepository wishListingRepository;

    public WishListing addWishList(WishListing wishListing) {
        log.info(">>> [Wishlist Service] Add product to wish list: Started.");
        log.info(">>> [Wishlist Service] WishListing before save: id={}, buyerId={}, postId={}",
                wishListing.getId(),
                wishListing.getBuyer() != null ? wishListing.getBuyer().getBuyerId() : null,
                wishListing.getPostProduct() != null ? wishListing.getPostProduct().getId() : null);
        return wishListingRepository.save(wishListing);
    }

    public void removePostProduct(long id) {
        WishListing wishListing = wishListingRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Can not find wish list with id: " + id)
        );

        wishListingRepository.delete(wishListing);
    }

    public Page<WishListing> getWishList(Buyer buyer, int page, int size, WishListPriority priority) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (priority != null) {
            // Lọc theo priority nếu có
            return wishListingRepository.findByBuyer_BuyerIdAndPriority(buyer.getBuyerId(), priority, pageable);
        }

        // Không truyền priority → lấy toàn bộ wishlist của buyer
        return wishListingRepository.findByBuyer_BuyerId(buyer.getBuyerId(), pageable);
    }
}
