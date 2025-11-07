package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.service.GoogleVerifierService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Service dùng để xác minh Google ID Token (JWT từ Google Identity Service).
 * Token này được FE gửi về sau khi user login bằng Google.
 */
@Service
@Slf4j
public class GoogleVerifierServiceImpl implements GoogleVerifierService {

    private GoogleIdTokenVerifier verifier;

    public GoogleVerifierServiceImpl(@Value("${google.client.id:}") String googleClientId) {
        GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory()
        );

        if (googleClientId != null && !googleClientId.isBlank()) {
            builder.setAudience(Collections.singletonList(googleClientId));
        }

        this.verifier = builder.build();
    }

    public GoogleIdToken.Payload verify(String idTokenString) throws Exception {
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            return idToken.getPayload();
        }
        throw new IllegalArgumentException("Invalid Google ID token.");
    }
}
