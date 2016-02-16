#!/bin/bash

pass=bYLTCgtNgSk7EMcqzhSLx5FftcQpnmQV;

declare -a arr=("20150617" "20150622" "20150629" "20150811" "20150813" "20150916" "20151015" "20151115" "20151215" "20160115" "20160212" "20160215");

echo "" > all_access_token.csv;
echo "" > all_authentication_holder.csv;

#for i in "${arr[@]}"
for i in `ls --literal | grep oidc_.*sql | sed -e "s/oidc_//g;s/\.sql$//g"`
do
	
	# IMPORTANT: Every oidc_yyyymmdd.sql file is expected to set a database name OpenIDConnect_yyyymmdd internally. 
	#            Only then, two different versions of the same database can be used simultaneously.
	#			 If this is not the case, then use the line below to make sure this condition is met.
	# 
    # cat dump_${i}.sql | sed -e "s/\`OpenIDConnect\`/\`OpenIDConnect_$i\`/g" > oidc_${i}.sql;
	
	
	echo "Pumping OIDC from Dump $i to Server..."
	# pump OIDC database DDL and values to server
	mysql -uroot -p$pass < oidc_${i}.sql;

	echo "Querying Access Tokens from Dump $i"
	# extract all available access tokens and store ID, token value, and client/auth-holder IDs ín CSV
	mysql -N -u root -p$pass -e "select id,token_value,client_id,auth_holder_id from OpenIDConnect_$i.access_token;" |\
	sed -e 's/\t/;/g' | grep -v "^[[:space:]]*$" >> all_access_token.csv

	echo "Querying Authentication Holders from Dump $i"
	# extract all names in auth-holders
	mysql -N -u root -p$pass -e "select convert(authentication USING latin1),id from OpenIDConnect_$i.authentication_holder;" |\
	grep "dc=learning-layers" |\
	sed "s/.*\\0//g" |\
	grep -v -e '^[[:space:]]$' |\
	grep -v -e '^[0-9]*$' |\
	sed -e 's/\t/;/g' |\
	sed -e 's/[^A-Za-z0-9._-;]//g' >> all_authentication_holder.csv

done

# remove all duplicate lines from access tokens and authentication holders
echo "id;token_value;client_id;auth_holder_id" > merged_access_token.csv;
echo "name;id" > merged_authentication_holder.csv;

cat all_access_token.csv | sort | uniq >> merged_access_token.csv
cat all_authentication_holder.csv | sort | uniq >> merged_authentication_holder.csv

cat merged_access_token.csv | grep -v "^[[:space:]]*$" > final_access_token.csv
cat merged_authentication_holder.csv | grep -v "^[[:space:]]*$" > final_authentication_holder.csv

rm all_access_token.csv all_authentication_holder.csv merged_authentication_holder.csv merged_access_token.csv