package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.TransactionStatus;
import Green_trade.green_trade_platform.enumerate.TransactionType;
import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.model.WalletTransaction;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.repository.WalletTransactionRepository;
import Green_trade.green_trade_platform.service.implement.WalletTransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WalletTransactionServiceTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private BuyerRepository buyerRepository;

    @InjectMocks
    private WalletTransactionServiceImpl walletTransactionService;

    private Wallet createWallet(BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setBalance(balance);
        return wallet;
    }

    @BeforeEach
    void setup() {
        // Default save behavior to return the same entity passed in
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void shouldCreateSuccessfulDepositTransactionFromParams() {
        Wallet wallet = createWallet(new BigDecimal("1500"));
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "123400"); // should parse to 1234
        params.put("vnp_TxnRef", "TXN-123");

        WalletTransaction result = walletTransactionService.handleDepositIntoMoney(wallet, params);

        assertNotNull(result);
        assertEquals(TransactionType.DEPOSIT, result.getType());
        assertEquals(new BigDecimal("1234"), result.getAmount());
        assertEquals(wallet.getBalance(), result.getBalanceBefore());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals("Deposit money into user's wallet.", result.getDescription());
        assertEquals("TXN-123", result.getExternalTransactionReference());
        assertEquals(wallet, result.getWallet());

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository, times(1)).save(captor.capture());
        WalletTransaction saved = captor.getValue();
        assertEquals(TransactionType.DEPOSIT, saved.getType());
    }

    @Test
    public void shouldCreateSignPackageTransactionWithNegativeAmount() {
        Wallet wallet = createWallet(new BigDecimal("2000"));
        double amount = 99.5;

        WalletTransaction result = walletTransactionService.handleSignPackageForSeller(wallet, amount);

        assertNotNull(result);
        assertEquals(TransactionType.SIGN_PACKAGE, result.getType());
        assertEquals(BigDecimal.valueOf(amount).negate(), result.getAmount());
        assertEquals(wallet.getBalance(), result.getBalanceBefore());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals("None", result.getExternalTransactionReference());
        assertEquals(wallet, result.getWallet());

        verify(walletTransactionRepository, times(1)).save(any(WalletTransaction.class));
    }

    @Test
    public void shouldCreateRefundOrDepositBasedOnFlag() {
        Wallet wallet = createWallet(new BigDecimal("500"));
        BigDecimal money = new BigDecimal("250.75");

        // Refund branch
        WalletTransaction refundTx = walletTransactionService.handleRefundMoney(wallet, money, true, "Order canceled");
        assertNotNull(refundTx);
        assertEquals(TransactionType.REFUND, refundTx.getType());
        assertEquals(money, refundTx.getAmount());
        assertEquals(TransactionStatus.SUCCESS, refundTx.getStatus());
        assertEquals("Order canceled", refundTx.getDescription());
        assertEquals("None", refundTx.getExternalTransactionReference());
        assertEquals(wallet.getBalance(), refundTx.getBalanceBefore());

        // Non-refund branch -> deposit
        WalletTransaction depositTx = walletTransactionService.handleRefundMoney(wallet, money, false, "ignored");
        assertNotNull(depositTx);
        assertEquals(TransactionType.DEPOSIT, depositTx.getType());
        assertEquals(money, depositTx.getAmount());
        assertEquals(TransactionStatus.SUCCESS, depositTx.getStatus());
        assertEquals("Get money from order", depositTx.getDescription());
        assertEquals("None", depositTx.getExternalTransactionReference());

        verify(walletTransactionRepository, times(2)).save(any(WalletTransaction.class));
    }

    @Test
    public void shouldThrowWhenDepositAmountInvalid() {
        Wallet wallet = createWallet(new BigDecimal("0"));
        Map<String, String> paramsMissing = new HashMap<>();
        // missing vnp_Amount

        assertThrows(RuntimeException.class, () ->
                walletTransactionService.handleDepositIntoMoney(wallet, paramsMissing));

        Map<String, String> paramsInvalid = new HashMap<>();
        paramsInvalid.put("vnp_Amount", "abc");
        paramsInvalid.put("vnp_TxnRef", "TXN-ERR");

        assertThrows(RuntimeException.class, () ->
                walletTransactionService.handleDepositIntoMoney(wallet, paramsInvalid));
    }

    @Test
    public void shouldWrapRepositoryExceptionsInRuntimeException() {
        Wallet wallet = createWallet(new BigDecimal("100"));
        // Force repository to throw for any save call
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenThrow(new IllegalStateException("DB down"));

        // Deposit path
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000");
        params.put("vnp_TxnRef", "TXN-1");
        assertThrows(RuntimeException.class, () ->
                walletTransactionService.handleDepositIntoMoney(wallet, params));

        // Sign package path
        assertThrows(RuntimeException.class, () ->
                walletTransactionService.handleSignPackageForSeller(wallet, 10.0));

        // Refund path (refund branch)
        assertThrows(RuntimeException.class, () ->
                walletTransactionService.handleRefundMoney(wallet, new BigDecimal("5"), true, "desc"));

        // Refund path (non-refund branch)
        assertThrows(RuntimeException.class, () ->
                walletTransactionService.handleRefundMoney(wallet, new BigDecimal("5"), false, "desc"));
    }

    @Test
    public void shouldHandleZeroAndNegativeMoneyInRefundHandling() {
        Wallet wallet = createWallet(new BigDecimal("750.00"));

        // Zero money - refund
        WalletTransaction zeroRefund = walletTransactionService.handleRefundMoney(wallet, BigDecimal.ZERO, true, "zero refund");
        assertEquals(TransactionType.REFUND, zeroRefund.getType());
        assertEquals(BigDecimal.ZERO, zeroRefund.getAmount());
        assertEquals(TransactionStatus.SUCCESS, zeroRefund.getStatus());
        assertEquals("zero refund", zeroRefund.getDescription());

        // Negative money - refund (should keep sign as provided)
        BigDecimal negative = new BigDecimal("-42.00");
        WalletTransaction negativeRefund = walletTransactionService.handleRefundMoney(wallet, negative, true, "negative refund");
        assertEquals(TransactionType.REFUND, negativeRefund.getType());
        assertEquals(negative, negativeRefund.getAmount());
        assertEquals(TransactionStatus.SUCCESS, negativeRefund.getStatus());
        assertEquals("negative refund", negativeRefund.getDescription());

        // Zero money - non-refund (deposit branch)
        WalletTransaction zeroDeposit = walletTransactionService.handleRefundMoney(wallet, BigDecimal.ZERO, false, "ignored");
        assertEquals(TransactionType.DEPOSIT, zeroDeposit.getType());
        assertEquals(BigDecimal.ZERO, zeroDeposit.getAmount());
        assertEquals("Get money from order", zeroDeposit.getDescription());

        // Negative money - non-refund (deposit branch, sign preserved)
        WalletTransaction negativeDeposit = walletTransactionService.handleRefundMoney(wallet, negative, false, "ignored");
        assertEquals(TransactionType.DEPOSIT, negativeDeposit.getType());
        assertEquals(negative, negativeDeposit.getAmount());
        assertEquals("Get money from order", negativeDeposit.getDescription());

        verify(walletTransactionRepository, times(4)).save(any(WalletTransaction.class));
    }
}
