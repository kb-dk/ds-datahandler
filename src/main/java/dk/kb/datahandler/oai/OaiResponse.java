package dk.kb.datahandler.oai;

import java.util.ArrayList;

public class OaiResponse {

    
    private ArrayList<OaiRecord> records = new  ArrayList<OaiRecord> (); 
    private String resumptionToken = null;
    private long totalRecords=0;
    
    public ArrayList<OaiRecord> getRecords() {
        return records;
    }
    public void setRecords(ArrayList<OaiRecord> records) {
        this.records = records;
    }
    public String getResumptionToken() {
        return resumptionToken;
    }
    public void setResumptionToken(String resumptionToken) {
        this.resumptionToken = resumptionToken;
    }
    public long getTotalRecords() {
        return totalRecords;
    }
    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }
    
    
    

}

