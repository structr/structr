#!/bin/bash
java -Dfile.encoding=utf-8 -Xms1g -Xmx1g -XX:+UseNUMA -cp target/lib/*:target/structr-ui-0.7-SNAPSHOT.jar org.structr.Ui
