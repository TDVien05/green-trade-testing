package Green_trade.green_trade_platform.request;

import Green_trade.green_trade_platform.enumerate.WishListPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishListRequest {
    private Long postId;
    private WishListPriority priority;
    private String note;
}
