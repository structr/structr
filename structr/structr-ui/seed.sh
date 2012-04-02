#!/bin/bash

curl -i -3k https://localhost:8081/structr/rest/users -d'{"name":"admin","password":"admin"}' -HX-User:superadmin -HX-Password:s3hrg3h37m
curl -i -3k https://localhost:8081/structr/rest/users -d'{"name":"test","password":"test"}' -HX-User:superadmin -HX-Password:s3hrg3h37m
curl -i -3k https://localhost:8081/structr/rest/users -d'{"name":"test1","password":"test1"}' -HX-User:superadmin -HX-Password:s3hrg3h37m
curl -i -3k https://localhost:8081/structr/rest/users -HX-User:superadmin -HX-Password:s3hrg3h37m

