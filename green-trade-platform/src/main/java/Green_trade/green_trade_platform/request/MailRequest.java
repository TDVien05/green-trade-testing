package Green_trade.green_trade_platform.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MailRequest {
    private String from;
    private String to;
    private String subject;
    private String message;
}
