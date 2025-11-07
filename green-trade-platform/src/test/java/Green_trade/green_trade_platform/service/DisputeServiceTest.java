package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.AccountType;
import Green_trade.green_trade_platform.enumerate.DisputeDecision;
import Green_trade.green_trade_platform.enumerate.DisputeStatus;
import Green_trade.green_trade_platform.enumerate.OrderStatus;
import Green_trade.green_trade_platform.enumerate.ResolutionType;
import Green_trade.green_trade_platform.mapper.DisputeMapper;
import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Dispute;
import Green_trade.green_trade_platform.model.DisputeCategory;
import Green_trade.green_trade_platform.model.Evidence;
import Green_trade.green_trade_platform.model.Notification;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.repository.DisputeCategoryRepository;
import Green_trade.green_trade_platform.repository.DisputeRepository;
import Green_trade.green_trade_platform.repository.NotificationRepository;
import Green_trade.green_trade_platform.repository.OrderRepository;
import Green_trade.green_trade_platform.request.RaiseDisputeRequest;
import Green_trade.green_trade_platform.request.ResolveDisputeRequest;
import Green_trade.green_trade_platform.response.DisputeResponse;
import Green_trade.green_trade_platform.service.implement.DisputeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DisputeServiceTest {

    @Mock
    private DisputeCategoryRepository disputeCategoryRepository;
    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private DisputeMapper disputeMapper;
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private DisputeServiceImpl disputeService;

    private Order buildOrder(Long id, OrderStatus status, long buyerId) {
        Buyer buyer = new Buyer();
        buyer.setBuyerId(buyerId);
        return Order.builder()
                .id(id)
                .orderCode("ORD-001")
                .price(BigDecimal.TEN)
                .shippingFee(BigDecimal.ONE)
                .status(status)
                .buyer(buyer)
                .build();
    }

    private DisputeCategory buildCategory(Long id) {
        return DisputeCategory.builder()
                .id(id)
                .title("Late delivery")
                .reason("Delay")
                .description("Delivery was late")
                .build();
    }

    @Test
    void shouldReturnDisputeWhenExists() {
        // Arrange
        Dispute dispute = Dispute.builder()
                .id(1L)
                .description("Product arrived late")
                .build();

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        // Act
        Dispute result = disputeService.getDisputeInfo(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(disputeRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenDisputeNotFound() {
        // Arrange
        when(disputeRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> disputeService.getDisputeInfo(99L)
        );

        assertTrue(ex.getMessage().contains("Can not find dispute infor with this id"));
        verify(disputeRepository).findById(99L);
    }

    @Test
    public void shouldReceiveDisputeForCompletedOrderAndSave() throws Exception {
        // Arrange
        Long orderId = 100L;
        Long categoryId = 10L;
        RaiseDisputeRequest request = RaiseDisputeRequest.builder()
                .orderId(orderId)
                .disputeCategoryId(categoryId)
                .description("Item damaged")
                .build();

        Order completedOrder = buildOrder(orderId, OrderStatus.COMPLETED, 1L);
        DisputeCategory category = buildCategory(categoryId);

        when(disputeCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));

        ArgumentCaptor<Dispute> disputeCaptor = ArgumentCaptor.forClass(Dispute.class);
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
            Dispute d = invocation.getArgument(0);
            d.setId(999L);
            return d;
        });

        // Act
        Dispute saved = disputeService.receiveDispute(request);

        // Assert
        verify(disputeRepository).save(disputeCaptor.capture());
        Dispute toSave = disputeCaptor.getValue();

        assertEquals(completedOrder, toSave.getOrder());
        assertEquals(category, toSave.getDisputeCategory());
        assertNull(toSave.getAdmin());
        assertNull(toSave.getEvidences());
        assertEquals("Item damaged", toSave.getDescription());
        assertEquals(DisputeDecision.NOT_HAVE_YET, toSave.getDecision());
        assertEquals("No Resolution Yet", toSave.getResolution());
        assertEquals(ResolutionType.NOT_HAVE_YET, toSave.getResolutionType());
        assertEquals(DisputeStatus.PENDING, toSave.getStatus());

        assertNotNull(saved.getId());
        assertEquals(999L, saved.getId());
    }

    @Test
    public void shouldUpdateDisputeEvidencesAndSave() {
        // Arrange
        Dispute dispute = Dispute.builder().id(1L).build();
        Evidence ev1 = Evidence.builder().id(5L).build();
        Evidence ev2 = Evidence.builder().id(6L).build();
        List<Evidence> evidences = List.of(ev1, ev2);

        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Dispute updated = disputeService.updateEvidencesForDispute(evidences, dispute);

        // Assert
        verify(disputeRepository, times(1)).save(dispute);
        assertEquals(evidences, updated.getEvidences());
    }

    @Test
    public void shouldReturnPagedPendingDisputesMappedToResponses() {
        // Arrange
        int page = 0;
        int size = 2;
        Pageable expectedPageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        Dispute d1 = Dispute.builder().id(1L).disputeCategory(buildCategory(101L)).evidences(List.of()).build();
        Dispute d2 = Dispute.builder().id(2L).disputeCategory(buildCategory(102L)).evidences(List.of()).build();
        Page<Dispute> disputePage = new PageImpl<>(List.of(d1, d2), expectedPageable, 5);

        when(disputeRepository.findAllByStatus(DisputeStatus.PENDING, expectedPageable)).thenReturn(disputePage);

        DisputeResponse r1 = DisputeResponse.builder().disputeId(1L).disputeCategoryId(101L).build();
        DisputeResponse r2 = DisputeResponse.builder().disputeId(2L).disputeCategoryId(102L).build();
        when(disputeMapper.toDto(d1)).thenReturn(r1);
        when(disputeMapper.toDto(d2)).thenReturn(r2);

        // Act
        Page<DisputeResponse> result = disputeService.getAllDispute(page, size);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(0, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(r1, result.getContent().get(0));
        assertEquals(r2, result.getContent().get(1));
    }

    @Test
    public void shouldRejectDisputeAndNotifyBuyer() {
        // Arrange
        long disputeId = 50L;
        long buyerId = 7L;

        Order order = buildOrder(200L, OrderStatus.COMPLETED, buyerId);
        Dispute dispute = Dispute.builder()
                .id(disputeId)
                .status(DisputeStatus.PENDING)
                .order(order)
                .build();

        Admin admin = Admin.builder().id(1L).fullName("Admin A").build();

        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setCreatedAt(LocalDateTime.now());
            return n;
        });

        ResolveDisputeRequest request = ResolveDisputeRequest.builder()
                .disputeId(disputeId)
                .decision(DisputeDecision.REJECTED)
                .resolution("Insufficient evidence")
                .build();

        // Act
        Notification notification = disputeService.handlePendingDispute(admin, request);

        // Assert
        assertNotNull(notification);
        assertEquals(buyerId, notification.getReceiverId());
        assertEquals(AccountType.BUYER, notification.getType());
        assertEquals("REJECT YOUR ORDER DISPUTE", notification.getTitle());
        assertEquals("Insufficient evidence", notification.getContent());

        assertEquals(DisputeStatus.REJECTED, dispute.getStatus());
        assertEquals(ResolutionType.REJECTED, dispute.getResolutionType());
        assertEquals("Insufficient evidence", dispute.getResolution());
        assertEquals(admin, dispute.getAdmin());

        verify(disputeRepository, times(1)).save(dispute);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    public void shouldAcceptDisputeAndNotifyBuyer() {
        // Arrange
        long disputeId = 60L;
        long buyerId = 9L;

        Order order = buildOrder(300L, OrderStatus.COMPLETED, buyerId);
        Dispute dispute = Dispute.builder()
                .id(disputeId)
                .status(DisputeStatus.PENDING)
                .order(order)
                .build();

        Admin admin = Admin.builder().id(2L).fullName("Admin B").build();

        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResolveDisputeRequest request = ResolveDisputeRequest.builder()
                .disputeId(disputeId)
                .decision(DisputeDecision.ACCEPTED)
                .resolution("Refund 50%")
                .resolutionType(ResolutionType.REFUND)
                .refundPercent(50.0)
                .build();

        // Act
        Notification notification = disputeService.handlePendingDispute(admin, request);

        // Assert
        assertNotNull(notification);
        assertEquals(buyerId, notification.getReceiverId());
        assertEquals(AccountType.BUYER, notification.getType());
        assertEquals("ACCEPTED YOUR ORDER DISPUTE", notification.getTitle());
        assertEquals("Refund 50%", notification.getContent());

        assertEquals(DisputeStatus.ACCEPTED, dispute.getStatus());
        assertEquals(ResolutionType.REFUND, dispute.getResolutionType());
        assertEquals("Refund 50%", dispute.getResolution());
        assertEquals(admin, dispute.getAdmin());

        verify(disputeRepository, times(1)).save(dispute);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    public void shouldThrowWhenReceivingDisputeForNonCompletedOrder() {
        // Arrange
        Long orderId = 101L;
        Long categoryId = 11L;
        RaiseDisputeRequest request = RaiseDisputeRequest.builder()
                .orderId(orderId)
                .disputeCategoryId(categoryId)
                .description("Issue")
                .build();

        Order processingOrder = buildOrder(orderId, OrderStatus.PROCESSING, 1L);
        DisputeCategory category = buildCategory(categoryId);

        when(disputeCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(processingOrder));

        // Act + Assert
        Exception ex = assertThrows(Exception.class, () -> disputeService.receiveDispute(request));
        assertEquals("Only completed order can be dispute", ex.getMessage());
        verify(disputeRepository, never()).save(any());
    }
}
