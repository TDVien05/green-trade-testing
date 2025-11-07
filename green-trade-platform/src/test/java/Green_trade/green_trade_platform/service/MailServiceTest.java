package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.request.MailRequest;
import Green_trade.green_trade_platform.service.implement.MailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MailServiceTest {

    private JavaMailSender mailSender;
    private MimeMessage mimeMessage;
    private MailServiceImpl mailService;

    @BeforeEach
    void setup() {
        mailSender = mock(JavaMailSender.class);
        mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        mailService = new MailServiceImpl(mailSender);
    }

    @Test
    void shouldSendHtmlEmailWithUtf8AndPrefixedSubject() throws Exception {
        try (MockedConstruction<MimeMessageHelper> mocked = Mockito.mockConstruction(MimeMessageHelper.class,
                (mock, context) -> {
                    assertEquals(3, context.arguments().size());
                    assertSame(mimeMessage, context.arguments().get(0));
                    assertEquals(true, context.arguments().get(1));
                    assertEquals("UTF-8", context.arguments().get(2));
                })) {

            MailRequest req = MailRequest.builder()
                    .from("sender@example.com")
                    .to("recipient@example.com")
                    .subject("Welcome")
                    .message("Hello content")
                    .build();

            mailService.sendBeautifulMail(req);

            MimeMessageHelper helperMock = mocked.constructed().get(0);
            verify(helperMock).setFrom("sender@example.com");
            verify(helperMock).setTo("recipient@example.com");
            verify(helperMock).setSubject("💚 " + "Welcome");

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(helperMock).setText(bodyCaptor.capture(), eq(true));
            assertTrue(bodyCaptor.getValue().contains("Hello content"));

            verify(mailSender).send(mimeMessage);
        }
    }

    @Test
    void shouldBuildHtmlBodyWithRequestMessage() throws Exception {
        try (MockedConstruction<MimeMessageHelper> mocked = Mockito.mockConstruction(MimeMessageHelper.class)) {
            MailRequest req = MailRequest.builder()
                    .from("a@b.com")
                    .to("c@d.com")
                    .subject("S")
                    .message("This is the core content to include.")
                    .build();

            mailService.sendBeautifulMail(req);

            MimeMessageHelper helperMock = mocked.constructed().get(0);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(helperMock).setText(bodyCaptor.capture(), eq(true));
            String html = bodyCaptor.getValue();

            assertNotNull(html);
            assertTrue(html.contains("This is the core content to include."));
            assertTrue(html.contains("Green Trade Platform"));
            assertTrue(html.contains("Đội ngũ Green Trade"));
        }
    }

    @Test
    void shouldThrowRuntimeExceptionOnMessagingFailure() throws Exception {
        try (MockedConstruction<MimeMessageHelper> mocked = Mockito.mockConstruction(MimeMessageHelper.class,
                (mock, context) -> doThrow(new MessagingException("boom")).when(mock).setFrom(anyString()))) {

            MailRequest req = MailRequest.builder()
                    .from("bad@sender")
                    .to("to@example.com")
                    .subject("X")
                    .message("Y")
                    .build();

            RuntimeException ex = assertThrows(RuntimeException.class, () -> mailService.sendBeautifulMail(req));
            assertTrue(ex.getMessage().contains("Failed to send email"));
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Test
    void shouldHandleNullOrEmptySubjectWithoutNpe() throws Exception {
        try (MockedConstruction<MimeMessageHelper> mocked = Mockito.mockConstruction(MimeMessageHelper.class)) {
            MailRequest reqNull = MailRequest.builder()
                    .from("sender@example.com")
                    .to("recipient@example.com")
                    .subject(null)
                    .message("Body")
                    .build();

            assertDoesNotThrow(() -> mailService.sendBeautifulMail(reqNull));
            MimeMessageHelper helperMock1 = mocked.constructed().get(0);
            verify(helperMock1).setSubject("💚 " + (String) null);

            MailRequest reqEmpty = MailRequest.builder()
                    .from("sender@example.com")
                    .to("recipient@example.com")
                    .subject("")
                    .message("Body")
                    .build();

            assertDoesNotThrow(() -> mailService.sendBeautifulMail(reqEmpty));
            MimeMessageHelper helperMock2 = mocked.constructed().get(1);
            verify(helperMock2).setSubject("💚 ");
        }
    }

    @Test
    void shouldHandleInvalidEmailAddressesGracefully() throws Exception {
        try (MockedConstruction<MimeMessageHelper> mocked = Mockito.mockConstruction(MimeMessageHelper.class,
                (mock, context) -> {
                    doNothing().when(mock).setFrom(anyString());
                    doThrow(new MessagingException("Invalid address")).when(mock).setTo(eq("invalid-email"));
                })) {

            MailRequest req = MailRequest.builder()
                    .from("ok@example.com")
                    .to("invalid-email")
                    .subject("Hi")
                    .message("Body")
                    .build();

            assertThrows(RuntimeException.class, () -> mailService.sendBeautifulMail(req));
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }
}
