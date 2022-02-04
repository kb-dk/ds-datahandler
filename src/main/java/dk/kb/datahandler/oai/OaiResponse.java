package dk.kb.datahandler.oai;

import java.util.ArrayList;

public class OaiResponse {

    
    private ArrayList<OaiRecord> records = new  ArrayList<OaiRecord> (); 
    private String resumptionToken = null;
    private String totalRecords="0";// can be "?" if unknown
    
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
    public String getTotalRecords() {
        return totalRecords;
    }
    public void setTotalRecords(String totalRecords) {
        this.totalRecords = totalRecords;
    }
    
    
    

}

