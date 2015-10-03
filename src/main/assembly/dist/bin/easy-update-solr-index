#!/usr/bin/env bash

BINPATH=`readlink -f $0`
APPHOME=`dirname \`dirname $BINPATH \``

java -Dlogback.configurationFile=$APPHOME/cfg/logback.xml \
     -Dapp.home=$APPHOME \
     -jar $APPHOME/bin/easy-update-solr-index.jar $@
