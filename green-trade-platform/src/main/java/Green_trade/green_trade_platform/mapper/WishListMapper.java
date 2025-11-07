package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.WishListing;
import Green_trade.green_trade_platform.request.WishListRequest;
import Green_trade.green_trade_platform.response.WishListingResponse;
import org.springframework.stereotype.Component;

@Component
public class WishListMapper {
    public WishListing toEntity(WishListRequest request, Buyer buyer, PostProduct postProduct) {
        return WishListing.builder()
                .buyer(buyer)
                .postProduct(postProduct)
                .note(request.getNote())
                .priority(request.getPriority())
                .build();
    }

    public WishListingResponse toDto(WishListing wishListing) {
        return WishListingResponse.builder()
                .id(wishListing.getId())
                .postId(wishListing.getPostProduct().getId())
                .buyerId(wishListing.getBuyer().getBuyerId())
                .priority(wishListing.getPriority())
                .note(wishListing.getNote())
                .build();
    }
}
