package dk.kb.datahandler.util;

import dk.kb.datahandler.preservica.AccessResponseObject;
import dk.kb.datahandler.preservica.PreservicaManifestationExtractor;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.util.DsStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PreservicaUtils {

    private static final Logger log = LoggerFactory.getLogger(PreservicaUtils.class);

    /**
     * Initialize a {@link PreservicaManifestationExtractor} which fetches a presentation manifestation through the
     * Preservica 7 APIs and creates a DsRecord for it with the original record as its parent record.
     * @param record to get manifestation for.
     * @return the newly created child record with the ID of the original record as its parent.
     */
    public static DsRecordDto fetchManifestation(DsRecordDto record, PreservicaManifestationExtractor plugin, AtomicInteger counter, AtomicLong currentTime) {
        counter.getAndIncrement();

        if (counter.get() % 200 == 0){
            log.info("200 Records have been updated in '{}' milliseconds. In total '{}' records have been processed.",
                    System.currentTimeMillis() - currentTime.get(), counter.get());
            currentTime.set(System.currentTimeMillis());
        }
        record = plugin.apply(record);

        return record;
    }

    /**
     * Streaming wrapper for the recordPost method of the {@link DsStorageClient}
     * @param storageClient to post the record to.
     * @param record to post.
     */
    public static void safeRecordPost(DsStorageClient storageClient, DsRecordDto record) {
        try {
            storageClient.recordPost(record);
        } catch (ApiException e) {
            log.error("ApiException has been thrown. Record probably already exists.");
        }
    }

    /**
     * Extract Preservica ID for an InformationObject from {@link DsRecordDto} ID.
     * @param dsRecord with an ID in the format ds.tv:oai:io:3006e2f8-3f73-477a-a504-4d7cb1ae1e1c
     * @return a
     */
    public static String getPreservicaIoId(DsRecordDto dsRecord) {
        String prefix = ":oai:io:";
        int lengthOfPrefix = prefix.length();
        int endOfPrefix = dsRecord.getId().lastIndexOf(prefix);

        return dsRecord.getId().substring(endOfPrefix + lengthOfPrefix);
    }

    /**
     * Filter records for validity.
     * @param record to ensure has values correctly set.
     * @return true if record is valid.
     */
    public static boolean validateRecord(DsRecordDto record) {
        return record.getReferenceId() != null && !record.getReferenceId().isEmpty();
    }
}
