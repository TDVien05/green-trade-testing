package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.OrderStatus;
import Green_trade.green_trade_platform.enumerate.TransactionStatus;
import Green_trade.green_trade_platform.exception.OrderNotFound;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.CancelOrderReasonRepository;
import Green_trade.green_trade_platform.repository.OrderRepository;
import Green_trade.green_trade_platform.repository.TransactionRepository;
import Green_trade.green_trade_platform.request.CancelOrderRequest;
import Green_trade.green_trade_platform.service.implement.GhnServiceImpl;
import Green_trade.green_trade_platform.service.implement.OrderServiceImpl;
import Green_trade.green_trade_platform.service.implement.TransactionServiceImpl;
import Green_trade.green_trade_platform.service.implement.WalletServiceImpl;
import Green_trade.green_trade_platform.service.implement.WalletTransactionServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TransactionServiceImpl transactionService;
    @Mock
    private GhnServiceImpl ghnServiceImpl;
    @Mock
    private WalletTransactionServiceImpl walletTransactionServiceImpl;
    @Mock
    private WalletServiceImpl walletService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CancelOrderReasonRepository cancelOrderReasonRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Buyer buyer;
    private Seller seller;

    @BeforeEach
    void setUp() {
        buyer = Buyer.builder().buyerId(1L).build();
        seller = Seller.builder().sellerId(2L).build();
    }

    @Test
    void shouldSaveOrderSuccessfully() {
        // Arrange
        Order newOrder = Order.builder()
                .id(1L)
                .orderCode("ABC123")
                .build();
        when(orderRepository.save(newOrder)).thenReturn(newOrder);

        // Act
        Map<String, Object> result = orderService.saveOrder(newOrder);

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertEquals("save order successfully.", result.get("message"));
        assertEquals(newOrder, result.get("data"));
        verify(orderRepository).save(newOrder);
    }

    @Test
    void shouldHandleExceptionWhenSaveOrderFails() {
        // Arrange
        Order order = Order.builder().id(2L).build();
        when(orderRepository.save(order)).thenThrow(new RuntimeException("DB error"));

        // Act
        Map<String, Object> result = orderService.saveOrder(order);

        // Assert
        assertFalse((Boolean) result.get("success"));
        assertTrue(((String) result.get("message")).contains("DB error"));
        verify(orderRepository).save(order);
    }

    @Test
    void shouldUpdateOrderCodeAndSave() {
        // Arrange
        Order order = Order.builder().id(3L).orderCode(null).build();
        when(orderRepository.save(order)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.updateOrderCode("XYZ999", order);

        // Assert
        assertEquals("XYZ999", result.getOrderCode());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldUpdateSystemWalletAndSave() {
        // Arrange
        SystemWallet wallet = SystemWallet.builder().id(5L).balance(BigDecimal.valueOf(1000.0)).build();
        Order order = Order.builder().id(4L).systemWallet(null).build();
        when(orderRepository.save(order)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.updateSystemWallet(wallet, order);

        // Assert
        assertEquals(wallet, result.getSystemWallet());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldUpdateOrderTransactionsAndSave() {
        // Arrange
        Transaction t1 = Transaction.builder().id(10L).build();
        Transaction t2 = Transaction.builder().id(11L).build();
        List<Transaction> transactions = List.of(t1, t2);

        Order order = Order.builder().id(5L).transactions(new ArrayList<>()).build();
        when(orderRepository.save(order)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.updateOrderTransactions(order, transactions);

        // Assert
        assertEquals(transactions, result.getTransactions());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldReturnPagedOrdersForBuyerOrderedById() {
        // Arrange
        int size = 2;
        int page = 1;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        Order o1 = Order.builder().id(10L).buyer(buyer).build();
        Order o2 = Order.builder().id(11L).buyer(buyer).build();
        List<Order> content = Arrays.asList(o1, o2);
        Page<Order> repoPage = new PageImpl<>(content, pageable, 5);
        when(orderRepository.findAllByBuyer(eq(buyer), any(Pageable.class))).thenReturn(repoPage);

        // Act
        Page<Order> result = orderService.getOrdersOfCurrentUserPaging(size, page, buyer);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getSize());
        assertEquals(page, result.getNumber());
        assertEquals(5, result.getTotalElements());
        assertEquals(content, result.getContent());
        verify(orderRepository).findAllByBuyer(eq(buyer), any(Pageable.class));
    }

    @Test
    void shouldCancelPendingOrderWithAllSideEffects() throws Exception {
        // Arrange
        Long orderId = 100L;
        CancelOrderRequest req = CancelOrderRequest.builder().cancelReasonId(9L).build();

        CancelOrderReason reason = CancelOrderReason.builder().id(9L).cancelReasonName("Buyer changed mind").build();

        PostProduct postProduct = PostProduct.builder().sold(true).seller(seller).build();
        Payment payment = Payment.builder().gatewayName("WALLET").build();
        Transaction lastTx = Transaction.builder().payment(payment).build();
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(lastTx);

        Order existing = Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .postProduct(postProduct)
                .transactions(transactions)
                .buyer(buyer)
                .build();

        when(orderRepository.findOrderById(orderId)).thenReturn(Optional.of(existing));
        when(cancelOrderReasonRepository.findById(9L)).thenReturn(Optional.of(reason));
        when(transactionService.createTransaction(eq(existing), eq(TransactionStatus.CANCELED), eq(payment)))
                .thenReturn(Transaction.builder().status(TransactionStatus.CANCELED).build());
        // Save after updates
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Order result = orderService.cancelOrder(orderId, req);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELED, result.getStatus());
        assertFalse(result.getPostProduct().isSold());
        assertEquals(reason, result.getCancelOrderReason());
        assertNotNull(result.getCanceledAt());
        verify(transactionService).createTransaction(eq(existing), eq(TransactionStatus.CANCELED), eq(payment));
        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertEquals(OrderStatus.CANCELED, saved.getStatus());
    }

    @Test
    void shouldVerifyOrderAndPersist() {
        // Arrange
        long id = 55L;
        Order existing = Order.builder().id(id).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Order result = orderService.verifyOrder(id);

        // Assert
        assertEquals(OrderStatus.VERIFIED, result.getStatus());
        verify(orderRepository).findById(id);
        verify(orderRepository).save(existing);
    }

    @Test
    void shouldCancelPaidOrderAndRefundBuyer() throws Exception {
        // Arrange
        Long orderId = 200L;
        CancelOrderRequest req = CancelOrderRequest.builder().cancelReasonId(7L).build();
        CancelOrderReason reason = CancelOrderReason.builder().id(7L).cancelReasonName("Out of stock").build();

        Wallet buyerWallet = Wallet.builder().walletId(1L).balance(BigDecimal.valueOf(1000)).build();
        SystemWallet systemWallet = SystemWallet.builder().id(1L).balance(BigDecimal.valueOf(500)).build();

        Buyer orderBuyer = Buyer.builder().buyerId(3L).wallet(buyerWallet).build();
        PostProduct postProduct = PostProduct.builder().sold(true).seller(seller).build();

        Payment payment = Payment.builder().gatewayName("WALLET").build();
        Transaction lastTx = Transaction.builder().payment(payment).build();
        List<Transaction> txs = new ArrayList<>();
        txs.add(lastTx);

        Order existing = Order.builder()
                .id(orderId)
                .status(OrderStatus.PAID)
                .postProduct(postProduct)
                .transactions(txs)
                .buyer(orderBuyer)
                .systemWallet(systemWallet)
                .build();

        when(orderRepository.findOrderById(orderId)).thenReturn(Optional.of(existing));
        when(cancelOrderReasonRepository.findById(7L)).thenReturn(Optional.of(reason));
        when(transactionService.createTransaction(eq(existing), eq(TransactionStatus.CANCELED), eq(payment)))
                .thenReturn(Transaction.builder().status(TransactionStatus.CANCELED).build());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Order result = orderService.cancelOrder(orderId, req);

        // Assert
        assertEquals(OrderStatus.CANCELED, result.getStatus());
        assertFalse(result.getPostProduct().isSold());
        assertEquals(reason, result.getCancelOrderReason());
        assertNotNull(result.getCanceledAt());
        verify(walletService).handleBuyerRefundForCancelledOrder(systemWallet, 100, buyerWallet);
        verify(transactionService).createTransaction(eq(existing), eq(TransactionStatus.CANCELED), eq(payment));
        verify(orderRepository, atLeastOnce()).save(any(Order.class));
    }

    @Test
    void shouldThrowWhenCancellingNonexistentOrder() {
        // Arrange
        Long id = 404L;
        CancelOrderRequest req = CancelOrderRequest.builder().cancelReasonId(1L).build();
        when(orderRepository.findOrderById(id)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(OrderNotFound.class, () -> orderService.cancelOrder(id, req));
        verify(orderRepository).findOrderById(id);
        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldThrowWhenTransactionNotFoundByOrderId() {
        // Arrange
        long id = 300L;
        Order order = Order.builder().id(id).build();
        when(orderRepository.findOrderById(id)).thenReturn(Optional.of(order));
        when(transactionRepository.findValidTransactionsByOrderId(eq(order), eq(TransactionStatus.FAIL)))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> orderService.getTransactionByOrderId(id));
        verify(orderRepository).findOrderById(id);
        verify(transactionRepository).findValidTransactionsByOrderId(eq(order), eq(TransactionStatus.FAIL));
    }
}
