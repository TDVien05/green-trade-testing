package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.enumerate.TransactionStatus;
import Green_trade.green_trade_platform.enumerate.TransactionType;
import Green_trade.green_trade_platform.exception.PaymentMethodNotSupportedException;
import Green_trade.green_trade_platform.exception.PostProductNotFound;
import Green_trade.green_trade_platform.exception.ProfileException;
import Green_trade.green_trade_platform.exception.WalletNotFoundException;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.*;
import Green_trade.green_trade_platform.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final WalletServiceImpl walletService;
    private final BuyerServiceImpl buyerService;
    private final BuyerRepository buyerRepository;
    private final PostProductRepository postProductRepository;
    private final PostProductServiceImpl postProductService;
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public List<Transaction> getTransactionsOfOrder(Order order) {
        List<Transaction> transactions = transactionRepository.findAllByOrder(order);
        return transactions;
    }

    public Transaction checkoutWalletPayment(String username, Long postProductId, Long paymentId, Order order) throws Exception {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isEmpty()) {
                throw new PaymentMethodNotSupportedException();
            }

            Buyer buyer = buyerService.findBuyerByUsername(username);

            if (buyer == null) {
                throw new ProfileException("Buyer with Username: " + username + "is not existed");
            }

            if (!walletService.isBuyerHasWallet(buyer)) {
                throw new WalletNotFoundException("The wallet of Buyer is not existed");
            }

            Optional<PostProduct> postProductOpt = postProductRepository.findById(postProductId);
            if (postProductOpt.isEmpty()) {
                throw new PostProductNotFound();
            }

            if (postProductOpt.get().isSold()) {
                throw new Exception("The product is unavailable");
            }

            Wallet wallet = buyerService.getWallet();
            log.info(">>> wallet balance: {}", wallet.getBalance());
            log.info(">>> order total price: {}", order.getPrice());
            BigDecimal moneyHandler = wallet.getBalance().subtract(order.getPrice().add(order.getShippingFee()));
            log.info(">>> moneyHandler: {}", moneyHandler);
            if (moneyHandler.compareTo(new BigDecimal("0")) < 0) {
                Transaction newTransaction = Transaction.builder()
                        .order(order)
                        .payment(paymentOpt.get())
                        .amount(order.getPrice().add(order.getShippingFee()))
                        .currency("VND")
                        .paymentMethod(paymentOpt.get().getGatewayName())
                        .status(TransactionStatus.FAIL)
                        .build();
                transactionRepository.save(newTransaction);
                throw new Exception("The money in wallet is not enough to checkout");
            }
            wallet.setBalance(moneyHandler);
            walletRepository.save(wallet);
            // Create a wallet transaction
            WalletTransaction walletTransaction = WalletTransaction.builder()
                    .type(TransactionType.PLACE_ORDER)
                    .amount((order.getPrice().add(order.getShippingFee()).negate()))
                    .balanceBefore(wallet.getBalance())
                    .status(TransactionStatus.SUCCESS)
                    .description("Đặt hàng cho đơn " + order.getId())
                    .order(order)
                    .wallet(wallet)
                    .build();
            walletTransactionRepository.save(walletTransaction);

            Transaction newTransaction = Transaction.builder()
                    .order(order)
                    .payment(paymentOpt.get())
                    .amount(postProductOpt.get().getPrice().add(order.getShippingFee()))
                    .currency("VND")
                    .paymentMethod(paymentOpt.get().getGatewayName())
                    .status(TransactionStatus.SUCCESS)
                    .build();
            return transactionRepository.save(newTransaction);
        } catch (Exception e) {
            log.info(">>> Error at checkoutWalletPayment: {}", e.getMessage());
            throw e;
        }
    }

    public Transaction checkoutCODPayment(String username, Long postProductId, Long paymentId, Order order) throws Exception {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isEmpty()) {
                throw new PaymentMethodNotSupportedException();
            }
            if (!buyerService.isBuyerExisted(username)) {
                throw new ProfileException("Buyer is not existed");
            }

            Buyer buyer = buyerRepository.findByUsername(username)
                    .orElseThrow(() -> new ProfileException("Buyer is not existed"));

            Optional<PostProduct> postProductOpt = postProductRepository.findById(paymentId);
            if (postProductOpt.isEmpty()) {
                throw new PostProductNotFound();
            }

            Transaction newTransaction = Transaction.builder()
                    .order(order)
                    .payment(paymentOpt.get())
                    .amount(order.getPrice().add(order.getShippingFee()))
                    .currency("VND")
                    .paymentMethod(paymentOpt.get().getGatewayName())
                    .status(TransactionStatus.PENDING)
                    .build();

            return transactionRepository.save(newTransaction);
        } catch (Exception e) {
            log.info(">>> Error at checkoutCODPayment: {}", e.getMessage());
            throw e;
        }
    }

    public Transaction createTransaction(Order order, TransactionStatus status, Payment payment) {
        log.info(">>> [TransactionServiceImpl] came createTransaction");
        Transaction transaction = Transaction.builder()
                .amount(order.getPrice())
                .currency("VND")
                .status(status)
                .paymentMethod(payment.getGatewayName())
                .order(order)
                .payment(payment)
                .build();
        log.info(">>> [TransactionServiceImpl] created transaction successfully");
        return transactionRepository.save(transaction);
    }
}
