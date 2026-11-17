package com.example.projectlabexam;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    // SMTP credentials are read from environment variables (never commit them)
    private final String from = System.getenv("SMTP_USER");
    private final String password = System.getenv("SMTP_APP_PASSWORD");

    public void sendToken(String to, String token) {
        String host = "smtp.gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("Your Verification Token");
            message.setText("Your 4-digit verification token is: " + token + "\nIt will expire in 5 minutes.");

            Transport.send(message);
            System.out.println("Token sent to " + to);
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
