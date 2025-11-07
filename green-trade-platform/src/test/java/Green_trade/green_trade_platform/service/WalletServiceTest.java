package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.exception.WalletNotFoundException;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.SystemWallet;
import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.model.WalletTransaction;
import Green_trade.green_trade_platform.repository.WalletRepository;
import Green_trade.green_trade_platform.repository.WalletTransactionRepository;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import Green_trade.green_trade_platform.service.implement.WalletServiceImpl;
import Green_trade.green_trade_platform.service.implement.WalletTransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private BuyerServiceImpl buyerService;

    @Mock
    private WalletTransactionServiceImpl walletTransactionService;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Buyer buyer;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        buyer = Buyer.builder().buyerId(1L).username("user").build();
        wallet = Wallet.builder()
                .walletId(10L)
                .buyer(buyer)
                .balance(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void shouldRefundCorrectAmountAndUpdateBalance() {
        // Arrange
        Wallet wallet = Wallet.builder()
                .walletId(1L)
                .balance(new BigDecimal("1000.00"))
                .build();

        SystemWallet systemWallet = SystemWallet.builder()
                .balance(new BigDecimal("500.00"))
                .build();

        WalletTransaction transaction = WalletTransaction.builder().transactionId(99L).build();
        when(walletTransactionService.handleRefundMoney(any(), any(), eq(true), anyString()))
                .thenReturn(transaction);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Wallet result = walletService.handleBuyerRefundForCancelledOrder(systemWallet, 20.0, wallet);

        // Assert
        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletTransactionService, times(1))
                .handleRefundMoney(wallet, new BigDecimal("100.00"), true, "REFUNDED FROM CANCELED ORDER");

        verify(walletRepository).save(captor.capture());
        Wallet savedWallet = captor.getValue();

        // Refund 20% of 500 = 100
        assertEquals(new BigDecimal("1100.00"), savedWallet.getBalance());
        assertSame(savedWallet, result);
    }

    @Test
    void shouldHandleZeroRefundGracefully() {
        Wallet wallet = Wallet.builder().balance(new BigDecimal("200")).build();
        SystemWallet systemWallet = SystemWallet.builder().balance(BigDecimal.ZERO).build();

        when(walletTransactionService.handleRefundMoney(any(), any(), anyBoolean(), anyString()))
                .thenReturn(new WalletTransaction());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.handleBuyerRefundForCancelledOrder(systemWallet, 50.0, wallet);

        assertEquals(new BigDecimal("200.00"), result.getBalance());
        verify(walletTransactionService).handleRefundMoney(wallet, new BigDecimal("0.00"), true, "REFUNDED FROM CANCELED ORDER");
    }

    @Test
    void shouldReturnWalletWhenIdExists() {
        Wallet wallet = Wallet.builder().walletId(5L).balance(new BigDecimal("500")).build();
        when(walletRepository.findByWalletId(5L)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.findWalletById(5L);

        assertNotNull(result);
        assertEquals(wallet, result);
        verify(walletRepository).findByWalletId(5L);
    }

    @Test
    void shouldThrowExceptionWhenWalletIdNotFound() {
        when(walletRepository.findByWalletId(10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> walletService.findWalletById(10L));

        assertTrue(ex.getMessage().contains("Can not find wallet with this wallet id: 10"));
        verify(walletRepository).findByWalletId(10L);
    }

    @Test
    void shouldHandleSignPackageAndDeductBalanceSuccessfully() {
        // Arrange
        Buyer buyer = Buyer.builder().buyerId(1L).build();
        Wallet wallet = Wallet.builder()
                .walletId(10L)
                .buyer(buyer)
                .balance(new BigDecimal("1000.00"))
                .build();

        when(walletRepository.findByBuyer(buyer)).thenReturn(Optional.of(wallet));

        // Act
        Map<String, Object> result = walletService.handleSignPackageForSeller(buyer, 200.0);

        // Assert
        verify(walletRepository).findByBuyer(buyer);
        verify(walletTransactionService).handleSignPackageForSeller(wallet, 200.0);

        assertNotNull(result);
        assertFalse((Boolean) result.get("success"), "Expected 'success' key to be false");
        assertEquals("Trừ tiền thành công.", result.get("message"));
        assertEquals(wallet, result.get("data"));

        // Balance reduced correctly
        assertEquals(new BigDecimal("800.00"), wallet.getBalance());
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        // Arrange
        Buyer buyer = Buyer.builder().buyerId(99L).build();
        when(walletRepository.findByBuyer(buyer)).thenReturn(Optional.empty());

        // Act + Assert
        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> walletService.handleSignPackageForSeller(buyer, 50.0)
        );

        assertTrue(ex.getMessage().contains("Không tìm thấy ví người dùng."));
        verify(walletRepository).findByBuyer(buyer);
        verifyNoInteractions(walletTransactionService);
    }

    @Test
    void shouldReturnTrueWhenBuyerHasWallet() {
        Buyer buyer = Buyer.builder().buyerId(1L).build();
        Wallet wallet = Wallet.builder().buyer(buyer).build();
        when(walletRepository.findByBuyer(buyer)).thenReturn(Optional.of(wallet));

        boolean result = walletService.isBuyerHasWallet(buyer);

        assertTrue(result);
        verify(walletRepository).findByBuyer(buyer);
    }

    @Test
    void shouldReturnFalseWhenBuyerHasNoWallet() {
        Buyer buyer = Buyer.builder().buyerId(2L).build();
        when(walletRepository.findByBuyer(buyer)).thenReturn(Optional.empty());

        boolean result = walletService.isBuyerHasWallet(buyer);

        assertFalse(result);
        verify(walletRepository).findByBuyer(buyer);
    }

    @Test
    void shouldProcessDepositAndIncreaseBalanceWhenTxnRefIsNew() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "txn123");
        params.put("vnp_OrderInfo", "1 something");
        params.put("vnp_Amount", "150000"); // equals 1500 after /100 per service

        when(walletTransactionRepository.existsByExternalTransactionReference("txn123")).thenReturn(false);
        when(buyerService.getBuyerFromVnPayRequest("1 something")).thenReturn(buyer);
        when(walletRepository.findByBuyer(buyer)).thenReturn(Optional.of(wallet));

        WalletTransaction savedTxn = WalletTransaction.builder()
                .transactionId(99L)
                .wallet(wallet)
                .amount(new BigDecimal("1500"))
                .build();
        when(walletTransactionService.handleDepositIntoMoney(wallet, params)).thenReturn(savedTxn);

        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.processDepositMoneyIntoWallet(params);

        assertNotNull(result);
        assertEquals(new BigDecimal("1600.00"), result.getBalance()); // 100 + 1500 = 1600.00
        verify(walletRepository).save(result);
        verify(walletTransactionService).handleDepositIntoMoney(wallet, params);
    }

    @Test
    void shouldCreateWalletAndSaveItForGivenBuyer() {
        // Arrange
        Buyer buyer = Buyer.builder().buyerId(1L).build();
        Wallet savedWallet = Wallet.builder().walletId(100L).buyer(buyer).build();

        // Simulate repository returning saved entity
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // Act
        Wallet result = walletService.createLocalWalletForBuyer(buyer);

        // Assert
        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(captor.capture());

        Wallet walletToSave = captor.getValue();
        assertEquals(buyer, walletToSave.getBuyer(), "Wallet should be linked to given Buyer");

        assertNotNull(result, "Returned wallet should not be null");
        assertEquals(savedWallet, result, "Returned wallet should match saved wallet");
    }

    @Test
    void shouldThrowExceptionIfRepositoryThrows() {
        Buyer buyer = Buyer.builder().buyerId(99L).build();
        when(walletRepository.save(any(Wallet.class))).thenThrow(new RuntimeException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> walletService.createLocalWalletForBuyer(buyer));

        assertTrue(ex.getMessage().contains("DB error"));
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void shouldReturnWalletWhenVnPayOrderInfoIsValid() {
        when(buyerService.getBuyerFromVnPayRequest("1 info")).thenReturn(buyer);
        when(walletRepository.findByBuyer(buyer)).thenReturn(Optional.of(wallet));

        Wallet found = walletService.getWalletWithVnPayRequest("1 info");

        assertNotNull(found);
        assertEquals(wallet.getWalletId(), found.getWalletId());
        verify(buyerService).getBuyerFromVnPayRequest("1 info");
        verify(walletRepository).findByBuyer(buyer);
    }

    @Test
    void shouldReturnPagedTransactionHistorySortedByCreatedAtDesc() {
        Wallet buyerWallet = Wallet.builder().walletId(22L).buyer(buyer).build();
        buyer.setWallet(buyerWallet);

        int page = 0, size = 5;
        Pageable expectedPageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        WalletTransaction t1 = WalletTransaction.builder().transactionId(1L).wallet(buyerWallet).build();
        WalletTransaction t2 = WalletTransaction.builder().transactionId(2L).wallet(buyerWallet).build();
        Page<WalletTransaction> pageResult = new PageImpl<>(Arrays.asList(t1, t2), expectedPageable, 2);

        when(walletTransactionRepository.findByWallet(eq(buyerWallet), any(Pageable.class))).thenReturn(pageResult);

        Page<WalletTransaction> result = walletService.getTransactionHistory(buyer, page, size);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(walletTransactionRepository).findByWallet(eq(buyerWallet), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertEquals(expectedPageable.getPageNumber(), used.getPageNumber());
        assertEquals(expectedPageable.getPageSize(), used.getPageSize());
        assertEquals(expectedPageable.getSort(), used.getSort());
    }

    @Test
    void shouldSkipDepositWhenTxnRefAlreadyProcessed() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "dupTxn");
        params.put("vnp_OrderInfo", "1 info");

        when(walletTransactionRepository.existsByExternalTransactionReference("dupTxn")).thenReturn(true);

        WalletTransaction existingTxn = WalletTransaction.builder().transactionId(5L).wallet(wallet).build();
        when(walletTransactionRepository.findByExternalTransactionReference("dupTxn")).thenReturn(Optional.of(existingTxn));

        Wallet result = walletService.processDepositMoneyIntoWallet(params);

        assertNotNull(result);
        assertEquals(wallet, result);
        verify(walletTransactionService, never()).handleDepositIntoMoney(any(), any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldThrowWalletNotFoundWhenBuyerHasNoWalletFromVnPay() {
        when(buyerService.getBuyerFromVnPayRequest("1 info")).thenReturn(buyer);
        when(walletRepository.findByBuyer(buyer)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletWithVnPayRequest("1 info"));
    }

    @Test
    void shouldComputeAndApplyRefundForBuyerAndSellerWithRounding() {
        // Setup system wallet with balance 123.45; refund percent 33% -> 40.74 (rounded to 2 decimals)
        SystemWallet systemWallet = SystemWallet.builder().balance(new BigDecimal("123.45")).build();
        Wallet targetWalletBuyer = Wallet.builder().walletId(30L).balance(new BigDecimal("200.00")).build();
        Wallet targetWalletSeller = Wallet.builder().walletId(31L).balance(new BigDecimal("300.00")).build();

        // Stubbing refund transactions for both branches
        when(walletTransactionService.handleRefundMoney(eq(targetWalletBuyer), eq(new BigDecimal("40.74")), eq(true), anyString()))
                .thenReturn(WalletTransaction.builder().wallet(targetWalletBuyer).amount(new BigDecimal("40.74")).build());
        when(walletTransactionService.handleRefundMoney(eq(targetWalletSeller), eq(new BigDecimal("40.74")), eq(false), anyString()))
                .thenReturn(WalletTransaction.builder().wallet(targetWalletSeller).amount(new BigDecimal("40.74")).build());

        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Buyer refund (isSeller=false)
        Wallet updatedBuyerWallet = walletService.handleBuyerRefund(systemWallet, 33.0, targetWalletBuyer, false);
        assertEquals(new BigDecimal("240.74"), updatedBuyerWallet.getBalance());

        // Seller refund (isSeller=true)
        Wallet updatedSellerWallet = walletService.handleBuyerRefund(systemWallet, 33.0, targetWalletSeller, true);
        assertEquals(new BigDecimal("340.74"), updatedSellerWallet.getBalance());

        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(walletTransactionService, times(1))
                .handleRefundMoney(eq(targetWalletBuyer), eq(new BigDecimal("40.74")), eq(true), anyString());
        verify(walletTransactionService, times(1))
                .handleRefundMoney(eq(targetWalletSeller), eq(new BigDecimal("40.74")), eq(false), anyString());
    }

}
