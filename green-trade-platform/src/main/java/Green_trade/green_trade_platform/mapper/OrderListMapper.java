package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.response.OrderListResponse;
import Green_trade.green_trade_platform.response.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OrderListMapper {
    private final OrderMapper orderMapper;

    public OrderListMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public OrderListResponse toDto(List<Order> orders, Map<String, Object> meta) {
        List<OrderResponse> orderListResponses = orders.stream()
                .map(
                        order -> orderMapper.toDto(order)
                )
                .toList();
        return OrderListResponse.builder()
                .orderResponses(orderListResponses)
                .meta(meta)
                .build();
    }
}
