#!/bin/bash

#######################################################################
# Use this script to create ResourceAccess nodes
#
#   FORBIDDEN             = 0
#   AUTH_USER_GET         = 1
#   AUTH_USER_PUT         = 2
#   AUTH_USER_POST        = 4
#   AUTH_USER_DELETE      = 8
#   NON_AUTH_USER_GET     = 16
#   NON_AUTH_USER_PUT     = 32
#   NON_AUTH_USER_POST    = 64
#   NON_AUTH_USER_DELETE  = 128
#   AUTH_USER_OPTIONS     = 256
#   NON_AUTH_USER_OPTIONS = 512
#
#   ELEM_POS is the position at which the resource will show
#   up at in the structr UI. (your entities show up in the order of this file)
#######################################################################

. ./su

ELEM_POS=0

# clear grants
delete resource_access

# allow GET access to grants resource itself (changes are only allowed for superuser)
post resource_access '{"signature":"ResourceAccess","flags":17}'
post resource_access '{"signature":"ResourceAccess/_Ui","flags":17}'

# allow public users POST access to registration resource
post resource_access '{"signature":"_registration","flags":64}'

# allow public users POST access to login resource
post resource_access '{"signature":"_login","flags":64}'

# allow authenticated users POST access to logout resource
post resource_access '{"signature":"_logout","flags":4}'

post resource_access '{"signature":"/","flags":255}'
post resource_access '{"signature":"/_All","flags":255}'
post resource_access '{"signature":"/_Ui","flags":255}'
post resource_access '{"signature":"/_Html","flags":255}'
post resource_access '{"signature":"/_Public","flags":255}'
post resource_access '{"signature":"/_Protected","flags":255}'


###################################################
#        Add one '_schema' Entry per Entity       #
###################################################

post resource_access '{"signature":"_schema","flags":255}'
post resource_access '{"signature":"_schema/User","flags":255}'
post resource_access '{"signature":"_schema/Group","flags":255}'
post resource_access '{"signature":"_schema/Page","flags":255}'
post resource_access '{"signature":"_schema/Folder","flags":255}'
post resource_access '{"signature":"_schema/File","flags":255}'
post resource_access '{"signature":"_schema/Image","flags":255}'
post resource_access '{"signature":"_schema/Content","flags":255}'
post resource_access '{"signature":"_schema/MailTemplate","flags":255}'
post resource_access '{"signature":"_schema/ResourceAccess","flags":255}'
post resource_access '{"signature":"_schema/Person","flags":255}'
post resource_access '{"signature":"_schema/Template","flags":255}'
post resource_access '{"signature":"_schema/Widget","flags":255}'
post resource_access '{"signature":"_schema/SchemaNode","flags":255}'
post resource_access '{"signature":"_schema/SchemaRelationship","flags":255}'


################################################
#     Add your entities like so (and don't
#       forget the '_schema' entry above)
#
#
#    post resource_access '{"signature":"ENTITIY_NAME","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
#    ((ELEM_POS++))
#    post resource_access '{"signature":"ENTITIY_NAME/_All","flags":255}'
#    post resource_access '{"signature":"ENTITIY_NAME/_Html","flags":255}'
#    post resource_access '{"signature":"ENTITIY_NAME/_Public","flags":255}'
#    post resource_access '{"signature":"ENTITIY_NAME/_Protected","flags":255}'
#    post resource_access '{"signature":"ENTITIY_NAME/_Ui","flags":255}'
#    post resource_access '{"signature":"ENTITIY_NAME/Id","flags":255}'
#
#
################################################

post resource_access '{"signature":"Person","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"Person/_All","flags":255}'
post resource_access '{"signature":"Person/_Html","flags":255}'
post resource_access '{"signature":"Person/_Public","flags":255}'
post resource_access '{"signature":"Person/_Protected","flags":255}'
post resource_access '{"signature":"Person/_Ui","flags":255}'
post resource_access '{"signature":"Person/Id","flags":255}'

post resource_access '{"signature":"User","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"User/_All","flags":255}'
post resource_access '{"signature":"User/_Html","flags":255}'
post resource_access '{"signature":"User/_Public","flags":255}'
post resource_access '{"signature":"User/_Protected","flags":255}'
post resource_access '{"signature":"User/_Ui","flags":255}'
post resource_access '{"signature":"User/Id","flags":255}'

post resource_access '{"signature":"Group","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"Group/_All","flags":255}'
post resource_access '{"signature":"Group/_Html","flags":255}'
post resource_access '{"signature":"Group/_Public","flags":255}'
post resource_access '{"signature":"Group/_Protected","flags":255}'
post resource_access '{"signature":"Group/_Ui","flags":255}'
post resource_access '{"signature":"Group/Id","flags":255}'

post resource_access '{"signature":"Page","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
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

post resource_access '{"signature":"Content","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"Content/_All","flags":255}'
post resource_access '{"signature":"Content/_Html","flags":255}'
post resource_access '{"signature":"Content/_Public","flags":255}'
post resource_access '{"signature":"Content/_Protected","flags":255}'
post resource_access '{"signature":"Content/_Ui","flags":255}'
post resource_access '{"signature":"Content/Id","flags":255}'

post resource_access '{"signature":"Folder","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"Folder/_All","flags":255}'
post resource_access '{"signature":"Folder/_Html","flags":255}'
post resource_access '{"signature":"Folder/_Public","flags":255}'
post resource_access '{"signature":"Folder/_Protected","flags":255}'
post resource_access '{"signature":"Folder/_Ui","flags":255}'
post resource_access '{"signature":"Folder/Id","flags":255}'
post resource_access '{"signature":"Folder/File","flags":255}'
post resource_access '{"signature":"Folder/File/_Ui","flags":255}'
post resource_access '{"signature":"Folder/File//_Ui","flags":255}'

post resource_access '{"signature":"File","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"File/_All","flags":255}'
post resource_access '{"signature":"File/_Html","flags":255}'
post resource_access '{"signature":"File/_Public","flags":255}'
post resource_access '{"signature":"File/_Protected","flags":255}'
post resource_access '{"signature":"File/_Ui","flags":255}'
post resource_access '{"signature":"File/Id","flags":255}'

post resource_access '{"signature":"Image","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"Image/_All","flags":255}'
post resource_access '{"signature":"Image/_Html","flags":255}'
post resource_access '{"signature":"Image/_Public","flags":255}'
post resource_access '{"signature":"Image/_Protected","flags":255}'
post resource_access '{"signature":"Image/_Ui","flags":255}'
post resource_access '{"signature":"Image/Id","flags":255}'

post resource_access '{"signature":"MailTemplate","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"MailTemplate/_All","flags":255}'
post resource_access '{"signature":"MailTemplate/_Html","flags":255}'
post resource_access '{"signature":"MailTemplate/_Public","flags":255}'
post resource_access '{"signature":"MailTemplate/_Protected","flags":255}'
post resource_access '{"signature":"MailTemplate/_Ui","flags":255}'
post resource_access '{"signature":"MailTemplate/Id","flags":255}'

post resource_access '{"signature":"Template","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"Template/_All","flags":255}'
post resource_access '{"signature":"Template/_Html","flags":255}'
post resource_access '{"signature":"Template/_Public","flags":255}'
post resource_access '{"signature":"Template/_Protected","flags":255}'
post resource_access '{"signature":"Template/_Ui","flags":255}'
post resource_access '{"signature":"Template/Id","flags":255}'

post resource_access '{"signature":"Widget","flags":1023, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"Widget/_All","flags":1023}'
post resource_access '{"signature":"Widget/_Html","flags":1023}'
post resource_access '{"signature":"Widget/_Public","flags":1023}'
post resource_access '{"signature":"Widget/_Protected","flags":1023}'
post resource_access '{"signature":"Widget/_Ui","flags":1023}'
post resource_access '{"signature":"Widget/Id","flags":1023}'

post resource_access '{"signature":"SchemaNode","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"SchemaNode/_All","flags":1023}'
post resource_access '{"signature":"SchemaNode/_Html","flags":1023}'
post resource_access '{"signature":"SchemaNode/_Public","flags":1023}'
post resource_access '{"signature":"SchemaNode/_Protected","flags":1023}'
post resource_access '{"signature":"SchemaNode/_Ui","flags":1023}'
post resource_access '{"signature":"SchemaNode/Id","flags":1023}'

post resource_access '{"signature":"SchemaRelationship","flags":255, "position":'$ELEM_POS', "visibleToPublicUsers":true}'
((ELEM_POS++))
post resource_access '{"signature":"SchemaRelationship/_All","flags":1023}'
post resource_access '{"signature":"SchemaRelationship/_Html","flags":1023}'
post resource_access '{"signature":"SchemaRelationship/_Public","flags":1023}'
post resource_access '{"signature":"SchemaRelationship/_Protected","flags":1023}'
post resource_access '{"signature":"SchemaRelationship/_Ui","flags":1023}'
post resource_access '{"signature":"SchemaRelationship/Id","flags":1023}'
