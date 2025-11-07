package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.response.OrderHistoryResponse;
import Green_trade.green_trade_platform.response.OrderResponse;
import Green_trade.green_trade_platform.response.PostProductResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderHistoryMapper {

    private final OrderMapper orderMapper;
    private final PostProductMapper postProductMapper;

    public OrderHistoryMapper(OrderMapper orderMapper, PostProductMapper postProductMapper) {
        this.orderMapper = orderMapper;
        this.postProductMapper = postProductMapper;
    }

    public OrderHistoryResponse toDto(Order order) {

        return OrderHistoryResponse.builder()
                .orderResponse(orderMapper.toDto(order))
                .postProduct(postProductMapper.toDto(order.getPostProduct()))
                .build();
    }
}
