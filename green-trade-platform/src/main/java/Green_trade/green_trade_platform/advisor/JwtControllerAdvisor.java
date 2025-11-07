package Green_trade.green_trade_platform.advisor;

import Green_trade.green_trade_platform.exception.JwtException;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.response.RestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class JwtControllerAdvisor {
    private final ResponseMapper responseMapper;

    public JwtControllerAdvisor(ResponseMapper responseMapper) {
        this.responseMapper = responseMapper;
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> handleJwtException(JwtException ex) {
        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "Unauthorized",
                null,
                Map.of(
                        "origin", ex.getStackTrace()[0].toString(),
                        "message", ex.getMessage(),
                        "errorType", ex.getClass().getSimpleName()
                )
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()).body(response);
    }
}
