package dk.kb.datahandler.util;

import dk.kb.storage.model.v1.DsRecordDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreservicaUtilTest {
    @Test
    public void testPreservicaIdExtraction(){
        DsRecordDto testRecord = new DsRecordDto();
        testRecord.setId("ds.tv:oai:io:3006e2f8-3f73-477a-a504-4d7cb1ae1e1c");

        String preservicaId = PreservicaUtils.getPreservicaIoId(testRecord);
        assertEquals("3006e2f8-3f73-477a-a504-4d7cb1ae1e1c", preservicaId);
    }
}
