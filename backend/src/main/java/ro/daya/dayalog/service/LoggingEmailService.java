package ro.daya.dayalog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("=== EMAIL OUTBOUND ===");
        log.info("TO: {}", to);
        log.info("SUBJECT: {}", subject);
        log.info("BODY:\n{}", body);
        log.info("======================");
    }
}