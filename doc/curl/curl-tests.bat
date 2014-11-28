:: MobSOS Monitor - Generate Logs with curl
:: ===============================================

set ep=http://steen.informatik.rwth-aachen.de:9999/mobsos-surveys
set cred=-9147513385456075294:userAPass

for /L %%N in (1,1,5) curl -v -X GET %ep%/questionnaires -H "Accept: text/html" -H "Accept-Language: en-US" --user %cred%

curl -v -X POST %ep%/surveys --user %cred% -H "Content-Type: application/json" -d "{\"name\":\"Survey %RANDOM%\",\"description\":\"A survey.\",\"resource\":\"http://nillenposse.de\",\"organization\":\"Nillenposse\",\"logo\":\"http://nillenposse.de/pics/slogo.jpg\",\"start\":\"2014-05-20T00:00:00Z\",\"end\":\"2014-06-20T23:59:59Z\",\"lang\":\"de-DE\"}";

