package dk.kb.datahandler.util;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.DsRecordMinimalDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;


public class PreservicaUtils {

    private static final Logger log = LoggerFactory.getLogger(PreservicaUtils.class);
    private static final XMLInputFactory factory = XMLInputFactory.newInstance();

    /**
     * Extract Preservica ID for an InformationObject from {@link DsRecordDto} ID.
     * @param dsRecord with an ID in the format ds.tv:oai:io:3006e2f8-3f73-477a-a504-4d7cb1ae1e1c
     * @return a
     */
    public static String getPreservicaIoId(DsRecordMinimalDto dsRecord) {
        String prefix = ":oai:io:";
        int lengthOfPrefix = prefix.length();
        int endOfPrefix = dsRecord.getId().lastIndexOf(prefix);

        return dsRecord.getId().substring(endOfPrefix + lengthOfPrefix);
    }
}
