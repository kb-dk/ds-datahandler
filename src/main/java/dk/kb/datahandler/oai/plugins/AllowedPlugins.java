package dk.kb.datahandler.oai.plugins;

public enum AllowedPlugins {
    FETCH_MANIFESTATION("fetchManifestation"),
    SPEECH_TO_TEXT("speechToText");

    private final String name;
    AllowedPlugins(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
