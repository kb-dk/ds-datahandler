package dk.kb.datahandler.util;

import dk.kb.datahandler.oai.plugins.PreservicaManifestationPlugin;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.util.DsStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PreservicaUtils {

    private static final Logger log = LoggerFactory.getLogger(PreservicaUtils.class);

    /**
     * Validate that the given record is a Preservica 7 InformationObject by ID.
     * @param dsRecord to validate
     * @return true if record is an InformationObject, otherwise return false.
     */
    public static boolean isInformationObject(DsRecordDto dsRecord) {
        String id = dsRecord.getId();
        // Check if the id contains ':oai:io:' as this is what characterises InformationObjects from Preservica 7.
        return id != null && id.contains(":oai:io:");
    }

    /**
     * Initialize a {@link PreservicaManifestationPlugin} which fetches a presentation manifestation through the
     * Preservica 7 APIs and creates a DsRecord for it with the original record as its parent record.
     * @param record to get manifestation for.
     * @return the newly created child record with the ID of the original record as its parent.
     */
    public static DsRecordDto fetchManifestation(DsRecordDto record, PreservicaManifestationPlugin plugin) {
        plugin.apply(record);

        return PreservicaManifestationPlugin.createdRecord;
    }

    /**
     * Streaming wrapper for the recordPost method of the {@link DsStorageClient}
     * @param storageClient to post the record to.
     * @param record to post.
     * @return the posted record for further streaming.
     */
    public static DsRecordDto safeRecordPost(DsStorageClient storageClient, DsRecordDto record, AtomicInteger counter, AtomicLong currentTime) {
        counter.getAndIncrement();

        if (counter.get() % 10 == 0){
            log.info("10 Records have been updated in '{}' milliseconds.", System.currentTimeMillis() - currentTime.get());
            currentTime.set(System.currentTimeMillis());
        }

        try {
            storageClient.recordPost(record);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        return record;
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

    public static boolean needsChildrenIds(DsRecordDto dsRecordDto) {
        return dsRecordDto.getChildrenIds() == null || dsRecordDto.getChildrenIds().isEmpty() || dsRecordDto.getChildrenIds().get(0) == null;
    }
}
