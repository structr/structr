#!/bin/bash

MODULE_SOURCE_FILE=`cat ../structr-base/src/main/java/org/structr/module/AdvancedScriptingModule.java | grep "Functions.put" | sed "s/^[[:space:]]*Functions.put(licenseManager, new//" | sed "s/());//g"`

echo "## Built-in Functions"

for FUNCTION in $MODULE_SOURCE_FILE
do
  SOURCE_FILE=`find ../structr-base/src/main/java/org/structr -name $FUNCTION.java`
  NAME=`cat $SOURCE_FILE | grep -A1 ' getName()' | paste - - | sed 's/^[^"]*"//' | sed 's/";//' | sed 's/find./Predicate function: /'`
  DESC=`cat $SOURCE_FILE | grep -A1 'shortDescription()' | paste - - | sed 's/^[^"]*"//' | sed 's/";//'`
  USAGE=`cat $SOURCE_FILE | grep 'Usage' | sed 's/.*Usage: \(.*\) Example:.*/\1/'`;
  EXAMPLE=`cat $SOURCE_FILE | grep 'Example' | sed 's/.*Example: //' | sed 's/";//'`
  echo "### $NAME"

  echo
  echo $DESC
  echo
  echo "Usage: \`$USAGE\`"
  echo "Example: \`$EXAMPLE\`"

  echo

done
