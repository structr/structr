#!/bin/bash

STRUCTR_CONF=`find ./ -name structr.conf`

SUPERUSER_USERNAME=`grep superuser.username $STRUCTR_CONF | awk '{ print $3 }' | tr -d [:cntrl:]`
SUPERUSER_PASSWORD=`grep superuser.password $STRUCTR_CONF | awk '{ print $3 }' | tr -d [:cntrl:]`

curl -i http://localhost:8082/structr/rest/resource_access -d'{"signature":"User","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/users -d'{"name":"admin","password":"admin", "frontendUser":true, "backendUser":true }' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -XDELETE -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"ResourceAccess","flags":17}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"ResourceAccess/_Ui","flags":17}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"/","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"/_All","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"/_Ui","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"/_Html","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"/_Public","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"/_Protected","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"_schema","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"_schema/User","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"_schema/Group","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"_schema/Page","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"_schema/Folder","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"_schema/File","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"_schema/Image","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"_schema/Content","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"_schema/ResourceAccess","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"User","flags":255, "position":0, "visibleToPublicUsers":true}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"User/_All","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"User/_Html","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"User/_Public","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"User/_Protected","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"User/_Ui","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"User/Id","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Group","flags":255, "position":1, "visibleToPublicUsers":true}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Group/_All","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Group/_Html","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Group/_Public","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Group/_Protected","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Group/_Ui","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Group/Id","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page","flags":255, "position":2, "visibleToPublicUsers":true}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page/_All","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page/_Html","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page/_Public","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page/_Protected","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page/_Ui","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page/Id","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page/Id/All","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page/Id/In","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page/Id/Out","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Page/Id/_html","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Content","flags":255, "position":3, "visibleToPublicUsers":true}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Content/_All","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Content/_Html","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Content/_Public","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Content/_Protected","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Content/_Ui","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Content/Id","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Folder","flags":255, "position":4, "visibleToPublicUsers":true}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Folder/_All","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Folder/_Html","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Folder/_Public","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Folder/_Protected","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Folder/_Ui","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Folder/Id","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"File","flags":255, "position":5, "visibleToPublicUsers":true}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"File/_All","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"File/_Html","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"File/_Public","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"File/_Protected","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"File/_Ui","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"File/Id","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Image","flags":255, "position":6, "visibleToPublicUsers":true}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Image/_All","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Image/_Html","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Image/_Public","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Image/_Protected","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Image/_Ui","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"Image/Id","flags":255}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD

curl -i http://localhost:8082/structr/rest/resource_access -d '{"signature":"ResourceAccess","flags":17}' -HX-User:$SUPERUSER_USERNAME -HX-Password:$SUPERUSER_PASSWORD
