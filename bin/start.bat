cd %~dp0
cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*"

java -cp %CLASSPATH% acis.dbis.rwth.mobsos.monitor.Monitor
pause
