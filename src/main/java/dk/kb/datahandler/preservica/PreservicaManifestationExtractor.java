package dk.kb.datahandler.preservica;

import dk.kb.datahandler.preservica.client.DsPreservicaClient;
import dk.kb.datahandler.util.PreservicaUtils;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 *
 */
public class PreservicaManifestationExtractor {
    private static final Logger log = LoggerFactory.getLogger(PreservicaManifestationExtractor.class);
    private final String filenameField = "cmis:contentStreamFileName";

    /**
     * Apply the extractor to a DsRecord.
     * If the record is a DeliverableUnit (A record containing metadata) the ID of the manifestation, which the record
     * is about will be resolved from the backing Preservica instance and added as a referenceId in the record it has been applied to.
     * @param dsRecord DeliverableUnit to fetch manifestation from.
     */
    public DsRecordMinimalDto apply(DsRecordMinimalDto dsRecord) {
        try {
            // Get the clean Preservica InformationObject ID.
            String preservicaIoID = PreservicaUtils.getPreservicaIoId(dsRecord);
            // Extract filename from Preservica and create a prefixed version for DS.
            String filename = DsPreservicaClient.getInstance().getFileRefFromInformationObjectAsStream(preservicaIoID);

            // Update the record that is to be returned.
            if (!filename.isEmpty()){
                dsRecord.setReferenceId(filename);
            }
        } catch (IOException e) {
            log.error("Manifestation could not be extracted. mTime of record in hand at this point is: '{}'. PreservicaManifestationExtractor threw the " +
                            "following exception: ", dsRecord.getmTime(), e);
        } catch (InterruptedException e) {
            log.error("The thread applying the PreservicaManifestationExtractor to record with id: '{}', was interrupted while waiting on a timeout from Preservica. mTime of the" +
                            " record in hand is: '{}'", dsRecord.getId(), dsRecord.getmTime());
            throw new InternalServiceException("The thread applying the PreservicaManifestationExtractor to record with id: '" + dsRecord.getId()+ "', was interrupted while waiting on " +
                    "a timeout from Preservica. mTime of the record in hand is: " + dsRecord.getmTime());
        }

        return dsRecord;
    }

    /**
     * Initialize the Preservica Manifestation Plugin. This constructor extracts all needed endpoints from config files
     * and gets the first accessToken from the backing preservica installation.
     * Furthermore, it starts a timer, which updates the accesToken every 14th minute, by exchanging a refreshToken.
     */
    public PreservicaManifestationExtractor() throws IOException {
        DsPreservicaClient.getInstance();
    }

}
