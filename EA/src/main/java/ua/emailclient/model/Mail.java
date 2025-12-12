package ua.emailclient.model;

import java.time.LocalDateTime;

public class Mail {
    private String id;
    private String sender;
    private String receiver;
    private String subject;
    private String body;
    private String folder;
    private LocalDateTime createdAt;
    private int ownerUserId;

    public Mail() {}

    private Mail(MailBuilder builder) {
        this.id = builder.id;
        this.sender = builder.sender;
        this.receiver = builder.receiver;
        this.subject = builder.subject;
        this.body = builder.body;
        this.folder = builder.folder;
        this.createdAt = builder.createdAt;
        this.ownerUserId = builder.ownerUserId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(int ownerUserId) { this.ownerUserId = ownerUserId; }

    public static class MailBuilder {
        private final String sender;
        private final int ownerUserId;

        private String id = null;
        private String receiver = "";
        private String subject = "";
        private String body = "";
        private String folder = "Draft";
        private LocalDateTime createdAt = LocalDateTime.now();

        public MailBuilder(String sender, int ownerUserId) {
            this.sender = sender;
            this.ownerUserId = ownerUserId;
        }
        public MailBuilder id(String id) {
            this.id = id;
            return this;
        }
        public MailBuilder receiver(String receiver) {
            this.receiver = receiver;
            return this;
        }
        public MailBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }
        public MailBuilder body(String body) {
            this.body = body;
            return this;
        }
        public MailBuilder folder(String folder) {
            this.folder = folder;
            return this;
        }
        public MailBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Mail build() {
            return new Mail(this);
        }
    }
}