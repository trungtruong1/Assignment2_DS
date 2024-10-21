import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Custom JSONParser to handle some JSON data.
 */
public class JSONParser {

    /**
     * Converts a plain text file into JSON format. Reject JSON object with no ID field.
     *
     * @param filePath The path of the file to be converted to JSON.
     * @return A string containing the formatted JSON data, or null if the file does not exist or is invalid.
     * @throws IOException If there is an error reading the file.
     */
    public static String convertFileToJson(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.err.println("File does not exist: " + filePath);
            return null;
        }

        StringBuilder jsonData = new StringBuilder();
        jsonData.append("{\n");
        boolean hasID = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if (key.equalsIgnoreCase("id")) {
                        hasID = true;
                    }

                    jsonData.append("    \"").append(key).append("\": ");
                    if (isNumber(value)) { //not add quotes to numeric values.
                        jsonData.append(value);
                    } else {
                        jsonData.append("\"").append(value).append("\"");
                    }

                    if (reader.ready()) {
                        jsonData.append(",\n");
                    }
                } else {
                    jsonData.append(line).append("\n");
                    System.err.println("Invalid line format: " + line);
                }
            }
        }

        jsonData.append("\n}");

        if (!hasID) {
            System.err.println("Feed is invalid: No id field found.");
            return null;
        }
        return jsonData.toString().trim();
    }

    /**
     * Validates if the given string is a well-formed JSON.
     *
     * @param stringCheck The string to be validated as JSON.
     * @return True if the string is valid JSON, false otherwise.
     */
    public static boolean isValidJson(String stringCheck) {
        if (!stringCheck.startsWith("{") || !stringCheck.endsWith("}")) {
            return false;
        }
        // Remove the outer braces
        stringCheck = stringCheck.substring(1, stringCheck.length() - 1).trim();
        // Split the JSON string by commas to separate key-value pairs
        String[] pairs = stringCheck.split(",");
        for (String pair : pairs) {
            pair = pair.trim();
            if (!pair.contains(":")) { // Check if there's a valid key-value pair with a colon
                return false;
            }
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length != 2) { // Invalid key-value pair
                return false;
            }
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();
            if (!key.startsWith("\"") || !key.endsWith("\"")) { // Check if the key is valid (wrapped in double quotes)
                return false;
            }
            // Check if the value is valid (string, number, object, boolean, null)
            if (!(isNumber(value) || value.startsWith("\"") && value.endsWith("\"") ||
                    value.equals("true") || value.equals("false") || value.equals("null") ||
                    (value.startsWith("{") && value.endsWith("}")) ||
                    (value.startsWith("[") && value.endsWith("]")))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method to check if a string is numeric.
     *
     * @param value The string to be checked.
     * @return True if the string is a valid number, false otherwise.
     */
    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Converts a normal JSON string to a single-line JSON string.
     *
     * @param json The JSON string to be converted to a single line.
     * @return A single-line JSON string.
     */
    public static String convertToSingleLineJson(String json) {
        return json.replaceAll("\\s+", " ");
    }
}
