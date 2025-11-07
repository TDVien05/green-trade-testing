package Green_trade.green_trade_platform.advisor;

import Green_trade.green_trade_platform.exception.SubscriptionExpiredException;
import Green_trade.green_trade_platform.exception.SubscriptionNotFound;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.response.RestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class SubscriptionAdvisor {

    private final ResponseMapper responseMapper;

    public SubscriptionAdvisor(ResponseMapper responseMapper) {
        this.responseMapper = responseMapper;
    }

    @ExceptionHandler(SubscriptionExpiredException.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> subscriptionExpiredHandler(SubscriptionExpiredException e) {
        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "SELLER SUBSCRIPTION EXPIRED",
                null,
                Map.of(
                        "origin", e.getStackTrace()[0].toString(),
                        "message", e.getMessage(),
                        "errorType", e.getClass().getSimpleName()
                )
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(response);
    }

    @ExceptionHandler(SubscriptionNotFound.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> subscriptionNotFoundHandler(SubscriptionNotFound e) {
        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "USER'S SUBSCRIPTION NOT FOUND",
                null,
                Map.of(
                        "origin", e.getStackTrace()[0].toString(),
                        "message", e.getMessage(),
                        "errorType", e.getClass().getSimpleName()
                )
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(response);
    }
}
