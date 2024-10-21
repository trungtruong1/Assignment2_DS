import org.junit.jupiter.api.*;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GETClientTest {
    private GETClient client;
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private DataOutputStream mockOutput;
    private DataInputStream mockInput;

    @BeforeEach
    void setUp() {
        client = new GETClient();

        System.setErr(new PrintStream(errContent));
        System.setOut(new PrintStream(outContent));

        mockOutput = mock(DataOutputStream.class);
        mockInput = mock(DataInputStream.class);

        outContent.reset();
        errContent.reset();
    }

    @AfterEach
    void tearDown() {
        System.setErr(System.err);
        System.setOut(System.out);
    }

    @Test
    void testSendGetRequest() throws IOException {
        GETClient.sendGetRequest(mockOutput, "localhost", 4567);

        String actualRequest = client.getRequest();
        String expectedRequest = "GET /weather HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "Lamport-Clock: 0\r\n";

        assertEquals(expectedRequest, actualRequest);
        verify(mockOutput).writeUTF(expectedRequest);
        verify(mockOutput).flush();
    }

    @Test
    void testReadServerResponse() throws IOException {
        String mockResponse = "2024-10-03 16:01:03 [Thread ID: 27] { \"id\": \"IDS60901\", \"name\": \"Atlantic\", \"state\": \"SEA\", \"time_zone\": \"CST\" }";
        when(mockInput.readUTF()).thenReturn(mockResponse);
        GETClient.readServerResponse(mockInput);
        String expectedOutput = "Received weather data:\nid: IDS60901\nname: Atlantic\nstate: SEA\ntime_zone: CST";
        expectedOutput = expectedOutput.replaceAll("\\s+", " ").trim();
        String actualOutput = outContent.toString().replaceAll("\\s+", " ").trim();

        assertEquals(expectedOutput, actualOutput, "Output does not match expected result");
    }

    @Test
    void testInvalidJsonResponse() throws IOException {
        when(mockInput.readUTF()).thenReturn("Invalid response without JSON");
        GETClient.readServerResponse(mockInput);
        String actualErrOutput = errContent.toString().trim();

        assertTrue(actualErrOutput.contains("Error: No JSON data found in the response."),
                "Expected error message was not printed");
    }

    @Test
    void testHeartbeat() throws IOException {
        GETClient.startHeartbeat(mockOutput);
        try {
            Thread.sleep(1000); // Sleep for a short duration to allow the heartbeat to execute
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(mockOutput, atLeastOnce()).writeUTF("HEARTBEAT");
        verify(mockOutput, atLeastOnce()).flush();
    }

    @Test
    void testHeartbeatFailure() throws IOException {
        doThrow(new IOException("Connection error")).when(mockOutput).writeUTF(anyString());
        GETClient.startHeartbeat(mockOutput);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(client.isHeartBeating(), "Heartbeat should be false after a connection error");
    }
}
