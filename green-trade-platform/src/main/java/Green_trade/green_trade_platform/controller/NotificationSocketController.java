package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Notification;
import Green_trade.green_trade_platform.response.ApproveSellerResponse;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final BuyerServiceImpl buyerService;

    // Called by services when new notifications are created
    public void sendUpgradeNotificationToUser(ApproveSellerResponse notification) {
        log.info(">>> [Notification Socket Controller] Send upgrade notification to user: {}", notification);
        Buyer buyer = buyerService.findBuyerBySellerId(notification.getSellerId());
        String destination = "/queue/notifications/" + buyer.getBuyerId();
        messagingTemplate.convertAndSend(destination, notification);
    }

    public void sendNotificationToUser(Notification notification) {
        log.info(">>> [Notification Socket Controller]: {}", notification);
        String destination = "/queue/notifications/" + notification.getReceiverId();
        messagingTemplate.convertAndSend(destination, notification);
    }
}
