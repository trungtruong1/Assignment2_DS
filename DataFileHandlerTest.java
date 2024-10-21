import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class DataFileHandlerTest {

    private static final String dataFilePath = "data/testDataFileHandler11.txt";

    @BeforeEach
    void setUp() throws IOException {
        File file = new File(dataFilePath);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            if (!result) {
                throw new IOException("Could not create file " + dataFilePath);
            }
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        File file = new File(dataFilePath);
        if (file.exists()) {
            boolean result = file.delete();
            if (!result) {
                throw new IOException("Could not delete file " + dataFilePath);
            }
        }
    }

    @Test
    void testSaveDataToFile() throws IOException {
        String jsonData = "{\"temperature\": 100}";
        DataFileHandler.saveDataToFile(jsonData, dataFilePath);
        String content = Files.readString(Path.of(dataFilePath)).trim();

        assertTrue(content.contains(jsonData), "The file should contain the provided JSON data");
        assertTrue(content.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\[Thread ID: \\d+] \\{.*}$"),
                "The data should be preceded by a timestamp and thread ID");
    }

    @Test
    void testSortFileByTimestamp() throws IOException {
        String[] testLines = {
                "2024-10-01 12:30:45 [Thread ID: 1] {\"temperature\": 100}",
                "2024-10-01 12:30:46 [Thread ID: 1] {\"temperature\": 85}",
                "2024-10-01 12:45:30 [Thread ID: 1] {\"temperature\": 80}"
        };
        Files.write(Path.of(dataFilePath), String.join("\n", testLines).getBytes());
        // Sort the file by timestamp in descending order
        DataFileHandler.sortFileByTimestamp(dataFilePath);

        // Verify the sorting
        String[] sortedLines = Files.readString(Path.of(dataFilePath)).split("\n");
        assertEquals("2024-10-01 12:45:30 [Thread ID: 1] {\"temperature\": 80}", sortedLines[0].trim());
        assertEquals("2024-10-01 12:30:46 [Thread ID: 1] {\"temperature\": 85}", sortedLines[1].trim());
        assertEquals("2024-10-01 12:30:45 [Thread ID: 1] {\"temperature\": 100}", sortedLines[2].trim());
    }

    @Test
    void testKeepTop20Entries() throws IOException, InterruptedException {
        // Prepare the file with 21 lines of data
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < 21; i++) {
            Thread.sleep(1000);
            data.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append(" [Thread ID: 1] {\"temperature\": ").append(i).append("}\n");
        }
        Files.write(Path.of(dataFilePath), data.toString().getBytes());
        // Keep only the latest 20 data entries
        DataFileHandler.keepTop20Entries(dataFilePath);

        // Verify that only the last 20 lines remain in the file
        String[] remainingLines = Files.readString(Path.of(dataFilePath)).split("\n");
        assertEquals(20, remainingLines.length, "The file should contain only the latest 20 lines");
    }
}
