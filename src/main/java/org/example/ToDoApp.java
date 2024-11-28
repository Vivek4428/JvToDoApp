package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import com.mongodb.client.MongoCollection;

import java.util.ArrayList;
import java.util.List;

public class ToDoApp extends JFrame {
    private JTextField taskField;
    private JButton addButton;
    private JTextArea taskDisplayArea;
    private JTextField actionField;
    private JButton completeButton;
    private JButton deleteButton;
    private MongoDatabase database;

    public ToDoApp(MongoDatabase db) {
        this.database = db; // Initialize the database connection
        setTitle("To-Do List App");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create UI components
        taskField = new JTextField(20);
        addButton = new JButton("Add Task");
        taskDisplayArea = new JTextArea();
        taskDisplayArea.setEditable(false); // Prevent user editing

        actionField = new JTextField(5); // For entering task index
        completeButton = new JButton("Complete Task");
        deleteButton = new JButton("Delete Task");

        // Create input panel and add components
        JPanel inputPanel = new JPanel();
        inputPanel.add(taskField);
        inputPanel.add(addButton);

        // Create action panel for task completion and deletion
        JPanel actionPanel = new JPanel();
        actionPanel.add(new JLabel("Task Index:"));
        actionPanel.add(actionField);
        actionPanel.add(completeButton);
        actionPanel.add(deleteButton);

        // Add panels and task display area to the frame
        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(taskDisplayArea), BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        // Action listener for the "Add Task" button
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String task = taskField.getText();
                if (!task.isEmpty()) {
                    addTaskToDatabase(task); // Add task to the database
                    displayTasks();         // Refresh displayed tasks
                    taskField.setText("");  // Clear input field
                } else {
                    JOptionPane.showMessageDialog(null, "Task cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Action listener for the "Complete Task" button
        completeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int index = Integer.parseInt(actionField.getText());
                    markTaskAsCompleted(index);
                    displayTasks();
                    actionField.setText(""); // Clear the input field
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Invalid index! Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Action listener for the "Delete Task" button
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int index = Integer.parseInt(actionField.getText());
                    deleteTask(index);
                    displayTasks();
                    actionField.setText(""); // Clear the input field
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Invalid index! Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Display tasks when the app starts
        displayTasks();
    }

    // Method to add a task to MongoDB
    private void addTaskToDatabase(String taskDescription) {
        try {
            MongoCollection<Document> tasks = database.getCollection("tasks");
            Document newTask = new Document("description", taskDescription)
                    .append("status", "pending"); // Default status: pending
            tasks.insertOne(newTask);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding task: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to mark a task as completed
    private void markTaskAsCompleted(int index) {
        try {
            MongoCollection<Document> tasks = database.getCollection("tasks");
            List<Document> taskList = tasks.find().into(new ArrayList<>());

            if (index <= 0 || index > taskList.size()) {
                JOptionPane.showMessageDialog(this, "Invalid task index!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Document taskToComplete = taskList.get(index - 1);
            tasks.updateOne(taskToComplete, new Document("$set", new Document("status", "completed")));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error marking task as completed: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to delete a task
    private void deleteTask(int index) {
        try {
            MongoCollection<Document> tasks = database.getCollection("tasks");
            List<Document> taskList = tasks.find().into(new ArrayList<>());

            if (index <= 0 || index > taskList.size()) {
                JOptionPane.showMessageDialog(this, "Invalid task index!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Document taskToDelete = taskList.get(index - 1);
            tasks.deleteOne(taskToDelete);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting task: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to display all tasks in the taskDisplayArea
    private void displayTasks() {
        try {
            MongoCollection<Document> tasks = database.getCollection("tasks");
            taskDisplayArea.setText(""); // Clear the text area before updating

            int index = 1;
            for (Document task : tasks.find()) {
                String taskDescription = task.getString("description");
                String taskStatus = task.getString("status");
                taskDisplayArea.append(index++ + ". " + taskDescription + " [" + taskStatus + "]\n");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error fetching tasks: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        MongoClient mongoClient = null;
        try {
            // Establish MongoDB connection
            mongoClient = MongoClients.create("mongodb+srv://vivekmon4428:Vivek1234@cluster0.jkama.mongodb.net/?retryWrites=true&w=majority");
            MongoDatabase database = mongoClient.getDatabase("ToDoApp");

            // Launch the To-Do List GUI
            SwingUtilities.invokeLater(() -> {
                ToDoApp app = new ToDoApp(database);
                app.setVisible(true);
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error connecting to MongoDB: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Ensure the MongoDB client is closed on shutdown
            MongoClient finalMongoClient = mongoClient;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (finalMongoClient != null) {
                    finalMongoClient.close();
                }
            }));
        }
    }
}
