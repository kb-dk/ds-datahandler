package dk.kb.datahandler.oai;

import dk.kb.datahandler.oai.plugins.Plugin;

public class TestPlugin  implements Plugin {
    private String resultOftest = "";
    @Override
    public void apply(OaiRecord oaiRecord) {
        resultOftest = "Test plugin has been activated.";
    }

    public String getResultOftest() {
        return resultOftest;
    }
}
