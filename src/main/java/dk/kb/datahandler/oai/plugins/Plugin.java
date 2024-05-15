package dk.kb.datahandler.oai.plugins;

import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.storage.model.v1.DsRecordDto;

public interface Plugin {
    void apply(OaiRecord oaiRecord);
    void apply(DsRecordDto dsRecord);
}
