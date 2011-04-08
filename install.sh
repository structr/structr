#!/bin/sh

#cd structr
#git pull

cd structr/structr
mvn clean && mvn install
cd ../..

#cd t5s
#git pull

cd t5s/t5s-module/t5s-module
mvn clean && mvn install
cd ../../..

cp structr/structr/structr-core/target/structr-core-*.jar /opt/structr/t5s/modules
cp structr/structr/structr-core-ui/target/structr-core-ui-*.jar /opt/structr/t5s/modules
cp structr/structr/structr-module-web/target/structr-module-web-*.jar /opt/structr/t5s/modules
cp t5s/t5s-module/t5s-module/target/t5s-module-*.jar /opt/structr/t5s/modules
rm -rf /opt/structr/t5s/modules/index

cp structr/structr/structr-webapp/target/structr-webapp-*-standalone.jar /opt/structr/t5s/lib

