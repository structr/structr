#!/bin/bash
java -Dfile.encoding=utf-8 -Xms512m -Xmx521m -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+UseNUMA -cp target/lib/*:target/structr-ui-0.7-SNAPSHOT.jar org.structr.Ui
