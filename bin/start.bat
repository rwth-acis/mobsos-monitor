cd %~dp0
cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;%BASE%/export/jars/*;"

:: tail -n10 -f %1 | java -cp %CLASSPATH% -Dorg.slf4j.simpleLogger.defaultLogLevel=debug acis.dbis.rwth.mobsos.monitor.Monitor
cat %1 | java -cp %CLASSPATH% -Dorg.slf4j.simpleLogger.defaultLogLevel=debug acis.dbis.rwth.mobsos.monitor.Monitor