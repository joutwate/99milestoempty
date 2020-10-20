package com.nnmilestoempty.base.service;

import com.nnmilestoempty.base.utils.EmailTemplateUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final Environment environment;
    private final JavaMailSender mailSender;

    @Value("${nnmilestoempty.noreply.email}")
    private String noReplyEmailAddress;

    private EmailService(Environment environment, JavaMailSender mailSender) {
        this.environment = environment;
        this.mailSender = mailSender;
    }

    public void sendEmail(String toEmail, String fromEmail, String subject, Map<String, String> data,
                          EmailTemplate template) {
        boolean isTestEnv = false;
        for (String profile : environment.getActiveProfiles()) {
            if ("test".compareToIgnoreCase(profile) == 0) {
                logger.info("!!! Not sending email since this is the test env !!!");
                return;
            }
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper msg = new MimeMessageHelper(mimeMessage);
            msg.setTo(toEmail);
            msg.setFrom(noReplyEmailAddress);
            msg.setSubject(subject);

            msg.setText(EmailTemplateUtil.getInstance().loadEmailTemplate(template.getLocation(), data), true);

            mailSender.send(mimeMessage);
        } catch (MailException | MessagingException e) {
            // We may need to re-send the email.
            logger.error("Exception thrown when sending confirm email", e);
        }
    }

    public enum EmailTemplate {
        CONFIRM_REGISTRATION("/templates/confirm-email.html"),
        REGISTRATION_SUCCESSFUL("/templates/registration-successful-email.html"),
        MULTI_FACTOR_ENABLED("/templates/multifactor-enabled-email.html");

        @Getter
        private final String location;

        EmailTemplate(String location) {
            this.location = location;
        }
    }
}
