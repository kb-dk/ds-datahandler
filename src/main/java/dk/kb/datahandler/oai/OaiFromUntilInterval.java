package dk.kb.datahandler.oai;

import dk.kb.datahandler.util.HarvestTimeUtil;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

/**
 * Date interval wrapper class used as dates 'from' and 'until' when harvesting OAi targets 
 * 
 */
public class OaiFromUntilInterval {
    
    private String from;
    private String until;


    /**
     * Interval for harvesting a OAI target. <br>
     * Some OAI servers do not all support UTC format. (KB-COP only accepts day format etc.)
     * From and until must have same granularity. Datestamp formats must be one of the following formats:<br
     * yyyy-MM-dd or yyyy-MM-ddT0mm:hh:ssZ <br>
     * Examples:  2022-07-12T00:00:00Z or 2022-07-12
     * 
     * @param from Datestamp for start of the harvest. Must not be null
     * @param until If null, it will harvest everything until today
     * 
     */
    public OaiFromUntilInterval(String from, String until) throws InvalidArgumentServiceException{  
        validateFormat(from, until);    
    
        this.from=from;
        this.until=until;    
    }


    public String getFrom() {
        return from;
    }


    public void setFrom(String from) {
        this.from = from;
    }


    public String getUntil() {
        return until;
    }

    private void validateFormat(String from, String until) throws InvalidArgumentServiceException{
        if (from == null ) {        
            throw new InvalidArgumentServiceException("From date can not be null");
        }
        if (until != null && (from.length() != until.length()) ) {
            throw new InvalidArgumentServiceException("From and until are in different dateformats. from="+from  +"until="+until);

        }
        //Validate format is one of the two accepted formats.
        if (from.length() == 10) { //Until will have same length
            if (!HarvestTimeUtil.validateDayFormat(from)) {
                throw new InvalidArgumentServiceException("Invalid date format. from="+from);
            }
            if (until != null) {
                if (!HarvestTimeUtil.validateDayFormat(until)) {
                    throw new InvalidArgumentServiceException("Invalid date format. until="+from);
                }          
            }

        }
        else if(from.length() ==20 ) {
            if (!HarvestTimeUtil.validateOaiDateFormat(from)) {
                throw new InvalidArgumentServiceException("Invalid date format. from="+from);
            }
            if (until != null) {
                if (!HarvestTimeUtil.validateOaiDateFormat(until)) {
                    throw new InvalidArgumentServiceException("Invalid date format. until="+from);
                }          
            }         
        }
        else {
            throw new InvalidArgumentServiceException("Date format is not yyyy-MM-dd or yyyy-MM-ddT0mm:hh:ssZ . from="+from);
        }
    }


    @Override
    public String toString() {
        return "OaiFromUntilInterval [from=" + from + ", until=" + until + "]";
    }

    
    
}

