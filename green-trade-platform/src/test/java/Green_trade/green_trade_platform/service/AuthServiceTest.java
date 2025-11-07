package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.exception.AuthException;
import Green_trade.green_trade_platform.exception.PasswordMismatchException;
import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.repository.AdminRepository;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.request.ChangePasswordRequest;
import Green_trade.green_trade_platform.request.ForgotPasswordRequest;
import Green_trade.green_trade_platform.request.VerifyOtpForgotPasswordRequest;
import Green_trade.green_trade_platform.service.implement.AuthServiceImpl;
import Green_trade.green_trade_platform.service.implement.OtpServiceImpl;
import Green_trade.green_trade_platform.service.implement.RedisOtpService;
import Green_trade.green_trade_platform.service.implement.RedisTokenService;
import Green_trade.green_trade_platform.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private OtpServiceImpl otpService;
    @Mock
    private RedisOtpService redisOtpService;
    @Mock
    private DelegatingPasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private RedisTokenService redisTokenService;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthServiceImpl authService;

    private Buyer sampleBuyer;

    @BeforeEach
    void setup() {
        sampleBuyer = Buyer.builder()
                .buyerId(1L)
                .username("testuser")
                .email("test@example.com")
                .password("oldHash")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testVerifyUsernameForgotPassword_successSavesOtpAndSendsEmail() throws Exception {
        when(buyerRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleBuyer));
        when(otpService.generateOtpCode()).thenReturn("123456");

        Map<String, Object> result = authService.verifyUsernameForgotPassword("testuser");

        assertTrue((Boolean) result.get("success"));
        assertEquals("Username exits.", result.get("message"));
        assertNotNull(result.get("data"));
        assertNull(result.get("error"));

        verify(redisOtpService, times(1)).savePending(eq("testuser"), eq("test@example.com"), eq("123456"));
        verify(otpService, times(1)).sendOtpEmail(eq("test@example.com"), eq("123456"));
    }

    @Test
    void testVerifyOtpForgotPassword_validOtpDeletesPending() {
        VerifyOtpForgotPasswordRequest req = VerifyOtpForgotPasswordRequest.builder()
                .email("test@example.com")
                .otp("654321")
                .build();

        when(redisOtpService.getPending("test@example.com"))
                .thenReturn(Map.of("username", "testuser", "email", "test@example.com", "otp", "654321"));

        assertDoesNotThrow(() -> authService.verifyOtpForgotPassword(req));
        verify(redisOtpService, times(1)).deletePending("test@example.com");
    }

    @Test
    void testChangePassword_successUpdatesPassword() throws Exception {
        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .username("testuser")
                .oldPassword("oldPass")
                .newPassword("Newpass1!")
                .confirmPassword("Newpass1!")
                .build();

        when(buyerRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleBuyer));
        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("Newpass1!")).thenReturn("newHash");
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Buyer updated = authService.changePassword(req);

        assertEquals("newHash", updated.getPassword());
        ArgumentCaptor<Buyer> captor = ArgumentCaptor.forClass(Buyer.class);
        verify(buyerRepository).save(captor.capture());
        assertEquals("newHash", captor.getValue().getPassword());
    }

    @Test
    void testVerifyUsernameForgotPassword_usernameNotFoundReturnsFailurePayload() throws Exception {
        when(buyerRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

        Map<String, Object> result = authService.verifyUsernameForgotPassword("unknownuser");

        assertFalse((Boolean) result.get("success"));
        assertEquals("Username is not exits.", result.get("message"));
        assertNull(result.get("data"));
        assertNotNull(result.get("error"));
        verifyNoInteractions(otpService);
        verify(redisOtpService, never()).savePending(anyString(), anyString(), anyString());
    }

    @Test
    void testVerifyOtpForgotPassword_missingPendingThrows() {
        VerifyOtpForgotPasswordRequest req = VerifyOtpForgotPasswordRequest.builder()
                .email("missing@example.com")
                .otp("000000")
                .build();

        when(redisOtpService.getPending("missing@example.com")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.verifyOtpForgotPassword(req));
        assertTrue(ex.getMessage().contains("Invalid email"));
        verify(redisOtpService, never()).deletePending(anyString());
    }

    @Test
    void testRefreshToken_headerTokenValidationAndRoleBranching() {
        // Missing header -> throws AuthException
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        assertThrows(AuthException.class, () -> authService.refreshToken(httpServletRequest));

        // Invalid prefix -> throws AuthException
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Token abc");
        assertThrows(AuthException.class, () -> authService.refreshToken(httpServletRequest));

        // Invalid token -> jwtUtils.verifyToken returns false leads to AuthException
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer invalidtoken");
        when(jwtUtils.verifyToken("invalidtoken")).thenReturn(false);
        assertThrows(AuthException.class, () -> authService.refreshToken(httpServletRequest));

        // Valid admin flow (10 digits username)
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer admintoken");
        when(jwtUtils.verifyToken("admintoken")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("admintoken")).thenReturn("0123456789");
        Admin admin = Admin.builder().id(1L).employeeNumber("0123456789").email("a@b.com").build();
        when(adminRepository.findByEmployeeNumber("0123456789")).thenReturn(Optional.of(admin));

        Map<String, Object> adminResult = authService.refreshToken(httpServletRequest);
        assertEquals("admintoken", adminResult.get("refresh_token"));
        assertSame(admin, adminResult.get("admin"));
        assertFalse(adminResult.containsKey("buyer"));

        // Valid buyer flow (non-10-digit username)
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer buyertoken");
        when(jwtUtils.verifyToken("buyertoken")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("buyertoken")).thenReturn("testuser");
        when(buyerRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleBuyer));

        Map<String, Object> buyerResult = authService.refreshToken(httpServletRequest);
        assertEquals("buyertoken", buyerResult.get("refresh_token"));
        assertSame(sampleBuyer, buyerResult.get("buyer"));
        assertFalse(buyerResult.containsKey("admin"));

        // Missing user for token -> UsernameNotFoundException
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer notfoundtoken");
        when(jwtUtils.verifyToken("notfoundtoken")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("notfoundtoken")).thenReturn("anotheruser");
        when(buyerRepository.findByUsername("anotheruser")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> authService.refreshToken(httpServletRequest));
    }
}
