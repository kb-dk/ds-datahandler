# ds-datahandler


Developed and maintained by the Royal Danish Library.

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
