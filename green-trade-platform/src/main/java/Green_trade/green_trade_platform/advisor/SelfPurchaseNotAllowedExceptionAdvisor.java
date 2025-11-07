package Green_trade.green_trade_platform.advisor;


import Green_trade.green_trade_platform.exception.SelfPurchaseNotAllowedException;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.response.RestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class SelfPurchaseNotAllowedExceptionAdvisor {
    private final ResponseMapper responseMapper;

    public SelfPurchaseNotAllowedExceptionAdvisor(ResponseMapper responseMapper) {
        this.responseMapper = responseMapper;
    }

    @ExceptionHandler(SelfPurchaseNotAllowedException.class)
    public ResponseEntity<RestResponse<?, ?>> selfPurchaseNotAllowedExceptionHandler(SelfPurchaseNotAllowedException e) {
        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "SELF PURCHASED DECTECTION",
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

