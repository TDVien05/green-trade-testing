package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.response.RestResponse;
import org.springframework.stereotype.Component;

@Component
public class ResponseMapper {
    public <T, E> RestResponse<T, E> toDto(boolean isSuccess, String message, T data, E error) {
        return RestResponse.<T, E>builder()
                .success(isSuccess)
                .message(message)
                .data(data)
                .error(error)
                .build();
    }
}
