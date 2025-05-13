import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AddressPhoneScreen extends JFrame {
    private String username;
    private int userId;
    private JTextField addressField, phoneField;
    private JPasswordField passwordField;
    private JButton submitButton, cancelButton;

    public AddressPhoneScreen(String username) {
        this.username = username;
        this.userId = getUserIdByUsername(username);

        setTitle("Enter Address, Phone, and Password");
        setSize(400, 300);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(4, 2));
        JLabel addressLabel = new JLabel("Address:");
        JLabel phoneLabel = new JLabel("Phone Number:");
        JLabel passwordLabel = new JLabel("Password:");

        addressField = new JTextField();
        phoneField = new JTextField();
        passwordField = new JPasswordField();

        submitButton = new JButton("Submit");
        cancelButton = new JButton("Cancel");

        formPanel.add(addressLabel);
        formPanel.add(addressField);
        formPanel.add(phoneLabel);
        formPanel.add(phoneField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);
        formPanel.add(submitButton);
        formPanel.add(cancelButton);

        add(formPanel, BorderLayout.CENTER);

        submitButton.addActionListener(e -> submitAddressPhoneAndPassword());
        cancelButton.addActionListener(e -> cancelRegistration());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private int getUserIdByUsername(String username) {
        int userId = -1;
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT user_id FROM users WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userId;
    }

    private void submitAddressPhoneAndPassword() {
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (address.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE users SET address = ?, phone = ?, password = ? WHERE user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, address);
            ps.setString(2, phone);
            ps.setString(3, password);
            ps.setInt(4, userId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                JOptionPane.showMessageDialog(this, "Registration completed!");
                new Dashboard(userId);
                this.setVisible(false);
            } else {
                JOptionPane.showMessageDialog(this, "Error saving data.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cancelRegistration() {
        new LoginScreen();
        this.setVisible(false);
    }
}
