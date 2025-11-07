package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.response.OrderHistoryListResponse;
import Green_trade.green_trade_platform.response.OrderHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OrderHistoryListMapper {

    private final OrderHistoryMapper orderHistoryMapper;

    public OrderHistoryListMapper(OrderHistoryMapper orderHistoryMapper) {
        this.orderHistoryMapper = orderHistoryMapper;
    }

    public OrderHistoryListResponse toDto(Page<Order> ordersPaging, Map<String, Object> meta) {
        List<OrderHistoryResponse> orderHistoryResponses = new ArrayList<>();
        ordersPaging.forEach(order -> {
            orderHistoryResponses.add(orderHistoryMapper.toDto(order));
        });
        return OrderHistoryListResponse.builder()
                .orderHistoryResponses(orderHistoryResponses)
                .meta(meta)
                .build();
    }
}
