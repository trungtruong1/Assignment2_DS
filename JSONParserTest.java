import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class JSONParserTest {
    private static final String testFilePath = "data/testJSONParserInput.txt";

    @BeforeEach
    void setUp() throws IOException {
        File file = new File(testFilePath);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            if (!result) {
                throw new IOException("Could not create file " + testFilePath);
            }
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        File file = new File(testFilePath);
        if (file.exists()) {
            boolean result = file.delete();
            if (!result) {
                throw new IOException("Could not delete file " + testFilePath);
            }
        }
    }

    @Test
    void testConvertFileToJson_ValidFile() throws IOException {
        setupDatabase("id: 123\n" +
                "temperature: 25.0\n" +
                "humidity: 60\n");
        String jsonString = JSONParser.convertFileToJson(testFilePath);

        assertNotNull(jsonString, "The JSON string should not be null.");
        assertTrue(jsonString.contains("\"id\": 123"), "JSON should contain the id field.");
        assertTrue(jsonString.contains("\"temperature\": 25.0"), "JSON should contain the temperature field.");
        assertTrue(jsonString.contains("\"humidity\": 60"), "JSON should contain the humidity field.");
    }

    @Test
    void testConvertFileToJson_InvalidFile_NoIDField() throws IOException {
        setupDatabase("temperature: 25.0\n" +
                "humidity: 60\n");
        String jsonString = JSONParser.convertFileToJson(testFilePath);

        assertNull(jsonString, "The JSON string should be null due to missing id field.");
    }

    @Test
    void testIsValidJson_ValidJson() {
        String validJson = "{ \"id\": \"123\", \"temperature\": 25.0, \"humidity\": 60 }";
        boolean isValid = JSONParser.isValidJson(validJson);

        assertTrue(isValid, "The JSON string should be valid.");
    }

    @Test
    void testIsValidJson_InvalidJson_MissingBraces() {
        String invalidJson = "\"id\": \"123\", \"temperature\": 25.0, \"humidity\": 60";
        boolean isValid = JSONParser.isValidJson(invalidJson);

        assertFalse(isValid, "The JSON string should be invalid due to missing braces.");
    }

    @Test
    void testConvertToSingleLineJson() {
        String prettyJson = "{\n  \"id\": \"123\",\n  \"temperature\": 25.0,\n  \"humidity\": 60\n}";
        String singleLineJson = JSONParser.convertToSingleLineJson(prettyJson);

        assertEquals("{ \"id\": \"123\", \"temperature\": 25.0, \"humidity\": 60 }", singleLineJson, "The JSON should be converted to a single line.");
    }

    @Test
    void testConvertFileToJson_FileDoesNotExist() throws IOException {
        String nonExistentFilePath = "data/nonExistentFile.txt";
        String jsonString = JSONParser.convertFileToJson(nonExistentFilePath);

        assertNull(jsonString, "The JSON string should be null because the file does not exist.");
    }

    private void setupDatabase(String content) throws IOException {
        File file = new File(testFilePath);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
