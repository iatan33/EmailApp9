package ua.emailclient.dao;

import ua.emailclient.DBUtil;
import ua.emailclient.model.Mail;
import org.springframework.stereotype.Component;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class MailDAO {

    private Mail extractMailFromResultSet(ResultSet rs) throws SQLException {
        return new Mail.MailBuilder(rs.getString("sender"), rs.getInt("owner_user_id"))
                .id(rs.getString("id"))
                .receiver(rs.getString("receiver"))
                .subject(rs.getString("subject"))
                .body(rs.getString("body"))
                .folder(rs.getString("folder"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }

    public Mail save(Mail mail) {
        String checkSql = "SELECT COUNT(*) FROM mails WHERE id = ?";
        String sql;

        boolean exists = false;
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(checkSql)) {
            ps.setString(1, mail.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) exists = true;
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (!exists) {
            sql = "INSERT INTO mails (id, sender, receiver, subject, body, folder, owner_user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE mails SET sender=?, receiver=?, subject=?, body=?, folder=?, owner_user_id=? WHERE id=?";
        }

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (!exists) {
                ps.setString(1, mail.getId());
                ps.setString(2, mail.getSender());
                ps.setString(3, mail.getReceiver());
                ps.setString(4, mail.getSubject());
                ps.setString(5, mail.getBody());
                ps.setString(6, mail.getFolder());
                ps.setInt(7, mail.getOwnerUserId());
            } else {
                ps.setString(1, mail.getSender());
                ps.setString(2, mail.getReceiver());
                ps.setString(3, mail.getSubject());
                ps.setString(4, mail.getBody());
                ps.setString(5, mail.getFolder());
                ps.setInt(6, mail.getOwnerUserId());
                ps.setString(7, mail.getId());
            }
            ps.executeUpdate();
            return mail;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Mail findByIdAndOwner(String id, int ownerUserId) {
        String sql = "SELECT * FROM mails WHERE id = ? AND owner_user_id = ?";

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.setInt(2, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractMailFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Mail> findByFolderAndOwner(String folder, int ownerUserId) {
        List<Mail> mails = new ArrayList<>();
        String sql = "SELECT * FROM mails WHERE owner_user_id = ? AND folder = ? ORDER BY created_at DESC";

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, ownerUserId);
            ps.setString(2, folder);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mails.add(extractMailFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding mails in folder: " + folder, e);
        }
        return mails;
    }

    public List<Mail> findByReceiver(String receiverUsername) {
        List<Mail> mails = new ArrayList<>();
        String sql = "SELECT * FROM mails WHERE receiver = ? AND folder = 'Sent' ORDER BY created_at DESC";

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, receiverUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mails.add(extractMailFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding received mails for receiver: " + receiverUsername, e);
        }
        return mails;
    }
}