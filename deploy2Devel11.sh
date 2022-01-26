#!/bin/sh

mvn clean package -DskipTests
mv target/ds-datahandler*.war target/ds-datahandler.war

scp target/ds-datahandler.war digisam@devel11:/home/digisam/services/tomcat-apps/

echo "ds-datahandler deployed to devel11"
