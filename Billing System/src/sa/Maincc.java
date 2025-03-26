package sa;

import java.sql.*;
import java.util.*;

public class Maincc {
    private static Connection connection;
    
    public static void main(String[] args) {
        try {
            String url = "jdbc:mysql://localhost:3306/billing_system";
            String username = "root"; 
            String password = "S@shank-1164"; 
            connection = DriverManager.getConnection(url, username, password);
            
            Scanner scanner = new Scanner(System.in);
            Map<String, Double> itemPrices = new LinkedHashMap<>();
            Map<String, Integer> cart = new HashMap<>();

            loadProductsFromDatabase(itemPrices);

            System.out.println("==== WELCOME TO Sim+Emmo BILLING SYSTEM ====");
            
            String customerName = getCustomerName(scanner);
            String mobileNumber = getMobileNumber(scanner);
            
            int customerId = getOrCreateCustomer(customerName, mobileNumber);

            displayAvailableItems(itemPrices);

            int choice;
            do {
                displayMenu();
                choice = getValidChoice(scanner);
                
                switch (choice) {
                    case 1:
                        addItemToCart(scanner, itemPrices, cart);
                        break;
                    case 2:
                        removeItemFromCart(scanner, cart);
                        break;
                    case 3:
                        modifyItemQuantity(scanner, cart, itemPrices);
                        break;
                    case 4:
                        displayCartItems(cart, itemPrices);
                        break;
                    case 5:
                        displayAvailableItems(itemPrices);
                        break;
                    case 6:
                        int transactionId = generateBill(customerId, cart, itemPrices);
                        System.out.println("\nBill generated with ID: " + transactionId);
                        System.out.println("Exiting the system...");
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice! Please try again.");
                }
            } while (choice != 0);

            scanner.close();
            connection.close();
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
    }

    private static void loadProductsFromDatabase(Map<String, Double> itemPrices) throws SQLException {
        String query = "SELECT name, price FROM products";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                itemPrices.put(rs.getString("name"), rs.getDouble("price"));
            }
        }
    }

    private static String getCustomerName(Scanner scanner) {
        System.out.println("Enter Your Name: ");
        String name;
        while (true) {
            name = scanner.nextLine().trim();
            if (!name.isEmpty()) {
                return name;
            }
            System.out.println("Name cannot be empty. Please enter your name:");
        }
    }

    private static String getMobileNumber(Scanner scanner) {
        System.out.println("Enter Your Mobile no: ");
        String mobileNumber;
        while (true) {
            mobileNumber = scanner.nextLine().trim();
            if (mobileNumber.matches("\\d{10}")) {
                return mobileNumber;
            }
            System.out.println("Invalid mobile number. Please enter 10 digits:");
        }
    }

    private static int getOrCreateCustomer(String name, String mobile) throws SQLException {
        // Check if customer exists
        String checkQuery = "SELECT customer_id FROM customers WHERE mobile = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(checkQuery)) {
            pstmt.setString(1, mobile);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("customer_id");
            }
        }
        
        // Create new customer
        String insertQuery = "INSERT INTO customers (name, mobile) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, mobile);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    private static void displayAvailableItems(Map<String, Double> itemPrices) {
        System.out.println("\nAvailable Items:");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-20s %-10s\n", "Item Name", "Price (Rs.)");
        System.out.println("--------------------------------------------------");
        for (Map.Entry<String, Double> entry : itemPrices.entrySet()) {
            System.out.printf("%-20s %-10.2f\n", entry.getKey(), entry.getValue());
        }
        System.out.println("--------------------------------------------------");
    }

    private static void displayMenu() {
        System.out.println("\n1. Add item");
        System.out.println("2. Remove item");
        System.out.println("3. Modify item quantity");
        System.out.println("4. Show items in the cart");
        System.out.println("5. Show all items");
        System.out.println("6. Generate bill and exit");
        System.out.print("Enter your choice: ");
    }

    private static int getValidChoice(Scanner scanner) {
        while (true) {
            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                return choice;
            } catch (InputMismatchException e) {
                System.out.print("Invalid input. Please enter a number (1-6): ");
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private static void addItemToCart(Scanner scanner, Map<String, Double> itemPrices, Map<String, Integer> cart) {
        System.out.print("Enter item name to add: ");
        String addItem = scanner.nextLine().toLowerCase();
        
        if (itemPrices.containsKey(addItem)) {
            System.out.print("Enter quantity: ");
            int quantity = getValidQuantity(scanner);
            cart.put(addItem, cart.getOrDefault(addItem, 0) + quantity);
            System.out.println(quantity + " " + addItem + "(s) added to cart!");
            displayCurrentCartTotal(cart, itemPrices);
        } else {
            System.out.println("Item not found. Available items are:");
            displayAvailableItems(itemPrices);
        }
    }

    private static void removeItemFromCart(Scanner scanner, Map<String, Integer> cart) {
        if (cart.isEmpty()) {
            System.out.println("Your cart is empty!");
            return;
        }
        
        displayCartItems(cart, null);
        System.out.print("Enter item name to remove: ");
        String removeItem = scanner.nextLine().toLowerCase();
        
        if (cart.containsKey(removeItem)) {
            cart.remove(removeItem);
            System.out.println(removeItem + " removed from cart!");
        } else {
            System.out.println("Item not in cart.");
        }
    }

    private static void modifyItemQuantity(Scanner scanner, Map<String, Integer> cart, Map<String, Double> itemPrices) {
        if (cart.isEmpty()) {
            System.out.println("Your cart is empty!");
            return;
        }
        
        displayCartItems(cart, itemPrices);
        System.out.print("Enter item name to modify quantity: ");
        String item = scanner.nextLine().toLowerCase();
        
        if (cart.containsKey(item)) {
            System.out.print("Enter new quantity: ");
            int newQuantity = getValidQuantity(scanner);
            if (newQuantity > 0) {
                cart.put(item, newQuantity);
                System.out.println(item + " quantity updated to " + newQuantity);
            } else {
                cart.remove(item);
                System.out.println(item + " removed from cart (quantity set to 0)");
            }
            displayCurrentCartTotal(cart, itemPrices);
        } else {
            System.out.println("Item not in cart.");
        }
    }

    private static int getValidQuantity(Scanner scanner) {
        while (true) {
            try {
                int quantity = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                if (quantity >= 0) {
                    return quantity;
                }
                System.out.print("Quantity cannot be negative. Please enter valid quantity: ");
            } catch (InputMismatchException e) {
                System.out.print("Invalid input. Please enter a number: ");
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private static void displayCartItems(Map<String, Integer> cart, Map<String, Double> itemPrices) {
        if (cart.isEmpty()) {
            System.out.println("\nYour cart is empty!");
            return;
        }
        
        System.out.println("\nItems in Cart:");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-20s %-10s %-10s %-10s\n", "Item Name", "Quantity", "Price", "Subtotal");
        System.out.println("--------------------------------------------------");
        
        double total = 0;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String item = entry.getKey();
            int quantity = entry.getValue();
            double price = itemPrices != null ? itemPrices.get(item) : 0;
            double subtotal = quantity * price;
            total += subtotal;
            System.out.printf("%-20s %-10d %-10.2f %-10.2f\n", item, quantity, price, subtotal);
        }
        
        System.out.println("--------------------------------------------------");
        System.out.printf("%-42s Rs.%.2f\n", "Total:", total);
        System.out.println("--------------------------------------------------");
    }

    private static void displayCurrentCartTotal(Map<String, Integer> cart, Map<String, Double> itemPrices) {
        double total = 0;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            total += entry.getValue() * itemPrices.get(entry.getKey());
        }
        System.out.printf("Current cart total: Rs.%.2f\n", total);
    }

    private static int generateBill(int customerId, Map<String, Integer> cart, 
                                  Map<String, Double> itemPrices) throws SQLException {
        if (cart.isEmpty()) {
            System.out.println("\nYour cart is empty. No bill generated.");
            return -1;
        }
        
        double subtotal = 0;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            subtotal += entry.getValue() * itemPrices.get(entry.getKey());
        }
        double gst = subtotal * 0.08;
        double totalAmount = subtotal + gst;
        
        connection.setAutoCommit(false);
        try {
            String transQuery = "INSERT INTO transactions (customer_id, subtotal, gst, total_amount) " +
                               "VALUES (?, ?, ?, ?)";
            int transactionId;
            try (PreparedStatement pstmt = connection.prepareStatement(transQuery, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, customerId);
                pstmt.setDouble(2, subtotal);
                pstmt.setDouble(3, gst);
                pstmt.setDouble(4, totalAmount);
                pstmt.executeUpdate();
                
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    transactionId = rs.getInt(1);
                } else {
                    throw new SQLException("Failed to get transaction ID");
                }
            }
            
            String itemQuery = "INSERT INTO transaction_items (transaction_id, product_id, quantity, price, total_price) " +
                             "VALUES (?, (SELECT product_id FROM products WHERE name = ?), ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(itemQuery)) {
                for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                    String itemName = entry.getKey();
                    int quantity = entry.getValue();
                    double price = itemPrices.get(itemName);
                    double totalPrice = quantity * price;
                    
                    pstmt.setInt(1, transactionId);
                    pstmt.setString(2, itemName);
                    pstmt.setInt(3, quantity);
                    pstmt.setDouble(4, price);
                    pstmt.setDouble(5, totalPrice);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            connection.commit();
            
            printBill(transactionId, customerId, cart, itemPrices);
            
            return transactionId;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void printBill(int transactionId, int customerId, 
                                Map<String, Integer> cart, Map<String, Double> itemPrices) throws SQLException {
        String customerQuery = "SELECT name, mobile FROM customers WHERE customer_id = ?";
        String customerName = "";
        String mobileNumber = "";
        try (PreparedStatement pstmt = connection.prepareStatement(customerQuery)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                customerName = rs.getString("name");
                mobileNumber = rs.getString("mobile");
            }
        }
        
        // Get transaction details
        String transQuery = "SELECT subtotal, gst, total_amount, transaction_date FROM transactions WHERE transaction_id = ?";
        double subtotal = 0;
        double gst = 0;
        double totalAmount = 0;
        String transactionDate = "";
        try (PreparedStatement pstmt = connection.prepareStatement(transQuery)) {
            pstmt.setInt(1, transactionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                subtotal = rs.getDouble("subtotal");
                gst = rs.getDouble("gst");
                totalAmount = rs.getDouble("total_amount");
                transactionDate = rs.getTimestamp("transaction_date").toString();
            }
        }
        
        // Print bill
        System.out.println("\nThank you for shopping with us\n");
        System.out.println("--------------------------------------------------");
        System.out.println("Bill ID: " + transactionId);
        System.out.println("Bill to: " + customerName);
        System.out.println("Mobile: " + mobileNumber);
        System.out.println("Date: " + transactionDate);
        System.out.println("--------------------------------------------------");
        System.out.printf("%-20s %-10s %-10s %-10s\n", "Item Name", "Quantity", "Rate", "Total Price");
        System.out.println("--------------------------------------------------");

        for (String item : cart.keySet()) {
            int qty = cart.get(item);
            double rate = itemPrices.get(item);
            double totalPrice = qty * rate;
            System.out.printf("%-20s %-10d %-10.2f %-10.2f\n", item, qty, rate, totalPrice);
        }

        System.out.println("--------------------------------------------------");
        System.out.printf("%-42s Rs.%.2f\n", "Subtotal:", subtotal);
        System.out.printf("%-42s Rs.%.2f\n", "GST (8%):", gst);
        System.out.printf("%-42s Rs.%.2f\n", "Final amount:", totalAmount);
        System.out.println("--------------------------------------------------");
        System.out.println("Thank you for your purchase!");
    }
}