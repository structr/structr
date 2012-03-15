#!/bin/bash

curl localhost:8080/structr/rest/users -d'{"name":"admin","password":"admin"}'
curl localhost:8080/structr/rest/users -d'{"name":"test","password":"test"}'
curl localhost:8080/structr/rest/users -d'{"name":"test1","password":"test1"}'
curl localhost:8080/structr/rest/users

