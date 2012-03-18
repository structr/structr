#!/bin/bash

curl -3k https://localhost:8081/structr/rest/users -d'{"name":"admin","password":"admin"}'
curl -3k https://localhost:8081/structr/rest/users -d'{"name":"test","password":"test"}'
curl -3k https://localhost:8081/structr/rest/users -d'{"name":"test1","password":"test1"}'
curl -3k https://localhost:8081/structr/rest/users

