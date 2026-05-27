package dk.kb.datahandler.kaltura;

import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

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


    @BeforeAll
    static void initSetup() {
        service = mockStatic(KalturaDeltaUploadJob.class, CALLS_REAL_METHODS);
        service.when(() -> KalturaDeltaUploadJob.initKalturaClient()).then(inv -> null);
        service.when(() -> KalturaDeltaUploadJob.uploadStream(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn("0_test");
        service.when(() -> KalturaDeltaUploadJob.updateKalturaIdForRecord(any(), any(), any()))
                .thenAnswer(inv -> null);
    }

    // ─── uploadStreamsToKaltura ───────────────────────────────────────────────

    @Test
    void testUploadStreamsToKaltura_whenSolrHasNoDocuments_thenNoRecordIsUploadedToKaltura() {

        SolrDocumentList emptySolrDocumentList = new SolrDocumentList(); // numFound = 0

        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(emptySolrDocumentList);

        int result = KalturaDeltaUploadJob.uploadStreamsToKaltura();

        service.verify(() -> KalturaDeltaUploadJob.processUpload(any(), anyLong(), any(), any(), any(), any(),
                any(), any(), any(), any()), never());
        assertEquals(0, result);
    }

    @Test
    void testUploadStreamsToKaltura_whenRecordAlreadyHasKalturaId_thenSkipsRecord() {
        SolrDocumentList solrDocumentList = buildSolrDocumentList(buildSolrDocument());
        SolrDocumentList empty = new SolrDocumentList();

        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList, empty);
        service.when(() -> KalturaDeltaUploadJob.recordAlreadyHasKalturaId(any(), eq(RECORD_ID)))
                .thenReturn(true);

        int result = KalturaDeltaUploadJob.uploadStreamsToKaltura();

        assertEquals(0, result);
        service.verify(() -> KalturaDeltaUploadJob.processUpload(any(), anyLong(), any(), any(), any(), any(),
                any(), any(), any(), any()), never());
    }

    @Test
    void testUploadStreamsToKaltura_whenProcessUploadSucceeds_thenCountUploadedStreams() {
        SolrDocumentList solrDocumentList = buildSolrDocumentList(buildSolrDocument());
        SolrDocumentList empty = new SolrDocumentList();

        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList, empty);
        service.when(() -> KalturaDeltaUploadJob.recordAlreadyHasKalturaId(any(), eq(RECORD_ID)))
                .thenReturn(false);
        service.when(() -> KalturaDeltaUploadJob.getInternalIdKaltura(anyString())).thenReturn(null);
        service.when(() -> KalturaDeltaUploadJob.hasStreamFileError(anyString(), anyLong())).thenReturn(null);
        service.when(() -> KalturaDeltaUploadJob.updateKalturaIdForRecord(any(), any(), any()))
                .thenAnswer(inv -> null);

        int result = KalturaDeltaUploadJob.uploadStreamsToKaltura();

        assertEquals(1, result);
        service.verify(() -> KalturaDeltaUploadJob.processUpload(any(), anyLong(), any(), any(), any(), any(), any(),
                any(), any(), any()), atLeastOnce());

    }

    @Test
    void testUploadStreamsToKaltura_onSolrServerException_thenThrowsInternalServiceException() {
        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenThrow(new SolrServerException("Solr is down"));

        assertThrows(InternalServiceException.class, KalturaDeltaUploadJob::uploadStreamsToKaltura);

    }

    @Test
    void testUploadStreamsToKaltura_onIOException_thenThrowsInternalServiceException() {
        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenThrow(new IOException("Network failure"));

        assertThrows(InternalServiceException.class, KalturaDeltaUploadJob::uploadStreamsToKaltura);

    }

    @Test
    void testUploadStreamsToKaltura_whenMultipleDocuments_thenAccumulatesCount() {
        SolrDocumentList docs = buildSolrDocumentList(buildSolrDocument("id-1"), buildSolrDocument("id-2"));
        SolrDocumentList empty = new SolrDocumentList();

        service.when(() -> KalturaDeltaUploadJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(docs, empty);
        service.when(() -> KalturaDeltaUploadJob.hasStreamFileError(anyString(), anyLong())).thenReturn(null);
        service.when(() -> KalturaDeltaUploadJob.recordAlreadyHasKalturaId(any(), anyString())).thenReturn(false);
        service.when(() -> KalturaDeltaUploadJob.getInternalIdKaltura(anyString())).thenReturn(null);

        int result = KalturaDeltaUploadJob.uploadStreamsToKaltura();

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