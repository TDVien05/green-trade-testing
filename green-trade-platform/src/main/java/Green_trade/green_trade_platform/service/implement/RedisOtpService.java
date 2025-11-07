package Green_trade.green_trade_platform.service.implement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class RedisOtpService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void savePending(String username, String email, String otp) {
        try {
            Map<String, String> data = Map.of("username", username,
                    "email", email,
                    "otp", otp
            );
            String json = objectMapper.writeValueAsString(data);
            String key = "pending:email:" + email;
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofMinutes(10));
            log.info("Save pending buyer in Redis with key: {}", key);
        } catch (Exception e) {
            log.info(">>> Error at SavePending: " + e);
        }
    }

    public Map<String, String> getPending(String email) {
        String key = "pending:email:" + email;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null)
            return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            log.info("Error when getting pending user from redis: {}", e.getMessage());
        }
        return null;
    }

    public void deletePending(String email) {
        String key = "pending:email:" + email;
        stringRedisTemplate.delete(key);
        log.info("Delete pending buyer from Redis: {}", key);
    }

    public void savePendingBuyer(String username, String password, String email, String otp) {
        try {
            Map<String, String> data = Map.of("username", username,
                    "password", password,
                    "email", email,
                    "otp", otp);
            String json = objectMapper.writeValueAsString(data);
            String key = "signup:email:" + email;
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofMinutes(10));
            log.info("Save pending buyer in Redis with key: {}", key);
        } catch (Exception e) {
            log.info("Error of saving pending buyer to redis: {}", e.getMessage());
        }
    }

    public Map<String, String> getPendingBuyer(String email) {
        String key = "signup:email:" + email;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null)
            return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            log.info("Error when getting pending buyer from redis: {}", e.getMessage());
        }
        return null;
    }

    public void deletePendingBuyer(String email) {
        String key = "signup:email:" + email;
        stringRedisTemplate.delete(key);
        log.info("Delete pending buyer from Redis: {}", key);
    }
}
