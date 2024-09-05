package dk.kb.datahandler.enrichment;

import dk.kb.util.Resolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class FragmentClientTest {

    private FragmentsClient fragmentsClient;
    private HttpURLConnection mockConnection;

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        // Initialize FragmentsClient and mock HttpURLConnection
        fragmentsClient = Mockito.spy(new FragmentsClient("http://test-url.com",5));
        mockConnection = mock(HttpURLConnection.class);

        // Mock the getConnection method to always return the mockConnection object
        doReturn(mockConnection).when(fragmentsClient).getConnection(anyString());
    }



    @Test
    public void testFetchFragments() throws IOException, URISyntaxException {
        when(mockConnection.getResponseCode()).thenReturn(200);

        String jsonResponse = Files.readString(Resolver.getPathFromClasspath("xml/fragments-multi.json"));
        InputStream jsonStream = new java.io.ByteArrayInputStream(jsonResponse.getBytes());
        when(mockConnection.getInputStream()).thenReturn(jsonStream);

        List<Fragment> fragments = fragmentsClient.fetchMetadataFragments("test-id");
        Assertions.assertEquals(2, fragments.size());
    }

    @Test
    public void testRetriesOnFailure() throws IOException, URISyntaxException {
        when(mockConnection.getResponseCode())
                .thenReturn(500)
                .thenReturn(500)
                .thenReturn(200);

        fragmentsClient.fetchMetadataFragments("test");
        verify(mockConnection, times(3)).getResponseCode();
    }

    @Test
    public void testFailure() throws IOException {
        when(mockConnection.getResponseCode()).thenReturn(500);
        assertThrows(RuntimeException.class, () -> fragmentsClient.fetchMetadataFragments("test"));
        verify(mockConnection, times(5)).getResponseCode();
    }

}
