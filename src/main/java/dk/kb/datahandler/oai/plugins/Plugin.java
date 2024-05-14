package dk.kb.datahandler.oai.plugins;

import dk.kb.datahandler.oai.OaiRecord;

public interface Plugin {
    void apply(OaiRecord oaiRecord);
}
