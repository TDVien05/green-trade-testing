package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.filter.BadWordFilter;
import Green_trade.green_trade_platform.mapper.ReviewMapper;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.model.Review;
import Green_trade.green_trade_platform.model.ReviewImage;
import Green_trade.green_trade_platform.repository.OrderRepository;
import Green_trade.green_trade_platform.repository.ReviewImagesRepository;
import Green_trade.green_trade_platform.repository.ReviewRepository;
import Green_trade.green_trade_platform.request.ReviewRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
public class ReviewServiceImpl {
    private final BadWordFilter badWordFilter;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ReviewMapper reviewMapper;
    private final CloudinaryService cloudinaryService;
    private final ReviewImagesRepository reviewImagesRepository;

    public Review createReview(ReviewRequest request, List<MultipartFile> reviewImages) {
        log.info(">>> [Review Service] Create Review: Started.");
        Order order = validateReviewRequest(request);

        if (badWordFilter.containsBadWord(request.getFeedback())) {
            throw new IllegalArgumentException("This feedback contains bad words. Please write again.");
        }

        log.info(">>> [Review Service] Upload images into Cloudinary: Started.");
        Map<String, Object> uploadResult = uploadReviewImages(reviewImages);

        // ✅ Extract uploaded images list
        List<Map<String, String>> uploadedFiles = (List<Map<String, String>>) uploadResult.get("uploaded");
        List<ReviewImage> reviewImageEntities = new ArrayList<>();

        if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
            int orderImage = 1;
            for (Map<String, String> imageData : uploadedFiles) {
                ReviewImage reviewImage = ReviewImage.builder()
                        .imageUrl(imageData.get("fileUrl"))
                        .publicImageId(imageData.get("publicId"))
                        .orderImage(orderImage++) // optional field if you track image order
                        .build();
                reviewImageEntities.add(reviewImage);
            }
        }

        log.info(">>> [Review Service] Create Review: Create review entity.");
        Review review = reviewMapper.toEntity(request, reviewImageEntities);
        review.setOrder(order);

        log.info(">>> [Review Service] Create Review: Ended.");
        Review savedReview = reviewRepository.save(review);
        for (ReviewImage i : reviewImageEntities) {
            i.setReview(savedReview);
            reviewImagesRepository.save(i);
        }
        return savedReview;
    }

    private Map<String, Object> uploadReviewImages(List<MultipartFile> reviewImages) {
        Map<String, Object> response = new HashMap<>();

        if (reviewImages == null || reviewImages.isEmpty()) {
            response.put("message", "Do not have any images.");
            response.put("uploaded", Collections.emptyList());
            response.put("failed", Collections.emptyList());
            return response;
        }

        List<Map<String, String>> uploadedFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (MultipartFile file : reviewImages) {
            try {
                Map<String, String> uploadResult = cloudinaryService.upload(file, "reviews");
                if (uploadResult != null) {
                    uploadedFiles.add(uploadResult);
                } else {
                    failedFiles.add(file.getOriginalFilename());
                }
            } catch (IOException e) {
                failedFiles.add(file.getOriginalFilename());
            }
        }

        response.put("uploaded", uploadedFiles);
        response.put("failed", failedFiles);
        response.put("totalUploaded", uploadedFiles.size());
        response.put("totalFailed", failedFiles.size());
        response.put("message", "Done upload images to Cloudinary.");

        return response;
    }

    /**
     * Validate rating and feedback fields before saving.
     */
    private Order validateReviewRequest(ReviewRequest request) {
        log.info(">>> [Review Service] Validate Review Request: Started.");
        Order order = orderRepository.findOrderById(request.getOrderId()).orElseThrow(
                () -> new IllegalArgumentException("This order id does not exists.")
        );
        if (request.getRating() < 0 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be in from 0 to 5.");
        }
        log.info(">>> [Review Service] Validate Review Request: Ended.");

        return order;
    }

    public Review getReviewsByOrderId(Long orderId) {
        return reviewRepository.findByOrder_Id(orderId).orElseThrow(
                () -> new IllegalArgumentException("Do not have any review for order id: " + orderId)
        );
    }
}
