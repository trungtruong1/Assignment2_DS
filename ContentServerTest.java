import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentServerTest {
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private DataOutputStream mockOutputStream;
    private DataInputStream mockInputStream;

    @BeforeEach
    void setUp() {
        mockOutputStream = Mockito.mock(DataOutputStream.class);
        mockInputStream = Mockito.mock(DataInputStream.class);
        System.setErr(new PrintStream(errContent));  // Redirect System.err to capture it
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setErr(System.err);
        System.setOut(System.out);
    }

    @Test
    void testSendPutUpdateRequestSuccess() throws IOException {
        String jsonResponse = "HTTP/1.1 200 OK";
        when(mockInputStream.readUTF()).thenReturn(jsonResponse);
        boolean result = ContentServer.readServerResponse(mockInputStream);

        assertTrue(result, "PUT request should be successful");
    }

    @Test
    void testSendPutCreateRequestSuccess() throws IOException {
        String jsonResponse = "HTTP/1.1 201 OK";
        when(mockInputStream.readUTF()).thenReturn(jsonResponse);
        boolean result = ContentServer.readServerResponse(mockInputStream);

        assertTrue(result, "PUT request should be successful");
    }

    @Test
    void testSendPutRequestFailure() throws IOException {
        String jsonResponse = "HTTP/1.1 500 Invalid JSON Data";
        when(mockInputStream.readUTF()).thenReturn(jsonResponse);
        boolean result = ContentServer.readServerResponse(mockInputStream);

        assertFalse(result, "PUT request should be failure");
    }

    @Test
    void testInvalidInputFilePath() {
        String invalidFilePath = "invalid/path/to/file.txt";
        Exception exception = assertThrows(IOException.class, () -> {
            ContentServer.sendPutRequest(mockOutputStream, invalidFilePath);
        });
        String expectedMessage = "Invalid file path: " + invalidFilePath;

        assertTrue(exception.getMessage().contains(expectedMessage), "Should throw exception with invalid file path message");
    }

    @Test
    void testEmptyInputFile() {
        String emptyFilePath = "data/emptyFile.txt";
        Exception exception = assertThrows(IOException.class, () -> {
            ContentServer.sendPutRequest(mockOutputStream, emptyFilePath);
        });
        String expectedMessage = "Data is null after conversion from file.";

        assertTrue(exception.getMessage().contains(expectedMessage), "Should throw exception with invalid file path message");
    }

    @Test
    void testInvalidInputDataNoID() {
        String invalidInputPath = "data/invalidInputDataNoID.txt";
        Exception exception = assertThrows(IOException.class, () -> {
            ContentServer.sendPutRequest(mockOutputStream, invalidInputPath);
        });
        String expectedMessage = "Data is null after conversion from file.";

        assertTrue(exception.getMessage().contains(expectedMessage), "Should throw exception with invalid file path message");
    }

    @Test
    void testInvalidInputBadJSON() throws IOException {
        String badJsonInputPath = "data/invalidInputBadJSON.txt";
        String jsonResponse = "HTTP/1.1 400 Bad Request";
        doNothing().when(mockOutputStream).writeUTF(anyString());
        when(mockInputStream.readUTF()).thenReturn(jsonResponse);
        ContentServer.sendPutRequest(mockOutputStream, badJsonInputPath);
        boolean result = ContentServer.readServerResponse(mockInputStream);

        assertFalse(result, "PUT request should fail due to bad JSON");
    }

    @Test
    void testPutRequestFormat() throws IOException {
        String inputPath = "data/inputForPutRequestFormat.txt";
        String expectedData = "{\n" +
                "    \"id\": \"IDS60901\",\n" +
                "    \"wind_spd_kt\": 8\n" +
                "}";

        ContentServer.sendPutRequest(mockOutputStream, inputPath);

        // ArgumentCaptor to capture the actual request
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockOutputStream).writeUTF(captor.capture());
        String actualRequest = captor.getValue();

        // Verify that the request contains the expected components, lamport clock is left out because the value is not determined
        assertTrue(actualRequest.contains("PUT /weather_data.txt HTTP/1.1"));
        assertTrue(actualRequest.contains("User-Agent: ATOMClient/1/0"));
        assertTrue(actualRequest.contains("Content-Type: text/Json"));
        assertTrue(actualRequest.contains("Content-Length: " + expectedData.length() + "\r\n"));
        assertTrue(actualRequest.contains(expectedData));
    }

}
