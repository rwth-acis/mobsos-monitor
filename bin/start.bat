cd %~dp0
cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;%BASE%/export/jars/*;"

tail -f example.log | java -cp %CLASSPATH% acis.dbis.rwth.mobsos.monitor.Monitor
