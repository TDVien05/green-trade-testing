package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Review;
import Green_trade.green_trade_platform.model.ReviewImage;
import Green_trade.green_trade_platform.request.ReviewRequest;
import Green_trade.green_trade_platform.response.ReviewImagesResponse;
import Green_trade.green_trade_platform.response.ReviewResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ReviewMapper {
    public Review toEntity(ReviewRequest request, List<ReviewImage> reviewImages) {
        return Review.builder()
                .rating(request.getRating())
                .feedback(request.getFeedback())
                .reviewImages(reviewImages)
                .build();
    }

    public ReviewResponse toDto(Review review) {
        return ReviewResponse.builder()
                .orderId(review.getOrder() != null ? review.getOrder().getId() : null)
                .feedback(review.getFeedback())
                .rating(review.getRating())
                .reviewImages(
                        review.getReviewImages() != null
                                ? review.getReviewImages().stream()
                                .map(this::toDto) // map ReviewImage → ReviewImagesResponse
                                .toList()
                                : Collections.emptyList()
                )
                .build();
    }

    public ReviewImagesResponse toDto(ReviewImage reviewImage) {
        return ReviewImagesResponse.builder()
                .id(reviewImage.getId())
                .orderImage(reviewImage.getOrderImage())
                .imageUrl(reviewImage.getImageUrl())
                .build();
    }
}
