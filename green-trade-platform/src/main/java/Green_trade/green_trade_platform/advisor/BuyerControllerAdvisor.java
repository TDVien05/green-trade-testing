package Green_trade.green_trade_platform.advisor;

import Green_trade.green_trade_platform.exception.DuplicateProfileException;
import Green_trade.green_trade_platform.exception.EmailException;
import Green_trade.green_trade_platform.exception.ProfileException;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.response.RestResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;


@RestControllerAdvice
@Slf4j
public class BuyerControllerAdvisor {
    @Autowired
    private ResponseMapper responseMapper;

    @ExceptionHandler(ProfileException.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> handleProfileNotFoundException(ProfileException e, HttpServletRequest request) {
        log.info(">>> Exception message of profile not found: {}", e.getMessage());

        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "PROFILE NOT FOUND",
                null,
                Map.of(
                        "origin", e.getStackTrace()[0].toString(),
                        "message", e.getMessage(),
                        "errorType", e.getClass().getSimpleName()
                )
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DuplicateProfileException.class)
    public ResponseEntity<RestResponse<Object, Map<String, String>>> handleDuplicateProfileException(DuplicateProfileException e, HttpServletRequest request) {
        RestResponse<Object, Map<String, String>> response = responseMapper.toDto(
                false,
                "DUPLICATE PROFILE",
                null,
                Map.of(
                        "origin", e.getStackTrace()[0].toString(),
                        "message", e.getMessage(),
                        "errorType", e.getClass().getSimpleName()
                )
        );
        return ResponseEntity.status(HttpStatus.CONFLICT.value()).body(response);
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<RestResponse<Object, Object>> handleEmailException(EmailException e) {
        RestResponse<Object, Object> response = responseMapper.toDto(
                false,
                "Email Exception",
                null,
                Map.of(
                        "origin", e.getStackTrace()[0].toString(),
                        "message", e.getMessage(),
                        "errorType", e.getClass().getSimpleName()
                )
        );
        return ResponseEntity.internalServerError().body(response);
    }
}
