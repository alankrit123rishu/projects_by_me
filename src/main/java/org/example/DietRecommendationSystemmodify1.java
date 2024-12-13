package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

public class DietRecommendationSystemmodify1 {

    private static String uri = "mongodb://localhost:27017";

    // Method to normalize a value using min-max normalization
    public static double minmaxNormalization(double value, String field, MongoCollection<Document> collection) {
        Document maxDoc = collection.find().sort(Sorts.descending(field)).first();
        Document minDoc = collection.find().sort(Sorts.ascending(field)).first();

        if (maxDoc != null && minDoc != null) {
            double max = convertToDouble(maxDoc.get(field));
            double min = convertToDouble(minDoc.get(field));

            return (value - min) / (max - min);
        }
        return 0; // Default to 0 if no data is found
    }

    // Helper method to handle Integer and Double types
    public static double convertToDouble(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Double) {
            return (Double) value;
        } else {
            throw new IllegalArgumentException("Unsupported data type for normalization: " + value.getClass().getName());
        }
    }

    // Method to calculate cosine similarity between two vectors
    public static double cosineSimilarity(RealVector v1, RealVector v2) {
        return v1.dotProduct(v2) / (v1.getNorm() * v2.getNorm());
    }

    // Method to one-hot encode the dietary preference
    public static double[] oneHotEncode(String category) {
        double[] encoded = new double[3]; // Assuming three categories: Vegetarian, Non-Vegetarian, Vegan
        switch (category.toLowerCase()) {
            case "vegetarian":
                encoded[0] = 1;
                encoded[1] = 0;
                encoded[2] = 0;
                break;
            case "non-vegetarian":
                encoded[0] = 0;
                encoded[1] = 1;
                encoded[2] = 0;
                break;
            case "vegan":
                encoded[0] = 0;
                encoded[1] = 0;
                encoded[2] = 1;
                break;
            default:
                throw new IllegalArgumentException("Unknown category: " + category);
        }
        return encoded;
    }

    // Method to get recommendations based on user input
    public static List<Map.Entry<RealVector, Document>> getRecommendations(
            int age, double height, double weight, String dietType, String fitnessGoal,
            int activityLevel, int gender) {

        double BMR = gender == 1
                ? 10 * weight + 6.25 * height - 5 * age + 5
                : 10 * weight + 6.25 * height - 5 * age - 161;

        double activityMultiplier;
        switch (activityLevel) {
            case 1:
                activityMultiplier = 1.2;
                break;
            case 2:
                activityMultiplier = 1.375;
                break;
            case 3:
                activityMultiplier = 1.55;
                break;
            case 4:
                activityMultiplier = 1.725;
                break;
            case 5:
                activityMultiplier = 1.9;
                break;
            default:
                throw new IllegalArgumentException("Invalid activity level");
        }

        double finalCalories = BMR * activityMultiplier;
        if (fitnessGoal.equalsIgnoreCase("Muscle Gain")) {
            finalCalories += 200;
        } else if (fitnessGoal.equalsIgnoreCase("Weight Loss")) {
            finalCalories -= 200;
        }

        double proteinFactor = (activityLevel == 1) ? 0.8 : 1.2;
        double proteinIntake = weight * proteinFactor;

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("dietmongodemo");
            MongoCollection<Document> dietCollection = database.getCollection("dietdemo");

            double[] userVectorArray = new double[5];
            System.arraycopy(oneHotEncode(dietType), 0, userVectorArray, 0, 3);
            userVectorArray[3] = minmaxNormalization(proteinIntake, "Protein (g/100g)", dietCollection);
            userVectorArray[4] = minmaxNormalization(finalCalories, "Calories (per 100g)", dietCollection);
            RealVector userVector = new ArrayRealVector(userVectorArray);

            List<Document> diets = dietCollection.find().into(new ArrayList<>());
            List<RealVector> dietVectors = diets.stream().map(diet -> {
                double[] vector = new double[5];
                System.arraycopy(oneHotEncode(diet.getString("Diet Type")), 0, vector, 0, 3);

                vector[3] = minmaxNormalization(convertToDouble(diet.get("Protein (g/100g)")), "Protein (g/100g)", dietCollection);
                vector[4] = minmaxNormalization(convertToDouble(diet.get("Calories (per 100g)")), "Calories (per 100g)", dietCollection);

                return new ArrayRealVector(vector);
            }).collect(Collectors.toList());

            List<Map.Entry<RealVector, Document>> bestMatches = dietVectors.stream()
                    .map(vector -> Map.entry(vector, diets.get(dietVectors.indexOf(vector))))
                    .sorted(Comparator.comparingDouble(entry -> -cosineSimilarity(userVector, entry.getKey())))
                    .filter(entry -> cosineSimilarity(userVector, entry.getKey()) > 0.5)
                    .limit(5)
                    .collect(Collectors.toList());

            return bestMatches;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
