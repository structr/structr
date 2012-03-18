#!/bin/bash
#
# IMPORTANT NOTICE:
# To use this script, you must put the
# file "bcprov-jdk16-140.jar" in the
# current directory!
#

IMPORT_FROM=$1
KEYSTORE_NAME=$2

if [ -z $IMPORT_FROM ]; then
	echo
	echo "usage: $0 <import-url> <keystore-name>"
	echo
	echo "Imports an SSL certificate from <import-url> to the keystore with name <keystore-name>."
	echo
	exit 0
fi

if [ -z $KEYSTORE_NAME ]; then
	echo
	echo "usage: $0 <import-url> <keystore-name>"
	echo
	echo "Imports an SSL certificate from <import-url> to the keystore with name <keystore-name>."
	echo
	exit 0
fi

openssl s_client -connect $IMPORT_FROM </dev/null |sed -n '/BEGIN/','/END/p' >certificate.pem
keytool -import -v -file certificate.pem -keystore $KEYSTORE_NAME -storetype BKS -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath bcprov-jdk16-140.jar
rm certificate.pem
