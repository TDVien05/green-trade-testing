package Green_trade.green_trade_platform.util;

import Green_trade.green_trade_platform.exception.JwtException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

@Component
@Slf4j
public class JwtUtils {
    @Value("${spring.application.issuer}")
    private String issuer;

    @Value("${spring.application.secret}")
    private String secret;

    public String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        log.info("Authorization token : {}", token);
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        } else {
            throw new JwtException("Do not have authentication token in request!!!");
        }
    }

    public String generateTokenFromUsername(String username, long expireTime) {
        try {
            return JWT.create()
                    .withSubject(username)
                    .withIssuer(issuer)
                    .withIssuedAt(new Date())
                    .withExpiresAt(new Date((new Date().getTime() + expireTime)))
                    .sign(Algorithm.HMAC256(secret));
        } catch (JWTCreationException e) {
            log.info("Generate token failed: {}", e.getMessage());
            throw new JwtException("Generate JWT token failed at generateTokenFromUsername: " + e.getMessage());
        }
    }

    public String getUsernameFromToken(String token) {
        return JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(issuer)
                .build()
                .verify(token)
                .getSubject();
    }

    public boolean verifyToken(String token) {
        JWTVerifier verifier = null;
        try {
            verifier = JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.info("Validate token failed: {}", e.getMessage());
            throw new JwtException("Validate token failed at verifyToken: " + e.getMessage());
        }
    }
}
