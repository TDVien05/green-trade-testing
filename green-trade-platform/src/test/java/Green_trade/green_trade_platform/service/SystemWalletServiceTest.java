package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.SystemWalletStatus;
import Green_trade.green_trade_platform.exception.SystemWalletException;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.SystemWalletRepossitory;
import Green_trade.green_trade_platform.service.implement.SystemWalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SystemWalletServiceTest {
    private SystemWalletRepossitory repository;
    private SystemWalletServiceImpl service;

    @BeforeEach
    void setup() {
        repository = mock(SystemWalletRepossitory.class);
        service = new SystemWalletServiceImpl(repository);
    }

    private Order buildOrder(BigDecimal price, BigDecimal shippingFee, long buyerWalletId, long sellerWalletId) {
        // Tạo wallet cho buyer và seller
        Wallet buyerWallet = Wallet.builder().walletId(buyerWalletId).build();
        Wallet sellerWallet = Wallet.builder().walletId(sellerWalletId).build();

        // Buyer có wallet riêng
        Buyer buyer = Buyer.builder().wallet(buyerWallet).build();

        // Seller có buyer (chủ sở hữu) và wallet riêng ✅
        Seller seller = Seller.builder()
                .buyer(buyer)
                .build();

        // PostProduct có Seller
        PostProduct postProduct = PostProduct.builder()
                .seller(seller)
                .build();

        // Mock Order
        Order order = mock(Order.class);
        when(order.getPrice()).thenReturn(price);
        when(order.getShippingFee()).thenReturn(shippingFee);
        when(order.getBuyer()).thenReturn(buyer);
        when(order.getPostProduct()).thenReturn(postProduct);

        return order;
    }

    @Test
    void shouldUpdateStatusAndSaveEscrowRecord() {
        // Arrange
        SystemWallet wallet = SystemWallet.builder()
                .id(1L)
                .status(SystemWalletStatus.ESCROW_HOLD)
                .build();

        when(repository.save(any(SystemWallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SystemWallet result = service.updateEscrowRecordStatus(wallet, SystemWalletStatus.IS_SOLVED);

        // Assert
        ArgumentCaptor<SystemWallet> captor = ArgumentCaptor.forClass(SystemWallet.class);
        verify(repository, times(1)).save(captor.capture());
        SystemWallet saved = captor.getValue();

        assertEquals(SystemWalletStatus.IS_SOLVED, saved.getStatus());
        assertSame(saved, result);
    }

    @Test
    void shouldPropagateExceptionWhenRepositoryFails() {
        SystemWallet wallet = SystemWallet.builder()
                .id(2L)
                .status(SystemWalletStatus.ESCROW_HOLD)
                .build();

        when(repository.save(any(SystemWallet.class)))
                .thenThrow(new RuntimeException("Database failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateEscrowRecordStatus(wallet, SystemWalletStatus.IS_SOLVED));

        assertTrue(ex.getMessage().contains("Database failure"));
        verify(repository).save(wallet);
    }

    @Test
    void shouldReturnSystemWalletWhenOrderExists() {
        Order order = buildOrder(new BigDecimal("100000"), BigDecimal.ZERO, 1L, 2L);
        SystemWallet wallet = SystemWallet.builder().id(1L).order(order).build();
        when(repository.findByOrder(order)).thenReturn(Optional.of(wallet));

        SystemWallet result = service.getSystemWalletByOrder(order);

        assertSame(wallet, result);
        verify(repository).findByOrder(order);
    }

    @Test
    void shouldHandleRefundAndPersistChanges() {
        SystemWallet wallet = SystemWallet.builder()
                .status(SystemWalletStatus.ESCROW_HOLD)
                .endAt(null)
                .build();

        LocalDateTime fixedNow = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        try (MockedStatic<LocalDateTime> mocked = Mockito.mockStatic(LocalDateTime.class)) {
            mocked.when(LocalDateTime::now).thenReturn(fixedNow);
            when(repository.save(any(SystemWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.handleRefund(wallet);

            assertEquals(SystemWalletStatus.IS_SOLVED, wallet.getStatus());
            assertEquals(fixedNow, wallet.getEndAt());
            verify(repository).save(wallet);
        }
    }

    @Test
    void shouldThrowSystemWalletExceptionWhenCreateEscrowRecordFails() {
        Order order = buildOrder(new BigDecimal("50000"), BigDecimal.ZERO, 3L, 4L);
        when(repository.save(any(SystemWallet.class))).thenThrow(new RuntimeException("DB down"));

        assertThrows(SystemWalletException.class, () -> service.createEscrowRecord(order));
    }

    @Test
    void shouldThrowIllegalArgumentWhenOrderHasNoEscrow() {
        Order order = buildOrder(new BigDecimal("10000"), BigDecimal.ZERO, 5L, 6L);
        when(repository.findByOrder(order)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getSystemWalletByOrder(order));
    }

    @Test
    void shouldComputeAndPersistBalanceAfterFeesForCODAndWalletPayment() {
        Order order = buildOrder(new BigDecimal("200000"), new BigDecimal("30000"), 7L, 8L);

        ArgumentCaptor<SystemWallet> captor = ArgumentCaptor.forClass(SystemWallet.class);
        when(repository.save(any(SystemWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // COD
        SystemWallet codWallet = service.createEscrowRecordAfterReduceFeeCOD(order, "250000");
        verify(repository, times(1)).save(captor.capture());
        SystemWallet savedCOD = captor.getAllValues().get(0);
        assertEquals(new BigDecimal("180000"), savedCOD.getBalance());
        assertEquals(SystemWalletStatus.ESCROW_HOLD, savedCOD.getStatus());

        // Wallet payment
        SystemWallet walletWallet = service.createEscrowRecordAfterReduceFeeWalletPayment(order, "50000");
        verify(repository, times(2)).save(captor.capture());
        SystemWallet savedWalletPayment = captor.getAllValues().get(1);
        assertEquals(new BigDecimal("180000"), savedWalletPayment.getBalance());
        assertEquals(SystemWalletStatus.ESCROW_HOLD, savedWalletPayment.getStatus());

        assertNotNull(codWallet.getEndAt());
        assertNotNull(walletWallet.getEndAt());
    }
}
