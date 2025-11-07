package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.ProductImage;
import Green_trade.green_trade_platform.response.PostProductResponse;
import Green_trade.green_trade_platform.response.ProductImageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostProductMapper {
    public PostProductResponse toDto(PostProduct postProduct) {

        List<ProductImageResponse> imageResponses = postProduct.getProductImages() != null
                ? postProduct.getProductImages().stream()
                .map(this::toImageResponse)
                .toList()
                : Collections.emptyList();

        return PostProductResponse.builder()
                .postId(postProduct.getId())
                .sellerId(postProduct.getSeller().getSellerId())
                .sellerStoreName(postProduct.getSeller().getStoreName())
                .title(postProduct.getTitle())
                .brand(postProduct.getBrand())
                .model(postProduct.getModel())
                .manufactureYear((postProduct.getManufactureYear()))
                .usedDuration(postProduct.getUsedDuration())
                .rejectedReason(postProduct.getRejectedReason())
                .conditionLevel(postProduct.getConditionLevel())
                .verifiedDecisionStatus(postProduct.getVerifiedDecisionstatus())
                .verified(postProduct.isVerified())
                .active(postProduct.isActive())
                .price(postProduct.getPrice())
                .locationTrading(postProduct.getLocationTrading())
                .categoryName(postProduct.getCategory().getName())
                .images(imageResponses)
                .build();
    }

    public Page<PostProductResponse> toDtoPage(Page<PostProduct> postProductPage) {
        List<PostProductResponse> responses = postProductPage.getContent()
                .stream()
                .map(this::toDto)
                .toList();

        Pageable pageable = postProductPage.getPageable();

        return new PageImpl<>(responses, pageable, postProductPage.getTotalElements());
    }

    public ProductImageResponse toImageResponse(ProductImage productImage) {
        return ProductImageResponse.builder()
                .id(productImage.getImageId())
                .imgUrl(productImage.getImageUrl())
                .order(productImage.getOrderImage())
                .build();
    }
}
