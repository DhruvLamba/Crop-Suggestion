import static spark.Spark.*;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CropSuggestion {

    public static void main(String[] args) {
        exception(Exception.class, (e, request, response) -> {
            response.status(500);
            response.body("Internal Server Error: " + e.getMessage());
        });
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));
        post("/suggestCrops", (req, res) -> {
            try (Connection dbConnection = getConnection()) {

                String soilType = req.queryParams("soilType");
                float minPh = Float.parseFloat(req.queryParams("minPh"));
                float maxPh = Float.parseFloat(req.queryParams("maxPh"));
                float minTemperature = Float.parseFloat(req.queryParams("minTemperature"));
                float maxTemperature = Float.parseFloat(req.queryParams("maxTemperature"));
                String season = req.queryParams("season");
                float minMoistureLevel = Float.parseFloat(req.queryParams("minMoistureLevel"));
                float maxMoistureLevel = Float.parseFloat(req.queryParams("maxMoistureLevel"));
                float minSalinityLevel = Float.parseFloat(req.queryParams("minSalinityLevel"));
                float maxSalinityLevel = Float.parseFloat(req.queryParams("maxSalinityLevel"));
                DecisionTree decisionTree = new DecisionTree(buildDecisionTree());
                List<String> suggestedCrops = makeSuggestions(decisionTree, dbConnection, soilType, minPh, maxPh,
                        minTemperature, maxTemperature, season, minMoistureLevel, maxMoistureLevel, minSalinityLevel,
                        maxSalinityLevel);

                StringBuilder jsonResponse = new StringBuilder("{");
                jsonResponse.append("\"suggestedCrops\": [");

                // Append crops to the response
                for (int i = 0; i < suggestedCrops.size(); i++) {
                    jsonResponse.append("\"").append(suggestedCrops.get(i)).append("\"");
                    if (i < suggestedCrops.size() - 1) {
                        jsonResponse.append(",");
                    }
                }

                jsonResponse.append("]}");


                res.type("application/json");
                return jsonResponse.toString();





            } catch (NumberFormatException e) {
                e.printStackTrace();
                res.status(400);
                return "Invalid input parameters: " + e.getMessage();
            } catch (SQLException e) {
                e.printStackTrace();
                res.status(500);
                return "Database error: " + e.getMessage();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                res.status(500);
                return "Class not found: " + e.getMessage();
            }  catch (NullPointerException e) {
                e.printStackTrace();
                res.status(500);
                return "Unexpected null reference: " + e.getMessage();
            }   catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "An unexpected error occurred: " + e.getMessage();
            }

        });
    }

    private static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/dataset";
        String user = "root";
        String password = "";
        return DriverManager.getConnection(url, user, password);
    }

    private static DecisionNode buildDecisionTree() {
        DecisionNode root = new DecisionNode("Is it suitable for cultivation?");
        DecisionNode soilNode = new DecisionNode("Is the soil type suitable?");
        DecisionNode phNode = new DecisionNode("Is pH level within the acceptable range?");
        DecisionNode temperatureNode = new DecisionNode("Is temperature within the acceptable range?");
        DecisionNode seasonNode = new DecisionNode("Is the season suitable?");
        DecisionNode moistureNode = new DecisionNode("Is moisture level within the acceptable range?");
        DecisionNode salinityNode = new DecisionNode("Is salinity level within the acceptable range?");
        DecisionNode suggestionNode = new DecisionNode("Suggested Crops");

        root.setYesNode(soilNode);
        root.setNoNode(suggestionNode);

        soilNode.setYesNode(phNode);
        soilNode.setNoNode(suggestionNode);

        phNode.setYesNode(temperatureNode);
        phNode.setNoNode(suggestionNode);

        temperatureNode.setYesNode(seasonNode);
        temperatureNode.setNoNode(suggestionNode);

        seasonNode.setYesNode(moistureNode);
        seasonNode.setNoNode(suggestionNode);

        moistureNode.setYesNode(salinityNode);
        moistureNode.setNoNode(suggestionNode);

        salinityNode.setYesNode(suggestionNode);
        salinityNode.setNoNode(suggestionNode);

        return root;
    }

    private static List<String> makeSuggestions(DecisionTree decisionTree, Connection connection, String soilType,
                                                float minPh, float maxPh, float minTemperature, float maxTemperature, String season,
                                                float minMoistureLevel, float maxMoistureLevel, float minSalinityLevel, float maxSalinityLevel)
            throws SQLException, ClassNotFoundException {
        List<String> crops = fetchCropsFromDatabase(connection, minPh, maxPh, minTemperature, maxTemperature, season,
                minMoistureLevel, maxMoistureLevel, minSalinityLevel, maxSalinityLevel);

        return decisionTree.traverse(soilType, minPh, maxPh, minTemperature, maxTemperature, season, minMoistureLevel,
                maxMoistureLevel, minSalinityLevel, maxSalinityLevel, crops);
    }

    private static List<String> fetchCropsFromDatabase(Connection connection, float minPh, float maxPh,
                                                       float minTemperature, float maxTemperature, String season, float minMoistureLevel, float maxMoistureLevel,
                                                       float minSalinityLevel, float maxSalinityLevel) throws SQLException {
        List<String> crops = new ArrayList<>();
        String sql = "SELECT CROP FROM fset " +
                "WHERE SOIL = ? AND PH_LOW <= ? AND PH_HIGH >= ? " +
                "AND TEMPERATURE_LOW <= ? AND TEMPERATURE_HIGH >= ? " +
                "AND SEASON = ? " +
                "AND MOISTURE_LOW <= ? AND MOISTURE_HIGH >= ? " +
                "AND SALINITY_LOW <= ? AND SALINITY_HIGH >= ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "Loamy Soil"); // Set the soil type dynamically
            statement.setFloat(2, maxPh);
            statement.setFloat(3, minPh);
            statement.setFloat(4, maxTemperature);
            statement.setFloat(5, minTemperature);
            statement.setString(6, season);
            statement.setFloat(7, maxMoistureLevel);
            statement.setFloat(8, minMoistureLevel);
            statement.setFloat(9, maxSalinityLevel);
            statement.setFloat(10, minSalinityLevel);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    crops.add(resultSet.getString("CROP"));
                }
            }
        }

        return crops;
    }

    private static float readFloat(Scanner scanner) {
        while (true) {
            try {
                return Float.parseFloat(scanner.nextLine());
            } catch (NumberFormatException ex) {
                System.out.print("Invalid input. Please enter a valid number: ");
            }
        }
    }

    static class DecisionNode {
        private String question;
        private DecisionNode yesNode;
        private DecisionNode noNode;

        public DecisionNode(String question) {
            this.question = question;
        }

        public String getQuestion() {
            return question;
        }

        public DecisionNode getYesNode() {
            return yesNode;
        }

        public void setYesNode(DecisionNode yesNode) {
            this.yesNode = yesNode;
        }

        public DecisionNode getNoNode() {
            return noNode;
        }

        public void setNoNode(DecisionNode noNode) {
            this.noNode = noNode;
        }
    }

    static class DecisionTree {
        private DecisionNode root;

        public DecisionTree(DecisionNode root) {

            this.root = root;
        }

        public List<String> traverse(String soilType, float minPh, float maxPh, float minTemperature,
                                     float maxTemperature, String season, float minMoistureLevel, float maxMoistureLevel,
                                     float minSalinityLevel, float maxSalinityLevel, List<String> crops) {
            List<String> suggestions = new ArrayList<>();
            traverse(root, soilType, minPh, maxPh, minTemperature, maxTemperature, season, minMoistureLevel,
                    maxMoistureLevel, minSalinityLevel, maxSalinityLevel, crops, suggestions);
            return suggestions;
        }

        private void traverse(DecisionNode node, String soilType, float minPh, float maxPh, float minTemperature,
                              float maxTemperature, String season, float minMoistureLevel, float maxMoistureLevel,
                              float minSalinityLevel, float maxSalinityLevel, List<String> crops, List<String> suggestions) {
            if (node != null) {
                if (node.getQuestion().equals("Suggested Crops")) {
                    addCropsToSuggestions(crops, suggestions);
                } else {
                    if (evaluateCondition(node, soilType, minPh, maxPh, minTemperature, maxTemperature, season,
                            minMoistureLevel, maxMoistureLevel, minSalinityLevel, maxSalinityLevel)) {
                        traverse(node.getYesNode(), soilType, minPh, maxPh, minTemperature, maxTemperature, season,
                                minMoistureLevel, maxMoistureLevel, minSalinityLevel, maxSalinityLevel, crops,
                                suggestions);
                    } else {
                        traverse(node.getNoNode(), soilType, minPh, maxPh, minTemperature, maxTemperature, season,
                                minMoistureLevel, maxMoistureLevel, minSalinityLevel, maxSalinityLevel, crops,
                                suggestions);
                    }
                }
            }
        }

        private boolean evaluateCondition(DecisionNode node, String soilType, float minPh, float maxPh,
                                          float minTemperature, float maxTemperature, String season, float minMoistureLevel,
                                          float maxMoistureLevel, float minSalinityLevel, float maxSalinityLevel) {
            String question = node.getQuestion();

            switch (question) {
                case "Is the soil type suitable?":
                    return evaluateDynamicCondition(soilType, "Loamy Soil");

                case "Is pH level within the acceptable range?":
                    System.out.print("Enter pH level (2-10): ");
                    float userPhLevel = readFloat(new Scanner(System.in));
                    return evaluateRangeCondition(userPhLevel, 2.0f, 10.0f);

                case "Is temperature within the acceptable range?":
                    System.out.print("Enter temperature (45-99): ");
                    float userTemperature = readFloat(new Scanner(System.in));
                    return evaluateRangeCondition(userTemperature, 45.0f, 99.0f);

                case "Is the season suitable?":
                    return evaluateDynamicCondition(season, "Rabi");

                case "Is moisture level within the acceptable range?":
                    System.out.print("Enter moisture level (40-100): ");
                    float userMoistureLevel = readFloat(new Scanner(System.in));
                    return evaluateRangeCondition(userMoistureLevel, 40.0f, 100.0f);

                case "Is salinity level within the acceptable range?":
                    System.out.print("Enter salinity level (0-6): ");
                    float userSalinityLevel = readFloat(new Scanner(System.in));
                    return evaluateRangeCondition(userSalinityLevel, 0.0f, 6.0f);

                default:
                    return false;
            }
        }

        private boolean evaluateRangeCondition(float userValue, float minValue, float maxValue) {
            return userValue >= minValue && userValue <= maxValue;
        }

        private boolean evaluateDynamicCondition(String actualValue, String expectedValue) {
            return actualValue.equalsIgnoreCase(expectedValue);
        }

        private void addCropsToSuggestions(List<String> crops, List<String> suggestions) {
            for (String crop : crops) {
                suggestions.add(crop);
            }
        }
    }
}