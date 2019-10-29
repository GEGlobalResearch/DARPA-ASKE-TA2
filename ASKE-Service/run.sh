#!/bin/bash

. env.sh

docker-compose -f $COMPOSE_FILE up -d
docker-compose -f $COMPOSE_FILE ps
