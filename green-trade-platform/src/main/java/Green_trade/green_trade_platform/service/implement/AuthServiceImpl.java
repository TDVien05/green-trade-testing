package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.exception.AuthException;
import Green_trade.green_trade_platform.exception.PasswordMismatchException;
import Green_trade.green_trade_platform.exception.UsernameException;
import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.repository.AdminRepository;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.request.ChangePasswordRequest;
import Green_trade.green_trade_platform.request.ForgotPasswordRequest;
import Green_trade.green_trade_platform.request.VerifyOtpForgotPasswordRequest;
import Green_trade.green_trade_platform.service.AuthService;
import Green_trade.green_trade_platform.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final BuyerRepository buyerRepository;
    private final OtpServiceImpl otpService;
    private final RedisOtpService redisOtpService;
    private final DelegatingPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AdminRepository adminRepository;
    private final RedisTokenService redisTokenService;

    @Override
    public Map<String, Object> verifyUsernameForgotPassword(String username) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info(">>> username from request: {}", username);
            Optional<Buyer> buyerOpt = buyerRepository.findByUsername(username);
            if (buyerOpt.isEmpty()) {
                throw new UsernameException("Username is not existed");
            }
            log.info(">>> Username is existed");

            String otp = otpService.generateOtpCode();
            log.info(">>> OTP: {}", otp);

            redisOtpService.savePending(buyerOpt.get().getUsername(), buyerOpt.get().getEmail(), otp);
            otpService.sendOtpEmail(buyerOpt.get().getEmail(), otp);

            result.put("success", true);
            result.put("message", "Username exits.");
            result.put("data", buyerOpt.get());
            result.put("error", null);
            return result;
        } catch (Exception e) {
            log.info(">>> Error at verifyForgotPassword: " + e);
            result.put("success", false);
            result.put("message", "Username is not exits.");
            result.put("data", null);
            result.put("error", e);
            return result;
        }
    }

    public void verifyOtpForgotPassword(VerifyOtpForgotPasswordRequest request) {
        Map<String, String> pending = redisOtpService.getPending(request.getEmail());
        if (pending == null) {
            throw new IllegalArgumentException("Invalid email or user did not forget password yet!");
        }
        log.info(">>> Passed pending is not null");

        String otp = pending.get("otp");
        log.info(">>> Otp From User: {}", request.getOtp());
        log.info(">>> Otp Redis: {}", otp);
        if (!request.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Otp are not the same!");
        }
        log.info(">>> Passed Otp matched");
        redisOtpService.deletePending(request.getEmail());
        log.info(">>> Passed delete pending on redis");
    }

    public Buyer forgotPassword(ForgotPasswordRequest request) throws Exception {
        try {
            log.info(">>> Executed forgotPassword");
            Optional<Buyer> buyerOpt = buyerRepository.findByUsername(request.getUsername());
            if (buyerOpt.isEmpty()) {
                throw new UsernameException("Username is not existed");
            }
            log.info(">>> Username is existed");

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new PasswordMismatchException();
            }
            log.info(">>> Password and Confirm Password is matched");

            String updatePassword = passwordEncoder.encode(request.getNewPassword());
            buyerOpt.get().setPassword(updatePassword);
            log.info(">>> Updated new password successfully");

            return buyerRepository.save(buyerOpt.get());
        } catch (Exception e) {
            log.info(">>> Error at forgotPassword: " + e);
            throw e;
        }
    }

    public Buyer changePassword(ChangePasswordRequest request) throws Exception {
        try {
            Buyer buyer = buyerRepository.findByUsername(request.getUsername()).orElseThrow(
                    () -> new RuntimeException("Buyer is not Existed")
            );

            boolean isPasswordMatched = passwordEncoder.matches(request.getOldPassword(), buyer.getPassword());
            if (!isPasswordMatched) {
                throw new Exception("Password is incorrect");
            }

            boolean isConfirmPasswordMatched = request.getNewPassword().equals(request.getConfirmPassword());
            if (!isConfirmPasswordMatched) {
                throw new PasswordMismatchException();
            }

            String newHashPassword = passwordEncoder.encode(request.getNewPassword());
            buyer.setPassword(newHashPassword);

            return buyerRepository.save(buyer);
        } catch (Exception e) {
            log.info(">>> Error at changePassword: " + e.getMessage());
            throw e;
        }


    }

    public Map<String, Object> refreshToken(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthException("Unauthorized account.");
        }

        String refreshToken = authHeader.substring(7);
        result.put("refresh_token", refreshToken);

        if (!jwtUtils.verifyToken(refreshToken)) {
            throw new AuthException("Invalid refresh token, sign in to get new one");
        }

        String username = jwtUtils.getUsernameFromToken(refreshToken);
        if (username.matches("^\\d{10}$")) {
            Admin admin = adminRepository.findByEmployeeNumber(username).orElseThrow(
                    () -> new UsernameNotFoundException("Can not find user with this refresh token.")
            );
            result.put("admin", admin);
        } else {
            Buyer buyer = buyerRepository.findByUsername(username).orElseThrow(
                    () -> new UsernameNotFoundException("Can not find user with this refresh token.")
            );
            result.put("buyer", buyer);
        }
        return result;
    }
}
