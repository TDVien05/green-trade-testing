package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.controller.NotificationSocketController;
import Green_trade.green_trade_platform.enumerate.*;
import Green_trade.green_trade_platform.exception.OrderNotFound;
import Green_trade.green_trade_platform.mapper.DisputeMapper;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.DisputeCategoryRepository;
import Green_trade.green_trade_platform.repository.DisputeRepository;
import Green_trade.green_trade_platform.repository.NotificationRepository;
import Green_trade.green_trade_platform.repository.OrderRepository;
import Green_trade.green_trade_platform.request.RaiseDisputeRequest;
import Green_trade.green_trade_platform.request.ResolveDisputeRequest;
import Green_trade.green_trade_platform.response.DisputeResponse;
import Green_trade.green_trade_platform.service.DisputeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class DisputeServiceImpl implements DisputeService {
    private final DisputeCategoryRepository disputeCategoryRepository;
    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final DisputeMapper disputeMapper;
    private final NotificationRepository notificationRepository;


    public Dispute updateEvidencesForDispute(List<Evidence> evidences, Dispute dispute) {
        dispute.setEvidences(evidences);
        return disputeRepository.save(dispute);
    }

    public Dispute receiveDispute(RaiseDisputeRequest request) throws Exception {
        try {
            DisputeCategory disputeCategory = disputeCategoryRepository.findById(request.getDisputeCategoryId())
                    .orElseThrow(() ->
                            new Exception("Dispute Category is not supported")
                    );
            Order disputedOrder = orderRepository.findById(request.getOrderId())
                    .orElseThrow(
                            () -> new OrderNotFound()
                    );
//            log.info(">>> disputedOrder: {}", disputedOrder.toString());
            if (!disputedOrder.getStatus().equals(OrderStatus.COMPLETED)) {
                throw new Exception("Only completed order can be dispute");
            }
            Dispute newDispute = Dispute.builder()
                    .order(disputedOrder)
                    .disputeCategory(disputeCategory)
                    .admin(null)
                    .evidences(null)
                    .description(request.getDescription())
                    .decision(DisputeDecision.NOT_HAVE_YET)
                    .resolution("No Resolution Yet")
                    .resolutionType(ResolutionType.NOT_HAVE_YET)
                    .status(DisputeStatus.PENDING)
                    .build();
            return disputeRepository.save(newDispute);
        } catch (Exception e) {
            throw e;
        }
    }

    public Page<DisputeResponse> getAllDispute(int page, int size) {
        log.info(">>> [Dispute Service] Get all disputes: Started.");
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        Page<Dispute> disputes = disputeRepository.findAllByStatus(DisputeStatus.PENDING, pageable);
        log.info(">>> [Dispute Service] Get all disputes: Get all dispute: {}", disputes.getContent());

        List<DisputeResponse> responses = disputes.getContent()
                .stream().map(disputeMapper::toDto)
                .toList();

        log.info(">>> [Dispute Service] Get all disputes: Ended.");
        return new PageImpl<>(responses, pageable, disputes.getTotalElements());
    }

    public Notification handlePendingDispute(Admin admin, ResolveDisputeRequest request) {
        log.info(">>> [Dispute service] Handling pending dispute: Started.");
        Map<String, Object> disputeOrder = getOrderByDisputeId(request.getDisputeId());
        Dispute dispute = (Dispute) disputeOrder.get("dispute");
        Order order = (Order) disputeOrder.get("order");
        log.info(">>> [handlePendingDispute Service] dispute infor: {}", dispute.getId());
        log.info(">>> [handlePendingDispute Service] order infor: {}", order.getId());
        log.info(">>> user id: {}", order.getBuyer().getBuyerId());
        Notification notification = null;

        if (request.getDecision() == DisputeDecision.REJECTED) {
            dispute.setStatus(DisputeStatus.REJECTED);
            dispute.setResolutionType(ResolutionType.REJECTED);
            dispute.setResolution(request.getResolution());
            dispute.setAdmin(admin);
            disputeRepository.save(dispute);

            notification = Notification.builder()
                    .receiverId(order.getBuyer().getBuyerId())
                    .type(AccountType.BUYER)
                    .title("REJECT YOUR ORDER DISPUTE")
                    .content(request.getResolution())
                    .createdAt(LocalDateTime.now())
                    .build();
        } else if (request.getDecision() == DisputeDecision.ACCEPTED) {
            dispute.setStatus(DisputeStatus.ACCEPTED);
            dispute.setResolutionType(ResolutionType.REFUND);
            dispute.setResolution(request.getResolution());
            dispute.setAdmin(admin);
            disputeRepository.save(dispute);

            notification = Notification.builder()
                    .receiverId(order.getBuyer().getBuyerId())
                    .type(AccountType.BUYER)
                    .title("ACCEPTED YOUR ORDER DISPUTE")
                    .content(request.getResolution())
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        return notificationRepository.save(notification);
    }

    public Map<String, Object> getOrderByDisputeId(long disputeId) {
        Map<String, Object> data = new HashMap<>();
        Dispute dispute = disputeRepository.findById(disputeId).orElseThrow(
                () -> new IllegalArgumentException("Can not find dispute with this dispute id.")
        );
        data.put("dispute", dispute);
        data.put("order", dispute.getOrder());
        return data;
    }

    public Dispute getDisputeInfo(long disputeId) {
        return disputeRepository.findById(disputeId).orElseThrow(
                () -> new IllegalArgumentException("Can not find dispute infor with this id: " + disputeId)
        );
    }


}
