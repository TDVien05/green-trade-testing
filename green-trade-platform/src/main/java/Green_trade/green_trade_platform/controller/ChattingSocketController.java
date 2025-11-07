package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Message;
import Green_trade.green_trade_platform.model.Notification;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChattingSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final BuyerServiceImpl buyerService;

    public void sendMessage(Message message) {
        log.info(">>> [Chatting Socket Controller] Send message notification to user: {}", message);
        Long receiverId = message.getReceiverId();
        String destination = "/chatting/notifications/" + receiverId;
        messagingTemplate.convertAndSend(destination, message);
    }
}
