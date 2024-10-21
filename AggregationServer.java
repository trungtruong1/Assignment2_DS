import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AggregationServer, takes in incoming socket connections and create threads to handle each connection.
 */
public class AggregationServer {
    private ServerSocket serverSocket;
    private static final String dataFilePath = "data/weather_data.txt";
    private static final LamportClock lamportClock = new LamportClock();
    private static final Map<Long, Long> socketTimestamps = new ConcurrentHashMap<>();

    /**
     * AggregationServer Constructor.
     * Starts a periodic check for inactive sockets every 5 seconds.
     */
    public AggregationServer() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        // Start the periodic check every 5 seconds
        scheduler.scheduleAtFixedRate(this::checkInactiveSockets, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Starts the aggregation server given the port number.
     * Listens for incoming GETClient and ContentServer connections and handles requests in separate threads.
     *
     * @param port The port number on which the server listens.
     */
    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Aggregation Server started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                AggregationServerThread thread = new AggregationServerThread(socket);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            System.exit(-1);
        }
    }

    /**
     * AggregationServerThread, handle each socket connection.
     */
    static class AggregationServerThread extends Thread {
        private final Socket socket;

        /**
         * Constructor for the AggregationServerThread.
         * Initializes the thread with the connected socket.
         *
         * @param socket The socket connected to the GETClient or ContentServer.
         */
        public AggregationServerThread(Socket socket) { // Constructor
            this.socket = socket;
        }

        /**
         * Handles incoming requests (GET, PUT, and HEARTBEAT) in a loop until the socket is closed.
         * Updates the Lamport clock for each message received.
         */
        public synchronized void run() {
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                while (!socket.isClosed()) { // Keep listening for incoming connections
                    String message = in.readUTF();

                    //update timestamps to know when which socket has its last interaction
                    socketTimestamps.put(this.getId(), System.currentTimeMillis());
                    //update lamportClock
                    int messageLamportValue = extractLamportClock(message);
                    lamportClock.update(messageLamportValue);

                    // Check if the message is a GET or PUT request
                    if (message.startsWith("GET")) {
                        handleGetRequest(out);
                    } else if (message.startsWith("PUT")) {
                        handlePutRequest(message, out);
                    } else if (message.startsWith("HEARTBEAT")) { //Not a Request, just receive to know that
                        // the connection between the GETClient and the AggregationServer is still existent.
                        System.out.println("Received HEARTBEAT");
                    } else {
                        String response = "HTTP/1.1 400 Bad Request\r\nInvalid request.\n";
                        out.writeUTF(response);
                        out.flush();
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling request in socket ID: " + this.getId() + " - " + e.getMessage());
            }
        }

        /**
         * Handles GET requests from the GETClient, reading weather data from the file and sending it to the client.
         *
         * @param out DataOutputStream to send the response to the GETClient.
         * @throws IOException if there is an issue with reading the data or sending the response.
         */
        void handleGetRequest(DataOutputStream out) throws IOException {
            String data = readData();
            String response;
            if (data != null) {
                response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + data.length() + "\r\n" + data + "\r\n";
            } else {
                response = "HTTP/1.1 404 Not Found\r\nNo weather data available.\n";
            }
            out.writeUTF(response);
            out.flush();
        }

        /**
         * Handles PUT requests from the ContentServer by saving and updating the weather data in the storage file.
         * Ensures the data is valid JSON and sorts the entries in the storage file by timestamp, return the latest entry.
         *
         * @param message The PUT request message containing the JSON data.
         * @param out DataOutputStream to send the response to the ContentServer.
         * @throws IOException if there is an issue with processing the data or sending the response.
         */
        void handlePutRequest(String message, DataOutputStream out) throws IOException {
            boolean createStorage = false;
            Path path = Paths.get(dataFilePath);
            boolean storageInitialized = Files.exists(path);

            if (!storageInitialized) {
                initializeFiles();
                createStorage = true;
            }
            String jsonData = extractJsonData(message);

            if (jsonData != null) {
                String response;
                if (!JSONParser.isValidJson(jsonData)) {
                    response = "HTTP/1.1 500 Invalid JSON Data\r\nThe JSON data is not valid.\n";
                    out.writeUTF(response);
                    out.flush();
                }
                String jsonObject = JSONParser.convertToSingleLineJson(jsonData);
                DataFileHandler.saveDataToFile(jsonObject,dataFilePath);
                DataFileHandler.sortFileByTimestamp(dataFilePath); //sort data entries in the storage file, latest entries are at the top
                DataFileHandler.keepTop20Entries(dataFilePath); //remove entries that are not among the 20 most recent ones
                if (createStorage) {
                    response = "HTTP/1.1 201 Created\r\nData is received and the storage file is created.\n";
                } else {
                    response = "HTTP/1.1 200\r\nData is successfully updated.\n";
                }
                out.writeUTF(response);
                out.flush();
            } else {
                String response = "HTTP/1.1 204 No Content Request\r\nNo content is sent.\n";
                out.writeUTF(response);
                out.flush();
            }
        }

        /**
         * Extracts JSON data from the request message.
         *
         * @param request The ContentServer's request containing the JSON data.
         * @return The extracted JSON data, or null if not found.
         */
        String extractJsonData(String request) {
            int jsonStartIndex = request.indexOf("{");
            if (jsonStartIndex == -1) {
                return null;
            }
            return request.substring(jsonStartIndex).trim();
        }

        /**
         * Initializes the data storage file by creating the necessary directory and file.
         *
         * @throws IOException if there is an issue while creating the directory or file.
         */
        private void initializeFiles() throws IOException {
            Files.createDirectories(Paths.get("data"));
            Files.createFile(Paths.get(dataFilePath));
        }

        /**
         * Reads a file line-by-line to find and return the first valid JSON string (latest valid JSON data in the file).
         *
         * This method looks at each line in the storage file and attempts to extract a JSON object.
         * It skips newlines and checks the extracted content for valid JSON structure.
         * The JSON is validated by the custom JSONParser.
         *
         * @return A valid JSON string from the file, or null if no valid JSON is found.
         *
         * @throws IOException If an I/O error occurs, such as if the file doesn't exist or cannot be read.
         * @throws FileNotFoundException If the file is not found at the specified path.
         */
        private String readData() throws IOException {
            Path path = Paths.get(dataFilePath);
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Data file not available at: " + dataFilePath);
            }
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String jsonData = extractJsonData(line);
                    // If valid JSON is found, return it
                    if (jsonData != null && JSONParser.isValidJson(jsonData)) {
                        return jsonData;
                    }
                }
            }
            return null;
        }

        /**
         * Extracts the Lamport clock value from the request headers.
         *
         * @param message The request message containing the Lamport clock.
         * @return The Lamport clock value or -1 if not found.
         */
        public static int extractLamportClock(String message) {
            String[] lines = message.split("\r\n");
            for (String line : lines) {
                if (line.startsWith("Lamport-Clock:")) {
                    return Integer.parseInt(line.split(":")[1].trim());
                }
            }
            return -1;
        }
    }

    /**
     * Periodically checks for inactive sockets that have not interacted within the last 30 seconds.
     * Removes inactive socket entries from memory and updates the data file.
     */
    private void checkInactiveSockets() {
        long currentTime = System.currentTimeMillis();
        List<Long> inactiveSockets = new ArrayList<>();
        socketTimestamps.entrySet().removeIf(entry -> {
            long lastInteractionTime = entry.getValue();
            // Check if the socket has not interacted for more than 30 seconds
            if (currentTime - lastInteractionTime > 30000) {
                System.out.println("Adding inactive socket: " + entry.getKey());
                inactiveSockets.add(entry.getKey()); // Add to inactive list
                return true; // Remove the socket from the map
            }
            return false; // Keep the socket in the map
        });
        try {
            removeInactiveEntriesFromFile(inactiveSockets); // Call method to remove entries of inactive sockets
        } catch (IOException e) {
            System.err.println("Error removing inactive entries from file: " + e.getMessage());
        }
    }

    /**
     *
     * Removes entries of inactive socket from storage and updates the storage file.
     *
     * @param inactiveSocketIds A list of socket IDs for which data entries need to be removed.
     *
     */
    private void removeInactiveEntriesFromFile(List<Long> inactiveSocketIds) throws IOException {
        List<String> nonExpiredEntries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                boolean remove = false;
                for (Long socketId : inactiveSocketIds) {
                    if (line.contains("[Thread ID: " + socketId + "]")) {
                        remove = true; //mark this entry as going to be removed
                        break;
                    }
                }
                if (!remove) {
                    nonExpiredEntries.add(line); //add back the non expired entries
                }
            }
        }
        //update the data file after the expired entries are removed
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFilePath))) {
            for (String entry : nonExpiredEntries) {
                writer.write(entry);
                writer.newLine();
            }
        }
        System.out.println("Removed entries for inactive sockets from the file.");
    }

    /**
     * Main method that starts the Aggregation. Listen to connections from sockets and
     * handle those connections.
     *
     * @param args Command line arguments containing the server URL and input file path.
     *
     */
    public static void main(String[] args) {
        int portNumber = 4567;
        if (args.length == 1) {
            try {
                portNumber = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number provided. Using default port " + portNumber);
            }
        }
        AggregationServer server = new AggregationServer();
        server.start(portNumber);
    }
}