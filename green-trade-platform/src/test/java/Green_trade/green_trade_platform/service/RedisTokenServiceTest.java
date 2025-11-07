package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.service.implement.RedisTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RedisTokenServiceTest {

    private RedisTokenService redisTokenService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);

        // Create a partial instance and inject mock via reflection since field is package-private and @Autowired in prod
        redisTokenService = new RedisTokenService();
        try {
            var field = RedisTokenService.class.getDeclaredField("redisTemplate");
            field.setAccessible(true);
            field.set(redisTokenService, redisTemplate);
        } catch (Exception e) {
            fail("Failed to inject redisTemplate mock: " + e.getMessage());
        }

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shouldSaveAndRetrieveRefreshToken() {
        String email = "user@example.com";
        String token = "refresh-token";
        long ttl = 3600L;

        // save
        redisTokenService.saveTokenToRedis(email, token, ttl);

        verify(valueOps, times(1)).set("refresh_token:" + email, token, ttl, TimeUnit.SECONDS);

        // retrieve
        when(valueOps.get("refresh_token:" + email)).thenReturn(token);
        String fetched = redisTokenService.getRefreshToken(email);

        assertEquals(token, fetched);
        verify(valueOps, times(1)).get("refresh_token:" + email);
    }

    @Test
    void shouldReturnTrueWhenRefreshTokenExists() {
        String email = "has.token@example.com";
        when(valueOps.get("refresh_token:" + email)).thenReturn("some-token");

        boolean exists = redisTokenService.verifyRefreshToken(email);

        assertTrue(exists);
        verify(valueOps).get("refresh_token:" + email);
    }

    @Test
    void shouldDeleteRefreshToken() {
        String email = "delete.me@example.com";

        redisTokenService.deleteRefreshToken(email);

        verify(redisTemplate, times(1)).delete("refresh_token:" + email);
    }

    @Test
    void shouldReturnFalseWhenNoRefreshTokenExists() {
        String email = "no.token@example.com";
        when(valueOps.get("refresh_token:" + email)).thenReturn(null);

        boolean exists = redisTokenService.verifyRefreshToken(email);

        assertFalse(exists);
        verify(valueOps).get("refresh_token:" + email);
    }

    @Test
    void shouldReturnNullAfterTokenExpiration() {
        String email = "expire@example.com";
        String token = "temp-token";
        long ttl = 1L;

        // Save token with short TTL
        redisTokenService.saveTokenToRedis(email, token, ttl);
        verify(valueOps).set("refresh_token:" + email, token, ttl, TimeUnit.SECONDS);

        // Simulate Redis behavior after expiration by returning null
        when(valueOps.get("refresh_token:" + email)).thenReturn(null);

        String fetched = redisTokenService.getRefreshToken(email);

        assertNull(fetched);
        verify(valueOps).get("refresh_token:" + email);
    }

}
