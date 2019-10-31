#!/bin/bash

. env.sh

docker-compose -f $COMPOSE_FILE down
docker-compose -f $COMPOSE_FILE ps
