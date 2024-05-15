package dk.kb.datahandler.oai;

import dk.kb.datahandler.oai.plugins.Plugin;
import dk.kb.storage.model.v1.DsRecordDto;

public class TestPlugin  implements Plugin {
    private String resultOftest = "";
    @Override
    public void apply(OaiRecord oaiRecord) {
        resultOftest = "Test plugin has been activated.";
    }

    @Override
    public void apply(DsRecordDto dsRecord) {
        resultOftest = "Test plugin has been activated.";
    }

    public String getResultOftest() {
        return resultOftest;
    }
}
