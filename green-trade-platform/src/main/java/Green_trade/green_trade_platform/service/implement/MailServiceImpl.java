package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.request.MailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class MailServiceImpl {
    private final JavaMailSender mailSender;

    public void sendBeautifulMail(MailRequest req) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(req.getFrom());
            helper.setTo(req.getTo());
            helper.setSubject("💚 " + req.getSubject());
            helper.setText(buildBeautifulHtml(req), true);

            mailSender.send(message);
            log.info("✅ Email sent successfully to {}", req.getTo());
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {}: {}", req.getTo(), e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildBeautifulHtml(MailRequest req) {
        return """
                    <div style="font-family: 'Segoe UI', Tahoma, sans-serif;
                                background-color: #f4f4f9;
                                padding: 30px;
                                border-radius: 12px;
                                max-width: 650px;
                                margin: auto;
                                box-shadow: 0 0 12px rgba(0,0,0,0.1);">
                
                        <div style="background-color: #4CAF50; color: white; 
                                    padding: 18px; border-radius: 8px 8px 0 0;
                                    text-align: center;">
                            <h2 style="margin: 0;">🌿 Green Trade Platform</h2>
                        </div>
                
                        <div style="background: white; padding: 25px 30px; border-radius: 0 0 8px 8px;">
                            <p style="font-size: 16px; color: #333;">Xin chào,</p>
                
                            <div style="font-size: 15px; color: #444; line-height: 1.6;">
                                %s
                            </div>
                
                            <br/>
                            <p style="font-size: 14px; color: #777;">
                                Trân trọng,<br/>
                                <strong>Đội ngũ Green Trade 💚</strong>
                            </p>
                        </div>
                
                        <footer style="margin-top: 20px; text-align: center; font-size: 12px; color: #999;">
                            © 2025 Green Trade Platform. All rights reserved.
                        </footer>
                    </div>
                """.formatted(req.getMessage());
    }
}
