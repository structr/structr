#!/bin/bash

mkdir /tmp/structr-ui
rm -rf /tmp/structr-ui/*
cd structr/structr-ui
mvn clean package
cp target/structr-ui.war /tmp/structr-ui
cd /tmp/structr-ui
java -jar structr-ui.war

