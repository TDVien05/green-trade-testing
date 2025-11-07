package Green_trade.green_trade_platform.advisor;

import Green_trade.green_trade_platform.exception.InvalidOrderStateException;
import Green_trade.green_trade_platform.exception.OrderNotFound;
import Green_trade.green_trade_platform.exception.PasswordMismatchException;
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
public class OrderControllerAdvisor {

    private final ResponseMapper responseMapper;

    public OrderControllerAdvisor(ResponseMapper responseMapper) {
        this.responseMapper = responseMapper;
    }

    @ExceptionHandler(OrderNotFound.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> handleOrderNotFoundException(OrderNotFound e) {
        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "ORDER NOT FOUND",
                null,
                Map.of(
                        "origin", e.getStackTrace()[0].toString(),
                        "message", e.getMessage(),
                        "errorType", e.getClass().getSimpleName()
                )
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND.value()).body(response);
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> handleInvalidOrderStateException(InvalidOrderStateException e) {
        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "ORDER STATUS INVALID",
                null,
                Map.of(
                        "origin", e.getStackTrace()[0].toString(),
                        "message", e.getMessage(),
                        "errorType", e.getClass().getSimpleName()
                )
        );
        return ResponseEntity.status(HttpStatus.CONFLICT.value()).body(response);
    }
}
