package dk.kb.datahandler.oai.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreservicaManifestationPlugin  implements Plugin{
    private static final Logger log = LoggerFactory.getLogger(PreservicaManifestationPlugin.class);

    @Override
    public void apply() {
        log.info("Preservica Manifestation Plugin has been applied.");
    }
}
