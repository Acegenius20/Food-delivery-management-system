import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginScreen extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginScreen() {
        setTitle("Food Delivery - Login");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create UI components
        JLabel usernameLabel = new JLabel("Username:");
        JLabel passwordLabel = new JLabel("Password:");
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        // Login action
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (!username.isEmpty() && !password.isEmpty()) {
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "SELECT user_id FROM users WHERE username = ? AND password = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, username);
                    ps.setString(2, password);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        int userId = rs.getInt("user_id");  // get the actual user ID
                        JOptionPane.showMessageDialog(this, "Login successful!");
                        new Dashboard(userId);  // pass int user ID
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid username or password.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Database error.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please enter both username and password.");
            }
        });

        // Register action
        registerButton.addActionListener(e -> {
            String username = usernameField.getText().trim();

            if (!username.isEmpty()) {
                // Pass only username to next screen
                new AddressPhoneScreen(username);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Please enter a username.");
            }
        });

        // Layout setup
        setLayout(new GridLayout(4, 2, 10, 10));
        add(usernameLabel);
        add(usernameField);
        add(passwordLabel);
        add(passwordField);
        add(loginButton);
        add(registerButton);

        setVisible(true);
    }
}
