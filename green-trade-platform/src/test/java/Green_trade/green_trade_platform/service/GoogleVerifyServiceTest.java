package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.service.implement.GoogleVerifierServiceImpl;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class GoogleVerifyServiceTest {

    @Test
    public void verify_validToken_returnsPayload() throws Exception {
        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
                     Mockito.mockConstruction(GoogleIdTokenVerifier.Builder.class, (builderMock, context) -> {
                         GoogleIdTokenVerifier verifierMock = mock(GoogleIdTokenVerifier.class);
                         GoogleIdToken tokenMock = mock(GoogleIdToken.class);
                         Payload payloadMock = new Payload();

                         when(tokenMock.getPayload()).thenReturn(payloadMock);
                         when(verifierMock.verify((String) eq("valid.jwt.token"))).thenReturn(tokenMock);
                         when(builderMock.build()).thenReturn(verifierMock);
                     })) {

            GoogleVerifierServiceImpl service = new GoogleVerifierServiceImpl("client-id-123");

            Payload result = service.verify("valid.jwt.token");

            assertNotNull(result);
        }
    }

    @Test
    public void constructor_withClientId_setsAudienceRestriction() {
        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
                     Mockito.mockConstruction(GoogleIdTokenVerifier.Builder.class, (builderMock, context) -> {
                         when(builderMock.setAudience(any())).thenReturn(builderMock);
                         GoogleIdTokenVerifier verifierMock = mock(GoogleIdTokenVerifier.class);
                         when(builderMock.build()).thenReturn(verifierMock);
                     })) {

            String clientId = "client-id-123";
            new GoogleVerifierServiceImpl(clientId);

            GoogleIdTokenVerifier.Builder builderInstance = mockedBuilder.constructed().get(0);
            verify(builderInstance, times(1)).setAudience(eq(List.of(clientId)));
            verify(builderInstance, times(1)).build();
        }
    }

    @Test
    public void constructor_withoutClientId_allowsAnyAudience() {
        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
                     Mockito.mockConstruction(GoogleIdTokenVerifier.Builder.class, (builderMock, context) -> {
                         GoogleIdTokenVerifier verifierMock = mock(GoogleIdTokenVerifier.class);
                         when(builderMock.build()).thenReturn(verifierMock);
                     })) {

            new GoogleVerifierServiceImpl("   ");

            GoogleIdTokenVerifier.Builder builderInstance = mockedBuilder.constructed().get(0);
            verify(builderInstance, never()).setAudience(any());
            verify(builderInstance, times(1)).build();
        }
    }

    @Test
    public void verify_nullToken_throwsException() throws Exception {
        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
                     Mockito.mockConstruction(GoogleIdTokenVerifier.Builder.class, (builderMock, context) -> {
                         GoogleIdTokenVerifier verifierMock = mock(GoogleIdTokenVerifier.class);
                         when(verifierMock.verify((String) isNull())).thenThrow(new NullPointerException("token is null"));
                         when(builderMock.build()).thenReturn(verifierMock);
                     })) {

            GoogleVerifierServiceImpl service = new GoogleVerifierServiceImpl("");

            assertThrows(NullPointerException.class, (Executable) () -> service.verify(null));
        }
    }

    @Test
    public void verify_blankToken_throwsException() throws Exception {
        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
                     Mockito.mockConstruction(GoogleIdTokenVerifier.Builder.class, (builderMock, context) -> {
                         GoogleIdTokenVerifier verifierMock = mock(GoogleIdTokenVerifier.class);
                         when(verifierMock.verify((String) eq("   "))).thenReturn(null);
                         when(builderMock.build()).thenReturn(verifierMock);
                     })) {

            GoogleVerifierServiceImpl service = new GoogleVerifierServiceImpl("");

            assertThrows(IllegalArgumentException.class, () -> service.verify("   "));
        }
    }

    @Test
    public void verify_internalFailure_propagatesException() throws Exception {
        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
                     Mockito.mockConstruction(GoogleIdTokenVerifier.Builder.class, (builderMock, context) -> {
                         GoogleIdTokenVerifier verifierMock = mock(GoogleIdTokenVerifier.class);
                         when(verifierMock.verify((String) any())).thenThrow(new RuntimeException("network failure"));
                         when(builderMock.build()).thenReturn(verifierMock);
                     })) {

            GoogleVerifierServiceImpl service = new GoogleVerifierServiceImpl("client-id-123");

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.verify("any.token"));
            assertEquals("network failure", ex.getMessage());
        }
    }
}
