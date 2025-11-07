package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.exception.AuthException;
import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.repository.AdminRepository;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.request.SignInAdminRequest;
import Green_trade.green_trade_platform.request.SignInGoogleRequest;
import Green_trade.green_trade_platform.request.SignInRequest;
import Green_trade.green_trade_platform.service.SignInService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SignInServiceImpl implements SignInService {
    @Autowired
    private BuyerRepository buyerRepository;

    @Autowired
    private DelegatingPasswordEncoder passwordEncoder;

    @Autowired
    private GoogleVerifierServiceImpl googleVerifier;
    @Autowired
    private WalletServiceImpl walletService;
    @Autowired
    private AdminRepository adminRepository;

    public Buyer startSignIn(SignInRequest request) {
        try {
            log.info(">>> [Sign In Service] Starting sign in service");
            String username = request.getUsername();
            String password = request.getPassword();

            Buyer buyerOpt = buyerRepository.findByUsername(username).orElseThrow(
                    () -> new AuthException(">>> [Sign In Service] Can not find account with this username: " + request.getUsername()));
            log.info(">>> [Sign In Service] buyerOpt: {}", buyerOpt);
            if (!passwordEncoder.matches(password, buyerOpt.getPassword())) {
                log.info(">>> [Sign In Service] Authentication failed with user {}", username);
                throw new AuthException("Username/password is incorrect");
            }
            log.info(">>> [Sign In Service] Authentication successfully with username: {}", username);
            log.info(">>> [Sign In Service] Ended sign in service");
            return buyerOpt;
        } catch (Exception e) {
            log.info(">>> [Sign In Service] Error occured in sign in service: {}", e.getMessage());
            throw e;
        }
    }

    public Admin startSignInAdmin(SignInAdminRequest request) {
        try {
            log.info(">>> startSignInAdmin of SignInServiceImpl: started");
            String employeeNumber = request.getEmployeeNumber();
            String password = request.getPassword();

            Optional<Admin> adminOpt = adminRepository.findByEmployeeNumber(employeeNumber);
            if (adminOpt.isEmpty() || !passwordEncoder.matches(password, adminOpt.get().getPassword())) {
                log.info(">>> startSignInAdmin at SignInServiceImpl: user: {} authenticated failed", employeeNumber);
                throw new AuthException("Username/password is incorrect");
            }
            log.info(">>> startSignInAdmin at SignInServiceImpl: user: {} authenticated successfully", employeeNumber);
            log.info(">>> startSignInAdmin of SignInServiceImpl: ended");
            return adminOpt.get();
        } catch (Exception e) {
            log.info(">>> startSignInAdmin of SignServiceImpl: Error occurred");
            log.info(">>> startSignInAdmin of SignInServiceImpl: ended");
            throw e;
        }
    }

    @Override
    public Buyer startSignInWithGoogle(SignInGoogleRequest body) throws Exception {
        try {
            log.info("startSignInWithGoogle of GoogleVerifierService: started");
            String idToken = body.getIdToken();
            GoogleIdToken.Payload googleUserData = googleVerifier.verify(idToken);

            String email = googleUserData.getEmail();
            log.info("startSignInWithGoogle of GoogleVerifierService: user with email: {}", email);

            Optional<Buyer> buyerOpt = buyerRepository.findByEmail(email);

            if (buyerOpt.isEmpty()) {
                log.info("startSignInWithGoogle of GoogleVerifierService: New User with email: {}", email);
                String username = googleUserData.getEmail().split("@")[0];
                String password = passwordEncoder.encode(UUID.randomUUID().toString());
                Buyer user = Buyer.builder()
                        .username(username)
                        .password(password)
                        .email(email)
                        .build();
                user = buyerRepository.save(user);
                walletService.createLocalWalletForBuyer(user);
                return user;
            }
            log.info("startSignInWithGoogle of GoogleVerifierService: end");
            return buyerOpt.get();
        } catch (Exception e) {
            log.info("startSignInWithGoogle of GoogleVerifierService: Error occurred:" + e);
            throw new Exception("Sign In With Google Failed");
        }
    }
}
