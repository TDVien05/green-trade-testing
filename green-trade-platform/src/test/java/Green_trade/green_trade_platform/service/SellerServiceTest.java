package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.SellerStatus;
import Green_trade.green_trade_platform.enumerate.VerifiedDecisionStatus;
import Green_trade.green_trade_platform.exception.ProfileException;
import Green_trade.green_trade_platform.exception.SubscriptionExpiredException;
import Green_trade.green_trade_platform.mapper.RegisterShopShippingServiceMapper;
import Green_trade.green_trade_platform.mapper.SellerMapper;
import Green_trade.green_trade_platform.mapper.SubscriptionMapper;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.*;
import Green_trade.green_trade_platform.request.ApproveSellerRequest;
import Green_trade.green_trade_platform.request.MailRequest;
import Green_trade.green_trade_platform.response.ApproveSellerResponse;
import Green_trade.green_trade_platform.response.SellerResponse;
import Green_trade.green_trade_platform.response.SubscriptionResponse;
import Green_trade.green_trade_platform.service.implement.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class SellerServiceTest {

    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private SellerMapper sellerMapper;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionMapper subscriptionMapper;
    @Mock
    private AdminServiceImpl adminService;
    @Mock
    private BuyerServiceImpl buyerService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private GhnServiceImpl ghnService;
    @Mock
    private RegisterShopShippingServiceMapper registerShopShippingServiceMapper;
    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private PostProductRepository postProductRepository;
    @Mock
    private MailServiceImpl mailSender;

    @InjectMocks
    private SellerServiceImpl sellerService;

    @Test
    @DisplayName("Handle pending seller APPROVED successfully")
    void testHandlePendingSeller_Approved() throws Exception {
        log.info("=== START testHandlePendingSeller_Approved ===");

        Seller seller = Seller.builder()
                .sellerId(1L)
                .buyer(Buyer.builder().email("user@example.com").build())
                .status(SellerStatus.PENDING)
                .build();

        when(sellerRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(adminService.getCurrentUser()).thenReturn(Admin.builder().id(99L).build());
        when(registerShopShippingServiceMapper.toDto(any())).thenReturn(Map.of("shop_id", 999));
        when(ghnService.registerShop(any())).thenReturn("{\"data\":{\"shop_id\":123}}");
        when(sellerRepository.save(any(Seller.class))).thenReturn(seller);

        ApproveSellerRequest req = ApproveSellerRequest.builder()
                .sellerId(1L)
                .decision(VerifiedDecisionStatus.APPROVED)
                .message("Approved successfully")
                .build();

        log.info("Calling handlePendingSeller() with APPROVED request...");
        ApproveSellerResponse res = sellerService.handlePendingSeller(req);
        log.info("Response received: {}", res);

        assertNotNull(res);
        assertEquals(VerifiedDecisionStatus.APPROVED, res.getDecision());
        assertEquals("Approved successfully", res.getReason());
        assertNotNull(res.getNotification());

        verify(sellerRepository, atLeastOnce()).save(any(Seller.class));
        verify(notificationRepository).save(any(Notification.class));
        verify(mailSender).sendBeautifulMail(any(MailRequest.class));

        log.info("=== END testHandlePendingSeller_Approved ===");
    }

    @Test
    @DisplayName("Handle pending seller REJECTED should delete and notify")
    void testHandlePendingSeller_Rejected() throws Exception {
        log.info("=== START testHandlePendingSeller_Rejected ===");

        Seller seller = Seller.builder()
                .sellerId(2L)
                .buyer(Buyer.builder().buyerId(10L).email("reject@example.com").build())
                .status(SellerStatus.PENDING)
                .build();

        when(sellerRepository.findById(2L)).thenReturn(Optional.of(seller));

        ApproveSellerRequest req = ApproveSellerRequest.builder()
                .sellerId(2L)
                .decision(VerifiedDecisionStatus.REJECTED)
                .message("Documents invalid")
                .build();

        log.info("Calling handlePendingSeller() with REJECTED request...");
        ApproveSellerResponse res = sellerService.handlePendingSeller(req);
        log.info("Response received: {}", res);

        assertNotNull(res);
        assertEquals("Documents invalid", res.getReason());
        assertEquals(VerifiedDecisionStatus.REJECTED, res.getDecision());

        verify(sellerRepository).delete(seller);
        verify(notificationRepository).save(any(Notification.class));
        verify(mailSender).sendBeautifulMail(any(MailRequest.class));

        log.info("=== END testHandlePendingSeller_Rejected ===");
    }

    @Test
    @DisplayName("Handle pending seller should throw when not found")
    void testHandlePendingSeller_NotFound() {
        log.info("=== START testHandlePendingSeller_NotFound ===");

        when(sellerRepository.findById(100L)).thenReturn(Optional.empty());

        ApproveSellerRequest req = ApproveSellerRequest.builder()
                .sellerId(100L)
                .decision(VerifiedDecisionStatus.APPROVED)
                .build();

        log.info("Calling handlePendingSeller() expecting ProfileException...");
        assertThrows(ProfileException.class, () -> sellerService.handlePendingSeller(req));

        log.info("=== END testHandlePendingSeller_NotFound ===");
    }

    @Test
    @DisplayName("Check service package valid and active")
    void testCheckServicePackageValidity_Success() throws Exception {
        log.info("=== START testCheckServicePackageValidity_Success ===");

        Buyer buyer = Buyer.builder().username("john").build();
        Seller seller = Seller.builder().sellerId(1L).buyer(buyer).build();
        SubscriptionPackages pkg = SubscriptionPackages.builder().name("Premium").build();
        Subscription sub = Subscription.builder()
                .endDay(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .remainPost(10)
                .subscriptionPackage(pkg)
                .build();

        when(buyerRepository.findByUsername("john")).thenReturn(Optional.of(buyer));
        when(sellerRepository.findByBuyer(buyer)).thenReturn(Optional.of(seller));
        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(1L))
                .thenReturn(Optional.of(sub));
        when(subscriptionMapper.toDto(true, sub.getEndDay(), pkg.getName()))
                .thenReturn(new SubscriptionResponse(true, sub.getEndDay(), pkg.getName()));

        log.info("Calling checkServicePackageValidity() for username: {}", buyer.getUsername());
        SubscriptionResponse res = sellerService.checkServicePackageValidity("john");
        log.info("Response: {}", res);

        assertTrue(res.isValid());
        assertEquals("Premium", res.getPackageName());
        verify(subscriptionRepository).findFirstBySeller_SellerIdOrderByEndDayDesc(1L);

        log.info("=== END testCheckServicePackageValidity_Success ===");
    }

    @Test
    @DisplayName("Throw SubscriptionExpiredException when service expired")
    void testCheckServicePackageValidity_Expired() {
        log.info("=== START testCheckServicePackageValidity_Expired ===");

        Buyer buyer = Buyer.builder().username("john").build();
        Seller seller = Seller.builder().sellerId(1L).buyer(buyer).build();
        Subscription expired = Subscription.builder()
                .endDay(LocalDateTime.now().minusDays(1))
                .isActive(false)
                .remainPost(0)
                .build();

        when(buyerRepository.findByUsername("john")).thenReturn(Optional.of(buyer));
        when(sellerRepository.findByBuyer(buyer)).thenReturn(Optional.of(seller));
        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(1L))
                .thenReturn(Optional.of(expired));

        log.info("Expect SubscriptionExpiredException due to expired package...");
        assertThrows(SubscriptionExpiredException.class,
                () -> sellerService.checkServicePackageValidity("john"));

        log.info("=== END testCheckServicePackageValidity_Expired ===");
    }

    @Test
    @DisplayName("Block seller account successfully and send mail")
    void testBlockAccount_Success() {
        log.info("=== START testBlockAccount_Success ===");

        Buyer buyer = Buyer.builder()
                .buyerId(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .isActive(true)
                .build();

        when(buyerRepository.findBySeller_SellerId(5L)).thenReturn(Optional.of(buyer));

        log.info("Calling blockAccount() for sellerId=5...");
        sellerService.blockAccount(5L, "Vi phạm điều khoản", "block");
        log.info("Account blocked for buyer: {}", buyer.getFullName());

        verify(buyerRepository).save(buyer);
        verify(mailSender).sendBeautifulMail(any(MailRequest.class));
        assertFalse(buyer.isActive());

        log.info("=== END testBlockAccount_Success ===");
    }

    @Test
    @DisplayName("Throw EntityNotFoundException when block non-existent seller")
    void testBlockAccount_NotFound() {
        log.info("=== START testBlockAccount_NotFound ===");

        when(buyerRepository.findBySeller_SellerId(10L)).thenReturn(Optional.empty());
        log.info("Expect EntityNotFoundException when blocking invalid seller...");
        assertThrows(EntityNotFoundException.class,
                () -> sellerService.blockAccount(10L, "Reason", "block"));

        log.info("=== END testBlockAccount_NotFound ===");
    }

    @Test
    @DisplayName("Get pending seller list successfully")
    void testGetAllPendingSeller() {
        log.info("=== START testGetAllPendingSeller ===");

        Seller s1 = Seller.builder().sellerId(1L).status(SellerStatus.PENDING).build();
        Seller s2 = Seller.builder().sellerId(2L).status(SellerStatus.PENDING).build();

        when(sellerRepository.findAllByStatus(eq(SellerStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(s1, s2)));

        log.info("Calling getAllPendingSeller()...");
        Page<SellerResponse> page = sellerService.getAllPendingSeller(0, 10);
        log.info("Received {} sellers", page.getContent().size());

        assertNotNull(page);
        assertEquals(2, page.getContent().size());
        verify(sellerRepository).findAllByStatus(eq(SellerStatus.PENDING), any(Pageable.class));

        log.info("=== END testGetAllPendingSeller ===");
    }

    @Test
    @DisplayName("Get list of post products by seller")
    void testGetListPostProduct() {
        log.info("=== START testGetListPostProduct ===");

        Seller seller = Seller.builder().sellerId(1L).build();
        PostProduct post1 = PostProduct.builder().id(1L).title("Product A").build();
        PostProduct post2 = PostProduct.builder().id(2L).title("Product B").build();

        when(postProductRepository.findAllBySeller(seller)).thenReturn(List.of(post1, post2));

        log.info("Calling getListPostProduct()...");
        List<PostProduct> result = sellerService.getListPostProduct(seller);
        log.info("Result size: {}", result.size());

        assertEquals(2, result.size());
        assertEquals("Product A", result.get(0).getTitle());
        verify(postProductRepository).findAllBySeller(seller);

        log.info("=== END testGetListPostProduct ===");
    }
}
