import java.io.*;
import java.net.Socket;

/**
 * ContentServer, connect to AggregationServer and send/update data.
 */
public class ContentServer {
    private static final LamportClock lamportClock = new LamportClock();

    /**
     * Sends a PUT request to the server with data from an input file.
     *
     * @param out           The output stream to send the request.
     * @param inputFilePath The path to the file that contains the data to be sent.
     * @throws IOException  If the file path is invalid, data conversion fails, or
     *                      there is an error during the request.
     */
    public static void sendPutRequest(DataOutputStream out, String inputFilePath) throws IOException {
        File inputFile = new File(inputFilePath);
        if (!inputFile.exists() || !inputFile.isFile()) {
            String errorMessage = "Invalid file path: " + inputFilePath;
            System.err.println(errorMessage);
            throw new IOException(errorMessage);
        }

        try {
            String data = JSONParser.convertFileToJson(inputFilePath);
            if (data == null) {
                System.err.println("Data is null after conversion from file.");
                throw new IOException("Data is null after conversion from file.");
            }

            String request = "PUT /weather_data.txt HTTP/1.1\r\nUser-Agent: ATOMClient/1/0\r\nContent-Type: text/Json\r\nContent-Length: "
                    + data.length() + "\r\nLamport-Clock: " + lamportClock.getValue() + "\r\n\r\n" + data + "\r\n";

            out.writeUTF(request);
            out.flush();
            lamportClock.increment();
        } catch (IOException e) {
            System.err.println("Error reading data/sending PUT: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Reads and processes the server's response to the PUT request.
     *
     * @param in The input stream to receive the server's response.
     * @return true if the server responds with success (200 or 201 status code), false otherwise.
     * @throws IOException If there is an error while reading the server's response.
     */
    public static boolean readServerResponse(DataInputStream in) throws IOException {
        String response = in.readUTF();
        if (response.startsWith("HTTP/1.1 200") || response.startsWith("HTTP/1.1 201")) {
            System.out.println("Content is successfully updated");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Attempts to send a PUT request with retries if the initial attempt fails.
     *
     * @param out           The output stream to send the request.
     * @param in            The input stream to receive the server's response.
     * @param inputFilePath The path to the file that contains the data to be sent.
     * @param maxRetries    The maximum number of retries in case of failure.
     * @return true if the PUT request is successful within the retry limit, false otherwise.
     */
    public static boolean sendPutRequestWithRetries(DataOutputStream out, DataInputStream in, String inputFilePath, int maxRetries) {
        boolean successPutRequest = false;
        while (maxRetries > 0 && !successPutRequest) {
            maxRetries--;
            try {
                sendPutRequest(out, inputFilePath);
                successPutRequest = readServerResponse(in);
            } catch (IOException e) {
                System.err.println("Error during PUT request: " + e.getMessage());
            }
            if (maxRetries > 0 && !successPutRequest) {
                System.out.println("Retrying PUT request...");
            }
        }
        return successPutRequest;
    }

    /**
     * Main method that starts the ContentServer. Sends a PUT request to the server
     * and retries up to 3 times if the request fails. Continues to send requests at
     * regular intervals (20 seconds).
     *
     * @param args Command line arguments containing the server URL and input file path.
     * @throws InterruptedException If the thread is interrupted during sleep.
     */
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.err.println("Usage: java ContentServer <servername:portnumber> <inputFilePath>");
            System.exit(1);
        }

        String url = args[0];
        String inputFilePath = args[1];

        String serverName = url.split(":")[0];
        int portNumber = Integer.parseInt(url.split(":")[1]);

        //initialize a new socket
        try (Socket clientSocket = new Socket(serverName, portNumber);
             DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
            boolean successPutRequest;
            int maxRetries = 3;
            while (true) {
                successPutRequest = sendPutRequestWithRetries(out, in, inputFilePath, maxRetries);
                if (successPutRequest) {
                    System.out.println("PUT request successful.");
                } else {
                    System.err.println("Failed to send PUT request after " + maxRetries + " attempts.");
                }
                Thread.sleep(20000);
            }
        } catch (IOException e) {
            System.err.println("Error: Unable to connect to " + serverName + " on port " + portNumber);
        }
    }
}
