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
package dk.kb.datahandler.oai;

import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.util.DsStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtering and delivery of Preservica OAI records. Generates {@code datasource} prefixed IDs,
 * derives type from {@link DsRecordDto#getId()} and resolves {@code parent} from content.
 */
public class OaiResponseFilterPreservica extends OaiResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(OaiResponseFilterPreservica.class);

    private static final Pattern PARENT_PATTERN = Pattern.compile(
            "<DeliverableUnitRef>([^<]+)</DeliverableUnitRef>");

    /**
     * Pattern for determining if a DeliverableUnit or Manifestation from Preservica 5
     * contains metadata about a radio resource.
     */
    private static final Pattern RADIO_PATTERN = Pattern.compile(
            "<formatMediaType>Sound</formatMediaType>|<ComponentType>Audio</ComponentType>");

    /**
     * Pattern for determining if a DeliverableUnit or Manifestation from Preservica 5
     * contains metadata about a television resource.
     */
    private static final Pattern TV_PATTERN = Pattern.compile(
            "<formatMediaType>Moving\\sImage</formatMediaType>|<ComponentType>Video</ComponentType>");

    /**
     * Pattern to match preservation manifestations from Preservica5 by type.
     */
    private static final Pattern PRESERVATION_MANIFESTATION_PATTERN = Pattern.compile(
            "<ManifestationRelRef>1</ManifestationRelRef>");

    private static final Pattern METADATA_PATTERN = Pattern.compile(
            "<Metadata schemaURI=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">");

    protected int emptyMetadataRecords = 0;

    /**
     * @param datasource source for records. Currently used for {@code origin}.
     * @param storage    destination for records.
     */
    public OaiResponseFilterPreservica(String datasource, DsStorageClient storage) {
        super(datasource, storage);
    }


    /**
     * Add records from Preservica OAI-PMH harvest to ds-storage. Records goes through a filtering where preservica
     * collections are filtered away and not added to ds-storage. Furthermore, types are resolved based on IDs.
     * @param response      OAI-PMH response containing preservica records.
     */
    @Override
    public void addToStorage(OaiResponse response) throws ApiException {
        for (OaiRecord oaiRecord: response.getRecords()) {
            String xml = oaiRecord.getMetadata();
            // Preservica collections are ignored.
            if (oaiRecord.getId().contains("oai:col")){
                continue;
            }
            // DeliverableUnits need to have the PBCore metadata tag. Manifestations does not seem to have it.
            // Therefore, we are checking the ID as well.
            Matcher metadataMatcher = METADATA_PATTERN.matcher(xml);
            if (oaiRecord.getId().contains("oai:du") && !metadataMatcher.find()){
                processed++;
                emptyMetadataRecords ++;
                log.warn("OAI-PMH record '{}' does not contain PBCore metadata and is therefore not added to storage. " +
                        "'{}' empty records have been found and '{}' records have been processed in total.",
                        oaiRecord.getId(), emptyMetadataRecords, processed);
                continue;
            }
            // Manifestations can not be of type 1 as type 1 = preservation manifestations.
            Matcher preservationMatcher = PRESERVATION_MANIFESTATION_PATTERN.matcher(xml);
            if (oaiRecord.getId().contains("oai:man") && preservationMatcher.find()) {
                log.debug("OAI-PMH record '{}' is a preservation manifestation and is not added to DS-storage.",
                            oaiRecord.getId());
                continue;
            }
            addToStorage(oaiRecord);
            processed++;
        }
    }

    @Override
    public String getOrigin(OaiRecord oaiRecord, String datasource) {
        String xml = oaiRecord.getMetadata();

        Matcher radioDeliverableunitMatcher = RADIO_PATTERN.matcher(xml);
        Matcher tvDeliverableUnitMatcher = TV_PATTERN.matcher(xml);

        if (radioDeliverableunitMatcher.find()){
            return "ds.radio";
        } else if (tvDeliverableUnitMatcher.find()){
            return "ds.tv";
        } else {
            log.warn("No specific origin has been extracted for preservica record '{}'.",
                    oaiRecord.getId());
            // Not quite sure what we should do in the case where nothing gets matched.
            return datasource;
        }
    }

    @Override
    public String getParentID(OaiRecord oaiRecord, String origin) {
        String xml = oaiRecord.getMetadata();
        if (!xml.contains("<xip:Manifestation")) {
            return null;
        }

        Matcher m = PARENT_PATTERN.matcher(xml);
        if (!m.find()) {
            log.debug("Unable to resolve parent ID for record '{}'", oaiRecord.getId());
            return null;
        }
        String parentID = m.group(1);
        if (parentID.length() < 30 || parentID.length() > 40) {
            log.warn("ParentID '{}' does not seem to have correct format for record '{}'",
                    parentID, oaiRecord.getId());
        }
        return origin + ":oai:du:" + parentID;
    }

    @Override
    public RecordTypeDto getRecordType(DsRecordDto dsRecord, String storageId) {
        if (storageId.contains("oai:col")){
            return RecordTypeDto.COLLECTION;
        } else if (storageId.contains("oai:du")) {
            return RecordTypeDto.DELIVERABLEUNIT;
        } else if (storageId.contains("oai:man")) {
            return RecordTypeDto.MANIFESTATION;
        }

        log.debug("Unable to derive record type for id '{}' from datasource '{}'. Falling back to '{}'",
                storageId, datasource, RecordTypeDto.DELIVERABLEUNIT);
        return RecordTypeDto.DELIVERABLEUNIT;
    }
}
