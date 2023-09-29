#!/bin/bash

if [ ! -d "logs" ]; 
then
     echo "logs directory does not exist - creating it"
     mkdir logs
fi

PID=`pgrep -f dbnSpecGenerator`
if [ -n "${PID}" ];
then
   echo "killing running dbn json service process"
   pkill -f dbnSpecGenerator
fi

echo "Restarting dbn json service"
java -jar ./target/dbnSpecGenerator-1.0-SNAPSHOT.jar --server.port=46000 > ./logs/service.log 2>&1 &
