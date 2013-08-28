#!/bin/bash

#######################################################################
# Use this script to create MailTemplate nodes
#######################################################################

. ./su

delete mail_templates

post mail_templates '{"name":"SUBJECT",        "visibleToAuthenticatedUsers":true, "text": {"content":"Welcome to Structr!", "visibleToAuthenticatedUsers":true}}'
post mail_templates '{"name":"SENDER_NAME",    "visibleToAuthenticatedUsers":true, "text": {"content":"Structr backend", "visibleToAuthenticatedUsers":true}}'
post mail_templates '{"name":"SENDER_ADDRESS", "visibleToAuthenticatedUsers":true, "text": {"content":"admin@structr.org", "visibleToAuthenticatedUsers":true}}'
post mail_templates '{"name":"TEXT_BODY",      "visibleToAuthenticatedUsers":true, "text": {"content":"Dear ${eMail},\n\nplease click <a href=''${link}''>here</a> to finalize your registration.", "visibleToAuthenticatedUsers":true}}'
post mail_templates '{"name":"HTML_BODY",      "visibleToAuthenticatedUsers":true, "text": {"content":"<div>Dear ${eMail},<br><br>please click <a href=''${link}''>here</a> to finalize your registration.</div>", "visibleToAuthenticatedUsers":true}}'
post mail_templates '{"name":"BASE_URL",       "visibleToAuthenticatedUsers":true, "text": {"content":"http://'$HOST':'$PORT'", "visibleToAuthenticatedUsers":true}}'
post mail_templates '{"name":"TARGET_PAGE",    "visibleToAuthenticatedUsers":true, "text": {"content":"register_thanks", "visibleToAuthenticatedUsers":true}}'


