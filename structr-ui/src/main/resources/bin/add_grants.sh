#!/bin/bash

#######################################################################
# Use this script to create ResourceAccess nodes
# 	
#	FORBIDDEN             = 0
#	AUTH_USER_GET         = 1
#	AUTH_USER_PUT         = 2
#	AUTH_USER_POST        = 4
#	AUTH_USER_DELETE      = 8
#	NON_AUTH_USER_GET     = 16
#	NON_AUTH_USER_PUT     = 32
#	NON_AUTH_USER_POST    = 64
#	NON_AUTH_USER_DELETE  = 128
#
#######################################################################

. ./su

# clear grants
delete resource_access

# allow GET access to grants resource itself (changes are only allowed for superuser)
post resource_access '{"signature":"ResourceAccess","flags":17}'
post resource_access '{"signature":"ResourceAccess/_Ui","flags":17}'

post resource_access '{"signature":"/","flags":255}'
post resource_access '{"signature":"/_All","flags":255}'
post resource_access '{"signature":"/_Ui","flags":255}'
post resource_access '{"signature":"/_Html","flags":255}'
post resource_access '{"signature":"/_Public","flags":255}'
post resource_access '{"signature":"/_Protected","flags":255}'

post resource_access '{"signature":"_schema","flags":255}'
post resource_access '{"signature":"_schema/User","flags":255}'
post resource_access '{"signature":"_schema/Group","flags":255}'
post resource_access '{"signature":"_schema/Page","flags":255}'
post resource_access '{"signature":"_schema/Folder","flags":255}'
post resource_access '{"signature":"_schema/File","flags":255}'
post resource_access '{"signature":"_schema/Image","flags":255}'
post resource_access '{"signature":"_schema/Content","flags":255}'
post resource_access '{"signature":"_schema/PropertyDefinition","flags":255}'
post resource_access '{"signature":"_schema/Post","flags":255}'
post resource_access '{"signature":"_schema/Comment","flags":255}'
post resource_access '{"signature":"_schema/ResourceAccess","flags":255}'

post resource_access '{"signature":"User","flags":255, "position":0, "visibleToPublicUsers":true}'
post resource_access '{"signature":"User/_All","flags":255}'
post resource_access '{"signature":"User/_Html","flags":255}'
post resource_access '{"signature":"User/_Public","flags":255}'
post resource_access '{"signature":"User/_Protected","flags":255}'
post resource_access '{"signature":"User/_Ui","flags":255}'
post resource_access '{"signature":"User/Id","flags":255}'

post resource_access '{"signature":"Group","flags":255, "position":1, "visibleToPublicUsers":true}'
post resource_access '{"signature":"Group/_All","flags":255}'
post resource_access '{"signature":"Group/_Html","flags":255}'
post resource_access '{"signature":"Group/_Public","flags":255}'
post resource_access '{"signature":"Group/_Protected","flags":255}'
post resource_access '{"signature":"Group/_Ui","flags":255}'
post resource_access '{"signature":"Group/Id","flags":255}'

post resource_access '{"signature":"Page","flags":255, "position":2, "visibleToPublicUsers":true}'
post resource_access '{"signature":"Page/_All","flags":255}'
post resource_access '{"signature":"Page/_Html","flags":255}'
post resource_access '{"signature":"Page/_Public","flags":255}'
post resource_access '{"signature":"Page/_Protected","flags":255}'
post resource_access '{"signature":"Page/_Ui","flags":255}'
post resource_access '{"signature":"Page/Id","flags":255}'
post resource_access '{"signature":"Page/Id/All","flags":255}'
post resource_access '{"signature":"Page/Id/In","flags":255}'
post resource_access '{"signature":"Page/Id/Out","flags":255}'
post resource_access '{"signature":"Page/Id/_html","flags":255}'

post resource_access '{"signature":"Content","flags":255, "position":3, "visibleToPublicUsers":true}'
post resource_access '{"signature":"Content/_All","flags":255}'
post resource_access '{"signature":"Content/_Html","flags":255}'
post resource_access '{"signature":"Content/_Public","flags":255}'
post resource_access '{"signature":"Content/_Protected","flags":255}'
post resource_access '{"signature":"Content/_Ui","flags":255}'
post resource_access '{"signature":"Content/Id","flags":255}'

post resource_access '{"signature":"Folder","flags":255, "position":4, "visibleToPublicUsers":true}'
post resource_access '{"signature":"Folder/_All","flags":255}'
post resource_access '{"signature":"Folder/_Html","flags":255}'
post resource_access '{"signature":"Folder/_Public","flags":255}'
post resource_access '{"signature":"Folder/_Protected","flags":255}'
post resource_access '{"signature":"Folder/_Ui","flags":255}'
post resource_access '{"signature":"Folder/Id","flags":255}'
post resource_access '{"signature":"Folder/File","flags":255}'
post resource_access '{"signature":"Folder/File/_Ui","flags":255}'
post resource_access '{"signature":"Folder/File//_Ui","flags":255}'

post resource_access '{"signature":"File","flags":255, "position":5, "visibleToPublicUsers":true}'
post resource_access '{"signature":"File/_All","flags":255}'
post resource_access '{"signature":"File/_Html","flags":255}'
post resource_access '{"signature":"File/_Public","flags":255}'
post resource_access '{"signature":"File/_Protected","flags":255}'
post resource_access '{"signature":"File/_Ui","flags":255}'
post resource_access '{"signature":"File/Id","flags":255}'

post resource_access '{"signature":"Image","flags":255, "position":6, "visibleToPublicUsers":true}'
post resource_access '{"signature":"Image/_All","flags":255}'
post resource_access '{"signature":"Image/_Html","flags":255}'
post resource_access '{"signature":"Image/_Public","flags":255}'
post resource_access '{"signature":"Image/_Protected","flags":255}'
post resource_access '{"signature":"Image/_Ui","flags":255}'
post resource_access '{"signature":"Image/Id","flags":255}'

post resource_access '{"signature":"PropertyDefinition","flags":255, "position":7, "visibleToPublicUsers":true}'
post resource_access '{"signature":"PropertyDefinition/_All","flags":255}'
post resource_access '{"signature":"PropertyDefinition/_Html","flags":255}'
post resource_access '{"signature":"PropertyDefinition/_Public","flags":255}'
post resource_access '{"signature":"PropertyDefinition/_Protected","flags":255}'
post resource_access '{"signature":"PropertyDefinition/_Ui","flags":255}'
post resource_access '{"signature":"PropertyDefinition/Id","flags":255}'

post resource_access '{"signature":"Post","flags":255, "position":8, "visibleToPublicUsers":true}'
post resource_access '{"signature":"Post/_All","flags":255}'
post resource_access '{"signature":"Post/_Html","flags":255}'
post resource_access '{"signature":"Post/_Public","flags":255}'
post resource_access '{"signature":"Post/_Protected","flags":255}'
post resource_access '{"signature":"Post/_Ui","flags":255}'
post resource_access '{"signature":"Post/Id","flags":255}'

post resource_access '{"signature":"Comment","flags":255, "position":9, "visibleToPublicUsers":true}'
post resource_access '{"signature":"Comment/_All","flags":255}'
post resource_access '{"signature":"Comment/_Html","flags":255}'
post resource_access '{"signature":"Comment/_Public","flags":255}'
post resource_access '{"signature":"Comment/_Protected","flags":255}'
post resource_access '{"signature":"Comment/_Ui","flags":255}'
post resource_access '{"signature":"Comment/Id","flags":255}'
