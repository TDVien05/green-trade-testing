package Green_trade.green_trade_platform.response;

import Green_trade.green_trade_platform.enumerate.OrderStatus;
import Green_trade.green_trade_platform.model.CancelOrderReason;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;

    private String orderCode;

    private String shippingAddress;

    private String phoneNumber;

    private BigDecimal price;

    private BigDecimal shippingFee;

    private OrderStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime canceledAt;

    private CancelOrderReasonResponse cancelOrderReasonResponse;
}
