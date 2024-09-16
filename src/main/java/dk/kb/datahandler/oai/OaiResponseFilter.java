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


/**
 * Filtering and delivery of OAI records. public implementation generates {@code datasource} prefixed ID,
 * sets type to {@link RecordTypeDto#DELIVERABLEUNIT} and does not resolve {@code parent}.
 */
public class OaiResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(OaiResponseFilter.class);

    protected final DsStorageClient storage;
    protected final String datasource;
    protected int processed = 0;

    /**
     * @param datasource source for records. Default implementation uses this for {@code origin}.
     * @param storage    destination for records.
     */
    public OaiResponseFilter(String datasource, DsStorageClient storage) {
        this.storage = storage;
        this.datasource = datasource;
    }

    /**
     * Add records from OAI-PMH harvest to ds-storage. The public implementation does not resolve parent and
     * sets the type to {@link RecordTypeDto#DELIVERABLEUNIT}.
     * @param response      OAI-PMH response containing records.
     */
    public void addToStorage(OaiResponse response) throws ApiException {
        for (OaiRecord oaiRecord: response.getRecords()) {
            addToStorage(oaiRecord);
            processed++;
        }
    }

    /**
     * Add record from an OAI-PMH harvest to ds-storage. The public implementation does not resolve parent and
     * sets the type to {@link RecordTypeDto#DELIVERABLEUNIT}.
     * @param oaiRecord     a record from an OAI-PMH response
     */
    public void addToStorage(OaiRecord oaiRecord) throws ApiException {
        String origin = getOrigin(oaiRecord, datasource);
        String storageId = origin + ":" + oaiRecord.getId();
        if (oaiRecord.isDeleted()) {
            storage.markRecordForDelete(storageId);
        } else if (origin.isEmpty()){
           log.warn("OAI Record with ID: '{}', has empty origin, it is not added to DS-Storage.", oaiRecord.getId());
        }
        else {
            String parentID = getParentID(oaiRecord, origin);
            addOrUpdateRecord(oaiRecord, storageId, parentID, origin);
        }
    }

    /**
     * Resolve the {@code origin} for the given {@code oaiRecord} from the given {@code datasource}.
     * <p>
     * public behaviour is to return the {@code datasource}.
     * @param oaiRecord     a record from an OAI-PMH response
     * @param datasource    where the harvested OAI-PMH record is extracted from.
     * @return {@code origin} for the given {@code oaiRecord}.
     */
    public String getOrigin(OaiRecord oaiRecord, String datasource) {
        return datasource;
    }

    /**
     * Derives {@code parent} record from the given {@code oaiRecord}.
     * @param oaiRecord  the record itself.
     * @param origin     the origin for the record.
     * @return the {@code parent} for {@code oaiRecord} or {@code null} if no parent could be derived.
     */
    public String getParentID(OaiRecord oaiRecord, String origin) {
        return null;
    }

    /**
     * Define RecordType from id. public implementation sets the type to {@link RecordTypeDto#DELIVERABLEUNIT}.
     * @param dsRecord to specify recordType for.
     * @param storageId used to define the type of record.
     * @return record type for the {@code dsRecord}.
     */
    public RecordTypeDto getRecordType(DsRecordDto dsRecord, String storageId) {
        return RecordTypeDto.DELIVERABLEUNIT;
    }

    /**
     * @return the number of processed oaiRecords.
     */
    public int getProcessed() {
        return processed;
    }

    /**
     * Add or update record in ds-storage.
     * @param oaiRecord record from OAI-PMH to ingest to ds-storage.
     * @param storageId id given to the record in ds-storage.
     * @param origin    the origin for the record.
     */
    private void addOrUpdateRecord(OaiRecord oaiRecord, String storageId, String parentID, String origin)
            throws ApiException {
        DsRecordDto dsRecord = new DsRecordDto();
        dsRecord.setId(storageId);
        dsRecord.setOrigin(origin);
        dsRecord.setParentId(parentID); // Does not matter if null is set
        dsRecord.setData(oaiRecord.getMetadata());
        dsRecord.setRecordType(getRecordType(dsRecord, storageId));
        storage.recordPost(dsRecord);
    }
}
