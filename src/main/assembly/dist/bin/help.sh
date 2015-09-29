#!/bin/sh

# the next sequence of commands sould work in a development environment:
# mvn clean install ; tar -zxvf target/*.tar.gz ; mv *-SNAPSHOT target ; */*/bin/help.sh

java -Dlogback.configurationFile=$(dirname $0)/../cfg/logback.xml \
     -jar $(dirname $0)/../bin/easy-update-solr-index.jar $@
