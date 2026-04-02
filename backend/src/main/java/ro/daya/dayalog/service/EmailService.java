package ro.daya.dayalog.service;

public interface EmailService {
    void send(String to, String subject, String body);
}