package dk.kb.datahandler.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DsDatahandlerFacade {
    
    
    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);
    public static Integer oaiIngestFull(String oaiTarget) {
        log.info("CALLED!");
        return 1000;
    }
    
}
