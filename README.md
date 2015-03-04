MobSOS Monitor
==

MobSOS Monitor is a tool to collect, filter, enrich, and persist reverse proxy logs recording user interaction with hosted Web services. Authentication to Web services happens with OpenID Connect. Typical filter operations include dropping log entries not interesting for analysis (e.g. access to static content). Geolocation is a typical enrichment operation. Persistence is achieved by storing log data in a relational database.

## Build 

MobSOS Monitor ships with an Apache Ant build file. 
Simply type `ant` on the command line to build.

```
ant
```

## Configuration

### Reverse Proxy Log Format & Database Configuration

The MobSOS Monitor reference implementation assumes the use of openresty (http://openresty.org/) as reverse proxy and MySQL (http://mysql.com) as relational database management system. We provide both nginx log directive and relational database schema for this reference setup.

1) Replace the `log_format` directive your openresty `nginx.conf` by the one in `./etc/openresty-log-directive.md`.
2) Change credentials in `./etc/sql/schema.sql`.
3) Create a database for MobSOS Monitor, e.g. using the following command line:

```
mysql -u... -p... -h... < schema.sql
```

### MobSOS Monitor Configuration

MobSOS Monitor ships with one central configuration file `./etc/conf.properties`. In this file, you can configure database access, OpenID Connect provider, geolocation enrichment and failure mail notification. For geolocation enrichment, MobSOS Monitor uses the free geolocation [IPInfoDB Geolocation JSON API](http://www.ipinfodb.com/ip_location_api_json.php), requiring the registration of an API key. Failure mail notification is optional and requires a mail server accessible to MobSOS Monitor.

1) Configure database access with the fields `jdbcUrl`, `jdbcLogin`, and `jdbcPass`.
2) Configure OpenID Connect Provider with field `oidcProviderUrl`.
3) Configure geolocation enrichment by setting IPInfoDB API key to field `ipinfodbKey`.
4) Optionally, configure settings for the failure notification mailer with fields `mail*`.

## Run

Simply start MobSOS Monitor by passing the path to the reverse proxy's log file to one of the available start scripts.
MobSOS Monitor ships with start scripts for Windows (`./bin/start.bat`) and Linux (`./bin/start.sh`).

```
./bin/start.sh <log-file> (e.g. /usr/local/openresty/nginx/logs/access.log)
```
