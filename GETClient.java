import java.io.*;
import java.net.Socket;

/**
 * GETClient, connect to AggregationServer and get data.
 */
public class GETClient {
    private static final LamportClock lamportClock = new LamportClock();
    private static String sentRequest;
    private static boolean heartBeating;

    /**
     * Sends a GET request to the AggregationServer for weather data.
     *
     * @param out   The DataOutputStream object used to send data to the AggregationServer.
     * @param host  The hostname or IP address of the AggregationServer.
     * @param port  The port number on which the AggregationServer is listening.
     * @throws IOException if an I/O error occurs during communication.
     */
    public static void sendGetRequest(DataOutputStream out, String host, int port) throws IOException {
        String request = "GET /weather HTTP/1.1\r\n" +
                "Host: " + host + ":" + port + "\r\n" +
                "Lamport-Clock: " + lamportClock.getValue() + "\r\n";

        sentRequest = request;
        out.writeUTF(request);
        out.flush();
        lamportClock.increment();
    }

    /**
     * Returns the last sent GET request as a string.
     *
     * @return The last sent GET request.
     */
    public String getRequest() {
        return sentRequest;
    }

    /**
     * Reads the AggregationServer's response to the GET request.
     *
     * @param in The DataInputStream object used to receive data from the AggregationServer.
     * @return The JSON data from the AggregationServer's response, or null if no valid data is found.
     * @throws IOException if an I/O error occurs during reading.
     */
    public static String readServerResponse(DataInputStream in) throws IOException {
        String response = in.readUTF();
        String jsonResponse = extractJsonData(response);

        if (jsonResponse == null || jsonResponse.isEmpty()) {
            System.err.println("Error: No JSON data found in the response.");
            return null;
        }

        System.out.println("Received weather data:\n");
        String data = jsonResponse.substring(1, jsonResponse.length() - 1);
        String[] pairs = data.split(",");
        for (String pair : pairs) { //print the data to standard output
            System.out.println(pair.trim().replaceAll("\"", ""));
        }
        return jsonResponse;
    }

    /**
     * Extracts the JSON portion of the AggregationServer's response.
     *
     * @param response The full response from the AggregationServer.
     * @return The JSON data as a string, or null if no JSON is found.
     */
    private static String extractJsonData(String response) {
        int jsonStartIndex = response.indexOf("{");
        if (jsonStartIndex == -1) {
            return null;
        }
        return response.substring(jsonStartIndex).trim(); // Return the JSON part
    }

    /**
     * Starts a heartbeat thread that sends a periodic "HEARTBEAT" every 10 seconds to the AggregationServer.
     *
     * @param out The DataOutputStream object used to send data to the AggregationServer.
     */
    static void startHeartbeat(DataOutputStream out) {
        Thread heartbeat = new Thread(() -> {
            try {
                while (true) {
                    sendHeartbeat(out);
                    Thread.sleep(10000);
                }
            } catch (InterruptedException e) {
                System.err.println("Error: Unable to connect to send heartbeat");
            }
        });
        heartbeat.start();
    }

    /**
     * Sends a "HEARTBEAT" message to the AggregationServer to keep the connection alive.
     *
     * @param out The DataOutputStream object used to send data to the AggregationServer.
     */
    private static void sendHeartbeat(DataOutputStream out) {
        try {
            String heartbeatMessage = "HEARTBEAT";
            out.writeUTF(heartbeatMessage);
            out.flush();
            heartBeating = true;
        } catch (Exception e) {
            heartBeating = false;
            System.err.println("Unable to connect to send heartbeat: " + e.getMessage());
        }
    }

    /**
     * Checks whether the heartbeat is successfully sent to the AggregationServer.
     *
     * @return True if the heartbeat is successfully sent, false otherwise.
     */
    public boolean isHeartBeating() {
        return heartBeating;
    }

    /**
     * The main method that connects to the AggregationServer, sends GET requests, and handles the AggregationServer's response.
     *
     * @param args Command-line arguments containing the AggregationServer URL in the format "hostname:port".
     * @throws IOException if an I/O error occurs during communication.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java GETClient <servername:portnumber>");
            System.exit(1);
        }
        String url = args[0];
        String serverName = url.split(":")[0];
        int portNumber = Integer.parseInt(url.split(":")[1]);

        //initialize a socket act as GETClient
        try (Socket clientSocket = new Socket(serverName, portNumber);
             DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            startHeartbeat(out);

            // Keep the connection open and send GET requests periodically
            while (true) {
                sendGetRequest(out, serverName, portNumber);
                readServerResponse(in);
                Thread.sleep(10000);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error: Unable to connect to " + serverName + " on port " + portNumber + " - " + e.getMessage());
        }
    }
}
