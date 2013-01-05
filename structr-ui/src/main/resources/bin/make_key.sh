#!/bin/bash
keytool -keystore keystore.jks -alias jetty -genkey -keyalg RSA -storepass structrKeystore -keypass structrKeystore

