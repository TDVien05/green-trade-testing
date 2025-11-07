package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.exception.AuthException;
import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.repository.AdminRepository;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.request.SignInAdminRequest;
import Green_trade.green_trade_platform.request.SignInGoogleRequest;
import Green_trade.green_trade_platform.request.SignInRequest;
import Green_trade.green_trade_platform.service.implement.GoogleVerifierServiceImpl;
import Green_trade.green_trade_platform.service.implement.SignInServiceImpl;
import Green_trade.green_trade_platform.service.implement.WalletServiceImpl;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SignInServiceTest {

    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private DelegatingPasswordEncoder passwordEncoder;

    @Mock
    private GoogleVerifierServiceImpl googleVerifier;

    @Mock
    private WalletServiceImpl walletService;

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private SignInServiceImpl signInService;

    private Buyer sampleBuyer;
    private Admin sampleAdmin;

    @BeforeEach
    void setup() {
        sampleBuyer = Buyer.builder()
                .buyerId(1L)
                .username("testuser")
                .password("hashedPassword")
                .email("testuser@example.com")
                .build();

        sampleAdmin = Admin.builder()
                .id(10L)
                .employeeNumber("EMP001")
                .password("hashedAdminPw")
                .email("admin@example.com")
                .build();
    }

    @Test
    void shouldAuthenticateBuyerWithValidCredentials() {
        SignInRequest request = SignInRequest.builder()
                .username("testuser")
                .password("plainPw")
                .build();

        when(buyerRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleBuyer));
        when(passwordEncoder.matches("plainPw", "hashedPassword")).thenReturn(true);

        Buyer result = signInService.startSignIn(request);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(buyerRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("plainPw", "hashedPassword");
        verifyNoMoreInteractions(buyerRepository, passwordEncoder, googleVerifier, walletService, adminRepository);
    }

    @Test
    void shouldAuthenticateAdminWithValidCredentials() {
        SignInAdminRequest request = SignInAdminRequest.builder()
                .employeeNumber("EMP001")
                .password("adminPw")
                .build();

        when(adminRepository.findByEmployeeNumber("EMP001")).thenReturn(Optional.of(sampleAdmin));
        when(passwordEncoder.matches("adminPw", "hashedAdminPw")).thenReturn(true);

        Admin result = signInService.startSignInAdmin(request);

        assertNotNull(result);
        assertEquals("EMP001", result.getEmployeeNumber());
        verify(adminRepository, times(1)).findByEmployeeNumber("EMP001");
        verify(passwordEncoder, times(1)).matches("adminPw", "hashedAdminPw");
        verifyNoMoreInteractions(buyerRepository, passwordEncoder, googleVerifier, walletService, adminRepository);
    }

    @Test
    void shouldReturnExistingBuyerOnGoogleSignIn() throws Exception {
        SignInGoogleRequest request = SignInGoogleRequest.builder()
                .idToken("validIdToken")
                .build();

        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn("testuser@example.com");

        when(googleVerifier.verify("validIdToken")).thenReturn(payload);
        when(buyerRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(sampleBuyer));

        Buyer result = signInService.startSignInWithGoogle(request);

        assertNotNull(result);
        assertEquals("testuser@example.com", result.getEmail());
        verify(googleVerifier, times(1)).verify("validIdToken");
        verify(buyerRepository, times(1)).findByEmail("testuser@example.com");
        verify(walletService, never()).createLocalWalletForBuyer(any());
        verify(buyerRepository, never()).save(any(Buyer.class));
    }

    @Test
    void shouldThrowWhenBuyerUsernameNotFound() {
        SignInRequest request = SignInRequest.builder()
                .username("missinguser")
                .password("whatever")
                .build();

        when(buyerRepository.findByUsername("missinguser")).thenReturn(Optional.empty());

        AuthException ex = assertThrows(AuthException.class, () -> signInService.startSignIn(request));
        assertTrue(ex.getMessage().contains("Can not find account with this username"));

        verify(buyerRepository, times(1)).findByUsername("missinguser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void shouldThrowAuthExceptionOnInvalidPassword() {
        // Buyer invalid password
        SignInRequest buyerReq = SignInRequest.builder()
                .username("testuser")
                .password("wrongPw")
                .build();
        when(buyerRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleBuyer));
        when(passwordEncoder.matches("wrongPw", "hashedPassword")).thenReturn(false);

        AuthException buyerEx = assertThrows(AuthException.class, () -> signInService.startSignIn(buyerReq));
        assertTrue(buyerEx.getMessage().contains("Username/password is incorrect"));

        // Admin invalid password
        SignInAdminRequest adminReq = SignInAdminRequest.builder()
                .employeeNumber("EMP001")
                .password("wrongAdminPw")
                .build();
        when(adminRepository.findByEmployeeNumber("EMP001")).thenReturn(Optional.of(sampleAdmin));
        when(passwordEncoder.matches("wrongAdminPw", "hashedAdminPw")).thenReturn(false);

        AuthException adminEx = assertThrows(AuthException.class, () -> signInService.startSignInAdmin(adminReq));
        assertTrue(adminEx.getMessage().contains("Username/password is incorrect"));

        verify(passwordEncoder, times(1)).matches("wrongPw", "hashedPassword");
        verify(passwordEncoder, times(1)).matches("wrongAdminPw", "hashedAdminPw");
    }

    @Test
    void shouldCreateNewBuyerAndWalletOnGoogleSignInWhenEmailNotFound() throws Exception {
        SignInGoogleRequest request = SignInGoogleRequest.builder()
                .idToken("newUserToken")
                .build();

        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn("newuser@example.com");

        when(googleVerifier.verify("newUserToken")).thenReturn(payload);
        when(buyerRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());

        // capture saved buyer to verify fields
        ArgumentCaptor<Buyer> buyerCaptor = ArgumentCaptor.forClass(Buyer.class);
        when(buyerRepository.save(buyerCaptor.capture())).thenAnswer(invocation -> {
            Buyer b = invocation.getArgument(0);
            // simulate persistence assigning ID
            return Buyer.builder()
                    .buyerId(100L)
                    .username(b.getUsername())
                    .password(b.getPassword())
                    .email(b.getEmail())
                    .build();
        });
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + UUID.randomUUID());

        Buyer result = signInService.startSignInWithGoogle(request);

        assertNotNull(result);
        assertEquals(100L, result.getBuyerId());
        assertEquals("newuser", result.getUsername());
        assertEquals("newuser@example.com", result.getEmail());

        Buyer saved = buyerCaptor.getValue();
        assertEquals("newuser", saved.getUsername());
        assertEquals("newuser@example.com", saved.getEmail());
        assertNotNull(saved.getPassword());

        verify(walletService, times(1)).createLocalWalletForBuyer(any(Buyer.class));
        verify(buyerRepository, times(1)).save(any(Buyer.class));
    }
}
