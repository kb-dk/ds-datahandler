package dk.kb.datahandler.kaltura;

import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KalturaDeltaUploadUnitTest {
    static MockedStatic<KalturaDeltaUploadJob> service;
    private static final String FILE_ID = "file-123";
    private static final String RECORD_ID = "record-456";
    private static final String TITLE = "Test Stream Title";
    private static final String DESCRIPTION = "Test Description";
    private static final String FILE_PATH = "/streams/test/file.mp4";
    private static final String FILE_EXTENSION = "mp4";
    private static final String RESOURCE_DESCRIPTION = "video";

    @BeforeEach
    void initSetup() {
        service = mockStatic(KalturaDeltaUploadJob.class, CALLS_REAL_METHODS);
        service.when(() -> KalturaDeltaUploadJob.initKalturaClient()).then(inv -> null);
        service.when(() -> KalturaDeltaUploadJob.uploadStream(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn("0_test");
        service.when(() -> KalturaDeltaUploadJob.updateKalturaIdForRecord(any(), any(), any()))
                .thenAnswer(inv -> null);
    }

    @AfterEach
    void tearDown() {
        service.close();
    }

    // ─── uploadStreamsToKaltura ───────────────────────────────────────────────

    @Test
    void testUploadStreamsToKaltura_whenSolrHasNoDocuments_thenNoRecordIsUploadedToKaltura() {
        // Arrange
        SolrDocumentList emptySolrDocumentList = new SolrDocumentList(); // numFound = 0

        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(emptySolrDocumentList);

        // Act
        int result = KalturaDeltaUploadJob.uploadStreamsToKaltura();

        // Assert
        service.verify(() -> KalturaDeltaUploadJob.uploadStream(any(), any(), any(), any(), any(), any(), any(),
                anyInt()), never());
        assertEquals(0, result);
    }

    @Test
    void testUploadStreamsToKaltura_whenRecordAlreadyHasKalturaId_thenSkipRecord() {
        // Arrange
        SolrDocumentList solrDocumentList = buildSolrDocumentList(buildSolrDocument());
        SolrDocumentList emptySolrDocumentList = new SolrDocumentList();
        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList, emptySolrDocumentList);
        service.when(() -> KalturaDeltaUploadJob.recordAlreadyHasKalturaId(any(), eq(RECORD_ID)))
                .thenReturn(true);

        // Act
        int result = KalturaDeltaUploadJob.uploadStreamsToKaltura();

        // Assert
        assertEquals(0, result);
        service.verify(() -> KalturaDeltaUploadJob.uploadStream(any(), any(), any(), any(), any(), any(), any(),
                anyInt()), never());
    }

    @Test
    void testUploadStreamsToKaltura_whenProcessUploadSucceeds_thenCountUploadedStreams() {
        // Arrange
        SolrDocumentList solrDocumentList = buildSolrDocumentList(buildSolrDocument());
        SolrDocumentList emptySolrDocumentList = new SolrDocumentList();

        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList, emptySolrDocumentList);
        service.when(() -> KalturaDeltaUploadJob.recordAlreadyHasKalturaId(any(), eq(RECORD_ID)))
                .thenReturn(false);
        service.when(() -> KalturaDeltaUploadJob.getInternalIdKaltura(anyString())).thenReturn(null);
        service.when(() -> KalturaDeltaUploadJob.hasStreamFileError(anyString(), anyLong())).thenReturn(null);

        // Act
        int result = KalturaDeltaUploadJob.uploadStreamsToKaltura();

        // Assert
        assertEquals(1, result);
        service.verify(() -> KalturaDeltaUploadJob.uploadStream(any(), any(), any(), any(), any(), any(), any(), anyInt()), times(1));
    }

    @Test
    void testUploadStreamsToKaltura_whenFetchSolrRecordsThrowsSolrServerException_thenThrowsInternalServiceException() {
        // Arrange
        String expectedMessage = "Solr is down";
        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenThrow(new SolrServerException(expectedMessage));

        // Act and Assert
        Exception exception = assertThrows(InternalServiceException.class, KalturaDeltaUploadJob::uploadStreamsToKaltura);
        assertEquals(expectedMessage, exception.getCause().getMessage());
    }

    @Test
    void testUploadStreamsToKaltura_whenFetchSolrRecordsThrowsIOException_thenThrowsInternalServiceException() {
        // Arrange
        String expectedMessage = "Network failure";
        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenThrow(new IOException(expectedMessage));

        // Act and Assert
        Exception exception = assertThrows(InternalServiceException.class, KalturaDeltaUploadJob::uploadStreamsToKaltura);
        assertEquals(expectedMessage, exception.getCause().getMessage());
    }

    @Test
    void testUploadStreamsToKaltura_whenGenerateStreamPathThrowsIOException_thenThrowsInternalServiceException() {
        // Arrange
        String expectedMessage = "Could not find a valid streamPath for that input";
        SolrDocumentList solrDocumentList = buildSolrDocumentList(buildSolrDocument());
        SolrDocumentList emptySolrDocumentList = new SolrDocumentList();
        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList, emptySolrDocumentList);

        try (MockedStatic<KalturaUtil> kalturaUtilMock = mockStatic(KalturaUtil.class)) {
            kalturaUtilMock.when(() -> KalturaUtil.generateStreamPath(any(), any(), any()))
                    .thenThrow(new IOException(expectedMessage));

            // Act and Assert
            Exception exception = assertThrows(InternalServiceException.class, () -> KalturaDeltaUploadJob.uploadStreamsToKaltura());
            assertEquals(expectedMessage, exception.getCause().getMessage());
        }
    }

    @Test
    void testUploadStreamsToKaltura_whenMultipleDocuments_thenAccumulatesCount() {

        // Arrange
        SolrDocumentList solrDocumentList = buildSolrDocumentList(buildSolrDocument("id-1"), buildSolrDocument("id-2"));
        SolrDocumentList emptySolrDocumentList = new SolrDocumentList();

        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList, emptySolrDocumentList);
        service.when(() -> KalturaDeltaUploadJob.hasStreamFileError(anyString(), anyLong())).thenReturn(null);
        service.when(() -> KalturaDeltaUploadJob.recordAlreadyHasKalturaId(any(), anyString())).thenReturn(false);
        service.when(() -> KalturaDeltaUploadJob.getInternalIdKaltura(anyString())).thenReturn(null);

        // Act
        int result = KalturaDeltaUploadJob.uploadStreamsToKaltura();

        // Assert
        assertEquals(2, result);
        service.verify(() -> KalturaDeltaUploadJob.uploadStream(any(), any(), any(), any(), any(), any(), any(),
                anyInt()), times(2));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private SolrDocument buildSolrDocument() {
        return buildSolrDocument(RECORD_ID);
    }

    private SolrDocument buildSolrDocument(String id) {
        SolrDocument solrDocument = new SolrDocument();
        solrDocument.setField("id", id);
        solrDocument.setField("file_id", FILE_ID);
        solrDocument.setField("file_path", FILE_PATH);
        solrDocument.setField("file_extension", FILE_EXTENSION);
        solrDocument.setField("resource_description", RESOURCE_DESCRIPTION);
        solrDocument.setField("description", DESCRIPTION);
        solrDocument.setField("originates_from", KalturaUtil.ORGINATES_FROM.Preservica.name());
        solrDocument.setField("internal_storage_mTime", 1_700_000_000L);

        ArrayList<String> titles = new ArrayList<>();
        titles.add(TITLE);
        titles.add("Secondary Title");
        solrDocument.setField("title", titles);

        return solrDocument;
    }

    private SolrDocumentList buildSolrDocumentList(SolrDocument... documents) {
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        solrDocumentList.addAll(Arrays.asList(documents));
        solrDocumentList.setNumFound(documents.length);
        return solrDocumentList;
    }
}