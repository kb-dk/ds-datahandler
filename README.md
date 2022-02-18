# Ds-datahandler(Digitale Samlinger) by the Royal Danish Library. 
   
    
## Notice
OAI-PMH harvest is just the first implementation feature in ds-datahandler. More features will implemented later.
    
## OAI harvest 
Ds-datahandler can harvest OAI from different OAI-PMH targets and ingest the metadata for the records into ds-storage. 
Having the metadata in ds-storage makes access much easier and faster for other applications. The metadata for each
record will be UTF-8 encoded before ingested into ds-storage. Invalid XML encoding characters will be replaced or removed.
      
     
## OAI targets configuration
The project yaml-file contains the configuration for each OAI-PMH target.
    
   **The below table describes the parameters to configure a OAI-PMH target**
        
   | Property         | Description                                                                                               |
   | ---------------- | ----------------------------------------------------------------------------------------------------------|
   | name             | The name used to specify when starting a new import                                                       |
   | url              | The base url to OAI-PMH service                                                                           |
   | set              | Parameter to the OAI-server if the server has multiple collections (optional)                             |
   | metadataPrefix   | Parameter to the OAI-server to specify the format for the metadata. Options depend on the OAI target.     |
   | user             | User if the OAI-server require basic authentication (optional)                                            |
   | password         | Password if the OAI-server require basic authentication (optional)                                        |    
   | recordBase       | The recordbase when sending records from this OAI target to ds-storage.                                   |  
   |                  | The recordbase must be configured in DS-storage. ID will be {recordbase}:{id in OAI record}               |
   | description      | Human text to give further description if needed                                                          |
    
  
## Full import and delta import
For each OAI target configured you can request a full import or a delta import from the OAI collection. A full import will
harvest all records from to OAI target. A delta import will only import records with a datestamp later than the last record
recieved in either an earlier full import or delta import. 
             
## Storing last harvest datestamp 
The yaml property file specificed a folder on the filesystem to store the last datestamps for each OAI target.
The file will only contain a single with an UTC timestamp identical to the last record successfully send ds-storage from
that OAI target. 
   
## Configure the yaml property file
Besides all the OAI targets it must also definere the property for the folder to store the datestamps. Also the
properties host,port,baseurl to the ds-storage server to submit the records to.
      
## More about the OAI-PMH protocol
See: http://www.openarchives.org/OAI/openarchivesprotocol.html
    

## Requirements

* Maven 3                                  
* Java 11

## Setup


## Build & run

Build with
``` 
mvn package
```

## Setup required to run the project local 
Create local yaml-file: Take a copy of 'ds-datastore-behaviour.yaml'  and name it'ds-datastore-environment.yaml'
Edit the  timestamps.folder variable to where you want the timestamps store. If the folder does not exist it will be created. 
 

Test the webservice with
```
mvn jetty:run
```
## Swagger UI
The Swagger UI is available at <http://localhost:8080/ds-datahandler/api/>, providing access to both the `v1` and the  `devel` versions of the GUI. 


## Deployment to a server (development/stage/production).
Install Tomcat9 server 

Configure tomcat with the context enviroment file conf/ocp/ds-datastore.xml. Notice it points to the location on the file system where the yaml and logback file are located.

Edit  conf/ds-datastore.logback.xml

Make a ds-datastore.yaml file. (Make a copy of /conf/ds-datastore-environment.yaml rename it, and edit the properties). 

 

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
