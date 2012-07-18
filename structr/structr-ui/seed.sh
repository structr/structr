#!/bin/bash

curl -i -3k https://localhost:8083/structr/rest/users -d'{"name":"admin","password":"admin", "frontendUser":true, "backendUser":true }' -HX-User:superadmin -HX-Password:Nk3i9E2MuHWS
curl -i -3k https://localhost:8083/structr/rest/users -d'{"name":"test","password":"test", "frontendUser":true, "backendUser":true}' -HX-User:admin -HX-Password:admin
curl -i -3k https://localhost:8083/structr/rest/users -d'{"name":"test1","password":"test1", "frontendUser":true, "backendUser":true}' -HX-User:admin -HX-Password:admin
curl -i -3k https://localhost:8083/structr/rest/users -HX-User:admin -HX-Password:admin

