#!/bin/bash

# IMPORTANT: This script must be run as root. First enter environment with sudo -s!

echo "Enter mysql root password:"
read pass;

ID=`echo $1 | sed -e 's/-//g' | sed -e 's/^.*mysqlbackup//g' | sed -e 's/[0-9][0-9]\.tar\.xz$//g'`;

echo "Restoring OIDC Backup $ID...";

tar xvzf $1 && 

service mysql stop;

rm -rf /var/lib/mysql;

cp -R ./var/lib/mysql /var/lib/ &&
chown -R mysql:mysql /var/lib/mysql &&
chmod -R 700 /var/lib/mysql &&

service mysql start;

mysqldump -uroot -p$pass --databases OpenIDConnect > dump_${ID}.sql

cat dump_${ID}.sql | sed -e "s/\`OpenIDConnect\`/\`OpenIDConnect_$ID\`/g" > oidc_${ID}.sql
rm dump_${ID}.sql