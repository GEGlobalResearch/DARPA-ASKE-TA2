#!/bin/bash

java -jar ./target/dbnSpecGenerator-1.0-SNAPSHOT.jar --server.port=46000 > ./logs/service.log 2>&1 &
