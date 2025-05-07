package dk.kb.datahandler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.kaltura.KalturaUtil;

@Tag("integration")
public class KalturaFilePathUtilTest {
    
    private static final Logger log = LoggerFactory.getLogger(KalturaFilePathUtilTest.class);
    
    @BeforeAll
    static void setup() {
        try {
       ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml","ds-datahandler-integration-test.yaml");
        }
        catch(Exception e) {
            log.error("error loading behaviour+integration test yaml");
        }
    }

    
    @Test   
    void streamPathResolvingTest() throws Exception {
        
        //Test all 4 different combinations.        
        String domsVideoPath= KalturaUtil.generateStreamPath("16b6a4bc-ea57-47c1-b535-efacfb85ea3a","DOMS","VideoObject");
        assertEquals("/radio-tv/1/6/b/6/16b6a4bc-ea57-47c1-b535-efacfb85ea3a.mp4", domsVideoPath);
        
        String domsAudioPath= KalturaUtil.generateStreamPath("0adfb324-d090-42a9-b11c-0d72973aa486","DOMS","AudioObject");
        assertEquals("/radio-tv/0/a/d/f/0adfb324-d090-42a9-b11c-0d72973aa486.mp3",domsAudioPath);        
        
        String preservicaVideoPath= KalturaUtil.generateStreamPath("977a470d-bd0b-4e74-85f9-33ff8767f570","Preservica","VideoObject");
        assertEquals("/kuana-store/bart-access-copies-tv/97/7a/47/977a470d-bd0b-4e74-85f9-33ff8767f570",preservicaVideoPath);
        
        String preservicaAudioPath= KalturaUtil.generateStreamPath("180afe12-f0c2-498e-8d7a-5a0f5fb92cb7","Preservica","AudioObject");
        assertEquals("/kuana-store/bart-access-copies-radio/18/0a/fe/180afe12-f0c2-498e-8d7a-5a0f5fb92cb7",preservicaAudioPath);                        
    }
    
}

