#!/bin/bash
. ./su
post resource_access '{"signature":"User","flags":255}'
post users '{"name":"admin","password":"admin", "frontendUser":true, "backendUser":true }'
get users

