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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KalturaDeltaUploadUnitTest {
    static MockedStatic<KalturaDeltaUploadJob> service;
    private static final String UPLOAD_TAG = "test-upload-tag";
    private static final long MIN_FILE_SIZE = 700L;
    private static final String FILE_ID = "file-123";
    private static final String RECORD_ID = "record-456";
    private static final String TITLE = "Test Stream Title";
    private static final String DESCRIPTION = "Test Description";
    private static final String FILE_PATH = "/streams/test/file.mp4";
    private static final String FILE_EXTENSION = "mp4";
    private static final String RESOURCE_DESCRIPTION = "video";
    private static final String KALTURA_ID = "kaltura-789";


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
    void testUploadStreamsToKaltura_whenRecordAlreadyHasKalturaId_thenSkipsRecord() {
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
        service.verify(() -> KalturaDeltaUploadJob.uploadStream(any(), any(), any(), any(), any(), any(), any(), anyInt()), atMostOnce());

    }

    @Test
    void testUploadStreamsToKaltura_onSolrServerException_thenThrowsInternalServiceException() {
        // Arrange
        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenThrow(new SolrServerException("Solr is down"));

        // Act and Assert
        assertThrows(InternalServiceException.class, KalturaDeltaUploadJob::uploadStreamsToKaltura);

    }

    @Test
    void testUploadStreamsToKaltura_onIOException_thenThrowsInternalServiceException() {
        // Arrange
        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenThrow(new IOException("Network failure"));

        // Act and Assert
        assertThrows(InternalServiceException.class, KalturaDeltaUploadJob::uploadStreamsToKaltura);

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

    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private SolrDocument buildSolrDocument() {
        return buildSolrDocument(RECORD_ID);
    }

    private SolrDocument buildSolrDocument(String id) {
        SolrDocument doc = new SolrDocument();
        doc.setField("id", id);
        doc.setField("file_id", FILE_ID);
        doc.setField("file_path", FILE_PATH);
        doc.setField("file_extension", FILE_EXTENSION);
        doc.setField("resource_description", RESOURCE_DESCRIPTION);
        doc.setField("description", DESCRIPTION);
        doc.setField("originates_from", KalturaUtil.ORGINATES_FROM.Preservica.name());
        doc.setField("internal_storage_mTime", 1_700_000_000L);

        ArrayList<String> titles = new ArrayList<>();
        titles.add(TITLE);
        titles.add("Secondary Title");
        doc.setField("title", titles);

        return doc;
    }

    private SolrDocumentList buildSolrDocumentList(SolrDocument... documents) {
        SolrDocumentList list = new SolrDocumentList();
        for (SolrDocument doc : documents) {
            list.add(doc);
        }
        list.setNumFound(documents.length);
        return list;
    }
}