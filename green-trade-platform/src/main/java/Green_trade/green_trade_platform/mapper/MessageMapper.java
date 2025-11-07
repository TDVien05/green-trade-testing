package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Message;
import Green_trade.green_trade_platform.request.MessageRequest;
import Green_trade.green_trade_platform.response.MessageResponse;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {
    public Message toEntity(MessageRequest request) {
        return Message.builder()
                .content(request.getContent())
                .build();
    }

    public MessageResponse toDto(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .receiverId(message.getReceiverId())
                .senderId(message.getSenderId())
                .status(message.getStatus())
                .sendAt(message.getSentAt())
                .content(message.getContent())
                .imageUrl(message.getAttachedUrl())
                .type(message.getMessageType())
                .build();
    }
}
