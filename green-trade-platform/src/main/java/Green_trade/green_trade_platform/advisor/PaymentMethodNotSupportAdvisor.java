package Green_trade.green_trade_platform.advisor;

import Green_trade.green_trade_platform.exception.PaymentMethodNotSupportedException;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.response.RestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PaymentMethodNotSupportAdvisor {

    private final ResponseMapper responseMapper;

    public PaymentMethodNotSupportAdvisor(ResponseMapper responseMapper) {
        this.responseMapper = responseMapper;
    }

    @ExceptionHandler(PaymentMethodNotSupportedException.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> paymentMethodNotSupportHandler(PaymentMethodNotSupportedException e) {
        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                true,
                "PAYMENT METHOD NOT SUPPORTED",
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
