package Green_trade.green_trade_platform.advisor;

import Green_trade.green_trade_platform.exception.AuthException;
import Green_trade.green_trade_platform.exception.InvalidArgumentException;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.response.RestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class AuthControllerAdvisor {
    @Autowired
    private ResponseMapper responseMapper;

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<RestResponse<Object, InvalidArgumentException>> handleInvalidArgumentException(InvalidArgumentException ex) {
        RestResponse<Object, InvalidArgumentException> response = responseMapper.toDto(
                false,
                ex.getMessage(),
                null,
                ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(response);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> handleAuthException(AuthException e) {
        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "Unauthorized",
                null,
                Map.of(
                        "errorType", e.getClass().getSimpleName(),
                        "message", e.getMessage(),
                        "origin", e.getStackTrace()[0].toString(),
                        "status", HttpStatus.UNAUTHORIZED.toString()
                )
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    // Handle validation errors for @Valid Buyer (or BuyerDto)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Lấy toàn bộ field lỗi + message
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "Validated data failed",
                null,
                errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
