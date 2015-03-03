@echo off

cd %~dp0
cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;%BASE%/export/jars/*;"

if not exist log mkdir log

tail -n0 -f %1 | java -cp %CLASSPATH% -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dorg.slf4j.simpleLogger.logFile=log\monitor.log -Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.dateTimeFormat="yyyy-MM-dd HH:mm:ss" acis.dbis.rwth.mobsos.monitor.Monitor