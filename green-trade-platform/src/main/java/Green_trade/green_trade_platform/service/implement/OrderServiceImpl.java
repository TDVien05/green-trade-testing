package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.enumerate.OrderStatus;
import Green_trade.green_trade_platform.enumerate.TransactionStatus;
import Green_trade.green_trade_platform.exception.OrderNotFound;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.CancelOrderReasonRepository;
import Green_trade.green_trade_platform.repository.OrderRepository;
import Green_trade.green_trade_platform.repository.TransactionRepository;
import Green_trade.green_trade_platform.request.CancelOrderRequest;
import Green_trade.green_trade_platform.service.OrderService;
import Green_trade.green_trade_platform.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final TransactionServiceImpl transactionService;
    private final GhnServiceImpl ghnServiceImpl;
    private final WalletTransactionServiceImpl walletTransactionServiceImpl;
    private final WalletServiceImpl walletService;
    private final TransactionRepository transactionRepository;
    private final CancelOrderReasonRepository cancelOrderReasonRepository;

    public Page<Order> getOrdersOfCurrentUserPaging(int size, int page, Buyer buyer) {
        try {
            log.info(">>> [OrderServiceImpl] camed getOrdersOfCurrentUserPaging");
            Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
            log.info(">>> [OrderServiceImpl] created pageable successfully");
            Page<Order> ordersPage = orderRepository.findAllByBuyer(buyer, pageable);
            log.info(">>> [OrderServiceImpl] created orderPage successfully");
            return new PageImpl<>(ordersPage.getContent(), pageable, ordersPage.getTotalElements());
        } catch (Exception e) {
            log.info(">>> Error at getOrdersOfCurrentUserPaging: {}", e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> saveOrder(Order newOrder) {
        log.info(">>> start save order service");
        Map<String, Object> data = new HashMap<>();
        try {
            Order order = orderRepository.save(newOrder);
            data.put("success", true);
            data.put("message", "save order successfully.");
            data.put("data", order);
        } catch (Exception e) {
            data.put("success", false);
            data.put("message", e.getMessage());
        }
        log.info(">>> save order service: {}", data.toString());
        return data;
    }

    public Order updateOrderCode(String orderCode, Order order) {
        order.setOrderCode(orderCode);
        return orderRepository.save(order);
    }

    public Order updateSystemWallet(SystemWallet systemWallet, Order order) {
        order.setSystemWallet(systemWallet);
        return orderRepository.save(order);
    }

    public Order updateOrderTransactions(Order order, List<Transaction> transactions) {
        order.setTransactions(transactions);
        return orderRepository.save(order);
    }

    public Order updateOrderStatus(Order order, OrderStatus status) {
        order.setStatus(status);
        return orderRepository.save(order);
    }

    public Order cancelOrder(Long id, CancelOrderRequest request) throws Exception {
        try {
            log.info(">>> [OrderServiceImpl] came cancelOrder");
            log.info(">>> request: {}", request);
            Optional<Order> orderOpt = orderRepository.findOrderById((id));
            if (orderOpt.isEmpty()) {
                throw new OrderNotFound();
            }
            log.info(">>> [OrderServiceImpl] found order successfully");

            CancelOrderReason cancelOrderReason = cancelOrderReasonRepository.findById(request.getCancelReasonId())
                    .orElseThrow(
                            () -> new Exception("Cancel Order Reason Not found")
                    );

            Order orderFound = orderOpt.get();

            if (orderFound.getStatus().equals(OrderStatus.PENDING)) {
                log.info(">>> [OrderServiceImpl] order pending status");
                Transaction transaction = transactionService.createTransaction(orderFound, TransactionStatus.CANCELED,
                        orderFound.getTransactions().getLast().getPayment()); //transaction không có thì sẽ lỗi, nên lưu ý
                log.info(">>> [OrderServiceImpl] created transaction successfully");
                orderFound = updateOrderStatus(orderFound, OrderStatus.CANCELED);
                log.info(">>> [OrderServiceImpl] update order status to canceled successfully");
                orderFound.getPostProduct().setSold(false);
                log.info(">>> [OrderServiceImpl] update order sold successfully");
                orderFound.setCancelOrderReason(cancelOrderReason);
                log.info(">>> [OrderServiceImpl] update cancel order reason successfully");
                orderFound.setCanceledAt(LocalDateTime.now());
                log.info(">>> [OrderServiceImpl] update canceled at successfully");
            } else if (orderFound.getStatus().equals(OrderStatus.PAID)) {
                log.info(">>> [OrderServiceImpl] order paid status");
                Transaction transaction = transactionService.createTransaction(orderFound, TransactionStatus.CANCELED,
                        orderFound.getTransactions().getLast().getPayment());
                log.info(">>> [OrderServiceImpl] created transaction successfully");
                orderFound = updateOrderStatus(orderFound, OrderStatus.CANCELED);
                log.info(">>> [OrderServiceImpl] update order status to canceled successfully");
                walletService.handleBuyerRefundForCancelledOrder(orderFound.getSystemWallet(), 100, orderFound.getBuyer().getWallet());
                log.info(">>> [OrderServiceImpl] refund successfully");
                orderFound.getPostProduct().setSold(false);
                log.info(">>> [OrderServiceImpl] update order sold successfully");
                orderFound.setCancelOrderReason(cancelOrderReason);
                log.info(">>> [OrderServiceImpl] update cancel order reason successfully");
                orderFound.setCanceledAt(LocalDateTime.now());
                log.info(">>> [OrderServiceImpl] update canceled at successfully");
            } else {
                throw new Exception("Cannot cancel order");
            }
            return orderRepository.save(orderFound);
        } catch (Exception e) {
            log.info(">>> [OrderServiceImpl] Error at cancelOrder: {}", e.getMessage());
            throw e;
        }
    }

    public Page<Order> getPendingOrders(Seller seller, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findByPostProduct_SellerAndStatus(seller, OrderStatus.PENDING, pageable);
    }

    public Order verifyOrder(long id) {
        Order order = orderRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Can not find order with this order id: " + id)
        );

        order.setStatus(OrderStatus.VERIFIED);
        return orderRepository.save(order);
    }

    public Order getOrderById(Long orderId) {
        Order result = null;
        Optional<Order> orderOpt = orderRepository.findOrderById(orderId);
        if (orderOpt.isPresent()) {
            result = orderOpt.get();
        }
        return result;
    }

    public Transaction getTransactionByOrderId(long id) {
        log.info(">>> [Order Service] Get transaction by order id: Started.");
        Order order = getOrderById(id);
        log.info(">>> [Order Service] Order info: {}", order);

        Transaction transaction = transactionRepository.findValidTransactionsByOrderId(order, TransactionStatus.FAIL).orElseThrow(
                () -> new EntityNotFoundException("Can not find transaction with this order id: " + id)
        );
        log.info(">>> [Order Service] Transaction info: {}", transaction);
        return transaction;
    }

    public Page<Order> getAllOrders(int page, int size, Seller seller) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findAllByPostProduct_Seller(seller, pageable);
    }
}
