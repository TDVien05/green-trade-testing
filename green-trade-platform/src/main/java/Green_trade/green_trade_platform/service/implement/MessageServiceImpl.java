package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.controller.ChattingSocketController;
import Green_trade.green_trade_platform.enumerate.MessageType;
import Green_trade.green_trade_platform.model.Conversation;
import Green_trade.green_trade_platform.model.Message;
import Green_trade.green_trade_platform.repository.MessageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class MessageServiceImpl {
    private final MessageRepository messageRepository;
    private final CloudinaryService cloudinaryService;
    private final ChattingSocketController chattingSocketController;

    public Message handleImageMessage(Message message, MultipartFile picture) throws IOException {
        log.info(">>> [Message Service] Handling Image Message: Started");
        message.setMessageType(MessageType.FILE);
        Map<String, String> uploadedPicture = cloudinaryService.upload(picture, "message/" + message.getConversation().getId());
        message.setPublicImageId(uploadedPicture.get("publicId"));
        message.setAttachedUrl(uploadedPicture.get("fileUrl"));
        Message response = messageRepository.save(message);
        log.info(">>> [Message Service] Image message: {}", message);
        chattingSocketController.sendMessage(response);
        return response;
    }

    public Message handleTextmessage(Message message) {
        log.info(">>> [Message Service] Handling Text Message: Started");
        message.setMessageType(MessageType.TEXT);
        Message response = messageRepository.save(message);
        log.info(">>> [Message Service] Text message: {}", message);
        chattingSocketController.sendMessage(response);
        return response;
    }


    public Page<Message> getConversationMessages(int page, int size, Conversation conversation) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return messageRepository.findAllByConversation(conversation, pageable);
    }
}
