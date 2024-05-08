package dk.kb.datahandler.oai;

public class TestPlugin  implements Plugin{
    private String resultOftest = "";
    @Override
    public void apply() {
        resultOftest = "Test plugin has been activated.";
    }

    public String getResultOftest() {
        return resultOftest;
    }
}
