import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AggregationServerTest {
    private Socket mockSocket;
    private DataOutputStream mockOut;

    @BeforeEach
    void setUp() throws IOException {
        mockSocket = mock(Socket.class);
        mockOut = mock(DataOutputStream.class);
        DataInputStream mockIn = mock(DataInputStream.class);

        when(mockSocket.getInputStream()).thenReturn(mockIn);
        when(mockSocket.getOutputStream()).thenReturn(mockOut);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockSocket.close();
    }

    @Test
    void testHandlePutRequestCreateStorage() throws IOException {
        File file = new File("data/weather_data.txt");
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (isDeleted) {
                System.out.println("File deleted successfully.");
            } else {
                System.err.println("Failed to delete the file.");
            }
        }
        String putMessage = "PUT /data HTTP/1.1\r\n" +
                "Content-Length: 29\r\n\r\n" +
                "{\"temperature\": 100}";

        new AggregationServer.AggregationServerThread(mockSocket).handlePutRequest(putMessage, mockOut);
        verify(mockOut).writeUTF(contains("HTTP/1.1 201")); // Ensure success response
    }

    @Test
    void testHandlePutRequestUpdateData() throws IOException {
        File file = new File("data/weather_data.txt");
        if (!file.exists()) {
            boolean created = file.createNewFile();
            if (created) {
                System.out.println("File created successfully.");
            } else {
                System.err.println("Failed to created the file.");
            }
        }
        String putMessage = "PUT /data HTTP/1.1\r\n" +
                "Content-Length: 29\r\n\r\n" +
                "{\"temperature\": 100}";

        new AggregationServer.AggregationServerThread(mockSocket).handlePutRequest(putMessage, mockOut);
        verify(mockOut).writeUTF(contains("HTTP/1.1 200")); // Ensure success response
    }

    @Test
    void testInvalidJsonOnPutRequest() throws IOException {
        String putMessage = "PUT /data HTTP/1.1\r\nLamport-Clock: 1\r\n\r\n{\"temperature\": }";
        new AggregationServer.AggregationServerThread(mockSocket).handlePutRequest(putMessage, mockOut);
        verify(mockOut).writeUTF(contains("HTTP/1.1 500 Invalid JSON Data")); // Ensure the error response is sent
    }

    @Test
    void testNoJsonOnPutRequest() throws IOException {
        String putMessage = "PUT /data HTTP/1.1\r\nLamport-Clock: 1\r\n\r\n\"temperature\": }";
        new AggregationServer.AggregationServerThread(mockSocket).handlePutRequest(putMessage, mockOut);
        verify(mockOut).writeUTF(contains("HTTP/1.1 204 No Content Request")); // Ensure the error response is sent
    }

    @Test
    void testHandleGetRequest() throws IOException {
        String filePath = "data/weather_data.txt";
        setupDatabase(filePath, "2024-10-03 23:49:54 [Thread ID: 1] {\"temperature\": 102}\n");
        new AggregationServer.AggregationServerThread(mockSocket).handleGetRequest(mockOut);
        verify(mockOut).writeUTF(contains("HTTP/1.1 200")); // Ensure success response
    }

    @Test
    void testExtractJsonDataWithValidJson() {
        AggregationServer.AggregationServerThread thread = new AggregationServer.AggregationServerThread(mockSocket);
        String request = "POST /data HTTP/1.1\r\nContent-Type: application/json\r\n\r\n{ \"key\": \"value\" }";
        String actualJson = thread.extractJsonData(request);
        String expectedJson = "{ \"key\": \"value\" }";
        assertEquals(expectedJson, actualJson);
    }

    @Test
    void testExtractJsonDataWithInvalidJson() {
        AggregationServer.AggregationServerThread thread = new AggregationServer.AggregationServerThread(mockSocket);
        String request = "POST /data HTTP/1.1\r\nContent-Type: application/json\r\n\r\n[ \"key\": \"value\" }";
        String actualJson = thread.extractJsonData(request);
        assertNull(actualJson);
    }

    @Test
    void testExtractJsonDataWithNullJson() {
        AggregationServer.AggregationServerThread thread = new AggregationServer.AggregationServerThread(mockSocket);
        String request = "";
        String actualJson = thread.extractJsonData(request);
        assertNull(actualJson);
    }

    @Test
    void testMultipleClientsGETRequests() throws IOException {
        String filePath = "data/weather_data.txt";
        setupDatabase(filePath, "2024-10-03 23:49:54 [Thread ID: 1] {\"temperature\": 102}\n");

        Socket clientSocket1 = new Socket("localhost", 4567);
        DataInputStream in1 = new DataInputStream(clientSocket1.getInputStream());
        DataOutputStream out1 = new DataOutputStream(clientSocket1.getOutputStream());
        GETClient.sendGetRequest(out1, "localhost", 4567);
        String data1 = GETClient.readServerResponse(in1);
        clientSocket1.close();

        Socket clientSocket2 = new Socket("localhost", 4567);
        DataInputStream in2 = new DataInputStream(clientSocket2.getInputStream());
        DataOutputStream out2 = new DataOutputStream(clientSocket2.getOutputStream());
        GETClient.sendGetRequest(out2, "localhost", 4567);
        String data2 = GETClient.readServerResponse(in2);
        clientSocket2.close();

        assertEquals(data1, data2);
    }

    @Test
    void testMultiplePUTRequests() throws IOException {
        String inputFilePath = "data/inputData.txt";
        String filePath = "data/weather_data.txt";
        setupDatabase(filePath, "");

        Socket contentSocket1 = new Socket("localhost", 4567);
        DataInputStream in1 = new DataInputStream(contentSocket1.getInputStream());
        DataOutputStream out1 = new DataOutputStream(contentSocket1.getOutputStream());
        ContentServer.sendPutRequestWithRetries(out1, in1, inputFilePath, 3);
        contentSocket1.close();

        Socket contentSocket2 = new Socket("localhost", 4567);
        DataInputStream in2 = new DataInputStream(contentSocket2.getInputStream());
        DataOutputStream out2 = new DataOutputStream(contentSocket2.getOutputStream());
        ContentServer.sendPutRequestWithRetries(out2, in2, inputFilePath, 3);
        contentSocket2.close();

        List<String> fileContent = Files.readAllLines(Paths.get(filePath));

        // Assert that there are exactly two entries in the file
        assertEquals(2, fileContent.size());
    }

    @Test
    void testMultiplePUTGETPUTGET() throws IOException, InterruptedException {
        String inputFilePath1 = "data/inputData.txt";
        String inputFilePath2= "data/inputData2.txt";
        String filePath = "data/weather_data.txt";
        setupDatabase(filePath, "");

        Socket contentSocket1 = new Socket("localhost", 4567);
        DataInputStream in1 = new DataInputStream(contentSocket1.getInputStream());
        DataOutputStream out1 = new DataOutputStream(contentSocket1.getOutputStream());
        ContentServer.sendPutRequestWithRetries(out1, in1, inputFilePath1, 3);
        contentSocket1.close();

        Socket clientSocket1 = new Socket("localhost", 4567);
        DataInputStream clientIn1 = new DataInputStream(clientSocket1.getInputStream());
        DataOutputStream clientOut1 = new DataOutputStream(clientSocket1.getOutputStream());
        GETClient.sendGetRequest(clientOut1, "localhost", 4567);
        String data1 = GETClient.readServerResponse(clientIn1);
        clientSocket1.close();

        Thread.sleep(1000);

        Socket contentSocket2 = new Socket("localhost", 4567);
        DataInputStream in2 = new DataInputStream(contentSocket2.getInputStream());
        DataOutputStream out2 = new DataOutputStream(contentSocket2.getOutputStream());
        ContentServer.sendPutRequestWithRetries(out2, in2, inputFilePath2, 3);
        contentSocket2.close();

        Socket clientSocket2 = new Socket("localhost", 4567);
        DataInputStream clientIn2 = new DataInputStream(clientSocket2.getInputStream());
        DataOutputStream clientOut2 = new DataOutputStream(clientSocket2.getOutputStream());
        GETClient.sendGetRequest(clientOut2, "localhost", 4567);
        String data2 = GETClient.readServerResponse(clientIn2);
        clientSocket2.close();

        // Assert that the data are received correctly by different GET calls
        assertTrue(data1 != null && data1.contains("Adelaide"));
        assertTrue(data2 != null && data2.contains("Hanoi"));
    }

    @Test
    void testRemoveExpiredData() throws IOException, InterruptedException {
        String inputFilePath = "data/inputData.txt";
        String filePath = "data/weather_data.txt";
        setupDatabase(filePath, "");

        Socket contentSocket1 = new Socket("localhost", 4567);
        DataInputStream in1 = new DataInputStream(contentSocket1.getInputStream());
        DataOutputStream out1 = new DataOutputStream(contentSocket1.getOutputStream());
        ContentServer.sendPutRequestWithRetries(out1, in1, inputFilePath, 3);
        contentSocket1.close();

        List<String> fileContent = Files.readAllLines(Paths.get(filePath));
        assertEquals(1, fileContent.size());
        Thread.sleep(36000);

        File fileAfterRemove = new File(filePath);
        assertEquals(0, fileAfterRemove.length());
    }

    public static void setupDatabase(String filePath, String dataEntry) throws IOException {
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            boolean result = file.getParentFile().mkdirs();
            if(result){
                System.out.println("Create directory successful;");
            }else{
                System.err.println("Create directory failed");
            }
        }
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(dataEntry);
        }
    }
}
