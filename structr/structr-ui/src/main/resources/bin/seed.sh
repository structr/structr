#!/bin/bash

STRUCTR_CONF=`find ../../../.. -name structr.conf`

SUPERUSER_USERNAME=`grep superuser.username $STRUCTR_CONF | awk '{ print $3 }' | tr -d [:cntrl:]`
SUPERUSER_PASSWORD=`grep superuser.password $STRUCTR_CONF | awk '{ print $3 }' | tr -d [:cntrl:]`

curl -i http://localhost:8082/structr/rest/users -d'{"name":"admin","password":"admin", "frontendUser":true, "backendUser":true }' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/users -d'{"name":"test","password":"test", "frontendUser":true, "backendUser":true}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/users -d'{"name":"test1","password":"test1", "frontendUser":true, "backendUser":true}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/users -HX-User:admin -HX-Password:admin

curl -i http://localhost:8082/structr/rest/type_definitions -d'{"name":"String",	"validationExpression":"[a-zA-z0-9]*",			"validationErrorMessage":"Value must be a string"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/type_definitions -d'{"name":"Username",	"validationExpression":"^[a-z0-9_-]{3,15}$",	"validationErrorMessage":"Username must not contain characters other than a-z, 0-9, underscore or hyphen, and length has to be between 3 and 15"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/type_definitions -d'{"name":"Password",	"validationExpression":"((?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%]).{6,20})",			"validationErrorMessage":"Password must contain at least one digit, one lowercase, one uppercase character, and one special symbol out of @#$%. Length between 6 and 20 characters."}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/type_definitions -d'{"name":"Email",	"validationExpression":"^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+
(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$",			"validationErrorMessage":"No valid e-mail"}' -HX-User:admin -HX-Password:admin
curl -i http://localhost:8082/structr/rest/type_definitions -d'{"name":"IpAddress",	"validationExpression":"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.
([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$",			"validationErrorMessage":"No valid IP address"}' -HX-User:admin -HX-Password:admin

curl -i http://localhost:8082/structr/rest/type_definitions -HX-User:admin -HX-Password:admin
