package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.response.OrderResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
    private final CancelOrderReasonMapper cancelOrderReasonMapper;

    public OrderMapper(CancelOrderReasonMapper cancelOrderReasonMapper) {
        this.cancelOrderReasonMapper = cancelOrderReasonMapper;
    }

    public OrderResponse toDto(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .shippingAddress(order.getShippingAddress())
                .phoneNumber(order.getPhoneNumber())
                .price(order.getPrice())
                .shippingFee(order.getShippingFee())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .canceledAt(order.getCanceledAt())
                .cancelOrderReasonResponse(cancelOrderReasonMapper.toDto(order.getCancelOrderReason()))
                .build();
    }
}
