package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.enumerate.WishListPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WishListingResponse {
    private Long id;
    private Long postId;
    private Long buyerId;
    private WishListPriority priority;
    private String note;
}
