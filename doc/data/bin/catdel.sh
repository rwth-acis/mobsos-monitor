#!/bin/bash

# Concatenates the lines of an input file with a delay.
# Output is tee'd to an output file.
cat $1 |\
grep Bearer |\
perl -e 'print && select undef,undef,undef,.1 while <>;' |\
tee -a $2 | cut -d',' -f1,2,5,6,18
