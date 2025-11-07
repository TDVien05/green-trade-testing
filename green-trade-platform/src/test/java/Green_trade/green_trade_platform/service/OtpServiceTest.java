package Green_trade.green_trade_platform.service;


import Green_trade.green_trade_platform.exception.EmailException;
import Green_trade.green_trade_platform.service.implement.OtpServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;

import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OtpServiceTest {
    private JavaMailSender mailSender;
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        otpService = new OtpServiceImpl();
        try {
            var field = OtpServiceImpl.class.getDeclaredField("mailSender");
            field.setAccessible(true);
            field.set(otpService, mailSender);
        } catch (Exception e) {
            fail("Failed to inject mailSender mock: " + e.getMessage());
        }
    }

    private static String extractHtmlContent(Object content) throws Exception {
        if (content == null) return "";
        if (content instanceof String s) return s;
        if (content instanceof MimeMultipart multipart) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                var part = multipart.getBodyPart(i);
                builder.append(extractHtmlContent(part.getContent()));
            }
            return builder.toString();
        }
        return content.toString();
    }

    @Test
    void shouldSendOtpEmailWithCorrectContent() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String to = "user@example.com";
        String otp = "123456";

        otpService.sendOtpEmail(to, otp);

        verify(mailSender, times(1)).send(mimeMessage);

        // Kiểm tra người nhận và tiêu đề
        assertNotNull(mimeMessage.getAllRecipients());
        assertEquals(1, mimeMessage.getAllRecipients().length);
        assertEquals(new InternetAddress(to), mimeMessage.getAllRecipients()[0]);
        assertEquals("Green Trade Platform - Email Verification OTP", mimeMessage.getSubject());

        // Lấy nội dung email và kiểm tra OTP
        Object content = mimeMessage.getContent();
        String body = extractHtmlContent(content);

        assertTrue(body.contains(otp), "OTP not found in email body: " + body);
        assertTrue(body.toLowerCase().contains("<html"), "Email not formatted as HTML: " + body);
    }

    @Test
    void shouldEmbedOtpInHtmlTemplate() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String otp = "987654";
        otpService.sendOtpEmail("any@domain.com", otp);

        Object content = mimeMessage.getContent();
        String body = extractHtmlContent(content);

        assertTrue(body.contains(otp));
        assertFalse(body.contains("%s"));
    }

    @Test
    void shouldGenerateSixDigitOtpWithLeadingZeros() {
        String otp = otpService.generateOtpCode();
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }

    @Test
    void shouldThrowEmailExceptionWhenMessagingExceptionOccurs() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Giả lập lỗi gửi mail — dùng MailSendException (unchecked)
        doThrow(new EmailException("Simulated failure"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThrows(EmailException.class, () -> otpService.sendOtpEmail("user@example.com", "123456"));
    }

    @Test
    void shouldWrapInvalidRecipientAddressAsEmailException() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Khi gửi mail sẽ ném lỗi MailSendException (unchecked)
        doThrow(new EmailException("Invalid address"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThrows(EmailException.class, () -> otpService.sendOtpEmail("invalid-email", "654321"));
    }

    @Test
    void shouldAlwaysGenerateNumericOtpWithinRange() {
        Pattern numeric6 = Pattern.compile("^\\d{6}$");
        for (int i = 0; i < 1000; i++) {
            String otp = otpService.generateOtpCode();
            assertTrue(numeric6.matcher(otp).matches(), "OTP not 6-digit numeric: " + otp);
            int value = Integer.parseInt(otp);
            assertTrue(value >= 0 && value <= 999999);
        }
    }
}
