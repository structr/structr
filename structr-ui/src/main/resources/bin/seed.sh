#!/bin/bash

STRUCTR_CONF=`find ../../../.. -name structr.conf`

SUPERUSER_USERNAME=`grep superuser.username $STRUCTR_CONF | awk '{ print $3 }' | tr -d [:cntrl:]`
SUPERUSER_PASSWORD=`grep superuser.password $STRUCTR_CONF | awk '{ print $3 }' | tr -d [:cntrl:]`

curl -i http://localhost:8082/structr/rest/users -d'{"name":"admin","password":"admin", "frontendUser":true, "backendUser":true }' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/users -d'{"name":"test","password":"test", "frontendUser":true, "backendUser":true}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/users -d'{"name":"test1","password":"test1", "frontendUser":true, "backendUser":true}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/users -HX-User:admin -HX-Password:admin

curl -i http://localhost:8082/structr/rest/type_definitions -HX-User:admin -HX-Password:admin
