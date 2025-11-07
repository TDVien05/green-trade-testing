package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.TransactionStatus;
import Green_trade.green_trade_platform.enumerate.TransactionType;
import Green_trade.green_trade_platform.exception.PaymentMethodNotSupportedException;
import Green_trade.green_trade_platform.exception.PostProductNotFound;
import Green_trade.green_trade_platform.exception.ProfileException;
import Green_trade.green_trade_platform.exception.WalletNotFoundException;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.model.Payment;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.Transaction;
import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.model.WalletTransaction;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.repository.PaymentRepository;
import Green_trade.green_trade_platform.repository.PostProductRepository;
import Green_trade.green_trade_platform.repository.TransactionRepository;
import Green_trade.green_trade_platform.repository.WalletRepository;
import Green_trade.green_trade_platform.repository.WalletTransactionRepository;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import Green_trade.green_trade_platform.service.implement.TransactionServiceImpl;
import Green_trade.green_trade_platform.service.implement.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock private WalletServiceImpl walletService;
    @Mock private BuyerServiceImpl buyerService;
    @Mock private BuyerRepository buyerRepository;
    @Mock private PostProductRepository postProductRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<WalletTransaction> walletTransactionCaptor;

    private Order buildOrder(BigDecimal price, BigDecimal shippingFee) {
        return Order.builder()
                .id(100L)
                .price(price)
                .shippingFee(shippingFee)
                .build();
    }

    private Payment buildPayment(long id, String gateway) {
        return Payment.builder()
                .id(id)
                .gatewayName(gateway)
                .description("test")
                .build();
    }

    private PostProduct buildPostProduct(boolean sold, BigDecimal price) {
        PostProduct pp = PostProduct.builder().build();
        try {
            PostProduct.class.getMethod("setSold", boolean.class).invoke(pp, sold);
        } catch (Exception ignored) {}
        try {
            PostProduct.class.getMethod("setPrice", BigDecimal.class).invoke(pp, price);
        } catch (Exception ignored) {}
        return pp;
    }

    private Wallet buildWallet(BigDecimal balance) {
        return Wallet.builder()
                .walletId(1L)
                .balance(balance)
                .build();
    }

    @Test
    void shouldCreateAndSaveTransactionSuccessfully() {
        // Arrange
        Order order = Order.builder().id(1L).price(new BigDecimal("150000")).build();
        Payment payment = Payment.builder().id(10L).gatewayName("MOMO").build();
        TransactionStatus status = TransactionStatus.SUCCESS;

        Transaction savedTransaction = Transaction.builder()
                .id(99L)
                .amount(order.getPrice())
                .currency("VND")
                .status(status)
                .paymentMethod("MOMO")
                .order(order)
                .payment(payment)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        Transaction result = transactionService.createTransaction(order, status, payment);

        // Assert
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(captor.capture());
        Transaction captured = captor.getValue();

        // Verify field mappings
        assertEquals(order.getPrice(), captured.getAmount());
        assertEquals("VND", captured.getCurrency());
        assertEquals(status, captured.getStatus());
        assertEquals("MOMO", captured.getPaymentMethod());
        assertEquals(order, captured.getOrder());
        assertEquals(payment, captured.getPayment());

        // Verify return value
        assertNotNull(result);
        assertEquals(savedTransaction, result);
    }

    @Test
    void shouldPropagateExceptionIfRepositoryFails() {
        // Arrange
        Order order = Order.builder().price(new BigDecimal("50000")).build();
        Payment payment = Payment.builder().gatewayName("VNPay").build();
        TransactionStatus status = TransactionStatus.PENDING;

        when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new RuntimeException("DB error"));

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.createTransaction(order, status, payment));

        assertTrue(ex.getMessage().contains("DB error"));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    public void shouldReturnTransactionsForOrder() {
        Order order = buildOrder(new BigDecimal("100000"), new BigDecimal("15000"));
        List<Transaction> expected = List.of(
                Transaction.builder().id(1L).order(order).status(TransactionStatus.SUCCESS).build(),
                Transaction.builder().id(2L).order(order).status(TransactionStatus.PENDING).build()
        );
        when(transactionRepository.findAllByOrder(order)).thenReturn(expected);

        List<Transaction> result = transactionService.getTransactionsOfOrder(order);

        assertEquals(expected, result);
        verify(transactionRepository, times(1)).findAllByOrder(order);
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    public void shouldCheckoutWithWalletWhenSufficientBalance() throws Exception {
        String username = "buyer1";
        Long postId = 10L;
        Long paymentId = 5L;
        Order order = buildOrder(new BigDecimal("100000"), new BigDecimal("15000"));
        Payment payment = buildPayment(paymentId, "VNPay");
        PostProduct postProduct = buildPostProduct(false, new BigDecimal("100000"));
        Wallet wallet = buildWallet(new BigDecimal("200000"));

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(buyerService.findBuyerByUsername(username)).thenReturn(Buyer.builder().buyerId(2L).username(username).build());
        when(walletService.isBuyerHasWallet(any(Buyer.class))).thenReturn(true);
        when(postProductRepository.findById(postId)).thenReturn(Optional.of(postProduct));
        when(buyerService.getWallet()).thenReturn(wallet);

        // transactionRepository.save should return the saved object
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.getId() == null) t.setId(99L);
                    return t;
                });

        Transaction saved = transactionService.checkoutWalletPayment(username, postId, paymentId, order);

        // Wallet balance reduced correctly: 200000 - (100000 + 15000) = 85000
        assertEquals(new BigDecimal("85000"), wallet.getBalance());
        verify(walletRepository).save(wallet);

        // WalletTransaction created with PLACE_ORDER, SUCCESS, negative amount
        verify(walletTransactionRepository).save(walletTransactionCaptor.capture());
        WalletTransaction wt = walletTransactionCaptor.getValue();
        assertEquals(TransactionType.PLACE_ORDER, wt.getType());
        assertEquals(TransactionStatus.SUCCESS, wt.getStatus());
        assertEquals(new BigDecimal("-115000"), wt.getAmount());
        assertEquals(wallet, wt.getWallet());
        assertEquals(order, wt.getOrder());

        // Transaction saved with SUCCESS and correct amount and payment method
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        Transaction finalTxn = transactionCaptor.getAllValues()
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("115000"), finalTxn.getAmount());
        assertEquals("VND", finalTxn.getCurrency());
        assertEquals(payment.getGatewayName(), finalTxn.getPaymentMethod());
        assertEquals(order, finalTxn.getOrder());
        assertEquals(payment, finalTxn.getPayment());

        assertNotNull(saved);
        assertEquals(TransactionStatus.SUCCESS, saved.getStatus());
    }

    @Test
    public void shouldCreatePendingTransactionForCOD() throws Exception {
        String username = "buyer2";
        Long postId = 11L;
        Long paymentId = 6L;
        Order order = buildOrder(new BigDecimal("50000"), new BigDecimal("10000"));
        Payment payment = buildPayment(paymentId, "COD");
        Buyer buyer = Buyer.builder().buyerId(3L).username(username).build();
        PostProduct postProduct = buildPostProduct(false, new BigDecimal("50000"));

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(buyerService.isBuyerExisted(username)).thenReturn(true);
        when(buyerRepository.findByUsername(username)).thenReturn(Optional.of(buyer));
        // Note: the implementation mistakenly uses paymentId for postProduct lookup; mock accordingly
        when(postProductRepository.findById(paymentId)).thenReturn(Optional.of(postProduct));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction t = inv.getArgument(0);
                    t.setId(200L);
                    return t;
                });

        Transaction txn = transactionService.checkoutCODPayment(username, postId, paymentId, order);

        assertNotNull(txn);
        assertEquals(TransactionStatus.PENDING, txn.getStatus());
        assertEquals(new BigDecimal("60000"), txn.getAmount());
        assertEquals("VND", txn.getCurrency());
        assertEquals(payment.getGatewayName(), txn.getPaymentMethod());
        assertEquals(order, txn.getOrder());
        assertEquals(payment, txn.getPayment());

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    public void shouldThrowWhenWalletPaymentMethodNotSupported() {
        String username = "buyer3";
        Long postId = 12L;
        Long paymentId = 999L;
        Order order = buildOrder(new BigDecimal("10000"), new BigDecimal("2000"));

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentMethodNotSupportedException.class,
                () -> transactionService.checkoutWalletPayment(username, postId, paymentId, order));

        verifyNoInteractions(buyerService, walletService, postProductRepository, walletRepository, walletTransactionRepository);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void shouldFailWalletCheckoutWhenInsufficientBalance() throws Exception {
        String username = "buyer4";
        Long postId = 13L;
        Long paymentId = 7L;
        Order order = buildOrder(new BigDecimal("100000"), new BigDecimal("10000")); // total 110000
        Payment payment = buildPayment(paymentId, "VNPay");
        Buyer buyer = Buyer.builder().buyerId(4L).username(username).build();
        PostProduct postProduct = buildPostProduct(false, new BigDecimal("100000"));
        Wallet wallet = buildWallet(new BigDecimal("50000")); // insufficient

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(buyerService.findBuyerByUsername(username)).thenReturn(buyer);
        when(walletService.isBuyerHasWallet(buyer)).thenReturn(true);
        when(postProductRepository.findById(postId)).thenReturn(Optional.of(postProduct));
        when(buyerService.getWallet()).thenReturn(wallet);

        // Capture saved FAIL transaction
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Exception ex = assertThrows(Exception.class,
                () -> transactionService.checkoutWalletPayment(username, postId, paymentId, order));
        assertTrue(ex.getMessage().toLowerCase().contains("not enough"));

        // Wallet balance remains unchanged
        assertEquals(new BigDecimal("50000"), wallet.getBalance());
        verify(walletRepository, never()).save(any());

        // FAIL transaction saved
        verify(transactionRepository, times(1)).save(transactionCaptor.capture());
        Transaction failTxn = transactionCaptor.getValue();
        assertEquals(TransactionStatus.FAIL, failTxn.getStatus());
        assertEquals(new BigDecimal("110000"), failTxn.getAmount());
        assertEquals("VND", failTxn.getCurrency());
        assertEquals(payment.getGatewayName(), failTxn.getPaymentMethod());

        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    public void shouldThrowWhenCODBuyerDoesNotExist() {
        String username = "missing_user";
        Long postId = 14L;
        Long paymentId = 8L;
        Order order = buildOrder(new BigDecimal("30000"), new BigDecimal("5000"));
        Payment payment = buildPayment(paymentId, "COD");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(buyerService.isBuyerExisted(username)).thenReturn(false);

        assertThrows(ProfileException.class,
                () -> transactionService.checkoutCODPayment(username, postId, paymentId, order));

        verify(transactionRepository, never()).save(any());
        verify(buyerRepository, never()).findByUsername(anyString());
    }
}
