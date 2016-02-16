# Layers Box Database Recovery from Backup

The Layers Box database is part of an automatic nightly backup routine. 
This routine uses the technique described in https://dev.mysql.com/doc/refman/5.7/en/innodb-backup.html.
The result is a zipped tar archive including a binary backup of the whole MySQL data folder.
In this document, we describe database recovery from such a binary backup.

## Recovery from Binary Backups

1) Unpack the backup archive. Contents will be available in the subdirectory ./var/lib/mysql of the current directory.

```
tar xvzf mysql-backup-yyyy-mm-dd-hh.tar.xz
```

2) If mysql server is still running, stop it.

```
sudo service mysql stop
```

3) If data of the running server (usually stored in /var/lib/mysql on any Ubuntu-based system) is important, archive and store it in a safe place.

```
sudo tar cvzf /path/to/safe/place/mysql-backup-current.tar.gz /var/lib/mysql
```

4) Remove current data from MySQL data directory.

```
sudo rm -rf /var/lib/mysql
```

5) Copy archive contents to MySQL data directory. Set `mysql` as owner and group recursively.

```
sudo cp ./var/lib/mysql /var/lib
sudo chown -R mysql:mysql /var/lib/mysql
```

6) Start MySQL Server again.

```
sudo service mysql start
```



