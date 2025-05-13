import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

public class Dashboard extends JFrame {
    private int userId;
    private JComboBox<String> paymentMethodCombo;
    private JPanel cartPanel, menuPanel, historyPanel;
    private DefaultListModel<String> cartListModel;
    private Map<Integer, Integer> cartItems; // item_id -> quantity

    public Dashboard(int userId) {
        this.userId = userId;
        this.cartItems = new HashMap<>();
        setTitle("Food Delivery Dashboard");
        setSize(900, 600);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        menuPanel = new JPanel(new GridLayout(0, 1));
        historyPanel = new JPanel(new BorderLayout());
        cartPanel = new JPanel(new BorderLayout());

        tabs.addTab("Menu", new JScrollPane(menuPanel));
        tabs.addTab("Cart", cartPanel);
        tabs.addTab("Order History", historyPanel);

        add(tabs, BorderLayout.CENTER);

        loadMenu();
        setupCart();
        loadHistory();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void loadMenu() {
        try (Connection conn = DBConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT fi.item_id, fi.item_name, fi.price, r.restaurant_name FROM food_items fi JOIN restaurants r ON fi.restaurant_id = r.restaurant_id");

            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                String itemName = rs.getString("item_name");
                double price = rs.getDouble("price");
                String restName = rs.getString("restaurant_name");

                JPanel itemPanel = new JPanel(new BorderLayout());
                itemPanel.setBorder(BorderFactory.createTitledBorder(restName));
                JLabel label = new JLabel(itemName + " - $" + price);
                JButton addButton = new JButton("Add to Cart");

                addButton.addActionListener(e -> {
                    cartItems.put(itemId, cartItems.getOrDefault(itemId, 0) + 1);
                    updateCart();
                });

                itemPanel.add(label, BorderLayout.CENTER);
                itemPanel.add(addButton, BorderLayout.EAST);
                menuPanel.add(itemPanel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupCart() {
        cartListModel = new DefaultListModel<>();
        JList<String> cartList = new JList<>(cartListModel);
        JButton orderButton = new JButton("Place Order");

        String[] payments = {"Cash", "Card", "Online Banking"};
        paymentMethodCombo = new JComboBox<>(payments);

        orderButton.addActionListener(e -> placeOrder());

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JLabel("Payment Method:"));
        bottomPanel.add(paymentMethodCombo);
        bottomPanel.add(orderButton);

        cartPanel.add(new JScrollPane(cartList), BorderLayout.CENTER);
        cartPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateCart() {
        cartListModel.clear();
        try (Connection conn = DBConnection.getConnection()) {
            for (Map.Entry<Integer, Integer> entry : cartItems.entrySet()) {
                int itemId = entry.getKey();
                int qty = entry.getValue();

                PreparedStatement ps = conn.prepareStatement("SELECT item_name FROM food_items WHERE item_id = ?");
                ps.setInt(1, itemId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    cartListModel.addElement(rs.getString("item_name") + " x" + qty);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void placeOrder() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }

        String paymentMethod = paymentMethodCombo.getSelectedItem().toString();
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            PreparedStatement orderStmt = conn.prepareStatement(
                    "INSERT INTO orders (user_id, payment_method) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            orderStmt.setInt(1, userId);
            orderStmt.setString(2, paymentMethod);
            orderStmt.executeUpdate();

            ResultSet rs = orderStmt.getGeneratedKeys();
            int orderId = 0;
            if (rs.next()) {
                orderId = rs.getInt(1);
            }

            PreparedStatement itemStmt = conn.prepareStatement(
                    "INSERT INTO order_items (order_id, item_id, quantity) VALUES (?, ?, ?)"
            );
            for (Map.Entry<Integer, Integer> entry : cartItems.entrySet()) {
                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, entry.getKey());
                itemStmt.setInt(3, entry.getValue());
                itemStmt.addBatch();
            }
            itemStmt.executeBatch();

            conn.commit();
            cartItems.clear();
            updateCart();
            loadHistory();
            JOptionPane.showMessageDialog(this, "Order placed! Status: Delivered");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadHistory() {
        historyPanel.removeAll();
        DefaultListModel<String> historyModel = new DefaultListModel<>();
        JList<String> historyList = new JList<>(historyModel);

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT o.order_id, o.order_date, o.payment_method, fi.item_name, oi.quantity " +
                            "FROM orders o " +
                            "JOIN order_items oi ON o.order_id = oi.order_id " +
                            "JOIN food_items fi ON fi.item_id = oi.item_id " +
                            "WHERE o.user_id = ? ORDER BY o.order_date DESC"
            );
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            int lastOrderId = -1;
            StringBuilder sb = new StringBuilder();

            while (rs.next()) {
                int orderId = rs.getInt("order_id");
                String item = rs.getString("item_name");
                int qty = rs.getInt("quantity");
                String date = rs.getString("order_date");
                String payment = rs.getString("payment_method");

                if (orderId != lastOrderId) {
                    if (lastOrderId != -1) {
                        historyModel.addElement(sb.toString());
                        sb = new StringBuilder();
                    }
                    sb.append("Order ID: ").append(orderId)
                            .append(" | Date: ").append(date)
                            .append(" | Payment: ").append(payment)
                            .append(" | Items: ");
                    lastOrderId = orderId;
                }

                sb.append(item).append(" x").append(qty).append(", ");
            }
            if (sb.length() > 0) {
                historyModel.addElement(sb.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        historyPanel.add(new JScrollPane(historyList), BorderLayout.CENTER);
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    private void loadMenuItems() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT fi.item_name, fi.price, r.restaurant_name " +
                    "FROM food_items fi JOIN restaurants r ON fi.restaurant_id = r.restaurant_id";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String item = rs.getString("item_name");
                double price = rs.getDouble("price");
                String restaurant = rs.getString("restaurant_name");

                JButton itemButton = new JButton(item + " - Rs. " + price + " (" + restaurant + ")");
                itemButton.addActionListener(e -> addToCart(item, price));
                menuPanel.add(itemButton);
            }

            menuPanel.revalidate();
            menuPanel.repaint();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    private void addToCart(String item, double price) {
        // Get the item ID by querying the database (as you have the item name)
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT item_id FROM food_items WHERE item_name = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, item);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int itemId = rs.getInt("item_id");
                cartItems.put(itemId, cartItems.getOrDefault(itemId, 0) + 1); // Add item to cart with quantity
                updateCart(); // Update the cart view
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
