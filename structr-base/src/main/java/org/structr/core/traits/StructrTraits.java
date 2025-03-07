/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.traits;

import org.structr.core.traits.definitions.*;

public class StructrTraits {

	// node types
	public static final String PROPERTY_CONTAINER                  = "PropertyContainer";
	public static final String GRAPH_OBJECT                        = "GraphObject";
	public static final String NODE_INTERFACE                      = "NodeInterface";
	public static final String RELATIONSHIP_INTERFACE              = "RelationshipInterface";
	public static final String ACCESS_CONTROLLABLE                 = "AccessControllable";
	public static final String PRINCIPAL                           = "Principal";
	public static final String USER                                = "User";
	public static final String GROUP                               = "Group";
	public static final String ABSTRACT_FILE                       = "AbstractFile";
	public static final String FOLDER                              = "Folder";
	public static final String FILE                                = "File";
	public static final String IMAGE                               = "Image";
	public static final String ABSTRACT_SCHEMA_NODE                = "AbstractSchemaNode";
	public static final String SCHEMA_NODE                         = "SchemaNode";
	public static final String SCHEMA_PROPERTY                     = "SchemaProperty";
	public static final String SCHEMA_METHOD                       = "SchemaMethod";
	public static final String SCHEMA_VIEW                         = "SchemaView";
	public static final String SCHEMA_GRANT                        = "SchemaGrant";
	public static final String SCHEMA_RELOADING_NODE               = "SchemaReloadingNode";
	public static final String SCHEMA_RELATIONSHIP_NODE            = "SchemaRelationshipNode";
	public static final String SCHEMA_NODE_METHOD                  = "SchemaNodeMethod";
	public static final String LINK_SOURCE                         = "LinkSource";
	public static final String LINKABLE                            = "Linkable";
	public static final String DOM_NODE                            = "DOMNode";
	public static final String DOM_ELEMENT                         = "DOMElement";
	public static final String SHADOW_DOCUMENT                     = "ShadowDocument";
	public static final String PAGE                                = "Page";
	public static final String PAGE_PATH                           = "PagePath";
	public static final String PAGE_PATH_PARAMETER                 = "PagePathParameter";
	public static final String TEMPLATE                            = "Template";
	public static final String CONTENT                             = "Content";
	public static final String ACTION_MAPPING                      = "ActionMapping";
	public static final String PARAMETER_MAPPING                   = "ParameterMapping";
	public static final String LOCALIZATION                        = "Localization";
	public static final String LOCATION                            = "Location";
	public static final String MAIL_TEMPLATE                       = "MailTemplate";
	public static final String PERSON                              = "Person";
	public static final String SESSION_DATA_NODE                   = "SessionDataNode";
	public static final String SCHEMA_METHOD_PARAMETER             = "SchemaMethodParameter";
	public static final String CORS_SETTING                        = "CorsSetting";
	public static final String RESOURCE_ACCESS                     = "ResourceAccess";
	public static final String DYNAMIC_RESOURCE_ACCESS             = "DynamicResourceAccess";
	public static final String EMAIL_MESSAGE                       = "EMailMessage";
	public static final String MAILBOX                             = "Mailbox";
	public static final String WIDGET                              = "Widget";
	public static final String VIRTUAL_TYPE                        = "VirtualType";
	public static final String VIRTUAL_PROPERTY                    = "VirtualProperty";
	public static final String LDAP_GROUP                          = "LDAPGroup";
	public static final String LDAP_USER                           = "LDAPUser";
	public static final String MESSAGE_CLIENT                      = "MessageClient";
	public static final String MESSAGE_SUBSCRIBER                  = "MessageSubscriber";
	public static final String KAFKA_CLIENT                        = "KafkaClient";
	public static final String MQTT_CLIENT                         = "MQTTClient";
	public static final String PULSAR_CLIENT                       = "PulsarClient";
	public static final String VIDEO_FILE                          = "VideoFile";
	public static final String ODF_EXPORTER                        = "ODFExporter";
	public static final String ODS_EXPORTER                        = "ODSExporter";
	public static final String ODT_EXPORTER                        = "ODTExporter";
	public static final String PAYMENT_NODE                        = "PaymentNode";
	public static final String PAYMENT_ITEM_NODE                   = "PaymentItemNode";
	public static final String ABSTRACT_FEED_ITEM                  = "AbstractFeedItem";
	public static final String DATA_FEED                           = "DataFeed";
	public static final String FEED_ITEM                           = "FeedItem";
	public static final String FEED_ITEM_CONTENT                   = "FeedItemContent";
	public static final String FEED_ITEM_ENCLOSURE                 = "FeedItemEnclosure";
	public static final String REMOTE_DOCUMENT                     = "RemoteDocument";
	public static final String LOG_EVENT                           = "LogEvent";
	public static final String LOG_OBJECT                          = "LogObject";
	public static final String LOG_SUBJECT                         = "LogSubject";
	public static final String XMPP_CLIENT                         = "XMPPClient";
	public static final String XMPP_REQUEST                        = "XMPPRequest";
	public static final String APPLICATION_CONFIGURATION_DATA_NODE = "ApplicationConfigurationDataNode";
	public static final String COMMENT                             = "Comment";
	public static final String COMPONENT                           = "Component";
	public static final String CSS_DECLARATION                     = "CssDeclaration";
	public static final String CSS_RULE                            = "CssRule";
	public static final String CSS_SELECTOR                        = "CssSelector";
	public static final String CSS_SEMANTIC_CLASS                  = "CssSemanticClass";
	public static final String SITE                                = "Site";
	public static final String STORAGE_CONFIGURATION               = "StorageConfiguration";
	public static final String STORAGE_CONFIGURATION_ENTRY         = "StorageConfigurationEntry";

	// relationship types
	public static final String SECURITY                                                       = "Security";
	public static final String PRINCIPAL_OWNS_NODE                                            = "PrincipalOwnsNode";
	public static final String PRINCIPAL_SCHEMA_GRANT_RELATIONSHIP                            = "PrincipalSchemaGrantRelationship";
	public static final String GROUP_CONTAINS_PRINCIPAL                                       = "GroupCONTAINSPrincipal";
	public static final String SCHEMA_EXCLUDED_VIEW_PROPERTY                                  = "SchemaExcludedViewProperty";
	public static final String SCHEMA_GRANT_SCHEMA_NODE_RELATIONSHIP                          = "SchemaGrantSchemaNodeRelationship";
	public static final String SCHEMA_METHOD_PARAMETERS                                       = "SchemaMethodParameters";
	public static final String SCHEMA_NODE_EXTENDS_SCHEMA_NODE                                = "SchemaNodeExtendsSchemaNode";
	public static final String SCHEMA_NODE_PROPERTY                                           = "SchemaNodeProperty";
	public static final String SCHEMA_NODE_VIEW                                               = "SchemaNodeView";
	public static final String SCHEMA_RELATIONSHIP_SOURCE_NODE                                = "SchemaRelationshipSourceNode";
	public static final String SCHEMA_RELATIONSHIP_TARGET_NODE                                = "SchemaRelationshipTargetNode";
	public static final String SCHEMA_VIEW_PROPERTY                                           = "SchemaViewProperty";
	public static final String VIRTUAL_TYPE_VIRTUAL_PROPERTY_VIRTUAL_PROPERTY                 = "VirtualTypevirtualPropertyVirtualProperty";
	public static final String EMAIL_MESSAGE_HAS_ATTACHMENT_FILE                              = "EMailMessageHAS_ATTACHMENTFile";
	public static final String MAILBOX_CONTAINS_EMAIL_MESSAGES_EMAIL_MESSAGE                  = "MailboxCONTAINS_EMAILMESSAGESEMailMessage";
	public static final String MESSAGE_CLIENT_HAS_MESSAGE_SUBSCRIBER                          = "MessageClientHASMessageSubscriber";
	public static final String VIDEO_FILE_HAS_CONVERTED_VIDEO_VIDEO_FILE                      = "VideoFileHAS_CONVERTED_VIDEOVideoFile";
	public static final String VIDEO_FILE_HAS_POSTER_IMAGE_IMAGE                              = "VideoFileHAS_POSTER_IMAGEImage";
	public static final String ODF_EXPORTER_EXPORTS_TO_FILE                                   = "ODFExporterEXPORTS_TOFile";
	public static final String ODF_EXPORTER_GETS_TRANSFORMATION_FROM_VIRTUAL_TYPE             = "ODFExporterGETS_TRANSFORMATION_FROMVirtualType";
	public static final String ODF_EXPORTER_USES_TEMPLATE_FILE                                = "ODFExporterUSES_TEMPLATEFile";
	public static final String PAYMENT_NODE_PAYMENT_ITEM_PAYMENT_ITEM                         = "PaymentNodepaymentItemPaymentItem";
	public static final String DATA_FEED_HAS_FEED_ITEMS_FEED_ITEM                             = "DataFeedHAS_FEED_ITEMSFeedItem";
	public static final String FEED_ITEM_FEED_ITEM_CONTENTS_FEED_ITEM_CONTENT                 = "FeedItemFEED_ITEM_CONTENTSFeedItemContent";
	public static final String FEED_ITEM_FEED_ITEM_ENCLOSURES_FEED_ITEM_ENCLOSURE             = "FeedItemFEED_ITEM_ENCLOSURESFeedItemEnclosure";
	public static final String OBJECT_EVENT_RELATIONSHIP                                      = "ObjectEventRelationship";
	public static final String SUBJECT_EVENT_RELATIONSHIP                                     = "SubjectEventRelationship";
	public static final String XMPP_CLIENT_REQUEST                                            = "XMPPClientRequest";
	public static final String ABSTRACT_FILE_CONFIGURED_BY_STORAGE_CONFIGURATION              = "AbstractFileCONFIGURED_BYStorageConfiguration";
	public static final String ACTION_MAPPING_PARAMETER_PARAMETER_MAPPING                     = "ActionMappingPARAMETERParameterMapping";
	public static final String CSS_RULE_CONTAINS_CSS_RULE                                     = "CssRuleCONTAINSCssRule";
	public static final String CSS_RULE_HAS_DECLARATION_CSS_DECLARATION                       = "CssRuleHAS_DECLARATIONCssDeclaration";
	public static final String CSS_RULE_HAS_SELECTOR_CSS_SELECTOR                             = "CssRuleHAS_SELECTORCssSelector";
	public static final String CSS_SEMANTIC_CLASS_MAPS_TO_CSS_SELECTOR                        = "CssSemanticClassMAPS_TOCssSelector";
	public static final String DOM_ELEMENT_INPUT_ELEMENT_PARAMETER_MAPPING                    = "DOMElementINPUT_ELEMENTParameterMapping";
	public static final String DOM_ELEMENT_RELOADS_DOM_ELEMENT                                = "DOMElementRELOADSDOMElement";
	public static final String DOM_ELEMENT_TRIGGERED_BY_ACTION_MAPPING                        = "DOMElementTRIGGERED_BYActionMapping";
	public static final String DOM_NODE_CONTAINS_DOM_NODE                                     = "DOMNodeCONTAINSDOMNode";
	public static final String DOM_NODE_CONTAINS_NEXT_SIBLING_DOM_NODE                        = "DOMNodeCONTAINS_NEXT_SIBLINGDOMNode";
	public static final String DOM_NODE_FAILURE_TARGET_ACTION_MAPPING                         = "DOMNodeFAILURE_TARGETActionMapping";
	public static final String DOM_NODE_PAGE_PAGE                                             = "DOMNodePAGEPage";
	public static final String DOM_NODE_SUCCESS_NOTIFICATION_ELEMENT_ACTION_MAPPING           = "DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping";
	public static final String DOM_NODE_FAILURE_NOTIFICATION_ELEMENT_ACTION_MAPPING           = "DOMNodeFAILURE_NOTIFICATION_ELEMENTActionMapping";
	public static final String DOM_NODE_SUCCESS_TARGET_ACTION_MAPPING                         = "DOMNodeSUCCESS_TARGETActionMapping";
	public static final String DOM_NODE_SYNC_DOM_NODE                                         = "DOMNodeSYNCDOMNode";
	public static final String FOLDER_CONTAINS_ABSTRACT_FILE                                  = "FolderCONTAINSAbstractFile";
	public static final String FOLDER_CONTAINS_FILE                                           = "FolderCONTAINSFile";
	public static final String FOLDER_CONTAINS_FOLDER                                         = "FolderCONTAINSFolder";
	public static final String FOLDER_CONTAINS_IMAGE                                          = "FolderCONTAINSImage";
	public static final String IMAGE_PICTURE_OF_USER                                          = "ImagePICTURE_OFUser";
	public static final String IMAGE_THUMBNAIL_IMAGE                                          = "ImageTHUMBNAILImage";
	public static final String PAGE_HAS_PATH_PAGE_PATH                                        = "PageHAS_PATHPagePath";
	public static final String LINK_SOURCE_LINK_LINKABLE                                      = "LinkSourceLINKLinkable";
	public static final String PAGE_PATH_HAS_PARAMETER_PAGE_PATH_PARAMETER                    = "PagePathHAS_PARAMETERPagePathParameter";
	public static final String SITE_CONTAINS_PAGE                                             = "SiteCONTAINSPage";
	public static final String STORAGE_CONFIGURATION_CONFIG_ENTRY_STORAGE_CONFIGURATION_ENTRY = "StorageConfigurationCONFIG_ENTRYStorageConfigurationEntry";
	public static final String USER_HOME_DIR_FOLDER                                           = "UserHOME_DIRFolder";
	public static final String USER_WORKING_DIR_FOLDER                                        = "UserWORKING_DIRFolder";


	public static void registerBaseType(final TraitDefinition definition) {

		final Traits traits = new TraitsImplementation(definition.getName(), true, false, false, false, false);

		traits.registerImplementation(definition, false);
	}

	public static void registerNodeInterface() {

		final Traits traits = new TraitsImplementation(StructrTraits.NODE_INTERFACE, true, true, false, false, false);

		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new NodeInterfaceTraitDefinition(), false);
	}

	public static void registerRelationshipInterface() {

		final Traits traits = new TraitsImplementation(StructrTraits.RELATIONSHIP_INTERFACE, true, false, true, false, false);

		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition(), false);
	}

	public static void registerDynamicNodeType(final String typeName, final boolean changelogEnabled, final boolean isServiceClass, final TraitDefinition... definitions) {

		Traits traits;

		// do not overwrite types
		if (Traits.getAllTypes(null).contains(typeName)) {

			// caution: this might return a relationship type..
			traits = Traits.of(typeName);

		} else {

			traits = new TraitsImplementation(typeName, false, true, false, changelogEnabled, isServiceClass);

			// Node types consist of at least the following traits
			traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
			traits.registerImplementation(new GraphObjectTraitDefinition(), false);
			traits.registerImplementation(new NodeInterfaceTraitDefinition(), false);
			traits.registerImplementation(new AccessControllableTraitDefinition(), false);
		}

		// add implementation (allow extension of existing types)
		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, true);
		}
	}

	public static void registerDynamicRelationshipType(final String typeName, final boolean changelogEnabled, final TraitDefinition... definitions) {

		// do not overwrite types
		if (Traits.getAllTypes(null).contains(typeName)) {
			return;
		}

		final Traits traits = new TraitsImplementation(typeName, false, false, true, changelogEnabled, false);

		// Node types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition(), false);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, true);
		}
	}

	public static void registerNodeType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new TraitsImplementation(typeName, true, true, false, false, false);

		// Node types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new NodeInterfaceTraitDefinition(), false);
		traits.registerImplementation(new AccessControllableTraitDefinition(), false);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, false);
		}
	}

	public static void registerRelationshipType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new TraitsImplementation(typeName, true, false, true, false, false);

		// Relationship types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition(), false);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, false);
		}
	}
}
