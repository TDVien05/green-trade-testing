package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.Subscription;
import Green_trade.green_trade_platform.repository.SubscriptionRepository;
import Green_trade.green_trade_platform.service.implement.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private Subscription subscription;

    @BeforeEach
    void setUp() {
        subscription = new Subscription();
    }

    @Test
    void shouldReturnTrueWhenSubscriptionIsExpired() throws Exception {
        LocalDateTime end = LocalDateTime.now().minusDays(1);
        subscription.setEndDay(end);
        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(1L))
                .thenReturn(Optional.of(subscription));

        boolean result = subscriptionService.isServicePackageExpired(1L);

        assertTrue(result);
        verify(subscriptionRepository).findFirstBySeller_SellerIdOrderByEndDayDesc(1L);
    }

    @Test
    void shouldReturnFalseWhenSubscriptionIsActive() throws Exception {
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        subscription.setEndDay(end);
        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(2L))
                .thenReturn(Optional.of(subscription));

        boolean result = subscriptionService.isServicePackageExpired(2L);

        assertFalse(result);
        verify(subscriptionRepository).findFirstBySeller_SellerIdOrderByEndDayDesc(2L);
    }

    @Test
    void shouldThrowWhenNoSubscriptionFound() {
        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(4L))
                .thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> subscriptionService.isServicePackageExpired(4L));
        assertTrue(ex.getMessage().contains("Seller doesn't subscribe any service"));
        verify(subscriptionRepository).findFirstBySeller_SellerIdOrderByEndDayDesc(4L);
    }

    @Test
    void shouldHandleVeryLargeSellerId() throws Exception {
        long veryLargeId = Long.MAX_VALUE;
        LocalDateTime end = LocalDateTime.now().plusYears(10);
        subscription.setEndDay(end);
        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(veryLargeId))
                .thenReturn(Optional.of(subscription));

        boolean result = subscriptionService.isServicePackageExpired(veryLargeId);

        assertFalse(result);
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(subscriptionRepository).findFirstBySeller_SellerIdOrderByEndDayDesc(captor.capture());
        assertEquals(veryLargeId, captor.getValue());
    }

    @Test
    void shouldReturnTrueForJustExpiredSubscription() throws Exception {
        LocalDateTime end = LocalDateTime.now().minusNanos(1);
        subscription.setEndDay(end);
        when(subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(5L))
                .thenReturn(Optional.of(subscription));

        boolean result = subscriptionService.isServicePackageExpired(5L);

        assertTrue(result);
        verify(subscriptionRepository).findFirstBySeller_SellerIdOrderByEndDayDesc(5L);
    }
}
