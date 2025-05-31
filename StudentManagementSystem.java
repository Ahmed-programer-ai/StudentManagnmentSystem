import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class StudentManagementSystem {

    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/student_management"; // Replace with your DB URL
    private static final String DB_USER = "root"; // Replace with your DB username
    private static final String DB_PASSWORD = "ah2006ah"; // Replace with your DB password

    // GUI components
    private JFrame frame;
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JTextField idField, nameField, majorField;
    private JButton addButton, viewButton, updateButton, deleteButton;
    private JPanel inputPanel, buttonPanel, tablePanel;
    private JLabel idLabel, nameLabel, majorLabel;

    public static void main(String[] args) {
        // Use invokeLater to ensure GUI updates are done on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                new StudentManagementSystem().start();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public void start() throws SQLException {
        // Initialize GUI components
        frame = new JFrame("Student Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null); // Center the frame

        // Input panel
        inputPanel = new JPanel(new GridLayout(3, 2));
        idLabel = new JLabel("ID:");
        idField = new JTextField();
        nameLabel = new JLabel("Name:");
        nameField = new JTextField();
        majorLabel = new JLabel("Major:");
        majorField = new JTextField();

        inputPanel.add(idLabel);
        inputPanel.add(idField);
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);
        inputPanel.add(majorLabel);
        inputPanel.add(majorField);

        // Button panel
        buttonPanel = new JPanel(new FlowLayout());
        addButton = new JButton("Add");
        viewButton = new JButton("View");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");

        buttonPanel.add(addButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        // Table panel
        tablePanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[] { "ID", "Name", "Major" }, 0);
        studentTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(studentTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Add panels to frame
        frame.setLayout(new BorderLayout());
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.add(tablePanel, BorderLayout.SOUTH);

        // Add action listeners to buttons
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addStudent();
            }
        });

        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewStudents();
            }
        });

        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStudent();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteStudent();
            }
        });

        // Make the frame visible
        frame.setVisible(true);
        createTable(); // Ensure the table exists
    }

    private void createTable() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            // Check if the table exists
            DatabaseMetaData dbmd = connection.getMetaData();
            ResultSet tables = dbmd.getTables(null, null, "students", null);
            if (!tables.next()) {
                // Table does not exist, create it
                String sql = "CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(255), major VARCHAR(255))";
                statement.executeUpdate(sql);
                System.out.println("Table 'students' created."); // For debugging
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error creating table: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e); // Terminate, as the program can't function without the table.
        }
    }

    private void addStudent() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String id = idField.getText();
            String name = nameField.getText();
            String major = majorField.getText();

            if (id.isEmpty() || name.isEmpty() || major.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields.", "Input Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int studentId = Integer.parseInt(id);
                // Check if student ID already exists
                String checkSql = "SELECT COUNT(*) FROM students WHERE id = ?";
                PreparedStatement checkStatement = connection.prepareStatement(checkSql);
                checkStatement.setInt(1, studentId);
                ResultSet resultSet = checkStatement.executeQuery();
                resultSet.next();
                int count = resultSet.getInt(1);
                if (count > 0) {
                    JOptionPane.showMessageDialog(null, "Student with this ID already exists.", "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sql = "INSERT INTO students (id, name, major) VALUES (?, ?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, studentId);
                preparedStatement.setString(2, name);
                preparedStatement.setString(3, major);
                preparedStatement.executeUpdate();

                JOptionPane.showMessageDialog(null, "Student added successfully.");
                clearInputFields();
                viewStudents(); // Refresh the table
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid ID. Please enter a number.", "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error adding student: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewStudents() {
        tableModel.setRowCount(0); // Clear the table before loading data
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM students")) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String major = resultSet.getString("major");
                tableModel.addRow(new Object[] { id, name, major });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error viewing students: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStudent() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String id = idField.getText();
            String name = nameField.getText();
            String major = majorField.getText();

            if (id.isEmpty() || name.isEmpty() || major.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields.", "Input Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int studentId = Integer.parseInt(id);
                // Check if the student exists
                String checkSql = "SELECT COUNT(*) FROM students WHERE id = ?";
                PreparedStatement checkStatement = connection.prepareStatement(checkSql);
                checkStatement.setInt(1, studentId);
                ResultSet resultSet = checkStatement.executeQuery();
                resultSet.next();
                int count = resultSet.getInt(1);
                if (count == 0) {
                    JOptionPane.showMessageDialog(null, "Student with this ID does not exist.", "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sql = "UPDATE students SET name = ?, major = ? WHERE id = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, major);
                preparedStatement.setInt(3, studentId);
                preparedStatement.executeUpdate();

                JOptionPane.showMessageDialog(null, "Student updated successfully.");
                clearInputFields();
                viewStudents(); // Refresh the table
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid ID. Please enter a number.", "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error updating student: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteStudent() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String id = idField.getText();

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter the ID of the student to delete.", "Input Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int studentId = Integer.parseInt(id);

                // Check if the student exists
                String checkSql = "SELECT COUNT(*) FROM students WHERE id = ?";
                PreparedStatement checkStatement = connection.prepareStatement(checkSql);
                checkStatement.setInt(1, studentId);
                ResultSet resultSet = checkStatement.executeQuery();
                resultSet.next();
                int count = resultSet.getInt(1);
                if (count == 0) {
                    JOptionPane.showMessageDialog(null, "Student with this ID does not exist.", "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String sql = "DELETE FROM students WHERE id = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, studentId);
                preparedStatement.executeUpdate();

                JOptionPane.showMessageDialog(null, "Student deleted successfully.");
                clearInputFields();
                viewStudents(); // Refresh the table
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid ID. Please enter a number.", "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error deleting student: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearInputFields() {
        idField.setText("");
        nameField.setText("");
        majorField.setText("");
    }
}  