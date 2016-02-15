#!/bin/bash

echo "name;id"
mysql -u root -p -e "select convert(authentication USING latin1),id from authentication_holder;" OpenIDConnect |\
grep "dc=learning-layers" |\
sed "s/.*\\0//g" |\
grep -v -e '^[[:space:]]$' |\
grep -v -e '^[0-9]*$' |\
sed -e 's/\t/;/g' |\
sed -e 's/[^A-Za-z0-9._-;]//g'