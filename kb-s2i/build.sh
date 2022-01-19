#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/ds-datahandler-*.war "$TOMCAT_APPS/ds-datahandler.war"
cp -- /tmp/src/conf/ocp/ds-datahandler.xml "$TOMCAT_APPS/ds-datahandler.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/ds-datahandler.war")
