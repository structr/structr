#!/bin/bash

#######################################################################
# Use this script to re-build the search indexes
#######################################################################

. ./su

post maintenance/rebuildIndex '{}'
