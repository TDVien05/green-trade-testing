package Green_trade.green_trade_platform.service.implement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisTokenService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void saveTokenToRedis(String email, String token, long expireTime) {
        String key = "refresh_token:" + email;
        redisTemplate.opsForValue().set(key, token, expireTime, TimeUnit.SECONDS);
        log.info("Refresh token was save in Redis with key: {}", key);
    }

    public boolean verifyRefreshToken(String email) {
        String refreshToken = getRefreshToken(email);
        return refreshToken != null;
    }

    public String getRefreshToken(String email) {
        String key = "refresh_token:" + email;
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteRefreshToken(String email) {
        String key = "refresh_token:" + email;
        redisTemplate.delete(key);
        log.info("Delete refresh token successfully with key: {}", key);
    }

}
