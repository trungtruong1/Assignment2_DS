import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DataFileHandler, provides methods to handle file data.
 */
public class DataFileHandler {

    /**
     * Saves the provided JSON data to the storage file. The method writes the data to a temporary file
     * first. If the write operation to the temp file is successful, the content is then moved
     * from the temporary file to the actual storage file. This approach prevents data
     * loss.
     *
     * @param json The JSON data to be saved. It should not be null.
     * @param dataFilePath The path to the file where the data should be saved.
     *                     The file will be overwritten if it already exists.
     */
    static void saveDataToFile(String json, String dataFilePath) {
        String tmpFilePath = dataFilePath + ".tmp"; // Temporary file path

        // Write to the temporary file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFilePath))) {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            long threadId = Thread.currentThread().getId();
            String dataWithTimestampAndThreadId = timeStamp + " [Thread ID: " + threadId + "] " + json;
            writer.write(dataWithTimestampAndThreadId);
        } catch (IOException e) {
            System.err.println("Error saving data to temporary file: " + e.getMessage());
            return; // Exit if unable to save to the temporary file
        }

        //Copy content from temporary file to the actual data file
        try {
            File dataFile = new File(dataFilePath);

            // Copy the content from the temporary file to the actual data file
            try (BufferedReader reader = new BufferedReader(new FileReader(tmpFilePath));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile, true))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error copying content from temporary file to data file: " + e.getMessage());
        }

        sortFileByTimestamp(dataFilePath);

        //Remove the temporary file
        File tmpFile = new File(tmpFilePath);
        if (!tmpFile.delete()) {
            System.err.println("Failed to delete temporary file after copying.");
        }
    }

    /**
     * Sorts the lines in a file by the timestamp (latest entries are at the top of the file).
     *
     * @param dataFilePath The path of the file to be sorted.
     */
    public static void sortFileByTimestamp(String dataFilePath) {
        List<String> lines = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line); // Add each line to the list
            }
        } catch (IOException e) {
            System.err.println("Error reading data from file: " + e.getMessage());
            return;
        }
        // Sort lines by the timestamp in descending order
        lines.sort((line1, line2) -> {
            try {
                // Extract the timestamp part of each line and parse it
                Date date1 = format.parse(line1.substring(0, 19));
                Date date2 = format.parse(line2.substring(0, 19));
                return date2.compareTo(date1);
            } catch (ParseException e) {
                throw new RuntimeException("Error parsing timestamp: " + e.getMessage());
            }
        });

        // Write the sorted data back to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFilePath))) {
            for (String sortedData : lines) {
                writer.write(sortedData);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing sorted data to file: " + e.getMessage());
        }
    }

    /**
     * Retains only the top 20 entries in the file.
     *
     * @param dataFilePath The path of the file to be truncated.
     */
    public static void keepTop20Entries(String dataFilePath) {
        List<String> top20Entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFilePath))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 20) {
                top20Entries.add(line);
                count++;
            }
        } catch (IOException e) {
            System.err.println("Error reading data from file: " + e.getMessage());
            return;
        }
        // Write the 20 entries back to the same file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFilePath))) {
            for (String topEntry : top20Entries) {
                writer.write(topEntry);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing data to file: " + e.getMessage());
        }
    }
}
