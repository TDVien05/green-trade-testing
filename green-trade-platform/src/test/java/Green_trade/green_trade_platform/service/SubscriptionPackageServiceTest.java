package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.mapper.SubscriptionMapper;
import Green_trade.green_trade_platform.mapper.SubscriptionPackageMapper;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.model.Subscription;
import Green_trade.green_trade_platform.model.SubscriptionPackages;
import Green_trade.green_trade_platform.repository.SellerRepository;
import Green_trade.green_trade_platform.repository.SubscriptionPackagesRepository;
import Green_trade.green_trade_platform.repository.SubscriptionRepository;
import Green_trade.green_trade_platform.request.SignPackageRequest;
import Green_trade.green_trade_platform.response.SignPackageResponse;
import Green_trade.green_trade_platform.response.SubscriptionPackageResponse;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import Green_trade.green_trade_platform.service.implement.SubscriptionPackageServiceImpl;
import Green_trade.green_trade_platform.service.implement.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SubscriptionPackageServiceTest {

    @Mock
    private SubscriptionPackagesRepository subscriptionPackagesRepository;

    @Mock
    private SubscriptionPackageMapper subscriptionPackageMapper;

    @Mock
    private BuyerServiceImpl buyerService;

    @Mock
    private WalletServiceImpl walletService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionPackageServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        // ✅ Create a spy so you can stub getCurrentSubscription()
        service = spy(new SubscriptionPackageServiceImpl(
                subscriptionPackagesRepository,
                subscriptionPackageMapper,
                buyerService,
                walletService,
                subscriptionRepository,
                sellerRepository,
                subscriptionMapper
        ));
    }

    @Test
    void shouldDecreaseRemainPostAndSaveSubscription() {
        // Arrange
        Seller seller = Seller.builder().sellerId(1L).build();
        Subscription subscription = Subscription.builder()
                .id(1L)
                .remainPost(5)
                .build();

        doReturn(subscription).when(service).getCurrentSubscription(seller);
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Subscription result = service.updateRemainPost(seller);

        // Assert
        verify(service).getCurrentSubscription(seller);
        verify(subscriptionRepository).save(subscription);

        assertEquals(4, result.getRemainPost());
    }

    @Test
    void shouldThrowExceptionWhenNoRemainingPosts() {
        // Arrange
        Seller seller = Seller.builder().sellerId(2L).build();
        Subscription subscription = Subscription.builder()
                .id(2L)
                .remainPost(0)
                .build();

        doReturn(subscription).when(service).getCurrentSubscription(seller);

        // Act + Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.updateRemainPost(seller));

        assertEquals("No remaining posts available for this subscription.", ex.getMessage());
        verify(service).getCurrentSubscription(seller);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void shouldHandleNegativeRemainPostGracefully() {
        // Arrange
        Seller seller = Seller.builder().sellerId(3L).build();
        Subscription subscription = Subscription.builder()
                .id(3L)
                .remainPost(-1)
                .build();

        doReturn(subscription).when(service).getCurrentSubscription(seller);

        // Act + Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.updateRemainPost(seller));

        assertTrue(ex.getMessage().contains("No remaining posts available"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void shouldSignPackageWhenNoActiveSubscriptionAndSufficientBalance() {
        // Arrange
        Buyer buyer = Buyer.builder().buyerId(1L).fullName("John Doe").build();
        Seller seller = Seller.builder().sellerId(10L).sellerName("Seller A").buyer(buyer).build();
        SubscriptionPackages pkg = SubscriptionPackages.builder()
                .id(100L)
                .name("Pro")
                .maxProduct(50L)
                .isActive(true)
                .build();

        SignPackageRequest request = SignPackageRequest.builder()
                .packageId(100L)
                .priceId(200L)
                .price(99.99)
                .durationByDay(30L)
                .build();

        when(buyerService.getCurrentUser()).thenReturn(buyer);
        when(sellerRepository.findByBuyer(buyer)).thenReturn(Optional.of(seller));
        when(subscriptionPackagesRepository.findById(100L)).thenReturn(Optional.of(pkg));
        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(10L)).thenReturn(Optional.empty());
        when(buyerService.getWalletBalance()).thenReturn(BigDecimal.valueOf(150.00));
        when(walletService.handleSignPackageForSeller(buyer, 99.99)).thenReturn(Map.of("success", true));
        // mock save to return subscription with id
        ArgumentCaptor<Subscription> subCaptor = ArgumentCaptor.forClass(Subscription.class);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(999L);
            s.setIsActive(true);
            return s;
        });
        when(subscriptionMapper.toSignPackageResponse(anyString(), anyString(), anyDouble(), anyLong(), any(), any()))
                .thenAnswer(invocation -> SignPackageResponse.builder()
                        .packageName(invocation.getArgument(0))
                        .fullName(invocation.getArgument(1))
                        .price(invocation.getArgument(2))
                        .durationByDay(invocation.getArgument(3))
                        .startDate(invocation.getArgument(4))
                        .endDate(invocation.getArgument(5))
                        .build());

        // Act
        Map<String, Object> result = service.handlesignPackage(request);

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("subscription"));
        SignPackageResponse resp = (SignPackageResponse) result.get("subscription");
        assertEquals("Pro", resp.getPackageName());
        assertEquals("John Doe", resp.getFullName());
        assertEquals(99.99, resp.getPrice(), 0.0001);
        assertEquals(30L, resp.getDurationByDay());

        verify(subscriptionRepository).save(subCaptor.capture());
        Subscription saved = subCaptor.getValue();
        assertEquals(seller, saved.getSeller());
        assertEquals(pkg, saved.getSubscriptionPackage());
        assertEquals(50L, saved.getRemainPost());
        assertNotNull(saved.getStartDay());
        assertNotNull(saved.getEndDay());
        assertEquals(saved.getStartDay().plusDays(30), saved.getEndDay());

        verify(walletService).handleSignPackageForSeller(buyer, 99.99);
    }

    @Test
    void shouldReturnActivePackageResponsesWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").ascending());
        SubscriptionPackages pkg1 = SubscriptionPackages.builder().id(1L).name("Basic").isActive(true).build();
        SubscriptionPackages pkg2 = SubscriptionPackages.builder().id(2L).name("Pro").isActive(true).build();
        Page<SubscriptionPackages> pkgPage = new PageImpl<>(List.of(pkg1, pkg2), pageable, 2);

        SubscriptionPackageResponse resp1 = SubscriptionPackageResponse.builder().id(1L).name("Basic").build();
        SubscriptionPackageResponse resp2 = SubscriptionPackageResponse.builder().id(2L).name("Pro").build();

        when(subscriptionPackagesRepository.findByIsActiveTrue(pageable)).thenReturn(pkgPage);
        when(subscriptionPackageMapper.toResponse(pkg1)).thenReturn(resp1);
        when(subscriptionPackageMapper.toResponse(pkg2)).thenReturn(resp2);

        // Act
        Page<SubscriptionPackageResponse> result = service.getActivePackageResponses(pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals("Basic", result.getContent().get(0).getName());
        assertEquals("Pro", result.getContent().get(1).getName());
        verify(subscriptionPackagesRepository).findByIsActiveTrue(pageable);
        verify(subscriptionPackageMapper, times(1)).toResponse(pkg1);
        verify(subscriptionPackageMapper, times(1)).toResponse(pkg2);
    }

    @Test
    void shouldCancelActiveSubscriptionAndPersistChanges() {
        // Arrange
        Seller seller = Seller.builder().sellerId(77L).build();
        Subscription active = Subscription.builder()
                .id(5L)
                .seller(seller)
                .isActive(true)
                .startDay(LocalDateTime.now().minusDays(10))
                .endDay(LocalDateTime.now().plusDays(20))
                .remainPost(10)
                .build();

        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(77L))
                .thenReturn(Optional.of(active));

        // Act
        service.cancelSubscription(seller);

        // Assert
        assertFalse(active.getIsActive());
        assertNotNull(active.getEndDay());
        verify(subscriptionRepository).save(active);
    }

    @Test
    void shouldThrowWhenSigningWithExistingActiveSubscription() {
        // Arrange
        Buyer buyer = Buyer.builder().buyerId(1L).fullName("John Doe").build();
        Seller seller = Seller.builder().sellerId(10L).buyer(buyer).build();
        SubscriptionPackages pkg = SubscriptionPackages.builder().id(100L).name("Pro").isActive(true).build();
        Subscription existing = Subscription.builder().id(9L).seller(seller).isActive(true).build();

        SignPackageRequest request = SignPackageRequest.builder()
                .packageId(100L)
                .price(50.0)
                .durationByDay(10L)
                .build();

        when(buyerService.getCurrentUser()).thenReturn(buyer);
        when(sellerRepository.findByBuyer(buyer)).thenReturn(Optional.of(seller));
        when(subscriptionPackagesRepository.findById(100L)).thenReturn(Optional.of(pkg));
        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(10L)).thenReturn(Optional.of(existing));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.handlesignPackage(request));
        assertTrue(ex.getMessage().contains("Bạn đã đăng kí gói"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void shouldReturnFailureWhenWalletBalanceInsufficient() {
        // Arrange
        Buyer buyer = Buyer.builder().buyerId(1L).fullName("John Doe").build();
        Seller seller = Seller.builder().sellerId(10L).buyer(buyer).build();
        SubscriptionPackages pkg = SubscriptionPackages.builder().id(100L).name("Pro").maxProduct(50L).isActive(true).build();

        SignPackageRequest request = SignPackageRequest.builder()
                .packageId(100L)
                .price(200.0)
                .durationByDay(30L)
                .build();

        when(buyerService.getCurrentUser()).thenReturn(buyer);
        when(sellerRepository.findByBuyer(buyer)).thenReturn(Optional.of(seller));
        when(subscriptionPackagesRepository.findById(100L)).thenReturn(Optional.of(pkg));
        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(10L)).thenReturn(Optional.empty());
        when(buyerService.getWalletBalance()).thenReturn(BigDecimal.valueOf(50.0));

        // Act
        Map<String, Object> result = service.handlesignPackage(request);

        // Assert
        assertFalse((Boolean) result.get("success"));
        assertNull(result.get("data"));
        verify(walletService, never()).handleSignPackageForSeller(any(), anyDouble());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCurrentSubscriptionIsInactive() {
        // Arrange
        Seller seller = Seller.builder().sellerId(33L).build();
        Subscription last = Subscription.builder()
                .id(3L)
                .seller(seller)
                .isActive(false)
                .startDay(LocalDateTime.now().minusDays(40))
                .endDay(LocalDateTime.now().minusDays(10))
                .remainPost(0)
                .build();

        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(33L))
                .thenReturn(Optional.of(last));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.getCurrentSubscription(seller));
        assertTrue(ex.getMessage().contains("out of date"));
    }
}
