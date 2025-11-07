package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.VerifiedDecisionStatus;
import Green_trade.green_trade_platform.exception.ImageUploadLimitExceededException;
import Green_trade.green_trade_platform.exception.ProfileException;
import Green_trade.green_trade_platform.exception.SubscriptionExpiredException;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.*;
import Green_trade.green_trade_platform.request.PostProductDecisionRequest;
import Green_trade.green_trade_platform.request.UpdatePostProductRequest;
import Green_trade.green_trade_platform.request.UploadPostProductRequest;
import Green_trade.green_trade_platform.request.VerifiedPostProductRequest;
import Green_trade.green_trade_platform.service.implement.AdminServiceImpl;
import Green_trade.green_trade_platform.service.implement.CloudinaryService;
import Green_trade.green_trade_platform.service.implement.PostProductServiceImpl;
import Green_trade.green_trade_platform.service.implement.SubscriptionServiceImpl;
import Green_trade.green_trade_platform.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PostProductServiceTest {

    @Mock
    private PostProductRepository postProductRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AdminServiceImpl adminService;
    @Mock
    private FileUtils fileUtils;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private SubscriptionServiceImpl subscriptionService;
    @Mock
    private WishListingRepository wishListingRepository;

    @InjectMocks
    private PostProductServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private Seller buildSellerWithBuyer(Long sellerId, String username) {
        Buyer buyer = Buyer.builder()
                .buyerId(10L)
                .username(username)
                .build();
        return Seller.builder()
                .sellerId(sellerId)
                .buyer(buyer)
                .storeName("Store")
                .identityFrontImageUrl("f")
                .identityBackImageUrl("b")
                .businessLicenseUrl("l")
                .selfieUrl("s")
                .storePolicyUrl("p")
                .taxNumber("t")
                .identityNumber("i")
                .sellerName("name")
                .nationality("nat")
                .home("home")
                .build();
    }

    private Subscription buildActiveSubscription(Seller seller, long maxImgPerPost) {
        SubscriptionPackages pkg = SubscriptionPackages.builder()
                .id(1L)
                .maxImgPerPost(maxImgPerPost)
                .build();
        return Subscription.builder()
                .id(100L)
                .seller(seller)
                .subscriptionPackage(pkg)
                .startDay(LocalDateTime.now().minusDays(1))
                .endDay(LocalDateTime.now().plusDays(1))
                .remainPost(5)
                .isActive(true)
                .build();
    }

    @Test
    void shouldVerifyPostWhenSubscriptionValid() throws Exception {
        Seller seller = Seller.builder().sellerId(1L).build();
        PostProduct post = PostProduct.builder().id(10L).seller(seller).build();

        when(postProductRepository.findById(10L)).thenReturn(Optional.of(post));
        when(subscriptionService.isServicePackageExpired(1L)).thenReturn(false);
        when(postProductRepository.save(any(PostProduct.class))).thenAnswer(i -> i.getArgument(0));

        VerifiedPostProductRequest req = new VerifiedPostProductRequest(10L);
        PostProduct result = service.postProductVerifiedRequest(req);

        assertEquals(VerifiedDecisionStatus.PENDING, result.getVerifiedDecisionstatus());
        verify(postProductRepository).save(post);
    }

    @Test
    void shouldThrowWhenPostNotExists() {
        when(postProductRepository.findById(99L)).thenReturn(Optional.empty());
        VerifiedPostProductRequest req = new VerifiedPostProductRequest(99L);

        Exception ex = assertThrows(Exception.class, () -> service.postProductVerifiedRequest(req));
        assertTrue(ex.getMessage().contains("Post"));
        verify(postProductRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSubscriptionExpired() throws Exception {
        Seller seller = Seller.builder().sellerId(2L).build();
        PostProduct post = PostProduct.builder().id(5L).seller(seller).build();

        when(postProductRepository.findById(5L)).thenReturn(Optional.of(post));
        when(subscriptionService.isServicePackageExpired(2L)).thenReturn(true);

        VerifiedPostProductRequest req = new VerifiedPostProductRequest(5L);
        assertThrows(SubscriptionExpiredException.class, () -> service.postProductVerifiedRequest(req));

        verify(postProductRepository, never()).save(any());
    }

    // ✅ updateSoldStatus

    @Test
    void shouldUpdateSoldStatusAndSave() {
        PostProduct post = PostProduct.builder().id(7L).sold(false).build();
        when(postProductRepository.save(any(PostProduct.class))).thenAnswer(i -> i.getArgument(0));

        PostProduct result = service.updateSoldStatus(true, post);

        assertTrue(result.isSold());
        verify(postProductRepository).save(post);
    }

    // ✅ findPostProductById

    @Test
    void shouldReturnPostWhenExists() {
        PostProduct post = PostProduct.builder().id(10L).build();
        when(postProductRepository.findById(10L)).thenReturn(Optional.of(post));

        PostProduct result = service.findPostProductById(10L);
        assertNotNull(result);
        assertEquals(post, result);
    }

    @Test
    void shouldReturnNullWhenPostNotFound() {
        when(postProductRepository.findById(15L)).thenReturn(Optional.empty());
        assertNull(service.findPostProductById(15L));
    }

    // ✅ getAllPostBySeller

    @Test
    void shouldReturnPagedPostsBySeller() {
        Seller seller = Seller.builder().sellerId(5L).build();
        Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());
        PostProduct post = PostProduct.builder().id(3L).seller(seller).build();
        Page<PostProduct> page = new PageImpl<>(List.of(post), pageable, 1);

        when(postProductRepository.findBySeller(seller, pageable)).thenReturn(page);

        Page<PostProduct> result = service.getAllPostBySeller(seller, 0, 2);
        assertEquals(1, result.getTotalElements());
        verify(postProductRepository).findBySeller(seller, pageable);
    }

    // ✅ hidePostProduct

    @Test
    void shouldHidePostWhenIsHideTrue() {
        PostProduct post = PostProduct.builder().id(1L).active(true).build();
        when(postProductRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postProductRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PostProduct result = service.hidePostProduct(1L, true);

        assertFalse(result.isActive());
        verify(postProductRepository).save(post);
    }

    @Test
    void shouldUnhidePostWhenIsHideFalse() {
        PostProduct post = PostProduct.builder().id(1L).active(false).build();
        when(postProductRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postProductRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PostProduct result = service.hidePostProduct(1L, false);

        assertTrue(result.isActive());
        verify(postProductRepository).save(post);
    }

    @Test
    void shouldThrowWhenPostNotFoundForHide() {
        when(postProductRepository.findById(77L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.hidePostProduct(77L, true));
    }

    // ✅ findPostByWishId

    @Test
    void shouldReturnPostByWishId() {
        PostProduct post = PostProduct.builder().id(11L).build();
        when(wishListingRepository.findPostProductByWishListId(1L)).thenReturn(Optional.of(post));

        PostProduct result = service.findPostByWishId(1L);
        assertEquals(post, result);
    }

    @Test
    void shouldThrowWhenWishPostNotFound() {
        when(wishListingRepository.findPostProductByWishListId(2L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.findPostByWishId(2L));
    }

    @Test
    void shouldThrowWhenPostNotFoundOnUpload() {
        when(postProductRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ProfileException.class, () -> service.uploadPostProductPicture(99L, List.of()));
    }

    @Test
    public void shouldCreateNewPostProductAndUploadImages() throws Exception {
        // Arrange
        Long sellerId = 1L;
        Seller seller = buildSellerWithBuyer(sellerId, "john");
        Category category = Category.builder().id(2).name("Cat").description("desc").build();
        Subscription subscription = buildActiveSubscription(seller, 3);

        UploadPostProductRequest req = UploadPostProductRequest.builder()
                .sellerId(sellerId)
                .categoryId(2L)
                .title("Title")
                .brand("Brand")
                .model("Model")
                .manufactureYear(2020L)
                .usedDuration("1 year")
                .conditionLevel("Good")
                .price(BigDecimal.valueOf(100))
                .length("10")
                .width("5")
                .height("3")
                .weight("2")
                .description("Desc")
                .locationTrading("HN")
                .build();

        MultipartFile f1 = mock(MultipartFile.class);
        MultipartFile f2 = mock(MultipartFile.class);
        List<MultipartFile> files = Arrays.asList(f1, f2);

        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(sellerId))
                .thenReturn(Optional.of(subscription));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));

        PostProduct savedAfterBuild = PostProduct.builder().id(99L).seller(seller).category(category).build();
        when(postProductRepository.save(any(PostProduct.class))).thenReturn(savedAfterBuild);

        Map<String, String> uploadResult = new HashMap<>();
        uploadResult.put("fileUrl", "http://img");
        uploadResult.put("publicId", "pub");
        when(cloudinaryService.upload(any(MultipartFile.class), anyString())).thenReturn(uploadResult);

        // Act
        PostProduct result = service.createNewPostProduct(req, files);

        // Assert
        assertNotNull(result);
        assertEquals(99L, result.getId());
        verify(fileUtils, times(2)).validateFile(any(MultipartFile.class));
        verify(postProductRepository, times(1)).save(any(PostProduct.class));
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
        verify(cloudinaryService, times(2)).upload(any(MultipartFile.class), contains("PostImages/99"));
    }

    @Test
    public void shouldReturnPagedActiveUnsoldProductsSorted() {
        // Arrange
        int page = 0, size = 2;
        String sortedBy = "createdAt";
        boolean isAsc = false;

        PostProduct p1 = PostProduct.builder().id(1L).build();
        PostProduct p2 = PostProduct.builder().id(2L).build();
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortedBy).descending());
        Page<PostProduct> repoPage = new PageImpl<>(Arrays.asList(p1, p2), pageable, 10);
        when(postProductRepository.findAllBySoldFalseAndActiveTrue(any(Pageable.class))).thenReturn(repoPage);

        // Act
        Page<PostProduct> result = service.getAllProductPaging(page, size, sortedBy, isAsc);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals(10, result.getTotalElements());
        verify(postProductRepository, times(1)).findAllBySoldFalseAndActiveTrue(any(Pageable.class));
    }

    @Test
    public void shouldApprovePostProductAndPersistDecision() throws Exception {
        // Arrange
        Long postId = 5L;
        Admin admin = Admin.builder().id(7L).build();
        PostProduct post = PostProduct.builder().id(postId).verified(false).verifiedDecisionstatus(VerifiedDecisionStatus.PENDING).build();

        when(adminService.getCurrentUser()).thenReturn(admin);
        when(postProductRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postProductRepository.save(any(PostProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        PostProductDecisionRequest request = PostProductDecisionRequest.builder()
                .postProductId(postId)
                .passed(true)
                .rejectedReason("should be cleared")
                .build();

        // Act
        PostProduct result = service.checkPostProductVerification(request);

        // Assert
        assertTrue(result.isVerified());
        assertEquals(VerifiedDecisionStatus.APPROVED, result.getVerifiedDecisionstatus());
        assertEquals("", result.getRejectedReason());
        assertEquals(7L, result.getAdmin().getId());
        verify(postProductRepository, times(2)).save(any(PostProduct.class));
    }

    @Test
    public void shouldThrowWhenSubscriptionExpiredOnCreate() throws Exception {
        // Arrange
        Long sellerId = 1L;
        Seller seller = buildSellerWithBuyer(sellerId, "john");
        Category category = Category.builder().id(2).name("Cat").description("desc").build();

        Subscription activeButExpired = buildActiveSubscription(seller, 3);
        activeButExpired.setEndDay(LocalDateTime.now().minusDays(1));

        UploadPostProductRequest req = UploadPostProductRequest.builder()
                .sellerId(sellerId)
                .categoryId(2L)
                .title("Title")
                .brand("Brand")
                .model("Model")
                .manufactureYear(2020L)
                .usedDuration("1 year")
                .conditionLevel("Good")
                .price(BigDecimal.valueOf(100))
                .description("Desc")
                .locationTrading("HN")
                .build();

        List<MultipartFile> files = Collections.singletonList(mock(MultipartFile.class));

        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(sellerId))
                .thenReturn(Optional.of(activeButExpired));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));

        // Act & Assert
        assertThrows(SubscriptionExpiredException.class, () -> service.createNewPostProduct(req, files));
        verify(postProductRepository, never()).save(any());
    }

    @Test
    public void shouldThrowWhenImageCountExceedsSubscriptionLimit() throws Exception {
        // Arrange
        Long sellerId = 1L;
        Seller seller = buildSellerWithBuyer(sellerId, "john");
        Category category = Category.builder().id(2).name("Cat").description("desc").build();
        Subscription subscription = buildActiveSubscription(seller, 1); // max 1

        UploadPostProductRequest req = UploadPostProductRequest.builder()
                .sellerId(sellerId)
                .categoryId(2L)
                .title("Title")
                .brand("Brand")
                .model("Model")
                .manufactureYear(2020L)
                .usedDuration("1 year")
                .conditionLevel("Good")
                .price(BigDecimal.valueOf(100))
                .description("Desc")
                .locationTrading("HN")
                .build();

        List<MultipartFile> files = Arrays.asList(mock(MultipartFile.class), mock(MultipartFile.class)); // 2 > 1

        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(sellerId))
                .thenReturn(Optional.of(subscription));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));

        // Act & Assert
        assertThrows(ImageUploadLimitExceededException.class, () -> service.createNewPostProduct(req, files));
        verify(postProductRepository, never()).save(any());
        verify(productImageRepository, never()).save(any());
    }

    @Test
    public void shouldReplaceImagesDeletingOldAndUploadingNew() throws Exception {
        // Arrange
        Seller seller = buildSellerWithBuyer(1L, "john");
        PostProduct post = PostProduct.builder()
                .id(11L)
                .seller(seller)
                .verified(true)
                .verifiedDecisionstatus(VerifiedDecisionStatus.APPROVED)
                .build();

        ProductImage old1 = ProductImage.builder().imageId(1L).imagePublicId("old_pub_1").orderImage(1L).postProduct(post).build();
        ProductImage old2 = ProductImage.builder().imageId(2L).imagePublicId("old_pub_2").orderImage(2L).postProduct(post).build();
        when(productImageRepository.findAllByPostProduct(post)).thenReturn(Arrays.asList(old1, old2));
        when(cloudinaryService.delete(eq("old_pub_1"), anyString())).thenReturn(true);
        when(cloudinaryService.delete(eq("old_pub_2"), anyString())).thenReturn(true);

        MultipartFile nf1 = mock(MultipartFile.class);
        MultipartFile nf2 = mock(MultipartFile.class);
        List<MultipartFile> newFiles = Arrays.asList(nf1, nf2);
        Map<String, String> upRes1 = Map.of("fileUrl", "http://new1", "publicId", "pub1");
        Map<String, String> upRes2 = Map.of("fileUrl", "http://new2", "publicId", "pub2");
        when(cloudinaryService.upload(any(MultipartFile.class), contains("PostImages/11"))).thenReturn(upRes1, upRes2);

        when(postProductRepository.save(any(PostProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePostProductRequest request = UpdatePostProductRequest.builder()
                .title("T")
                .brand("B")
                .model("M")
                .manufactureYear(2021L)
                .usedDuration("2y")
                .conditionLevel("Great")
                .price(BigDecimal.TEN)
                .width("1")
                .height("2")
                .length("3")
                .weight("4")
                .description("D")
                .locationTrading("HN")
                .build();

        // Act
        PostProduct result = service.updatePostProduct(post, request, newFiles);

        // Assert
        assertNotNull(result);
        assertFalse(result.isVerified());
        assertEquals(VerifiedDecisionStatus.PENDING, result.getVerifiedDecisionstatus());
        verify(cloudinaryService, times(2)).delete(anyString(), contains("PostImages/11"));
        verify(productImageRepository, times(1)).deleteAllInBatch(anyList());
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
        verify(cloudinaryService, times(2)).upload(any(MultipartFile.class), contains("PostImages/11"));
        verify(postProductRepository, times(1)).save(post);
    }
}
