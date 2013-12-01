#!/bin/bash
java -Dfile.encoding=utf-8 -Xms1g -Xmx1g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+UseNUMA -cp target/lib/*:target/structr-ui-0.9-SNAPSHOT.jar org.structr.Server
