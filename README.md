# Ds-datahandler(Digitale Samlinger) by the Royal Danish Library. 

## ⚠️ Warning: Copyright Notice
Please note that it is not permitted to download and/or otherwise reuse content from the DR-archive at The Danish Royal Library.
    
## Notice
OAI-PMH harvest is just the first implementation feature in ds-datahandler. More features will implemented later.
    


## OAI harvest 
Ds-datahandler can harvest OAI from different OAI-PMH targets and ingest the metadata for the records into ds-storage. 
Having the metadata in ds-storage makes access much easier and faster for other applications. The metadata for each
record will be UTF-8 encoded before ingested into ds-storage. Invalid XML encoding characters will be replaced or removed.
      
     
## OAI targets configuration
The project yaml-file contains the configuration for each OAI-PMH target.
    
   **The below table describes the parameters to configure a OAI-PMH target**
        
   | Property       | Description                                                                                           |
   |----------------|-------------------------------------------------------------------------------------------------------|
   | name           | The name used to specify when starting a new import                                                   |
   | url            | The base url to OAI-PMH service                                                                       |
   | set            | Parameter to the OAI-server if the server has multiple collections (optional)                         |
   | metadataPrefix | Parameter to the OAI-server to specify the format for the metadata. Options depend on the OAI target. |
   | user           | User if the OAI-server require basic authentication (optional)                                        |
   | password       | Password if the OAI-server require basic authentication (optional)                                    |    
   | origin         | The origin when sending records from this OAI target to ds-storage.                                   |  
   |                | The origin must be configured in DS-storage. ID will be {origin}:{id in OAI record}                   |
   | description    | Human text to give further description if needed                                                      |
    
  
## Full import and delta import
For each OAI target configured you can request a full import or a delta import from the OAI collection. A full import will
harvest all records from to OAI target. A delta import will only import records with a datestamp later than the last record
recieved in either an earlier full import or delta import. 
             
## Storing last harvest datestamp 
The yaml property file specificed a folder on the filesystem to store the last datestamps for each OAI target.
The file will only contain a single line with an UTC timestamp identical to the last record successfully send ds-storage from
that OAI target. 
   
## Configure the yaml property file
Besides all the OAI targets it must also definere the property for the folder to store the datestamps. Also the
properties host,port,baseurl to the ds-storage server to submit the records to.
      
## More about the OAI-PMH protocol
See: [link](http://www.openarchives.org/OAI/openarchivesprotocol.html)
    

## Requirements

* Maven 3                                  
* Java 11

## Setup


## Build & run

Due to license, the following  jar pm-dependency file must be copied to a local repository or .m2-folder.
https://mvnrepository.com/artifact/com.kaltura/KalturaClient/3.3.1

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
The Swagger UI is available at <http://localhost:9071/ds-datahandler/api/>, providing access to both the `v1` version of the GUI. 


## Deployment to a server (development/stage/production).
Install Tomcat9 server 

Configure tomcat with the context enviroment file conf/ocp/ds-datastore.xml. Notice it points to the location on the file system where the yaml and logback file are located.

Edit  conf/ds-datastore.logback.xml

Make a ds-datastore.yaml file. (Make a copy of /conf/ds-datastore-environment.yaml rename it, and edit the properties). 
To ingest into a local ds-storage tomcat server, you need to have ds-storage  running (just mvn jetty:run and change jetty port pom plugin)

## Using a client to call the service 
This project produces a support JAR containing client code for calling the service from Java.
This can be used from an external project by adding the following to the [pom.xml](pom.xml):
```xml
<!-- Used by the OpenAPI client -->
<dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>jackson-databind-nullable</artifactId>
    <version>0.2.2</version>
</dependency>

<dependency>
    <groupId>dk.kb.license</groupId>
    <artifactId>ds-datahandler</artifactId>
    <version>3.0.1-SNAPSHOT</version>
    <type>jar</type>
</dependency>
```
after this a client can be created with
```java
    DsLicenseClient datahandlerClient = new DsLicenseClient("https://example.com/ds-datahandler/v1");
```
During development, a SNAPSHOT for the OpenAPI client can be installed locally by running
```shell
mvn install
```


See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
