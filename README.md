MobSOS Monitor
==

MobSOS Monitor serves as framework to process and persist access logs provided by a reverse proxy.

The following figure depicts the typical setup
```
\ client \<------>\ reverse-proxy \
                    \ tail -f logs/access.log \------>\ mobsos-monitor 
                                                        \------>\ mobsos-db \
                                                        \------>\ mobsos-trigger \
```
## Build Instructions

```
ant all

```
TODO: more detailed build instructions

## Configuration

## Log Format

The expected log format is a CSV format consisting of the following fields:

TODO: list fields
TODO: example

## Run

```
./bin/start.sh

```
TODO::
## Reference Setup

TODO: describe reference setup with nginx as reverse proxy.


