package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.exception.WalletNotFoundException;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.SystemWallet;
import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.model.WalletTransaction;
import Green_trade.green_trade_platform.repository.WalletRepository;
import Green_trade.green_trade_platform.repository.WalletTransactionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class WalletServiceImpl {
    private final WalletRepository walletRepository;
    private final BuyerServiceImpl buyerService;
    private final WalletTransactionServiceImpl walletTransactionService;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletServiceImpl(
            WalletRepository walletRepository,
            @Lazy BuyerServiceImpl buyerService, // tránh vòng lặp dependency
            WalletTransactionServiceImpl walletTransactionService,
            WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.buyerService = buyerService;
        this.walletTransactionService = walletTransactionService;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    public Wallet createLocalWalletForBuyer(Buyer buyer) {
        Wallet wallet = Wallet.builder().buyer(buyer).build();
        return walletRepository.save(wallet);
    }

    public Wallet processDepositMoneyIntoWallet(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");

        if (walletTransactionRepository.existsByExternalTransactionReference(txnRef)) {
            log.warn(">>> [Duplicate Transaction] TxnRef {} already processed", txnRef);
            return walletTransactionRepository.findByExternalTransactionReference(txnRef)
                    .map(WalletTransaction::getWallet)
                    .orElse(null);
        }
        Wallet wallet = getWalletWithVnPayRequest(params.get("vnp_OrderInfo"));
        WalletTransaction walletTransaction = walletTransactionService.handleDepositIntoMoney(wallet, params);
        wallet.setBalance(wallet.getBalance().add(walletTransaction.getAmount()));
        walletRepository.save(wallet);
        return wallet;
    }

    public Wallet getWalletWithVnPayRequest(String params) {
        Buyer buyer = buyerService.getBuyerFromVnPayRequest(params);
        return walletRepository.findByBuyer(buyer).orElseThrow(() -> new WalletNotFoundException("Người dùng chưa được tạo ví."));
    }

    public Map<String, Object> handleSignPackageForSeller(Buyer buyer, double amount) {

        Map<String, Object> result = new HashMap<>();

        Wallet wallet = walletRepository.findByBuyer(buyer).orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy ví người dùng."));

        walletTransactionService.handleSignPackageForSeller(wallet, amount);
        wallet.setBalance(wallet.getBalance().subtract(BigDecimal.valueOf(amount)));

        result.put("success", false);
        result.put("message", "Trừ tiền thành công.");
        result.put("data", wallet);

        return result;
    }

    public boolean isBuyerHasWallet(Buyer buyer) {
        boolean result = false;
        Optional<Wallet> walletOpt = walletRepository.findByBuyer(buyer);
        if (walletOpt.isPresent()) {
            result = true;
        }
        return result;
    }

    public Wallet handleBuyerRefund(SystemWallet systemWallet, double refundPercent, Wallet wallet, boolean isSeller) {
        BigDecimal systemBalance = systemWallet.getBalance();
        BigDecimal money = systemBalance
                .multiply(BigDecimal.valueOf(refundPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (!isSeller) {
            WalletTransaction refundTransaction = walletTransactionService.handleRefundMoney(wallet, money, true, "Refund money from dispute");
            log.info(">>> [Wallet Service] Balance before refunding money for buyer: {}", wallet.getBalance());
            wallet.setBalance(wallet.getBalance().add(money));
            wallet = walletRepository.save(wallet);
            log.info(">>> [Wallet Service] Balance after refunding money for buyer: {}", wallet.getBalance());
        } else {
            WalletTransaction refundTransaction = walletTransactionService.handleRefundMoney(wallet, money, false, "Refund money from dispute");
            log.info(">>> [Wallet Service] Balance before refunding money for seller: {}", wallet.getBalance());
            wallet.setBalance(wallet.getBalance().add(money));
            wallet = walletRepository.save(wallet);
            log.info(">>> [Wallet Service] Balance after refunding money for seller: {}", wallet.getBalance());
        }
        return wallet;
    }

    public Wallet handleBuyerRefundForCancelledOrder(SystemWallet systemWallet, double refundPercent, Wallet wallet) {
        BigDecimal systemBalance = systemWallet.getBalance();
        BigDecimal money = systemBalance
                .multiply(BigDecimal.valueOf(refundPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        WalletTransaction refundTransaction = walletTransactionService.handleRefundMoney(wallet, money, true, "REFUNDED FROM CANCELED ORDER");
        log.info(">>> [Wallet Service] Balance before refunding money for buyer: {}", wallet.getBalance());
        wallet.setBalance(wallet.getBalance().add(money));
        wallet = walletRepository.save(wallet);
        log.info(">>> [Wallet Service] Balance after refunding money for buyer: {}", wallet.getBalance());
        return wallet;
    }

    public Wallet findWalletById(Long buyerWalletId) {
        return walletRepository.findByWalletId(buyerWalletId).orElseThrow(
                () -> new IllegalArgumentException("Can not find wallet with this wallet id: " + buyerWalletId)
        );
    }

    public Page<WalletTransaction> getTransactionHistory(Buyer buyer, int page, int size) {
        Wallet wallet = buyer.getWallet();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return walletTransactionRepository.findByWallet(wallet, pageable);
    }

}
