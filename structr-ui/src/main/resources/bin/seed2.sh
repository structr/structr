#!/bin/bash

STRUCTR_CONF=`find ./ -name structr.conf`

SUPERUSER_USERNAME=`grep superuser.username $STRUCTR_CONF | awk '{ print $3 }' | tr -d [:cntrl:]`
SUPERUSER_PASSWORD=`grep superuser.password $STRUCTR_CONF | awk '{ print $3 }' | tr -d [:cntrl:]`

curl -i http://localhost:8082/structr/rest/users -d'{"name":"admin","password":"admin", "frontendUser":true, "backendUser":true }' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/users -d'{"name":"test","password":"test", "frontendUser":true, "backendUser":true}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/users -d'{"name":"test1","password":"test1", "frontendUser":true, "backendUser":true}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/users -HX-User:admin -HX-Password:admin

curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Book","name":"chapters","dataType":"Collection","relType":"BOOK","relKind":"Chapter"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Book","name":"name","dataType":"String"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Book","name":"author","dataType":"String"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Book","name":"publishDate","dataType":"Date"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Book","name":"isbn","dataType":"String"}' -HX-User:admin -HX-Password:admin

curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Chapter","name":"book","dataType":"Entity","relType":"BOOK", "incoming": true, "relKind": "Book"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Chapter","name":"paragraphs","dataType":"Collection","relType":"CHAPTER", "relKind":"Paragraph"}' -HX-User:admin -HX-Password:admin

curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Paragraph","name":"content","dataType":"String"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Paragraph","name":"chapter","dataType":"Entity","relType":"CHAPTER", "incoming": true, "relKind": "Chapter"}' -HX-User:admin -HX-Password:admin
