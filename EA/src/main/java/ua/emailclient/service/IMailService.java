package ua.emailclient.service;

import ua.emailclient.model.Mail;
import java.util.List;

public interface IMailService {
    Mail saveDraft(Mail mail, String senderUsername);
    Mail sendMail(Mail mail, String senderUsername);

    Mail getMailById(String id, String username);
    Mail getDraftById(String id, String username);

    List<Mail> getInbox(String username);
    List<Mail> getSent(String username);
    List<Mail> getDrafts(String username);
}