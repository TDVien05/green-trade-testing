package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.mapper.BuyerMapper;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.request.SignUpRequest;
import Green_trade.green_trade_platform.request.VerifyOtpRequest;
import Green_trade.green_trade_platform.service.implement.OtpServiceImpl;
import Green_trade.green_trade_platform.service.implement.RedisOtpService;
import Green_trade.green_trade_platform.service.implement.SignUpServiceImpl;
import Green_trade.green_trade_platform.service.implement.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SignUpServiceTest {

    private BuyerRepository buyerRepository;
    private RedisOtpService redisOtpService;
    private BuyerMapper buyerMapper;
    private JavaMailSender javaMailSender;
    private DelegatingPasswordEncoder passwordEncoder;
    private WalletServiceImpl walletService;
    private OtpServiceImpl otpService;

    private SignUpServiceImpl signUpService;

    @BeforeEach
    void setUp() {
        buyerRepository = mock(BuyerRepository.class);
        redisOtpService = mock(RedisOtpService.class);
        buyerMapper = mock(BuyerMapper.class);
        javaMailSender = mock(JavaMailSender.class);
        passwordEncoder = mock(DelegatingPasswordEncoder.class);
        walletService = mock(WalletServiceImpl.class);
        otpService = mock(OtpServiceImpl.class);

        signUpService = new SignUpServiceImpl(
                buyerRepository,
                redisOtpService,
                buyerMapper,
                javaMailSender,
                passwordEncoder,
                walletService,
                otpService
        );
    }

    @Test
    void shouldStartSignUpAndSendOtp() {
        SignUpRequest req = SignUpRequest.builder()
                .username("Username")
                .password("Pass@123")
                .email("user@example.com")
                .build();

        when(buyerRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(buyerRepository.existsByUsername(req.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("encodedPass");

        signUpService.startSignUp(req);

        verify(passwordEncoder).encode("Pass@123");

        ArgumentCaptor<String> usernameCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> passwordCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> otpCap = ArgumentCaptor.forClass(String.class);

        verify(redisOtpService).savePendingBuyer(
                usernameCap.capture(),
                passwordCap.capture(),
                emailCap.capture(),
                otpCap.capture()
        );

        assertEquals("Username", usernameCap.getValue());
        assertEquals("encodedPass", passwordCap.getValue());
        assertEquals("user@example.com", emailCap.getValue());
        assertNotNull(otpCap.getValue());
        assertEquals(6, otpCap.getValue().length());

        verify(otpService).sendOtpEmail(eq("user@example.com"), anyString());
    }

    @Test
    void shouldVerifyOtpAndPersistBuyerAndCreateWallet() {
        String email = "user@example.com";
        String otp = "123456";
        Map<String, String> pending = new HashMap<>();
        pending.put("username", "Username");
        pending.put("password", "encodedPass");
        pending.put("email", email);
        pending.put("otp", otp);

        when(redisOtpService.getPendingBuyer(email)).thenReturn(pending);

        Buyer savedBuyer = Buyer.builder()
                .buyerId(1L)
                .username("Username")
                .password("encodedPass")
                .email(email)
                .build();

        when(buyerRepository.save(any(Buyer.class))).thenReturn(savedBuyer);

        Wallet wallet = Wallet.builder().walletId(10L).buyer(savedBuyer).build();
        when(walletService.createLocalWalletForBuyer(savedBuyer)).thenReturn(wallet);

        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(email)
                .otp(otp)
                .build();

        Buyer result = signUpService.verifyOtp(request);

        assertNotNull(result);
        assertEquals(savedBuyer.getBuyerId(), result.getBuyerId());
        assertEquals("Username", result.getUsername());
        assertEquals("encodedPass", result.getPassword());
        assertEquals(email, result.getEmail());

        ArgumentCaptor<Buyer> buyerCaptor = ArgumentCaptor.forClass(Buyer.class);
        verify(buyerRepository).save(buyerCaptor.capture());

        Buyer toSave = buyerCaptor.getValue();
        assertEquals("Username", toSave.getUsername());
        assertEquals("encodedPass", toSave.getPassword());
        assertEquals(email, toSave.getEmail());

        verify(walletService).createLocalWalletForBuyer(savedBuyer);
        verify(redisOtpService).deletePendingBuyer(email);
    }

    @Test
    void shouldGenerateSixDigitOtp() {
        SignUpRequest req = SignUpRequest.builder()
                .username("Username")
                .password("Pass@123")
                .email("user@example.com")
                .build();

        when(buyerRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(buyerRepository.existsByUsername(req.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("encodedPass");

        ArgumentCaptor<String> otpCap = ArgumentCaptor.forClass(String.class);

        signUpService.startSignUp(req);

        verify(redisOtpService).savePendingBuyer(anyString(), anyString(), anyString(), otpCap.capture());
        String otp = otpCap.getValue();

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        SignUpRequest req = SignUpRequest.builder()
                .username("Username")
                .password("Pass@123")
                .email("user@example.com")
                .build();

        when(buyerRepository.existsByEmail(req.getEmail())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> signUpService.startSignUp(req));
        assertEquals("Email already exits.", ex.getMessage());

        verify(buyerRepository, never()).existsByUsername(anyString());
        verify(redisOtpService, never()).savePendingBuyer(anyString(), anyString(), anyString(), anyString());
        verify(otpService, never()).sendOtpEmail(anyString(), anyString());
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        SignUpRequest req = SignUpRequest.builder()
                .username("Username")
                .password("Pass@123")
                .email("user@example.com")
                .build();

        when(buyerRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(buyerRepository.existsByUsername(req.getUsername())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> signUpService.startSignUp(req));
        assertEquals("Username already exits.", ex.getMessage());

        verify(redisOtpService, never()).savePendingBuyer(anyString(), anyString(), anyString(), anyString());
        verify(otpService, never()).sendOtpEmail(anyString(), anyString());
    }

    @Test
    void shouldThrowWhenPendingBuyerNotFoundDuringVerify() {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("user@example.com")
                .otp("123456")
                .build();

        when(redisOtpService.getPendingBuyer(request.getEmail())).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> signUpService.verifyOtp(request));
        assertEquals("Invalid email or user did not sign up yet!", ex.getMessage());

        verify(buyerRepository, never()).save(any());
        verify(walletService, never()).createLocalWalletForBuyer(any());
        verify(redisOtpService, never()).deletePendingBuyer(anyString());
    }
}
