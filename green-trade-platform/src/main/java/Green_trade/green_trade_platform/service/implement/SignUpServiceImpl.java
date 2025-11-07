package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.mapper.BuyerMapper;
import Green_trade.green_trade_platform.exception.EmailException;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.request.SignUpRequest;
import Green_trade.green_trade_platform.request.VerifyOtpRequest;
import Green_trade.green_trade_platform.service.OtpService;
import Green_trade.green_trade_platform.service.SignUpService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;


@Service
@Slf4j
public class SignUpServiceImpl implements SignUpService {

    private BuyerRepository repository;
    private RedisOtpService redisOtpService;
    private BuyerMapper buyerMapper;
    private JavaMailSender mailSender;
    private DelegatingPasswordEncoder passwordEncoder;
    private WalletServiceImpl walletServiceImpl;
    private OtpServiceImpl otpService;

    public SignUpServiceImpl(
            BuyerRepository buyerRepository,
            RedisOtpService redisOtpService,
            BuyerMapper buyerMapper,
            JavaMailSender javaMailSender,
            DelegatingPasswordEncoder passwordEncoder,
            WalletServiceImpl walletServiceImpl,
            OtpServiceImpl otpService
    ) {
        this.repository = buyerRepository;
        this.redisOtpService = redisOtpService;
        this.buyerMapper = buyerMapper;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.walletServiceImpl = walletServiceImpl;
        this.otpService = otpService;
    }

    // Starting sign up: saving buyer to redis and sending otp
    @Override
    public void startSignUp(SignUpRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exits.");
        }
        if (repository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exits.");
        }
        // Create OTP
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        log.info(">>> OTP: {}", otp);
        String hashPassword = passwordEncoder.encode(request.getPassword());

        // Save user temporary in Redis for verifying OTP via email
        // User send request to sign up -> create OTP + save user to Redis -> user send verify OTP -> save user into database
        redisOtpService.savePendingBuyer(request.getUsername(), hashPassword, request.getEmail(), otp);
        log.info(">>> Save pending buyer successfully.");
        otpService.sendOtpEmail(request.getEmail(), otp);
        log.info(">>> Send otp successfully.");
    }

    // Verify otp
    @Override
    public Buyer verifyOtp(VerifyOtpRequest request) {
        // Get pending buyer in Redis
        Map<String, String> pending = redisOtpService.getPendingBuyer(request.getEmail());
        if (pending == null) {
            throw new IllegalArgumentException("Invalid email or user did not sign up yet!");
        }
        log.info(">>> Passed pending: {}", pending);
        // Get OTP in map
        String otp = pending.get("otp");
        log.info(">>> Otp from request: {}", request.getOtp());
        log.info(">>> Otp from pending: {}", otp);
        if (!request.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Otp are not the same!");
        }
        log.info(">>> Passed OTP matched");

        Buyer buyer = Buyer.builder()
                .username(pending.get("username"))
                .password(pending.get("password"))
                .email(request.getEmail())
                .build();
        buyer = repository.save(buyer);
        Wallet wallet = walletServiceImpl.createLocalWalletForBuyer(buyer);
        log.info(">>> Created wallet for buyer: {} with id {}", buyer.getUsername(), wallet.getWalletId());
        redisOtpService.deletePendingBuyer(request.getEmail());
        return buyer;
    }
}
