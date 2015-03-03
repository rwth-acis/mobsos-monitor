#! /bin/bash

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${SCRIPTDIR}/../
BASE=${PWD}
export CLASSPATH="${BASE}/lib/*:${BASE}/export/jars/*"

if [ ! -d ./log ] 
then
    mkdir ./log
fi

tail -n0 -f $1 | java -cp ${CLASSPATH} -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dorg.slf4j.simpleLogger.logFile=log\monitor.log -Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.dateTimeFormat="yyyy-MM-dd HH:mm:ss" acis.dbis.rwth.mobsos.monitor.Monitor