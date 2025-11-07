package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.enumerate.MessageStatus;
import Green_trade.green_trade_platform.enumerate.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long receiverId;
    private Long senderId;
    private MessageStatus status;
    private LocalDateTime sendAt;
    private String content;
    private String imageUrl;
    private MessageType type;
}
