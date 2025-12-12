package ua.emailclient.dao;

import ua.emailclient.DBUtil;
import ua.emailclient.model.User;
import org.springframework.stereotype.Component;
import java.sql.*;

@Component
public class UserDAO {
    public User findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, display_name FROM users WHERE username = ?";

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("username"),
                            rs.getString("password_hash"), rs.getString("display_name"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding user: " + username, e);
        }
        return null;
    }
}