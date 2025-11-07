package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.AccountType;
import Green_trade.green_trade_platform.model.Notification;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.repository.NotificationRepository;
import Green_trade.green_trade_platform.service.implement.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Seller seller;

    @BeforeEach
    void setup() {
        seller = Seller.builder()
                .sellerId(42L)
                .build();
    }

    @Test
    void createsSellerNotificationWithExpectedFieldsAndSaves() {
        // Arrange
        String title = "Welcome";
        String content = "Your store is approved";
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        Notification saved = Notification.builder()
                .notificationId(1L)
                .receiverId(42L)
                .type(AccountType.SELLER)
                .title(title)
                .content(content)
                .readAt(null)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        // Act
        Notification result = notificationService.createNotificationForSeller(seller, title, content);

        // Assert
        verify(notificationRepository, times(1)).save(captor.capture());
        Notification toSave = captor.getValue();
        assertEquals(42L, toSave.getReceiverId());
        assertEquals(AccountType.SELLER, toSave.getType());
        assertEquals(title, toSave.getTitle());
        assertEquals(content, toSave.getContent());
        assertNull(toSave.getReadAt());
        assertEquals(saved, result);
    }

    @Test
    void returnsNotificationsOrderedByCreatedAtDesc() {
        // Arrange
        Long userId = 100L;
        Notification n1 = Notification.builder().notificationId(1L).createdAt(LocalDateTime.now()).build();
        Notification n2 = Notification.builder().notificationId(2L).createdAt(LocalDateTime.now().minusMinutes(1)).build();
        List<Notification> ordered = Arrays.asList(n1, n2);
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId)).thenReturn(ordered);

        // Act
        List<Notification> result = notificationService.getNotifications(userId);

        // Assert
        verify(notificationRepository, times(1)).findByReceiverIdOrderByCreatedAtDesc(userId);
        assertEquals(ordered, result);
    }

    @Test
    void marksNotificationAsReadAndPersists() {
        // Arrange
        Long notificationId = 5L;
        Notification existing = Notification.builder()
                .notificationId(notificationId)
                .readAt(null)
                .build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        notificationService.markAsRead(notificationId);

        // Assert
        assertNotNull(existing.getReadAt());
        verify(notificationRepository, times(1)).findById(notificationId);
        verify(notificationRepository, times(1)).save(existing);
    }

    @Test
    void markAsReadThrowsWhenNotificationNotFound() {
        // Arrange
        Long notificationId = 999L;
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> notificationService.markAsRead(notificationId));
        verify(notificationRepository, times(1)).findById(notificationId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createSellerNotificationPropagatesRepositorySaveException() {
        // Arrange
        String title = "Alert";
        String content = "Something happened";
        when(notificationRepository.save(any(Notification.class))).thenThrow(new RuntimeException("DB error"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> notificationService.createNotificationForSeller(seller, title, content));
        assertEquals("DB error", ex.getMessage());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void getNotificationsReturnsEmptyListWhenNoneFound() {
        // Arrange
        Long userId = 55L;
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId)).thenReturn(Collections.emptyList());

        // Act
        List<Notification> result = notificationService.getNotifications(userId);

        // Assert
        verify(notificationRepository, times(1)).findByReceiverIdOrderByCreatedAtDesc(userId);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
