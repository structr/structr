#!/bin/bash
shopt -s extglob
cp ../structr-modules/**/target/!(*sources|*javadoc).jar target/lib/
