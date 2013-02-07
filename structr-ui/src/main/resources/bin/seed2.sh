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

curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"string1","dataType":"String"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"string2","dataType":"String"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"string3","dataType":"String"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"string4","dataType":"String"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"string5","dataType":"String"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"string6","dataType":"String"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"string7","dataType":"String"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"string8","dataType":"String"}' -HX-User:admin -HX-Password:admin

curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"integer1","dataType":"Integer"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"integer2","dataType":"Integer"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"integer3","dataType":"Integer"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"integer4","dataType":"Integer"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"integer5","dataType":"Integer"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"integer6","dataType":"Integer"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"integer7","dataType":"Integer"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"integer8","dataType":"Integer"}' -HX-User:admin -HX-Password:admin

curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"boolean1","dataType":"Boolean"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"boolean2","dataType":"Boolean"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"boolean3","dataType":"Boolean"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"boolean4","dataType":"Boolean"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"boolean5","dataType":"Boolean"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"boolean6","dataType":"Boolean"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"boolean7","dataType":"Boolean"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"boolean8","dataType":"Boolean"}' -HX-User:admin -HX-Password:admin

curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"date1","dataType":"Date"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"date2","dataType":"Date"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"date3","dataType":"Date"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"date4","dataType":"Date"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"date5","dataType":"Date"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"date6","dataType":"Date"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"date7","dataType":"Date"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/property_definitions -d'{"kind":"Test","name":"date8","dataType":"Date"}' -HX-User:admin -HX-Password:admin

i=0;

while [ $i -lt 10 ]; do

	curl -i http://localhost:8082/structr/rest/tests -d'{"string1":"string1","string2":"string2","string3":"string3","string4":"string4","string5":"string5","string6":"string6","string7":"string7","string8":"string8","integer1":1,"integer2":2,"integer3":3,"integer4":4,"integer5":5,"integer6":6,"integer7":7,"integer8":8,"boolean1":false,"boolean2":true,"boolean3":false,"boolean4":true,"boolean5":false,"boolean6":true,"boolean7":false,"boolean8":true,"date1":"2013-02-07T11:00:00+0100","date2":"2013-02-07T11:00:00+0100","date3":"2013-02-07T11:00:00+0100","date4":"2013-02-07T11:00:00+0100","date5":"2013-02-07T11:00:00+0100","date6":"2013-02-07T11:00:00+0100","date7":"2013-02-07T11:00:00+0100","date8":"2013-02-07T11:00:00+0100"}' -HX-User:admin -HX-Password:admin
	i=$((i+1))	
done
