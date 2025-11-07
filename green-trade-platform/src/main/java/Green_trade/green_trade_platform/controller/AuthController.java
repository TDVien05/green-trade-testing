package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.enumerate.AccountStatus;
import Green_trade.green_trade_platform.enumerate.SellerStatus;
import Green_trade.green_trade_platform.exception.AuthException;
import Green_trade.green_trade_platform.mapper.AdminMapper;
import Green_trade.green_trade_platform.mapper.AuthMapper;
import Green_trade.green_trade_platform.mapper.BuyerMapper;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.repository.SellerRepository;
import Green_trade.green_trade_platform.request.*;
import Green_trade.green_trade_platform.response.AdminAuthResponse;
import Green_trade.green_trade_platform.response.AuthResponse;
import Green_trade.green_trade_platform.response.BuyerResponse;
import Green_trade.green_trade_platform.response.RestResponse;
import Green_trade.green_trade_platform.service.implement.AuthServiceImpl;
import Green_trade.green_trade_platform.service.implement.RedisTokenService;
import Green_trade.green_trade_platform.service.implement.SignInServiceImpl;
import Green_trade.green_trade_platform.service.implement.SignUpServiceImpl;
import Green_trade.green_trade_platform.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final BuyerMapper buyerMapper;
    private SignInServiceImpl signInService;
    private SignUpServiceImpl signUpService;
    private ResponseMapper responseMapper;
    private JwtUtils jwtUtils;
    private RedisTokenService redisTokenService;
    private AuthServiceImpl authService;
    private AuthMapper authMapper;
    private SellerRepository sellerRepository;
    private AdminMapper adminMapper;

    private final long REFRESH_EXPIRE_TIME = 7L * 24 * 60 * 60 * 1000; // 7 days
    private final long ACCESS_EXPIRE_TIME = 7L * 24 * 60 * 60 * 1000; // 15 * 60 * 1000; // 15 minutes
//    private final long ACCESS_EXPIRE_TIME = 30 * 1000; // 30 seconds

    @Operation(
            summary = "Register a new customer account",
            description = """
                        Handles new customer registration by collecting user information and initiating the OTP (One-Time Password)
                        verification process. This endpoint accepts user details such as name, email, and password, and sends a
                        verification OTP to the provided email address.
                    
                        **Workflow:**
                        1. The user submits their registration information via this endpoint.
                        2. The system validates the request and creates a pending registration record.
                        3. An OTP is generated and sent to the user's email for verification.
                        4. The user must confirm the OTP using a separate verification endpoint to activate their account.
                    
                        **Use cases:**
                        - Customer account registration for the e-commerce platform.
                        - Initiating secure OTP-based email verification during signup.
                    """
    )
    @PostMapping("/signup")
    public ResponseEntity<RestResponse<Object, Object>> signUp(@Valid @RequestBody SignUpRequest req) {
        signUpService.startSignUp(req);
        return ResponseEntity.ok(responseMapper.toDto(
                true, "Sent OTP to email", null, null
        ));
    }

    @Operation(
            summary = "Sign in for user",
            description = """
                        Authenticates a registered user (buyer or seller) using their username and password.
                        Upon successful login, the system issues an access token and a refresh token
                        for secure session management.
                    
                        **Workflow:**
                        1. The user submits their login credentials (username and password).
                        2. The system validates the credentials and checks if the user exists and is active.
                        3. On successful authentication:
                           - A short-lived **access token** (for API access) and a long-lived **refresh token** are generated.
                           - The refresh token is stored securely in Redis for future use.
                        4. The response contains token information and user details such as username, email, role, and ID.
                    
                        **Use cases:**
                        - Allow users to log in to their account via web.
                        - Initialize a session for authenticated API requests.
                        - Manage secure token-based authentication (JWT).
                    """
    )
    @PostMapping("/signin")
    public ResponseEntity<RestResponse<AuthResponse, Object>> signIn(@Valid @RequestBody SignInRequest req) {
        try {
            log.info(">>> [Auth Controller] Starting sign in controller");
            Buyer user = signInService.startSignIn(req);
            log.info(">>> [Auth Controller] User active: {}", user.isActive());

            if (!user.isActive()) {
                throw new AuthException("Account was be blocked.");
            }

            log.info(">>> [Auth Controller] Generating tokens");
            String accessToken = jwtUtils.generateTokenFromUsername(user.getUsername(), ACCESS_EXPIRE_TIME);
            String refreshToken = jwtUtils.generateTokenFromUsername(user.getUsername(), REFRESH_EXPIRE_TIME);

            log.info(">>> [Auth Controller] Saving refresh token into Redis");
            redisTokenService.saveTokenToRedis(user.getEmail(), refreshToken, REFRESH_EXPIRE_TIME);

            AuthResponse authResponse = authMapper.toDto(user, accessToken, refreshToken);
            authResponse.setBuyerId(user.getBuyerId());

            Optional<Seller> seller = sellerRepository.findByBuyer(user);
            if (seller.isPresent() && seller.get().getStatus() == SellerStatus.ACCEPTED) {
                authResponse.setRole("ROLE_SELLER");
            } else {
                authResponse.setRole("ROLE_BUYER");
            }

            log.info(">>> [Auth Controller] Ending sign in controller");
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "SIGN IN SUCCESSFULLY",
                    authResponse, null
            ));
        } catch (Exception e) {
            log.info(">>> [Auth Controller] Error occured in sign in controller");
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "SIGN IN FAILED",
                    null, e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Sign in for Admin",
            description = """
                        Authenticates an administrator using their employee number and password, and returns access and refresh tokens
                        for session management. This endpoint is intended exclusively for admin accounts.
                    
                        **Workflow:**
                        1. The admin submits valid credentials (employee number and password).
                        2. The system validates the credentials against admin records.
                        3. Upon successful authentication:
                           - A short-lived **access token** and a long-lived **refresh token** are generated.
                           - The refresh token is securely stored in Redis with a defined expiration time.
                        4. The response includes admin details, assigned role, and generated tokens for authorization.
                    
                        **Use cases:**
                        - Allow administrators to securely log into the management dashboard.
                        - Generate and return JWT tokens for admin session handling.
                        - Provide role-based access for administrative API endpoints.
                    """
    )
    @PostMapping("/admin/signin")
    public ResponseEntity<?> signInAdmin(@Valid @RequestBody SignInAdminRequest req) {
        try {
            Admin user = signInService.startSignInAdmin(req);
            if (user.getStatus() == AccountStatus.INACTIVE) {
                throw new AuthException("This admin account was be blocked.");
            }

            String accessToken = jwtUtils.generateTokenFromUsername(user.getEmployeeNumber(), ACCESS_EXPIRE_TIME);
            String refreshToken = jwtUtils.generateTokenFromUsername(user.getEmployeeNumber(), REFRESH_EXPIRE_TIME);
            redisTokenService.saveTokenToRedis(user.getEmail(), refreshToken, REFRESH_EXPIRE_TIME);

            AdminAuthResponse authResponse = AdminAuthResponse.builder()
                    .adminResponse(adminMapper.toDto(user))
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .role("ROLE_ADMIN")
                    .build();

            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "ADMIN SIGN IN SUCCESSFULLY.",
                    authResponse, null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "ADMIN SIGN IN FAILED.",
                    null, e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Sign in with Google for customer",
            description = """
                        Authenticates a customer using their Google account via OAuth 2.0
                        This endpoint verifies the Google-provided ID token, registers the user if they do not exist,  
                        and issues new JWT access and refresh tokens for session management.
                    
                        **Workflow:**
                        1. The client (frontend or mobile app) sends a Google ID token obtained from Google Sign-In SDK.
                        2. The system validates the token with Google's public key to confirm authenticity.
                        3. If the user already exists, their account is retrieved. Otherwise, a new account is created automatically.
                        4. JWT access and refresh tokens are generated for the user.
                        5. The refresh token is stored securely in Redis for session management.
                        6. The response includes authenticated user details and tokens.
                    
                        **Use cases:**
                        - Allowing users to log in or register quickly using their Google account.
                        - Streamlining onboarding without password-based authentication.
                        - Supporting mobile and web-based OAuth flows.
                    """
    )
    @PostMapping("/signin-google")
    public ResponseEntity<RestResponse<AuthResponse, Object>> loginWithGoogle(@RequestBody SignInGoogleRequest body) throws Exception {
        try {
            Buyer user = signInService.startSignInWithGoogle(body);
            if (!user.isActive()) {
                throw new AuthException("User was be blocked.");
            }

            String accessToken = jwtUtils.generateTokenFromUsername(user.getUsername(), ACCESS_EXPIRE_TIME);
            String refreshToken = jwtUtils.generateTokenFromUsername(user.getUsername(), REFRESH_EXPIRE_TIME);
            redisTokenService.saveTokenToRedis(user.getEmail(), refreshToken, REFRESH_EXPIRE_TIME);

            AuthResponse authResponse = authMapper.toDto(user, accessToken, refreshToken);
            Optional<Seller> seller = sellerRepository.findByBuyer(user);
            if (seller.isPresent() && seller.get().getStatus() == SellerStatus.ACCEPTED) {
                authResponse.setRole("ROLE_SELLER");
            } else {
                authResponse.setRole("ROLE_BUYER");
            }
            authResponse.setBuyerId(user.getBuyerId());

            return ResponseEntity.status(HttpStatus.OK.value())
                    .body(responseMapper.toDto(
                            true, "SIGN IN SUCCESSFULLY", authResponse, null
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK.value())
                    .body(responseMapper.toDto(
                            false, "SIGN IN FAILED", null, e.getMessage()
                    ));
        }
    }

    @Operation(
            summary = "Verify username for forgot password request",
            description = """
                        Validates a user's username  as part of the "forgot password" process.  
                        If the provided username is valid and associated with an existing account, the system generates a One-Time Password (OTP)
                        and sends it to the registered email address for verification.
                    
                        **Workflow:**
                        1. The user submits their username or email address.
                        2. The system checks if the account exists and is active.
                        3. If valid, an OTP is generated and sent to the associated email.
                        4. The response confirms that the OTP has been sent.
                    
                        **Use cases:**
                        - Initiating password reset flow for users who forgot their password.
                        - Ensuring that only valid and registered users can reset passwords.
                    """
    )
    @PostMapping("/verify-username-forgot-password")
    public ResponseEntity<RestResponse<Object, Object>> verifyForgotPassword(@RequestBody VerifyUsernameForgotPasswordRequest req) throws Exception {
        Map<String, Object> result = authService.verifyUsernameForgotPassword(req.getUsername());
        return ResponseEntity.status(HttpStatus.OK.value()).body(responseMapper.toDto(
                true, "OTP Sent To Email", result, null
        ));
    }

    @Operation(
            summary = "Verify OTP for forgot password",
            description = """
                        Validates the One-Time Password (OTP) sent to the user's registered email as part of the 
                        "forgot password" flow. This step ensures that the requester owns the email account 
                        associated with the username before allowing password reset.
                    
                        **Workflow:**
                        1. The user submits their email/username and the OTP they received via email.
                        2. The system verifies whether the OTP matches and is still valid (not expired).
                        3. If verification succeeds, the system authorizes the next step — password reset.
                        4. If verification fails, an appropriate error message is returned.
                    
                        **Use cases:**
                        - Step 2 in the forgot password process after requesting an OTP.
                        - Ensuring user identity before allowing password change.
                    """
    )
    @PostMapping("/verify-otp-forgot-password")
    public ResponseEntity<RestResponse<Object, Object>> verifyOtpForgotPassword(@RequestBody VerifyOtpForgotPasswordRequest request) {
        log.info(">>> We are at verifyOtpForgotPassword");
        authService.verifyOtpForgotPassword(request);
        return ResponseEntity.status(HttpStatus.OK.value()).body(responseMapper.toDto(
                true, "Verified OTP Successfully", null, null
        ));
    }

    @Operation(
            summary = "Reset password using forgot password flow",
            description = """
                        Allows a verified user to reset their password after successful OTP verification 
                        during the "forgot password" process.
                    
                        **Workflow:**
                        1. The user has already verified their OTP via `/verify-otp-forgot-password`.
                        2. The user sends this request with their username/email and new password.
                        3. The system validates the request, updates the stored password, and confirms success.
                    
                        **Use cases:**
                        - Completing the password recovery process for users who forgot their password.
                        - Ensuring secure password updates after identity verification.
                    
                        **Security Notes:**
                        - The new password is securely hashed before being stored.
                        - Users must have a valid OTP verification record before calling this API.
                    """
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<RestResponse<Buyer, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) throws Exception {
        Buyer result = authService.forgotPassword(request);
        return ResponseEntity.status(HttpStatus.OK.value()).body(responseMapper.toDto(
                true,
                "UPDATED PASSWORD SUCCESSFULLY",
                result,
                null
        ));
    }

    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @Operation(
            summary = "Change Password API",
            description = """
                        Allows an authenticated user to change their password while logged in.  
                        The user must provide their current password and a new password.  
                        The system validates the current password before securely updating it to the new one.
                    
                        **Workflow:**
                        1. The user sends their current and new password in the request body.
                        2. The system verifies that the current password is correct.
                        3. If validation passes, the password is updated (hashed and stored securely).
                        4. The response confirms that the password was successfully changed.
                    
                        **Use cases:**
                        - Users changing their password from their account settings.
                        - Security best practices (e.g., periodic password updates).
                    
                        **Security Notes:**
                        - Only authenticated users can access this endpoint.
                        - The new password must meet platform password policy requirements.
                        - Passwords are never stored in plain text and are hashed before persistence.
                    """
    )
    @PostMapping("/change-password")
    public ResponseEntity<RestResponse<BuyerResponse, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) throws Exception {
        Buyer buyer = authService.changePassword(request);
        BuyerResponse responseData = buyerMapper.toDto(buyer);
        return ResponseEntity.status(HttpStatus.OK.value()).body(
                responseMapper.toDto(
                        true,
                        "CHANGE PASSWORD SUCCESSFULLY",
                        responseData,
                        null)
        );
    }

    @PostMapping("/verify-otp")
    @Operation(
            summary = "Verify OTP via email",
            description = """
                        Verifies the One-Time Password (OTP) sent to the user's email address during registration.  
                        Upon successful verification, the user's account is activated, and access/refresh tokens are generated 
                        to authenticate the user immediately.
                    
                        **Workflow:**
                        1. The user submits the OTP received via email along with their email/username.
                        2. The system validates the OTP and ensures it hasn't expired.
                        3. If valid:
                           - The user's account is marked as verified/active.
                           - JWT access and refresh tokens are generated for the user.
                           - The refresh token is securely stored in Redis for future authentication.
                        4. The system returns a success response containing authentication tokens.
                    
                        **Use cases:**
                        - Final step in the sign-up flow for verifying a newly registered email address.
                        - Automatically signing in a user after OTP verification.
                    """
    )
    public ResponseEntity<RestResponse<AuthResponse, Object>> verify(@Valid @RequestBody VerifyOtpRequest req) {
        Buyer buyer = signUpService.verifyOtp(req);
        String refreshToken = jwtUtils.generateTokenFromUsername(buyer.getUsername(), REFRESH_EXPIRE_TIME);
        String accessToken = jwtUtils.generateTokenFromUsername(buyer.getUsername(), ACCESS_EXPIRE_TIME);
        redisTokenService.saveTokenToRedis(buyer.getEmail(), refreshToken, REFRESH_EXPIRE_TIME);

        AuthResponse authResponse = authMapper.toDto(buyer, accessToken, refreshToken);
        return ResponseEntity.status(HttpStatus.OK.value()).body(
                responseMapper.toDto(
                        true,
                        "SIGN UP SUCCESSFULLY",
                        authResponse,
                        null
                )
        );
    }

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Refresh Access Token",
            description = """
                        Issues a new **access token** (and optionally a new refresh token) when the existing access token has expired.  
                        This endpoint validates the provided refresh token to ensure it is still active and not revoked.
                    
                        **Workflow:**
                        1. The client sends the refresh token in the `Authorization` header (format: `Bearer <refresh_token>`).
                        2. The system validates the token and verifies it against the stored value in Redis.
                        3. If valid:
                           - A new access token and refresh token are generated.
                           - The old refresh token is deleted and replaced in Redis.
                           - The new tokens are returned to the client.
                        4. If invalid or expired:
                           - The system returns a `400 Bad Request` with an "INVALID REFRESH TOKEN" message.
                    
                        **Use cases:**
                        - Maintaining session continuity without forcing the user to log in again.
                        - Mobile or web applications where access tokens are short-lived but refresh tokens persist longer.
                    
                        **Security Notes:**
                        - The refresh token must be sent in the Authorization header.
                        - Each refresh token is single-use and is rotated upon refresh.
                    """
    )
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        log.info(">>> [Refresh token controller]: {}", request.getHeader("Authorization"));
        try {
            Map<String, Object> data = authService.refreshToken(request);
            String email, username;
            Admin admin = null;
            Buyer buyer = null;

            if (data.get("admin") != null) {
                admin = (Admin) data.get("admin");
                email = admin.getEmail();
                username = admin.getEmployeeNumber();
            } else {
                buyer = (Buyer) data.get("buyer");
                email = buyer.getEmail();
                username = buyer.getUsername();
            }

            log.info(">>> [User email]: {}", email);
            log.info(">>> [Username]: {}", username);
            String savedToken = redisTokenService.getRefreshToken(email);
            String token = (String) data.get("refresh_token");

            if (redisTokenService.verifyRefreshToken(email) &&
                    savedToken.equalsIgnoreCase(token)) {

                String newAccessToken = jwtUtils.generateTokenFromUsername(username, ACCESS_EXPIRE_TIME);
                String newRefreshToken = jwtUtils.generateTokenFromUsername(username, REFRESH_EXPIRE_TIME);
                redisTokenService.deleteRefreshToken(email);
                redisTokenService.saveTokenToRedis(email, newRefreshToken, REFRESH_EXPIRE_TIME);

                return ResponseEntity.ok(responseMapper.toDto(
                        true,
                        "GET NEW TOKEN SUCCESSFULLY.",
                        authMapper.toDto(username, email, newAccessToken, newRefreshToken),
                        null
                ));
            } else {
                return ResponseEntity.badRequest().body(responseMapper.toDto(
                        false,
                        "INVALID REFRESH TOKEN",
                        null, null
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(responseMapper.toDto(
                    false,
                    "ERROR OCCUR WHEN GET NEW TOKENS.",
                    null, e
            ));
        }
    }
}
