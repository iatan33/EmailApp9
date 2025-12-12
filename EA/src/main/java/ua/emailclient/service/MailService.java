package ua.emailclient.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import ua.emailclient.dao.MailDAO;
import ua.emailclient.dao.UserDAO;
import ua.emailclient.model.Mail;
import ua.emailclient.model.User;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

@Service("mailService")
public class MailService implements IMailService {

    private final MailDAO mailDAO;
    private final UserDAO userDAO;
    private final OAuth2AuthorizedClientService authorizedClientService;

    private static final String SIGNATURE = "\n\n--\nВідправлено з EmailClient";

    @Autowired
    public MailService(MailDAO mailDAO, UserDAO userDAO, OAuth2AuthorizedClientService authorizedClientService) {
        this.mailDAO = mailDAO;
        this.userDAO = userDAO;
        this.authorizedClientService = authorizedClientService;
    }

    private Gmail getGmailService() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            throw new RuntimeException("Необхідно увійти через Google!");
        }
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );
        if (client == null || client.getAccessToken() == null) {
            throw new RuntimeException("Токен доступу не знайдено. Перезайдіть.");
        }

        Credential credential = new GoogleCredential().setAccessToken(client.getAccessToken().getTokenValue());
        return new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Email Client App").build();
    }

    @Override
    public List<Mail> getInbox(String username) {
        return fetchMessages("label:INBOX");
    }

    @Override
    public List<Mail> getSent(String username) {
        return fetchMessages("label:SENT");
    }

    @Override
    public List<Mail> getDrafts(String username) {
        List<Mail> draftsList = new ArrayList<>();
        try {
            Gmail gmail = getGmailService();
            ListDraftsResponse response = gmail.users().drafts().list("me").execute();

            if (response.getDrafts() != null) {
                for (Draft d : response.getDrafts()) {
                    Draft fullDraft = gmail.users().drafts().get("me", d.getId()).setFormat("full").execute();
                    Mail mail = convertMessageToMail(fullDraft.getMessage());
                    mail.setId(d.getId());
                    mail.setFolder("Draft");
                    draftsList.add(mail);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return draftsList;
    }

    private List<Mail> fetchMessages(String query) {
        List<Mail> mails = new ArrayList<>();
        try {
            Gmail gmail = getGmailService();
            ListMessagesResponse response = gmail.users().messages().list("me")
                    .setQ(query).setMaxResults(20L).execute();

            if (response.getMessages() != null) {
                for (Message msg : response.getMessages()) {
                    Message fullMsg = gmail.users().messages().get("me", msg.getId()).setFormat("full").execute();
                    mails.add(convertMessageToMail(fullMsg));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mails;
    }

    @Override
    public Mail getMailById(String id, String username) {
        try {
            Gmail gmail = getGmailService();
            Message msg = gmail.users().messages().get("me", id).setFormat("full").execute();
            return convertMessageToMail(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Mail getDraftById(String id, String username) {
        try {
            Gmail gmail = getGmailService();
            Draft draft = gmail.users().drafts().get("me", id).setFormat("full").execute();
            Mail mail = convertMessageToMail(draft.getMessage());
            mail.setId(draft.getId());
            mail.setFolder("Draft");
            return mail;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Mail sendMail(Mail mail, String senderUsername) {
        mail.setBody(mail.getBody() + SIGNATURE);
        try {
            Gmail gmail = getGmailService();
            MimeMessage mimeMessage = createMimeMessage(senderUsername, mail.getReceiver(), mail.getSubject(), mail.getBody());
            Message message = createMessageWithEmail(mimeMessage);

            if (mail.getId() != null && !mail.getId().isEmpty() && "Draft".equals(mail.getFolder())) {
                Draft d = new Draft();
                d.setMessage(message);
                d.setId(mail.getId());
                message = gmail.users().drafts().send("me", d).execute();
            } else {
                message = gmail.users().messages().send("me", message).execute();
            }

            User user = userDAO.findByUsername(senderUsername);
            if (user != null) {
                mail.setId(message.getId());
                mail.setFolder("Sent");
                mailDAO.save(mail);
            }
            return mail;
        } catch (Exception e) {
            throw new RuntimeException("Помилка відправки: " + e.getMessage(), e);
        }
    }

    @Override
    public Mail saveDraft(Mail mail, String senderUsername) {
        try {
            Gmail gmail = getGmailService();
            MimeMessage mimeMessage = createMimeMessage(senderUsername, mail.getReceiver(), mail.getSubject(), mail.getBody());
            Message message = createMessageWithEmail(mimeMessage);
            Draft draft = new Draft();
            draft.setMessage(message);

            if (mail.getId() != null && !mail.getId().isEmpty() && "Draft".equals(mail.getFolder())) {
                draft.setId(mail.getId());
                draft = gmail.users().drafts().update("me", mail.getId(), draft).execute();
            } else {
                draft = gmail.users().drafts().create("me", draft).execute();
            }

            mail.setId(draft.getId());
            mail.setFolder("Draft");
            return mail;
        } catch (Exception e) {
            throw new RuntimeException("Помилка збереження: " + e.getMessage(), e);
        }
    }


    private Mail convertMessageToMail(Message msg) {
        Mail mail = new Mail();
        mail.setId(msg.getId());
        mail.setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(msg.getInternalDate()), ZoneId.systemDefault()));

        if (msg.getPayload() != null) {
            List<MessagePartHeader> headers = msg.getPayload().getHeaders();
            if (headers != null) {
                for (MessagePartHeader h : headers) {
                    if ("Subject".equalsIgnoreCase(h.getName())) mail.setSubject(h.getValue());
                    if ("From".equalsIgnoreCase(h.getName())) mail.setSender(h.getValue());
                    if ("To".equalsIgnoreCase(h.getName())) mail.setReceiver(h.getValue());
                }
            }
            mail.setBody(parseBody(msg.getPayload()));
        }
        return mail;
    }

    private String parseBody(MessagePart part) {
        if (part.getMimeType().equalsIgnoreCase("text/plain") && part.getBody().getData() != null) {
            return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
        }
        if (part.getMimeType().equalsIgnoreCase("text/html") && part.getBody().getData() != null) {
            return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
        }
        if (part.getParts() != null) {
            for (MessagePart p : part.getParts()) {
                String txt = parseBody(p);
                if (!txt.isEmpty()) return txt;
            }
        }
        return "";
    }

    private MimeMessage createMimeMessage(String from, String to, String subject, String body) throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage mm = new MimeMessage(session);
        mm.setFrom(new InternetAddress(from));
        mm.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        mm.setSubject(subject);
        mm.setText(body);
        return mm;
    }

    private Message createMessageWithEmail(MimeMessage mm) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mm.writeTo(buffer);
        String encoded = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
        Message m = new Message();
        m.setRaw(encoded);
        return m;
    }
}