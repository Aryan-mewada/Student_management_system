package student_management_system;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UI {
    // DB config
    static final String DB_URL = "jdbc:mysql://localhost:3306/student_ms?useSSL=false&serverTimezone=UTC";
    static final String DB_USER = "root";
    static final String DB_PASS = "Aryan@123";

    public static void main(String[] args) {
        // load driver (included as requested)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "MySQL Driver not found. Add connector jar.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Student Management System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 500);
            frame.setLayout(new BorderLayout());

            JTabbedPane tabs = new JTabbedPane();

            // Add student tab
            JPanel addPanel = new JPanel(new GridLayout(5, 2, 8, 8));
            addPanel.setBorder(BorderFactory.createEmptyBorder(20, 80, 20, 80));

            JLabel lblId = new JLabel("Student ID:");
            JTextField txtId = new JTextField();

            JLabel lblName = new JLabel("Name:");
            JTextField txtName = new JTextField();

            JLabel lblCourse = new JLabel("Course:");
            JTextField txtCourse = new JTextField();

            JLabel lblGrade = new JLabel("Grade:");
            JTextField txtGrade = new JTextField();

            JButton btnAdd = new JButton("Add Student");
            JLabel lblMsg = new JLabel("", SwingConstants.CENTER);

            addPanel.add(lblId); addPanel.add(txtId);
            addPanel.add(lblName); addPanel.add(txtName);
            addPanel.add(lblCourse); addPanel.add(txtCourse);
            addPanel.add(lblGrade); addPanel.add(txtGrade);
            addPanel.add(btnAdd); addPanel.add(lblMsg);

            // View / manage tab
            JPanel viewPanel = new JPanel(new BorderLayout());

            String[] cols = {"ID", "Name", "Course", "Grade"};
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };
            JTable table = new JTable(model);
            JScrollPane sp = new JScrollPane(table);
            viewPanel.add(sp, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.add(new JLabel("Search by ID:"));
            JTextField txtSearch = new JTextField(8);
            JButton btnSearch = new JButton("Search");
            JButton btnUpdate = new JButton("Update");
            JButton btnDelete = new JButton("Delete");

            bottom.add(txtSearch);
            bottom.add(btnSearch);
            bottom.add(btnUpdate);
            bottom.add(btnDelete);
            viewPanel.add(bottom, BorderLayout.SOUTH);

            tabs.add("Add Student", addPanel);
            tabs.add("View / Manage", viewPanel);

            frame.add(tabs, BorderLayout.CENTER);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // load data at start
            loadStudents(model);

            // Add action
            btnAdd.addActionListener(e -> {
                String idText = txtId.getText().trim();
                String name = txtName.getText().trim();
                String course = txtCourse.getText().trim();
                String grade = txtGrade.getText().trim();

                if (idText.isEmpty() || name.isEmpty() || course.isEmpty() || grade.isEmpty()) {
                    lblMsg.setText("Please fill all fields");
                    return;
                }

                int id;
                try { id = Integer.parseInt(idText); }
                catch (NumberFormatException ex) {
                    lblMsg.setText("ID must be number");
                    return;
                }

                if (addStudent(id, name, course, grade)) {
                    model.addRow(new Object[]{id, name, course, grade});
                    lblMsg.setText("Student added");
                    txtId.setText(""); txtName.setText(""); txtCourse.setText(""); txtGrade.setText("");
                } else {
                    lblMsg.setText("Add failed (maybe duplicate ID)");
                }
            });

            // Search action
            btnSearch.addActionListener(e -> {
                String s = txtSearch.getText().trim();
                if (s.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Enter ID to search");
                    return;
                }
                int id;
                try { id = Integer.parseInt(s); }
                catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "ID must be number");
                    return;
                }
                Student st = findStudent(id);
                if (st != null) {
                    // select row in table if present
                    for (int i = 0; i < model.getRowCount(); i++) {
                        if (Integer.parseInt(model.getValueAt(i, 0).toString()) == id) {
                            table.setRowSelectionInterval(i, i);
                            break;
                        }
                    }
                    JOptionPane.showMessageDialog(frame, "Found: " + st.name + " | " + st.course + " | " + st.grade);
                } else {
                    JOptionPane.showMessageDialog(frame, "Student not found");
                }
            });

            // Update action
            btnUpdate.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(frame, "Select a row to update");
                    return;
                }
                int id = Integer.parseInt(model.getValueAt(row, 0).toString());
                String curName = model.getValueAt(row, 1).toString();
                String curCourse = model.getValueAt(row, 2).toString();
                String curGrade = model.getValueAt(row, 3).toString();

                String newName = JOptionPane.showInputDialog(frame, "Name:", curName);
                String newCourse = JOptionPane.showInputDialog(frame, "Course:", curCourse);
                String newGrade = JOptionPane.showInputDialog(frame, "Grade:", curGrade);

                if (newName != null && newCourse != null && newGrade != null) {
                    if (updateStudent(id, newName.trim(), newCourse.trim(), newGrade.trim())) {
                        model.setValueAt(newName, row, 1);
                        model.setValueAt(newCourse, row, 2);
                        model.setValueAt(newGrade, row, 3);
                        JOptionPane.showMessageDialog(frame, "Updated");
                    } else {
                        JOptionPane.showMessageDialog(frame, "Update failed");
                    }
                }
            });

            // Delete action
            btnDelete.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(frame, "Select a row to delete");
                    return;
                }
                int id = Integer.parseInt(model.getValueAt(row, 0).toString());
                int c = JOptionPane.showConfirmDialog(frame, "Delete this record?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (c == JOptionPane.YES_OPTION) {
                    if (deleteStudent(id)) {
                        model.removeRow(row);
                        JOptionPane.showMessageDialog(frame, "Deleted");
                    } else {
                        JOptionPane.showMessageDialog(frame, "Delete failed");
                    }
                }
            });
        });
    }

    // simple student class
    static class Student {
        int id;
        String name, course, grade;
        Student(int id, String name, String course, String grade) {
            this.id = id; this.name = name; this.course = course; this.grade = grade;
        }
    }

    // DB helpers
    static void loadStudents(DefaultTableModel model) {
        model.setRowCount(0);
        String sql = "SELECT id, name, course, grade FROM students";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("course"),
                        rs.getString("grade")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading data: " + e.getMessage());
        }
    }

    static boolean addStudent(int id, String name, String course, String grade) {
        String sql = "INSERT INTO students (id, name, course, grade) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, course);
            ps.setString(4, grade);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Add error: " + e.getMessage());
            return false;
        }
    }

    static Student findStudent(int id) {
        String sql = "SELECT id, name, course, grade FROM students WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Student(rs.getInt("id"), rs.getString("name"),
                            rs.getString("course"), rs.getString("grade"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Find error: " + e.getMessage());
        }
        return null;
    }

    static boolean updateStudent(int id, String name, String course, String grade) {
        String sql = "UPDATE students SET name = ?, course = ?, grade = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, course);
            ps.setString(3, grade);
            ps.setInt(4, id);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException e) {
            System.err.println("Update error: " + e.getMessage());
            return false;
        }
    }

    static boolean deleteStudent(int id) {
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException e) {
            System.err.println("Delete error: " + e.getMessage());
            return false;
        }
    }
}
