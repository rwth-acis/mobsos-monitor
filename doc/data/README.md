## Layers Box Log Data Reconstruction

In this section, we provide documentation on routines for data recovery on the logs collected for Layers Box.
Data reconstruction becomes necessary if MobSOS Monitor has not reliably persisted requests for a longer time.
The helper scripts described in the following aid the data reconstruction process.

### OpenID Connect Database Recovery

Full backups of the Layers Box MySQL database server are generated every night as files 
mysql-backup-yyyymmdd.tar.xz. 
The backup includes a database schema OpenIDConnect, maintained by a MITRE ID Connect server. 
Unfortunately, tarball contents represent a binary database server backup instead of a much more migrateable dump.
For this case, we provide a script performing a conversion from binary backup to dump for all OpenIDConnect databases.
In order to later maintain multiple versions of the same OpenIDConnect database at the same time, the script renames the
database schema to OpenIDConnect_yyyyddmm in the resulting dump. The resulting dump is produced in the output file
oidc_yyyymmdd.sql.

NOTE: the recovery described here creates dumps of multiple versions of the same database. This step is a necessary 
      preparation for token reconstruction, as described in the next section. The script must be executed as root, as
      many copy operations require appropriate permissions. Prefixing the script call with sudo will not work!


```
sudo -s
<... then in root shell...>
./bin/oidc-restore.sh mysql-backup-20160216.tar.xz
head -n40 oidc_20160216.sql 
``` 

### Token Reconstruction

During regular operation, MobSOS Monitor exchanges logged tokens for an assignment of a request to the respective user and the used client.
However, if full data persistence was down for a certain time, tokens might have expired and removed from the OpenID Connect database.
Hence, the assignment of a token to a client and an authentication holder (i.e. an end-user) is not reconstructable anymore. 
However, tokens might have still existed in previous backups of the same database.
For token reconstruction, we simply create dumps for multiple versions of the same OpenID Connect database over time. 
From each dump we collect respective token assignments and merge information from all dumps in two CSV files:

* final_access_token.csv - All collected access token assignments to clients and authentication holders (format: id;token_value;client_id;auth_holder_id)
* final_authentication_holder.csv - All collected authentication holders resolved to a user name (format: name, id)

Assumed, the current directory contains a set of OpenID Connect database dumps named oidc_yyyymmdd.sql (e.g. created in the context of OpenID Connect 
Database Recovery; cf. previous section). 

```
./bin/auth-ext.sh
head merged_access_token.csv
head merged_authentication_holder.csv
```

### MobSOS Monitor Log Data Reconstruction

During regular operation, we pipe the results of an interactive `tail -n0 -f /path/to/nginx/access.log` into MobSOS Monitor. In reconstruction mode, 
we emulate this pipeline by concatenating the lost portions of an original nginx access.log file to another buffer file that is in turn tailed into 
MobSOS Monitor. In reconstruction mode, we furthermore do not exchange logged tokens at the OpenID Connect server's user info endpoint, but make use
of the reconstructed token assignments described in the previous sections.

In the following, we assume that access.log describes a complete log that should be reconstructed into a fresh MobSOS Monitor log database.
We thus start with respective preparations:

1) Use the predefined schema DDL definition to create a new MobSOS Monitor log database or reset an existing one (CAUTION!!!).

```
mysql -uroot -p ${MOBSOS_MONITOR}/etc/sql/schema.sql
```

2) In MySQL Workbench navigate to table `mobsos_logs.log`.
3) Right Click -> "Table Data Import Wizard"
4) In wizard, select path to `merged_access_token.csv`, then "create new table" and follow instructions.
5) Repeat steps 3) & 4) for `merged_authentication_holder.csv`.

With a prepared database, we can then begin to reproduce log data entries from a log file.

First, we start MobSOS Monitor, listening to the intermediary buffer file:
```
${HOME_MOBSOS_MONITOR}/bin/start.sh /path/to/buffer
```

Then, we use a script for delayed concatenation to the buffer file.
```
./bin/catdel.sh /path/to/original/access.log /path/to/buffer
```

For larger files, reproduction can take a while. The whole process of pumping data through the MobSOS Monitor pipeline can
be monitored by interactively tailing MobSOS Monitor's own log file.

```
tail -f ${MOBSOS_MONITOR}/log/monitor.log
```