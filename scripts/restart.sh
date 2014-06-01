#!/bin/sh

kill `cat RUNNING_PID`
play -Dhttp.port=80 -Dconfig.resource=application-prod.conf start