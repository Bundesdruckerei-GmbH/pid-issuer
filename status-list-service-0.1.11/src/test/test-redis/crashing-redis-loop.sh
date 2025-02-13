#!/bin/bash

while [ 1 ]
do
	docker-compose up &
	DCPID=$!
	sleep 5.3571
	PID=$(ps -ef | grep redis-server | grep -v grep | awk '{print $2;}')
	sudo kill -9 $PID
	wait $DCPID
	sleep 5.3571
done
