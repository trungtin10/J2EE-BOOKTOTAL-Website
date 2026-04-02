package com.bookstore.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8081}")
    private String appBaseUrl;

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("BookTotal Support <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject("Chào mừng bạn đến với BookTotal!");

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                    <h2 style="color: #C92127; text-align: center;">Chào mừng %s!</h2>
                    <p>Cảm ơn bạn đã đăng ký tài khoản tại <b>BookTotal</b>.</p>
                    <p>Tài khoản của bạn đã được tạo thành công.</p>
                </div>
                """.formatted(fullName);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Async
    public void sendResetPasswordEmail(String toEmail, String token) {
        try {
            String base = (appBaseUrl != null ? appBaseUrl.replaceAll("/+$", "") : "http://localhost:8081");
            String resetLink = base + "/reset-password?token=" + token;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("BookTotal Support <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject("[BookTotal] Mã xác nhận đặt lại mật khẩu");

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                    <h2 style="color: #C92127; text-align: center;">Đặt lại mật khẩu</h2>
                    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với email này.</p>
                    <p><strong>Mã xác nhận</strong> (có thể dán vào trình duyệt nếu cần):</p>
                    <p style="font-family: ui-monospace, Consolas, monospace; font-size: 1rem; background: #f8f9fa; padding: 12px 16px; border-radius: 8px; word-break: break-all;">%s</p>
                    <p>Link đặt lại mật khẩu có hiệu lực trong <strong>15 phút</strong>:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #C92127; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;">ĐẶT LẠI MẬT KHẨU</a>
                    </div>
                    <p style="color: #777; font-size: 12px;">Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>
                </div>
                """.formatted(token, resetLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.warn("Không gửi được email đặt lại mật khẩu tới {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Thông báo email khi admin đổi trạng thái đơn hàng (chạy bất đồng bộ).
     */
    @Async
    public void sendOrderStatusEmail(String toEmail, String recipientName, Long orderId,
                                     String statusLabelVi, String detailHtmlFragment) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        try {
            String safeName = escapeHtml(recipientName != null ? recipientName : "Quý khách");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("BookTotal <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject("[BookTotal] Đơn hàng #" + orderId + " — " + statusLabelVi);

            String body = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                        <h2 style="color: #C92127;">Xin chào %s,</h2>
                        <p>Đơn hàng <strong>#%d</strong> của bạn đã được cập nhật trạng thái:</p>
                        <p style="font-size: 1.1rem;"><strong>%s</strong></p>
                        %s
                        <p style="margin-top: 24px; color: #555;">Trân trọng,<br><strong>BookTotal</strong></p>
                    </div>
                    """.formatted(safeName, orderId, escapeHtml(statusLabelVi),
                    detailHtmlFragment != null ? detailHtmlFragment : "");

            helper.setText(body, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Không gửi được email cập nhật đơn #{}: {}", orderId, e.getMessage());
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
