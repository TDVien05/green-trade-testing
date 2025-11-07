package Green_trade.green_trade_platform.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageRequest {
    private Long conversationId;
    private Long buyerId;
    private Long postId;
    private String content;
}
