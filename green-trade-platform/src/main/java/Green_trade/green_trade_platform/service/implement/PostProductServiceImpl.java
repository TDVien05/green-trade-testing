package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.enumerate.SellerStatus;
import Green_trade.green_trade_platform.enumerate.VerifiedDecisionStatus;
import Green_trade.green_trade_platform.exception.*;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.*;
import Green_trade.green_trade_platform.request.PostProductDecisionRequest;
import Green_trade.green_trade_platform.request.UpdatePostProductRequest;
import Green_trade.green_trade_platform.request.UploadPostProductRequest;
import Green_trade.green_trade_platform.request.VerifiedPostProductRequest;
import Green_trade.green_trade_platform.response.SellerResponse;
import Green_trade.green_trade_platform.service.PostProductService;
import Green_trade.green_trade_platform.util.FileUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class PostProductServiceImpl implements PostProductService {

    private final PostProductRepository postProductRepository;
    private final CategoryRepository categoryRepository;
    private final AdminServiceImpl adminService;
    private final FileUtils fileUtils;
    private final CloudinaryService cloudinaryService;
    private final SellerRepository sellerRepository;
    private final ProductImageRepository productImageRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BuyerRepository buyerRepository;
    private final AdminRepository adminRepository;
    private final SubscriptionServiceImpl subscriptionService;
    private final WishListingRepository wishListingRepository;

    public PostProduct createNewPostProduct(
            UploadPostProductRequest request,
            List<MultipartFile> files
    ) throws Exception {
        try {
            Subscription subscription = subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(
                    request.getSellerId()).orElseThrow(() -> new SubscriptionNotFound()
            );
            Long maxImg = subscription.getSubscriptionPackage().getMaxImgPerPost();
            if (subscription.getEndDay().isBefore(LocalDateTime.now())) {
                throw new SubscriptionExpiredException();
            }
            if (files.size() > maxImg) {
                throw new ImageUploadLimitExceededException("Your subscription only allowed " + maxImg + "per post");
            }

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(
                            () -> new RuntimeException("Category is not existed")
                    );

            Seller seller = sellerRepository.findById(request.getSellerId())
                    .orElseThrow(
                            () -> new ProfileException("Seller is not existed")
                    );

            PostProduct newPost = PostProduct.builder()
                    .seller(seller)
                    .title(request.getTitle())
                    .brand(request.getBrand())
                    .model(request.getModel())
                    .manufactureYear((request.getManufactureYear()))
                    .usedDuration(request.getUsedDuration())
                    .rejectedReason("No decision yet")
                    .conditionLevel(request.getConditionLevel())
                    .price(request.getPrice())
                    .length(request.getLength())
                    .width(request.getWidth())
                    .height(request.getHeight())
                    .weight(request.getWeight())
                    .description(request.getDescription())
                    .locationTrading(request.getLocationTrading())
                    .active(true)
                    .verifiedDecisionstatus(VerifiedDecisionStatus.UNVAILABLE)
                    .verified(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .deletedAt(null)
                    .category(category)
                    .admin(null)
                    .build();
            log.info(">>> request data: {}", request.toString());
            log.info(">>> files data: {}", files);
            files.forEach((file) -> {
                fileUtils.validateFile(file);
                log.info(">>> Checked File name: {}", file.toString());
            });
            newPost = postProductRepository.save(newPost);

            for (int i = 0; i <= files.size() - 1; i++) {
                Map<String, String> uploadResult = cloudinaryService.upload(files.get(i), "PostImages/" + newPost.getId() + ":" + seller.getBuyer().getUsername() + "/product_image_" + i);
                String imageUrl = uploadResult.get("fileUrl");
                String publicId = uploadResult.get("publicId");
                log.info(">>> Passed uploaded picture {}", i);
                ProductImage productImage = ProductImage.builder()
                        .imageUrl(imageUrl)
                        .orderImage((long) i + 1)
                        .postProduct(newPost)
                        .build();
                productImageRepository.save(productImage);
            }
            log.info(">>> Passed uploaded file");

            return newPost;
        } catch (Exception e) {
            log.info(">>> Error at createNewPostProduct: {}", e.getMessage());
            throw e;
        }
    }

    public PostProduct uploadPostProductPicture(Long id, List<MultipartFile> files) throws Exception {
        try {
            List<PostProduct> postProducts = postProductRepository.findAll();
            PostProduct newPost = postProductRepository.findById(id)
                    .orElseThrow(() -> new ProfileException("Post Product is not existed"));
            postProducts.forEach((postProduct) -> {
                for (int i = 0; i <= files.size() - 1; i++) {
                    try {
                        Map<String, String> uploadResult = cloudinaryService.upload(
                                files.get(i),
                                "PostImages/" + postProduct.getId() + ":" +
                                        postProduct.getSeller().getBuyer().getUsername() + "/product_image_" + i
                        );
                        String imageUrl = uploadResult.get("fileUrl");
                        ProductImage productImage = ProductImage.builder()
                                .imageUrl(imageUrl)
                                .orderImage((long) i + 1)
                                .postProduct(postProduct)
                                .build();
                        productImageRepository.save(productImage);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return newPost;
        } catch (Exception e) {
            log.info(">>> Error at createNewPostProduct: {}", e.getMessage());
            throw e;
        }
    }


    public Page<PostProduct> getAllProductPaging(int page, int size, String sortedBy, boolean isAsc) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortedBy).descending());
        if (!isAsc) {
            pageable = PageRequest.of(page, size, Sort.by(sortedBy).descending());
        }
        Page<PostProduct> postProductsPaging = postProductRepository.findAllBySoldFalseAndActiveTrue(pageable);
        return new PageImpl<>(
                postProductsPaging.getContent(),
                pageable,
                postProductsPaging.getTotalElements()
        );
    }

    public Page<PostProduct> getAllPostProductForVerifiedReview(int size, int page) throws Exception {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
//            Page<PostProduct> postProductsPaging = postProductRepository.findAll(pageable);
//            List<PostProduct> postProducts = postProductsPaging.getContent();
//            List<PostProduct> result = new ArrayList<>();
//            for (int i = 0; i <= postProducts.size() - 1; i++) {
//                PostProduct postProduct = postProducts.get(i);
//                Subscription subscription = subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(postProduct.getSeller().getSellerId()).orElseThrow(() -> new Exception("Seller doesn't subscribe service"));
//                log.info(">>> subscription package id: {}", subscription.getSubscriptionPackage().getId());
//                log.info(">>> postProduct verified status: {}", postProduct.getVerifiedDecisionstatus().toString());
//                if (subscription.getSubscriptionPackage().getId() >= 2 && postProduct.getVerifiedDecisionstatus().equals(VerifiedDecisionStatus.PENDING)) {
//                    log.info(">>> result add: {} and {}", postProduct.getVerifiedDecisionstatus(), subscription.getSubscriptionPackage().getId());
//                    result.add(postProduct);
//                }
//            }
//            return new PageImpl<>(result, pageable, result.size());
            return postProductRepository.findAllByVerifiedDecisionstatus(VerifiedDecisionStatus.PENDING, pageable);
        } catch (Exception e) {
            log.info(">>> Error at PostProductServiceImpl: {}", e.getMessage());
            throw e;
        }
    }

    public PostProduct getPostProductById(Long postProductId) throws Exception {
        try {
            log.info(">>> [Post Product Service] 2 Find post product by id: Started.");
            PostProduct foundPostProduct = postProductRepository.findById(postProductId).orElseThrow(
                    () -> new PostProductNotFound()
            );
            log.info(">>> [Post Product Service] 2 Post product info: {}", foundPostProduct);
            return foundPostProduct;
        } catch (Exception e) {
            throw e;
        }
    }

    public PostProduct checkPostProductVerification(PostProductDecisionRequest request) throws Exception {
        try {
            log.info(">>> request: {}", request);
            Admin admin = adminService.getCurrentUser();
            log.info(">>> admin id: {}", admin.getId());
            PostProduct postProduct = postProductRepository.findById(
                    request.getPostProductId()).orElseThrow(() -> new PostProductNotFound()
            );

            if (!request.getPassed()) {
                postProduct.setVerifiedDecisionstatus(VerifiedDecisionStatus.REJECTED);
                postProduct.setVerified(false);
                postProduct.setAdmin(admin);
                postProduct.setRejectedReason(request.getRejectedReason());
            } else {
                postProduct.setVerifiedDecisionstatus(VerifiedDecisionStatus.APPROVED);
                postProduct.setVerified(true);
                postProduct.setAdmin(admin);
                postProduct.setRejectedReason("");
            }
            postProductRepository.save(postProduct);

            return postProductRepository.save(postProduct);
        } catch (Exception e) {
            log.info(">>> Error at decidePostContentValidation: {}", e.getMessage());
            throw e;
        }
    }

    public PostProduct postProductVerifiedRequest(VerifiedPostProductRequest request) throws Exception {
        PostProduct postProduct = postProductRepository.findById(request.getPostId()).orElseThrow(() -> new Exception("Post is not existed"));
        Long sellerId = postProduct.getSeller().getSellerId();
        if (subscriptionService.isServicePackageExpired(sellerId)) {
            throw new SubscriptionExpiredException();
        }
        postProduct.setVerifiedDecisionstatus(VerifiedDecisionStatus.PENDING);
        return postProductRepository.save(postProduct);
    }

    public PostProduct updateSoldStatus(boolean status, PostProduct postProduct) {
        postProduct.setSold(status);
        return postProductRepository.save(postProduct);
    }

    public PostProduct findPostProductById(Long id) {
        log.info(">>> [Post Product Service] Find post product by id: Started.");
        PostProduct foundPostProduct = null;
        Optional<PostProduct> postProductOpt = postProductRepository.findById(id);
        if (postProductOpt.isPresent()) {
            foundPostProduct = postProductOpt.get();
        }
        log.info(">>> [Post Product Service] Post product info: {}", foundPostProduct);
        return foundPostProduct;
    }

    public Page<PostProduct> getAllPostBySeller(Seller seller, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postProductRepository.findBySeller(seller, pageable);
    }

    public PostProduct hidePostProduct(Long id, boolean isHide) {
        PostProduct selected = postProductRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Post product id does not existed.")
        );

        selected.setActive(!isHide);
        return postProductRepository.save(selected);
    }

    public PostProduct findPostByWishId(long id) {
        return wishListingRepository.findPostProductByWishListId(id).orElseThrow(
                () -> new IllegalArgumentException("Can not find post product with this wish-list id: " + id)
        );
    }

    public PostProduct updatePostProduct(
            PostProduct postProduct,
            UpdatePostProductRequest request,
            List<MultipartFile> files
    ) throws Exception {
        try {
            postProduct.setTitle(request.getTitle());
            postProduct.setBrand(request.getBrand());
            postProduct.setModel(request.getModel());
            postProduct.setManufactureYear(request.getManufactureYear());
            postProduct.setUsedDuration(request.getUsedDuration());
            postProduct.setConditionLevel(request.getConditionLevel());
            postProduct.setPrice(request.getPrice());
            postProduct.setWidth(request.getWidth());
            postProduct.setHeight(request.getHeight());
            postProduct.setLength((request.getLength()));
            postProduct.setWeight(request.getWeight());
            postProduct.setDescription(request.getDescription());
            postProduct.setLocationTrading(request.getLocationTrading());


            postProduct.setVerified(false);
            postProduct.setVerifiedDecisionstatus(VerifiedDecisionStatus.PENDING);

            List<ProductImage> productImages = productImageRepository.findAllByPostProduct(postProduct);

            for (int i = 0; i <= productImages.size() - 1; i++) {
                String imagePublicId = productImages.get(i).getImagePublicId();
                String folder = "PostImages/" + postProduct.getId() + ":" + postProduct.getSeller().getBuyer().getUsername() + "/product_image_" + i;
                if (
                        (!(imagePublicId == null)) && (!imagePublicId.equalsIgnoreCase(""))
                ) {
                    boolean isDeleted = cloudinaryService.delete(imagePublicId, folder);

                    if (!isDeleted) {
                        throw new Exception("Delete product image failed");
                    }
                }
            }

            productImageRepository.deleteAllInBatch(productImages);

            if (files != null && files.size() > 0) {
                log.info(">>> files data: {}", files);
//                files.forEach((file) -> {
//                    fileUtils.validateFile(file);
//                    log.info(">>> Checked File name: {}", file.toString());
//                });

                for (int i = 0; i <= files.size() - 1; i++) {
                    Map<String, String> uploadResult = cloudinaryService.upload(files.get(i), "PostImages/" + postProduct.getId() + ":" + postProduct.getSeller().getBuyer().getUsername() + "/product_image_" + i);
                    String imageUrl = uploadResult.get("fileUrl");
                    String imagePublicId = uploadResult.get("publicId");
                    log.info(">>> Passed uploaded picture {}", i);
                    ProductImage productImage = ProductImage.builder()
                            .imageUrl(imageUrl)
                            .imagePublicId(imagePublicId)
                            .orderImage((long) i + 1)
                            .postProduct(postProduct)
                            .build();
                    productImageRepository.save(productImage);
                }
                log.info(">>> Passed uploaded file");
            }
            return postProductRepository.save(postProduct);
        } catch (Exception e) {
            log.info(">>> [PostProductServiceImpl] error at updatePostProduct: {}", e.getMessage());
            throw e;
        }
    }

    public Seller findSellerByPostId(Long id) {
        PostProduct postProduct = postProductRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can not find post product with id: " + id)
        );
        return postProduct.getSeller();
    }
}
