#! /bin/bash

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${SCRIPTDIR}/../
BASE=${PWD}
export CLASSPATH="${BASE}/lib/*:${BASE}/export/jars/*"

tail -f $1 | java -cp ${CLASSPATH} -Dorg.slf4j.simpleLogger.defaultLogLevel=debug acis.dbis.rwth.mobsos.monitor.Monitor