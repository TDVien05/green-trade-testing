package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.enumerate.SystemWalletStatus;
import Green_trade.green_trade_platform.exception.SystemWalletException;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.model.SystemWallet;
import Green_trade.green_trade_platform.repository.SystemWalletRepossitory;
import Green_trade.green_trade_platform.request.RefundResolveRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class SystemWalletServiceImpl {
    private final SystemWalletRepossitory systemWalletRepossitory;

    public void handleRefund(SystemWallet systemWallet) {
        systemWallet.setStatus(SystemWalletStatus.IS_SOLVED);
        systemWallet.setEndAt(LocalDateTime.now());
        systemWalletRepossitory.save(systemWallet);
    }

    public SystemWallet getSystemWalletByOrder(Order order) {
        return systemWalletRepossitory.findByOrder(order).orElseThrow(
                () -> new IllegalArgumentException("This order does not have any escrow service (system wallet).")
        );
    }

    public SystemWallet createEscrowRecord(Order order) {
        try {
            log.info(">>> [SystemWalletServiceImpl] the system came createEscrowRecord");
            SystemWallet escrowRecord = SystemWallet.builder()
                    .admin(null)
                    .order(order)
                    .buyerWalletId(order.getBuyer().getWallet().getWalletId())
                    .sellerWalletId(order.getPostProduct().getSeller().getBuyer().getWallet().getWalletId())
                    .concurrency("VND")
                    .balance(order.getPrice())
                    .status(SystemWalletStatus.ESCROW_HOLD)
                    .endAt(LocalDateTime.now().plusWeeks(2))
                    .build();
            log.info(">>> [SystemWalletServiceImpl] create new escrowRecord");
            return systemWalletRepossitory.save(escrowRecord);
        } catch (Exception e) {
            log.info(">>> [SystemWalletServiceImpl] Error at createEscrowRecord: {}", e.getMessage());
            throw new SystemWalletException();
        }
    }

    public SystemWallet createEscrowRecordAfterReduceFeeCOD(Order order, String totalFee) {
        try {
            BigDecimal productPrice = order.getPrice();
            BigDecimal shippingFee = order.getShippingFee();
            BigDecimal totalFeeInNumber = new BigDecimal(totalFee);
            BigDecimal actualReceivedMoney = productPrice.subtract(totalFeeInNumber.subtract(shippingFee).subtract(productPrice));
            log.info(">>> [SystemWalletServiceImpl] the system came createEscrowRecordAfterReduceFeeCOD");
            SystemWallet escrowRecord = SystemWallet.builder()
                    .admin(null)
                    .order(order)
                    .buyerWalletId(order.getBuyer().getWallet().getWalletId())
                    .sellerWalletId(order.getPostProduct().getSeller().getBuyer().getWallet().getWalletId())
                    .concurrency("VND")
                    .balance(actualReceivedMoney)
                    .status(SystemWalletStatus.ESCROW_HOLD)
                    .endAt(LocalDateTime.now().plusWeeks(2))
                    .build();
            log.info(">>> [SystemWalletServiceImpl] create new escrowRecord");
            return systemWalletRepossitory.save(escrowRecord);
        } catch (Exception e) {
            log.info(">>> [SystemWalletServiceImpl] Error at createEscrowRecord: {}", e.getMessage());
            throw new SystemWalletException();
        }
    }

    public SystemWallet createEscrowRecordAfterReduceFeeWalletPayment(Order order, String totalFee) {
        try {
            log.info(">>> 1 ");
            BigDecimal productPrice = order.getPrice();
            log.info(">>> 1 ");
            BigDecimal shippingFee = order.getShippingFee();
            log.info(">>> 1 ");
            BigDecimal totalFeeInNumber = new BigDecimal(totalFee);
            log.info(">>> 1 ");
            BigDecimal actualReceivedMoney = productPrice.subtract(totalFeeInNumber.subtract(shippingFee));
            log.info(">>> 1 ");
            log.info(">>> [SystemWalletServiceImpl] the system came createEscrowRecordAfterReduceFeeWalletPayment");
            SystemWallet escrowRecord = SystemWallet.builder()
                    .admin(null)
                    .order(order)
                    .buyerWalletId(order.getBuyer().getWallet().getWalletId())
                    .sellerWalletId(order.getPostProduct().getSeller().getBuyer().getWallet().getWalletId())
                    .concurrency("VND")
                    .balance(actualReceivedMoney)
                    .status(SystemWalletStatus.ESCROW_HOLD)
                    .endAt(LocalDateTime.now().plusWeeks(2))
                    .build();
            log.info(">>> [SystemWalletServiceImpl] create new escrowRecord");
            return systemWalletRepossitory.save(escrowRecord);
        } catch (Exception e) {
            log.info(">>> [SystemWalletServiceImpl] Error at createEscrowRecord: {}", e.getMessage());
            throw new SystemWalletException();
        }
    }

    public SystemWallet updateEscrowRecordStatus(SystemWallet escrowRecord, SystemWalletStatus status) {
        escrowRecord.setStatus(status);
        return systemWalletRepossitory.save(escrowRecord);
    }
}
