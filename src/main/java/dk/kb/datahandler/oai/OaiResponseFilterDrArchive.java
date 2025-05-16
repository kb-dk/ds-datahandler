package dk.kb.datahandler.oai;

import dk.kb.datahandler.enrichment.DataEnricher;
import dk.kb.datahandler.util.PreservicaOaiRecordHandler;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;

import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class OaiResponseFilterDrArchive extends OaiResponseFilterPreservicaSeven{
    private static final Logger log = LoggerFactory.getLogger(OaiResponseFilterDrArchive.class);
    public static int nonDrRecords = 0;

    private String fragmentServiceUrl = null;


    /**
     * @param datasource source for records. Default implementation uses this for {@code origin}.
     * @param storage    destination for records.
     */
    public OaiResponseFilterDrArchive(String datasource, DsStorageClient storage) {
        super(datasource, storage);
    }

    public OaiResponseFilterDrArchive(String datasource, DsStorageClient storage, String fragmentServiceUrl) {
        super(datasource, storage);
        this.fragmentServiceUrl = fragmentServiceUrl;
    }

    /**
     * Add records from Preservica OAI-PMH harvest to ds-storage if the record has been sent on a DR channel.
     * Records goes through a filtering where StructuralObjects from Preservica are filtered away and not added
     * to ds-storage. Furthermore, types are resolved based on IDs and lastly it is checked that the record has been
     * aired on a channel owned by DR.
     * @param response      OAI-PMH response containing preservica records.
     */
    @Override
    public void addToStorage(OaiResponse response) throws ServiceException {
        SAXParser saxParser = getSaxParser();

        for (OaiRecord oaiRecord: response.getRecords()) {
            String recordId = oaiRecord.getId();
            // Preservica StructuralObjects are ignored as they are only used as folders in the GUI.
            if (recordId.contains("oai:so")){
                log.debug("Skipped Structural object with id: '{}'", recordId);
                continue;
            }

            PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();
            try {
                InputStream inputXml = new ByteArrayInputStream(oaiRecord.getMetadata().getBytes(StandardCharsets.UTF_8));
                saxParser.parse(inputXml, handler);
            } catch (SAXException | IOException e) {
                throw new InternalServiceException(e);
            }

            // Filter out material that are not send on DR channels
            if (!handler.recordIsDr){
                processed++;
                nonDrRecords++;
                // Periodically logging of how many records have been filtered out.
                if (nonDrRecords % 1000 == 0) {
                    log.info("The DR filter has filtered '{}' records away. '{}' records have been processed.",
                            nonDrRecords, processed);
                }
                continue;
            }

            // InformationObjects from preservica 7 need to have the PBCore metadata tag.
            if (!informationObjectContainsPbcoreBoolean(handler, recordId)){
                continue;
            }

            String origin = getOrigin(oaiRecord, datasource, handler);

            try {
                addToStorage(oaiRecord, origin,handler.fileReference);
                processed++;
            } catch (ServiceException e){
                log.warn("DsStorage threw an exception when adding OAI record from Preservica 7 to storage.");
                throw e;
            }
        }
    }
}
