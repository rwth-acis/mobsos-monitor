MobSOS Monitor
==

MobSOS Monitor serves as framework to process and persist access logs provided by a reverse proxy.

The following figure depicts the conceptual setup.

```
\ client \<------>\ reverse-proxy \
                    \ tail -n0 -f logs/access.log \------>\ mobsos-monitor 
                                                        \------>\ mobsos-db \
                                                        \------>\ mobsos-trigger \
```

A client accesses Web services through a reverse proxy over HTTP. The reverse proxy usually produces logs. These logs are then piped directly into MobSOS Monitor, which in turn filters, enriches, and finally persists log data for further analysis. Typical filter operations include dropping log entries not interesting for analysis (e.g. access to static content). Typical enrichment operations include retrieval of geolocation data for logged IP addresses. 

## Build Instructions

```
ant all
```

TODO: more detailed build instructions

## Configuration

## Reverse Proxy Log Format

The expected log format is a CSV format consisting of the typical fields available from HTTP logs. Below you find a sample log directive as used in nginx. 

```
log_format main '"$time_iso8601","$remote_addr","$host","$request_method","$uri","$status","$http_referer","$http_user_agent","$request_length","$bytes_sent","$request_time","$args",';
```

## Run

```
./bin/start.sh <log-file>
```

## Reference Setup

TODO: describe reference setup with nginx as reverse proxy.


