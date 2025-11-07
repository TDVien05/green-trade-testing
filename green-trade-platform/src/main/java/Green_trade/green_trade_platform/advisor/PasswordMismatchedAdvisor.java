package Green_trade.green_trade_platform.advisor;

import Green_trade.green_trade_platform.exception.PasswordMismatchException;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.response.RestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PasswordMismatchedAdvisor {
    private final ResponseMapper responseMapper;

    public PasswordMismatchedAdvisor(ResponseMapper responseMapper) {
        this.responseMapper = responseMapper;
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> handlePasswordMismatchedException(PasswordMismatchException e) {
        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "Password and Confirm Password is not matched",
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
