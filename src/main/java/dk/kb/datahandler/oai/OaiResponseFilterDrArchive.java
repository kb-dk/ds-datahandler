package dk.kb.datahandler.oai;

import dk.kb.datahandler.enrichment.DataEnricher;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.util.DsStorageClient;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OaiResponseFilterDrArchive extends OaiResponseFilterPreservicaSeven{
    private static final Logger log = LoggerFactory.getLogger(OaiResponseFilterDrArchive.class);
    public static int nonDrRecords = 0;

    private String fragmentServiceUrl = null;

    protected static final Pattern DR_PATTERN = Pattern.compile(
            ">(?i:dr)[^<]*</(?:\\w+:)?publisher>");

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
    public void addToStorage(OaiResponse response) throws ApiException {
        for (OaiRecord oaiRecord: response.getRecords()) {
            String xml = oaiRecord.getMetadata();
            String recordId = oaiRecord.getId();
            // Preservica StructuralObjects are ignored as they are only used as folders in the GUI.
            if (recordId.contains("oai:so")){
                log.debug("Skipped Structural object with id: '{}'", recordId);
                continue;
            }

            // Filter out material that are not send on DR channels
            Matcher drMatcher = DR_PATTERN.matcher(xml);
            if (!drMatcher.find()){
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
            Matcher metadataMatcher = METADATA_PATTERN.matcher(xml);
            if ((recordId.contains("oai:io")) && !metadataMatcher.find()) {
                processed++;
                emptyMetadataRecords ++;
                log.warn("OAI-PMH record '{}' does not contain PBCore metadata and is therefore not added to storage. " +
                                "'{}' empty records have been found and '{}' records have been processed in total.",
                        recordId, emptyMetadataRecords, processed);
                continue;
            }

            Matcher transcodingDoneMatcher = TRANSCODING_PATTERN.matcher(xml);
            if (!transcodingDoneMatcher.find()) {
                processed++;
                transCodingNotDoneRecords++;
                log.debug("OAI-PMH record '{}' transcoding status not done. Record skipped",recordId);
                if (transCodingNotDoneRecords % 1000 == 0) {
                    log.info("'{}' records transcoding status not done filtered away. '{}' records have been processed.",
                            transCodingNotDoneRecords, processed);
                }
                continue;
            }

            // Enrich record
            if (!StringUtil.isEmpty(fragmentServiceUrl) && "ds.tv".equals(getOrigin(oaiRecord,datasource))) {
                    DataEnricher.apply(fragmentServiceUrl,oaiRecord);
            }

            try {
                addToStorage(oaiRecord);
                processed++;
            } catch (ApiException e){
                log.warn("DsStorage threw an exception when adding OAI record from Preservica 7 to storage.");
                throw e;
            }
        }
    }
}
