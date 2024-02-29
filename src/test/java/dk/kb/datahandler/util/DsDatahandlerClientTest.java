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

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.invoker.v1.ApiException;
import dk.kb.datahandler.model.v1.OaiTargetDto;

/**
 * Simple verification of client code generation. Integration test, will not be run by automatic build flow
 */
public class DsDatahandlerClientTest {
    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerClientTest.class);
       
    @Tag("integration")
    @Test
    public void testInstantiation() throws ApiException{
        String backendURIString = "http://devel11:10001/ds-datahandler/v1";
        log.debug("Creating inactive client for ds-datahandler with URI '{}'", backendURIString);
        DsDatahandlerClient dsDatahandlerClient = new DsDatahandlerClient(backendURIString);
        List<OaiTargetDto> targets = dsDatahandlerClient.getOaiTargetsConfiguration();
        log.info("Integrationtest called oaiTargets on devel11. Number of targets:"+targets.size());
        assertTrue(targets.size() >0);                
    }
}
