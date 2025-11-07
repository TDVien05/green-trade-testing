package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.service.implement.RedisOtpService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RedisOtpServiceTest {

    private RedisOtpService redisOtpService;
    private StringRedisTemplate stringRedisTemplate;
    private ObjectMapper objectMapper;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setup() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        objectMapper = mock(ObjectMapper.class);
        valueOperations = mock(ValueOperations.class);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        redisOtpService = new RedisOtpService();
        // Inject mocks via reflection since fields are @Autowired and private
        try {
            java.lang.reflect.Field redisField = RedisOtpService.class.getDeclaredField("stringRedisTemplate");
            redisField.setAccessible(true);
            redisField.set(redisOtpService, stringRedisTemplate);

            java.lang.reflect.Field mapperField = RedisOtpService.class.getDeclaredField("objectMapper");
            mapperField.setAccessible(true);
            mapperField.set(redisOtpService, objectMapper);
        } catch (Exception e) {
            fail("Failed to inject mocks: " + e.getMessage());
        }
    }

    @Test
    void shouldSavePendingOtpWithTtlAndRetrieveJson() throws Exception {
        String username = "alice";
        String email = "alice@example.com";
        String otp = "123456";

        Map<String, String> data = Map.of("username", username, "email", email, "otp", otp);
        String expectedJson = "{\"username\":\"alice\",\"email\":\"alice@example.com\",\"otp\":\"123456\"}";
        when(objectMapper.writeValueAsString(data)).thenReturn(expectedJson);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        redisOtpService.savePending(username, email, otp);

        verify(valueOperations, times(1)).set(keyCaptor.capture(), jsonCaptor.capture(), ttlCaptor.capture());
        assertEquals("pending:email:" + email, keyCaptor.getValue());
        assertEquals(expectedJson, jsonCaptor.getValue());
        assertEquals(Duration.ofMinutes(10), ttlCaptor.getValue());
    }

    @Test
    void shouldGetPendingOtpAsDeserializedMap() throws Exception {
        String email = "bob@example.com";
        String key = "pending:email:" + email;
        String json = "{\"username\":\"bob\",\"email\":\"bob@example.com\",\"otp\":\"654321\"}";

        when(valueOperations.get(key)).thenReturn(json);
        Map<String, String> expected = new HashMap<>();
        expected.put("username", "bob");
        expected.put("email", "bob@example.com");
        expected.put("otp", "654321");

        when(objectMapper.readValue(eq(json), any(TypeReference.class))).thenReturn(expected);

        Map<String, String> actual = redisOtpService.getPending(email);

        assertNotNull(actual);
        assertEquals(expected, actual);
        verify(valueOperations, times(1)).get(key);
    }

    @Test
    void shouldDeletePendingOtpKey() {
        String email = "charlie@example.com";
        String key = "pending:email:" + email;

        redisOtpService.deletePending(email);

        verify(stringRedisTemplate, times(1)).delete(key);
    }

    @Test
    void shouldSaveAndRetrievePendingBuyerWithTtl() throws Exception {
        String username = "dana";
        String password = "s3cr3t";
        String email = "dana@example.com";
        String otp = "777999";

        Map<String, String> data = Map.of(
                "username", username,
                "password", password,
                "email", email,
                "otp", otp
        );
        String expectedJson = "{\"username\":\"dana\",\"password\":\"s3cr3t\",\"email\":\"dana@example.com\",\"otp\":\"777999\"}";
        when(objectMapper.writeValueAsString(data)).thenReturn(expectedJson);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        redisOtpService.savePendingBuyer(username, password, email, otp);

        verify(valueOperations, times(1)).set(keyCaptor.capture(), jsonCaptor.capture(), ttlCaptor.capture());
        assertEquals("signup:email:" + email, keyCaptor.getValue());
        assertEquals(expectedJson, jsonCaptor.getValue());
        assertEquals(Duration.ofMinutes(10), ttlCaptor.getValue());

        // Now simulate retrieval
        String key = "signup:email:" + email;
        when(valueOperations.get(key)).thenReturn(expectedJson);
        when(objectMapper.readValue(eq(expectedJson), any(TypeReference.class))).thenReturn(data);

        Map<String, String> retrieved = redisOtpService.getPendingBuyer(email);
        assertNotNull(retrieved);
        assertEquals(username, retrieved.get("username"));
        assertEquals(password, retrieved.get("password"));
        assertEquals(email, retrieved.get("email"));
        assertEquals(otp, retrieved.get("otp"));
    }

    @Test
    void shouldReturnNullWhenPendingKeyMissing() {
        String email1 = "notfound1@example.com";
        String email2 = "notfound2@example.com";

        when(valueOperations.get("pending:email:" + email1)).thenReturn(null);
        when(valueOperations.get("signup:email:" + email2)).thenReturn(null);

        assertNull(redisOtpService.getPending(email1));
        assertNull(redisOtpService.getPendingBuyer(email2));
    }

    @Test
    void shouldReturnNullOnMalformedJsonDuringGet() throws Exception {
        String email1 = "badjson1@example.com";
        String email2 = "badjson2@example.com";
        String badJson = "{malformed";

        when(valueOperations.get("pending:email:" + email1)).thenReturn(badJson);
        when(valueOperations.get("signup:email:" + email2)).thenReturn(badJson);

        when(objectMapper.readValue(eq(badJson), any(TypeReference.class))).thenThrow(new RuntimeException("parse error"));

        assertNull(redisOtpService.getPending(email1));
        assertNull(redisOtpService.getPendingBuyer(email2));

        verify(objectMapper, times(2)).readValue(eq(badJson), any(TypeReference.class));
    }
}
