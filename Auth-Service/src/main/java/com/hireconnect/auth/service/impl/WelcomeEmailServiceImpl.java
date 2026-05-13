package com.hireconnect.auth.service.impl;

import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.enums.UserRole;
import com.hireconnect.auth.service.WelcomeEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WelcomeEmailServiceImpl implements WelcomeEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Override
    @Async
    public void sendWelcomeEmail(UserCredential user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Skipping welcome email because userId={} has no email", user.getUserId());
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject("Welcome to HireConnect");
            helper.setText(buildWelcomeEmail(user), true);
            mailSender.send(mimeMessage);
            log.info("Welcome email sent to userId={} email={}", user.getUserId(), user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to prepare welcome email for userId={} email={}: {}",
                    user.getUserId(), user.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send welcome email for userId={} email={}: {}",
                    user.getUserId(), user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(UserCredential user, String resetToken, String resetUrl) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Skipping password reset email because userId={} has no email", user.getUserId());
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject("Reset your HireConnect password");
            helper.setText(buildPasswordResetEmail(user, resetToken, resetUrl), true);
            mailSender.send(mimeMessage);
            log.info("Password reset email sent to userId={} email={}", user.getUserId(), user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to prepare password reset email for userId={} email={}: {}",
                    user.getUserId(), user.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send password reset email for userId={} email={}: {}",
                    user.getUserId(), user.getEmail(), e.getMessage(), e);
        }
    }

    private String buildWelcomeEmail(UserCredential user) {
        String name = user.getFullName() == null || user.getFullName().isBlank()
                ? "there"
                : user.getFullName();
        boolean recruiter = user.getRole() == UserRole.RECRUITER;
        String nextStep = recruiter
                ? "Complete your recruiter profile, post your first job, and start managing applicants from your dashboard."
                : "Complete your candidate profile, explore matching jobs, and track your applications from your dashboard.";
        String buttonUrl = recruiter ? "http://localhost:4200/recruiter/dashboard" : "http://localhost:4200/candidate/dashboard";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { margin: 0; padding: 0; background: #f4f7fb; font-family: Arial, sans-serif; color: #172033; }
                    .wrap { max-width: 620px; margin: 0 auto; padding: 28px 16px; }
                    .card { background: #ffffff; border: 1px solid #e3e8f0; border-radius: 12px; overflow: hidden; }
                    .header { background: #0f766e; color: #ffffff; padding: 26px 30px; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .body { padding: 30px; line-height: 1.6; }
                    .button { display: inline-block; margin-top: 18px; padding: 12px 18px; background: #0f766e;
                              color: #ffffff !important; text-decoration: none; border-radius: 8px; font-weight: 700; }
                    .footer { padding: 18px 30px; color: #64748b; font-size: 12px; border-top: 1px solid #e3e8f0; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="header"><h1>Welcome to HireConnect</h1></div>
                      <div class="body">
                        <p>Hi %s,</p>
                        <p>Your HireConnect account has been created successfully.</p>
                        <p>%s</p>
                        <a class="button" href="%s">Open Dashboard</a>
                      </div>
                      <div class="footer">
                        <p>You are receiving this email because you registered on HireConnect.</p>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(name, nextStep, buttonUrl);
    }

    private String buildPasswordResetEmail(UserCredential user, String resetToken, String resetUrl) {
        String name = user.getFullName() == null || user.getFullName().isBlank()
                ? "there"
                : user.getFullName();

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { margin: 0; padding: 0; background: #f4f7fb; font-family: Arial, sans-serif; color: #172033; }
                    .wrap { max-width: 620px; margin: 0 auto; padding: 28px 16px; }
                    .card { background: #ffffff; border: 1px solid #e3e8f0; border-radius: 12px; overflow: hidden; }
                    .header { background: #1d4ed8; color: #ffffff; padding: 26px 30px; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .body { padding: 30px; line-height: 1.6; }
                    .button { display: inline-block; margin: 18px 0; padding: 12px 18px; background: #1d4ed8;
                              color: #ffffff !important; text-decoration: none; border-radius: 8px; font-weight: 700; }
                    .token { padding: 12px; background: #f1f5f9; border-radius: 8px; word-break: break-all; font-family: monospace; }
                    .footer { padding: 18px 30px; color: #64748b; font-size: 12px; border-top: 1px solid #e3e8f0; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="header"><h1>Reset your password</h1></div>
                      <div class="body">
                        <p>Hi %s,</p>
                        <p>We received a request to reset your HireConnect password. This link expires in 30 minutes.</p>
                        <a class="button" href="%s">Reset Password</a>
                        <p>If the button does not work, paste this token into the reset password page:</p>
                        <div class="token">%s</div>
                        <p>If you did not request this, you can safely ignore this email.</p>
                      </div>
                      <div class="footer">
                        <p>You are receiving this email because a password reset was requested for your HireConnect account.</p>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(name, resetUrl, resetToken);
    }
}
