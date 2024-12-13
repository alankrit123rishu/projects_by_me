package org.example;

import org.apache.commons.math3.linear.RealVector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

public class MainApp {
    public static void main(String[] args) {
        // Create the main frame
        JFrame frame = new JFrame("Diet Recommendation System");
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Create input panel
        JPanel inputPanel = new JPanel(new GridLayout(10, 2));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Enter Your Details"));

        // Create fields for user inputs
        JTextField nameField = new JTextField();
        JTextField ageField = new JTextField();
        JTextField heightField = new JTextField();
        JTextField weightField = new JTextField();
        JComboBox<String> dietTypeBox = new JComboBox<>(new String[]{"Vegetarian", "Non-Vegetarian", "Vegan"});
        JComboBox<String> fitnessGoalBox = new JComboBox<>(new String[]{"Weight Loss", "Muscle Gain"});
        JComboBox<String> activityLevelBox = new JComboBox<>(new String[]{
                "Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Athletes/Bodybuilders"});
        JComboBox<String> genderBox = new JComboBox<>(new String[]{"Male", "Female"});

        // Add components to the panel
        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Age:"));
        inputPanel.add(ageField);
        inputPanel.add(new JLabel("Height (cm):"));
        inputPanel.add(heightField);
        inputPanel.add(new JLabel("Weight (kg):"));
        inputPanel.add(weightField);
        inputPanel.add(new JLabel("Diet Type:"));
        inputPanel.add(dietTypeBox);
        inputPanel.add(new JLabel("Fitness Goal:"));
        inputPanel.add(fitnessGoalBox);
        inputPanel.add(new JLabel("Activity Level:"));
        inputPanel.add(activityLevelBox);
        inputPanel.add(new JLabel("Gender:"));
        inputPanel.add(genderBox);

        // Output panel
        JTextArea outputArea = new JTextArea(10, 30);
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBorder(BorderFactory.createTitledBorder("Recommendations"));

        // Submit button
        JButton submitButton = new JButton("Get Recommendations");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Collect user inputs
                try {
                    String name = nameField.getText();
                    int age = Integer.parseInt(ageField.getText());
                    double height = Double.parseDouble(heightField.getText());
                    double weight = Double.parseDouble(weightField.getText());
                    String dietType = (String) dietTypeBox.getSelectedItem();
                    String fitnessGoal = (String) fitnessGoalBox.getSelectedItem();
                    String activityLevel = (String) activityLevelBox.getSelectedItem();
                    String gender = (String) genderBox.getSelectedItem();

                    // Map activity levels to numeric values
                    int activityValue = switch (activityLevel) {
                        case "Sedentary" -> 1;
                        case "Lightly Active" -> 2;
                        case "Moderately Active" -> 3;
                        case "Very Active" -> 4;
                        case "Athletes/Bodybuilders" -> 5;
                        default -> 1;
                    };

                    int genderValue = gender.equalsIgnoreCase("Male") ? 1 : 2;

                    // Call the recommendation logic
                    List<Map.Entry<RealVector, org.bson.Document>> recommendations =
                            DietRecommendationSystemmodify1.getRecommendations(
                                    age, height, weight, dietType, fitnessGoal, activityValue, genderValue);

                    // Display results
                    outputArea.setText("");
                    if (!recommendations.isEmpty()) {
                        for (Map.Entry<RealVector, org.bson.Document> entry : recommendations) {
                            org.bson.Document diet = entry.getValue();
                            outputArea.append("Name: " + diet.getString("Recipe Name") + "\n");
                            outputArea.append("Type: " + diet.getString("Diet Type") + "\n");
                            outputArea.append("Protein: " + diet.get("Protein (g/100g)") + "g\n");
                            outputArea.append("Calories: " + diet.get("Calories (per 100g)") + "kcal\n");
                            outputArea.append("-----------------------------------\n");
                        }
                    } else {
                        outputArea.append("No suitable diet found!\n");
                    }
                } catch (Exception ex) {
                    outputArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        // Add components to the frame
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(submitButton, BorderLayout.CENTER);
        frame.add(outputScrollPane, BorderLayout.SOUTH);

        // Display the frame
        frame.setVisible(true);
    }
}

