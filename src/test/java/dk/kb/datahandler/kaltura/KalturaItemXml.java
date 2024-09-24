package dk.kb.datahandler.kaltura;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

public class KalturaItemXml {
    
    private int type;
    private String referenceId;
    private String name;
    private String description;
    private String tag1;
    private String tag2;
    private String tag3;
    private int mediaType;
    private int conversionProfileId;
    private int flavorParamsId;
    private String downloadUrl;
    private String migratedFrom; 
    
    public KalturaItemXml(int type, String referenceId, String name, String description, String migratedFrom,String tag1, String tag2, String tag3, int conversionProfileId, int mediaType,
            int flavorParamsId, String downloadUrl) {        
        
        if (type<=0 || referenceId == null || name== null || description == null
            || tag1==null  || tag2==null || tag3==null ||  conversionProfileId<=0 ||  mediaType <=0
            || flavorParamsId <=0 ||  downloadUrl== null) { //migrated from can be null            
            throw new InvalidArgumentServiceException("All parameters must be defined");            
        }
        
        
        this.type = type;
        this.referenceId = referenceId;
        this.name = name;
        this.description = description;
        this.tag1 = tag1;
        this.tag2 = tag2;
        this.tag3 = tag3;
        this.conversionProfileId=conversionProfileId;
        this.mediaType = mediaType;
        this.flavorParamsId = flavorParamsId;
        this.downloadUrl = downloadUrl;
        this.migratedFrom=migratedFrom;
    }

    public int getType() {
        return type;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTag1() {
        return tag1;
    }

    public String getTag2() {
        return tag2;
    }

    public String getTag3() {
        return tag3;
    }

    public int getMediaType() {
        return mediaType;
    }

    public int getFlavorParamsId() {
        return flavorParamsId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }      
    
    public int getConversionProfileId() {
        return conversionProfileId;
    }    
    public String getMigratedFrom() {
        return migratedFrom;
    }    
}
