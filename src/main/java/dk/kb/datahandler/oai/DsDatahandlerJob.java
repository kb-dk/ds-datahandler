package dk.kb.datahandler.oai;

import java.util.Objects;

import dk.kb.datahandler.model.v1.OaiTargetDto;

public class DsDatahandlerJob {
    
    private OaiTargetDto dto;
    private long id;
    private long completedTime=0L;
    private STATUS status=STATUS.RUNNING;
    private boolean error =false;
    private int recordsHarvested=0;
    
    enum STATUS {
        RUNNING,
        COMPLETED        
      }
    
    public DsDatahandlerJob (long id, OaiTargetDto dto) {        
        this.id=id;        
        this.dto=dto;
    }

    public long getId() {
        return id;
    }

    public OaiTargetDto getDto() {
        return dto;
    }
  
    public void setId(long id) {
        this.id = id;
    }
  
    public boolean isError() {
        return error;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public int getRecordsHarvested() {
        return recordsHarvested;
    }

    public void setRecordsHarvested(int recordsHarvested) {
        this.recordsHarvested = recordsHarvested;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public long getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(long completedTime) {
        this.completedTime = completedTime;
    }

    @Override
    public String toString() {
        return "OaiTargetJob [id=" + id + ", status=" + status + ", error=" + error + ", recordsHarvested="
                + recordsHarvested + "]";
    }




}
