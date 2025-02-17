/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.datahandler.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.config.ServiceConfig;


/**
 * Simple verification of client code generation. Integration test, will not be run by automatic build flow
 */
@Tag("integration")
public class DsDatahandlerClientTest {
    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerClientTest.class);
       
    private static String dsDatahandlerDevel=null;
        
    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml","ds-datahandler-integration-test.yaml");                        
            dsDatahandlerDevel= ServiceConfig.getConfig().getString("integration.devel.datahandler");        
        } catch (IOException e) {          
          e.printStackTrace();
            log.error("Integration yaml 'ds-datahandler-integration-test.yaml' file most be present. Call 'kb init'");            
            fail();
        }
    }
           
}
