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
package org.structr.core.graph;

import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.entity.Folder;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.event.ActionMapping;

import java.util.*;

public class MigrationService {

	private static final Logger logger = LoggerFactory.getLogger(MigrationService.class);

	private static final Set<String> EventActionMappingActions = Set.of(
		"create",
		"update",
		"delete",
		"append-child",
		"remove-child",
		"insert-html",
		"replace-html",
		"open-tree-item",
		"close-tree-item",
		"toggle-tree-item",
		"sign-in",
		"sign-out",
		"sign-up",
		"reset-password",
		"method"
	);

	private static final Set<String> FQCNBlacklist = Set.of(
		"org.structr.web.property.ContentPathProperty",
		"org.structr.core.entity.Favoritable$FavoriteContentProperty",
		"org.structr.core.entity.Favoritable$FavoriteContextProperty",
		"org.structr.core.entity.Favoritable$FavoriteContentTypeProperty"
	);

	private static final Set<String> SchemaPropertyMigrationBlacklist = Set.of(
		"AbstractFile.nextSiblingId",
		"AbstractFile.parentId",
		"Audio._html_mediagroup",
		"DOMElement.",
		"DOMElement.data-structr-action",
		"DOMElement.data-structr-append-id",
		"DOMElement.data-structr-attr",
		"DOMElement.data-structr-attributes",
		"DOMElement.data-structr-confirm",
		"DOMElement.data-structr-custom-options-query",
		"DOMElement.data-structr-edit-class",
		"DOMElement.data-structr-format",
		"DOMElement.data-structr-hide",
		"DOMElement.data-structr-name",
		"DOMElement.data-structr-options",
		"DOMElement.data-structr-options-key",
		"DOMElement.data-structr-placeholder",
		"DOMElement.data-structr-raw-value",
		"DOMElement.data-structr-reload",
		"DOMElement.data-structr-return",
		"DOMNode.flow",
		"DOMNode.hideOnDetail",
		"DOMNode.hideOnIndex",
		"DOMNode.renderDetails",
		"DOMNode.xpathQuery",
		"DOMElement.data-structr-target",
		"DOMElement.data-structr-type",
		"LDAPUser.commonName",
		"LDAPUser.description",
		"LDAPUser.entryUuid",
		"LDAPUser.uid",
		"Localization.description",
		"MQTTClient.port",
		"MQTTClient.protocol",
		"MQTTClient.url",
		"Person.twitterName",
		"Principal.customPermissionQueryAccessControl",
		"Principal.customPermissionQueryDelete",
		"Principal.customPermissionQueryRead",
		"Principal.customPermissionQueryWrite",
		"Principal.twoFactorCode",
		"Textarea._html_maxlenght",
		"User.twitterName",
		"Video._html_mediagroup"
	);

	private static final Set<String> StaticTypeMigrationBlacklist = Set.of(
		"ConceptGroup", "ConceptGroupLabel", "ContentContainer", "ContentItem",
		"CustomConceptAttribute", "CustomNote", "CustomTermAttribute", "Note",
		"SimpleNonPreferredTerm", "StructuredDocument", "StructuredTextNode",
		"Thesaurus", "ThesaurusArray", "ThesaurusTerm", "VersionHistory",
		"Definition", "MetadataNode", "NodeLabel", "ThesaurusConcept",
		"Favoritable", "Indexable", "IndexedWord", "JavaScriptSource",
		"MinifiedCssFile", "MinifiedJavaScriptFile"
	);

	public static void execute() {

		//if (!Services.isTesting() && Services.getInstance().hasExclusiveDatabaseAccess()) {
		if (Services.getInstance().hasExclusiveDatabaseAccess()) {

			migrateStaticSchema();
			migratePrincipalToPrincipalInterface();
			migrateFolderMountTarget();
			migrateEventActionMapping();
			updateSharedComponentFlag();
			warnAboutRestQueryRepeaters();
		}
	}

	public static boolean typeShouldBeRemoved(final String name) {

		if (MigrationService.StaticTypeMigrationBlacklist.contains(name)) {
			return true;
		}

		return false;
	}

	public static boolean propertyShouldBeRemoved(final SchemaProperty property) {

		final AbstractSchemaNode parent  = property.getSchemaNode();
		final String propertyName        = property.getName();
		final String propertyType        = property.getPropertyType().toString().toLowerCase();
		final String fqcn                = property.getFqcn();

		return propertyShouldBeRemoved(parent.getClassName(), propertyName, propertyType, fqcn);
	}

	public static boolean propertyShouldBeRemoved(final String type, final String name, final String propertyType, final String fqcn) {

		if (MigrationService.SchemaPropertyMigrationBlacklist.contains(type + "." + name)) {
			return true;
		}

		// check if property already exists in the static schema
		if (Traits.exists(type)) {

			final Traits traits = Traits.of(type);
			if (traits.hasKey(name) && !traits.key(name).isDynamic()) {

				return true;
			}
		}

		// check if property has been blacklisted
		if ("custom".equals(propertyType)) {

			if (fqcn != null && FQCNBlacklist.contains(fqcn)) {

				return true;
			}
		}

		return false;
	}

	public static boolean methodShouldBeRemoved(final SchemaMethod method) {

		final AbstractSchemaNode parent = method.getSchemaNode();
		final String name               = method.getName();
		final String codeType           = method.getCodeType();

		if (parent != null) {

			return methodShouldBeRemoved(parent.getClassName(), name, codeType);
		}

		// methods with no parent are user-defined functions
		return false;
	}

	public static boolean methodShouldBeRemoved(final String type, final String name, final String codeType) {

		// we don't support Java methods anymore
		if ("java".equals(codeType)) {

			return true;
		}

		return false;
	}

	// ----- private methods -----
	private static void migrateStaticSchema() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			// check (and fix) principal nodes
			logger.info("Checking if static schema needs migration..");

			for (final NodeInterface p : app.nodeQuery(StructrTraits.SCHEMA_NODE).getResultStream()) {

				final SchemaNode schemaNode = p.as(SchemaNode.class);

				if (Boolean.TRUE.equals(schemaNode.getNode().getProperty("isBuiltinType"))) {

					logger.warn("Found built-in schema node {}", schemaNode.getName());

					for (final SchemaProperty property : schemaNode.getSchemaProperties()) {

						if (propertyShouldBeRemoved(property)) {

							logger.info("DELETING schema property {}.{}", schemaNode.getName(), property.getName());
							app.delete(property);
						}
					}

					for (final SchemaMethod method : schemaNode.getSchemaMethods()) {

						if (MigrationService.methodShouldBeRemoved(method)) {

							logger.info("DELETING schema method {}.{}", schemaNode.getName(), method.getName());
							app.delete(method);
						}
					}

					// remove empty schema nodes
					if (Iterables.isEmpty(schemaNode.getSchemaProperties()) && Iterables.isEmpty(schemaNode.getSchemaMethods())) {

						logger.info("DELETING empty schema node {}", schemaNode.getName());
						app.delete(schemaNode);
					}
				}
			}

			tx.success();

		} catch (Throwable fex) {
			logger.warn("Unable to migrate principal nodes: {}", fex.getMessage());
			fex.printStackTrace();
		}
	}

	private static void migratePrincipalToPrincipalInterface() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			// check (and fix) principal nodes
			logger.info("Checking if principal nodes need migration..");

			for (final NodeInterface p : app.nodeQuery(StructrTraits.PRINCIPAL).getResultStream()) {
				p.getNode().addLabels(Set.of(StructrTraits.PRINCIPAL));
			}

			tx.success();

		} catch (Throwable fex) {
			logger.warn("Unable to migrate principal nodes: {}", fex.getMessage());
			fex.printStackTrace();
		}
	}

	private static void migrateEventActionMapping() {

		final App app         = StructrApp.getInstance();
		int structrAppJsCount = 0;
		int eventMappingCount = 0;
		int directionCount    = 0;

		// the following set of schema relationships need to be deleted (because we flipped them)
		final Set<String> relationshipNodeNames = Set.of(
			"ActionMappingTRIGGERED_BYDOMElement",
			"ActionMappingSUCCESS_NOTIFICATION_ELEMENTDOMNode",
			"ActionMappingSUCCESS_TARGETDOMNode",
			"ActionMappingFAILURE_NOTIFICATION_ELEMENTDOMNode",
			"ActionMappingFAILURE_TARGETDOMNode",
			"ParameterMappingINPUT_ELEMENTDOMElement"
		);

		try (final Tx tx = app.tx()) {

			// check (and fix) schema relationships
			logger.info("Checking if event action mapping schema needs migration..");

			for (final String name : relationshipNodeNames) {

				final NodeInterface rel1 = app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).andName(name).getFirst();
				if (rel1 != null) {

					app.delete(rel1);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unable to migrate schema relationships for event action mapping: {}", fex.getMessage());
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			// check (and fix) event action mapping relationships
			logger.info("Checking if event action mapping relationships need migration..");

			final Traits actionMappingTraits                                          = Traits.of(StructrTraits.ACTION_MAPPING);
			final PropertyKey<Iterable<NodeInterface>> triggerElementsKey             = actionMappingTraits.key("triggerElements");
			final PropertyKey<Iterable<NodeInterface>> successTargetsKey              = actionMappingTraits.key("successTargets");
			final PropertyKey<Iterable<NodeInterface>> failureTargetsKey              = actionMappingTraits.key("failureTargets");
			final PropertyKey<Iterable<NodeInterface>> successNotificationElementsKey = actionMappingTraits.key("successNotificationElements");
			final PropertyKey<Iterable<NodeInterface>> failureNotificationElementsKey = actionMappingTraits.key("failureNotificationElements");

			final Traits parameterMappingTraits              = Traits.of(StructrTraits.PARAMETER_MAPPING);
			final PropertyKey<NodeInterface> inputElementKey = parameterMappingTraits.key("inputElement");

			for (final NodeInterface eam : app.nodeQuery(StructrTraits.ACTION_MAPPING).getResultStream()) {

				for (final RelationshipInterface rel : eam.getOutgoingRelationships()) {

					final NodeInterface targetNode = rel.getTargetNode();
					final String relType           = rel.getRelType().name();
					boolean delete                 = false;

					switch (relType) {

						case "TRIGGERED_BY":
							eam.setProperty(triggerElementsKey, List.of(targetNode));
							delete = true;
							break;

						case "SUCCESS_TARGET":
							eam.setProperty(successTargetsKey, List.of(targetNode));
							delete = true;
							break;

						case "FAILURE_TARGET":
							eam.setProperty(failureTargetsKey, List.of(targetNode));
							delete = true;
							break;

						case "SUCCESS_NOTIFICATION_ELEMENT":
							eam.setProperty(successNotificationElementsKey, List.of(targetNode));
							delete = true;
							break;

						case "FAILURE_NOTIFICATION_ELEMENT":
							eam.setProperty(failureNotificationElementsKey, List.of(targetNode));
							delete = true;
							break;

					}

					if (delete) {

						app.delete(rel);
						directionCount++;
					}
				}
			}

			for (final NodeInterface pm : app.nodeQuery(StructrTraits.PARAMETER_MAPPING).getResultStream()) {

				for (final RelationshipInterface rel : pm.getOutgoingRelationships()) {

					final NodeInterface targetNode = rel.getTargetNode();
					final String relType           = rel.getRelType().name();
					boolean delete                 = false;

					switch (relType) {

						case "INPUT_ELEMENT":
							pm.setProperty(inputElementKey, targetNode);
							delete = true;
							break;
					}

					if (delete) {

						app.delete(rel);
						directionCount++;
					}
				}
			}

			final Traits domElementTraits             = Traits.of(StructrTraits.DOM_ELEMENT);
			final PropertyKey<String> actionKey       = new StringProperty("data-structr-action");
			final PropertyKey<String> newActionKey    = actionMappingTraits.key("action");
			final PropertyKey<String> methodKey       = actionMappingTraits.key("method");
			final PropertyKey<String> eventMappingKey = domElementTraits.key("eventMapping");

			// check (and fix if possible) structr-app.js implementations
			logger.info("Checking for structr-app.js implementations that need migration..");

			for (final NodeInterface elem : app.nodeQuery(StructrTraits.DOM_ELEMENT).and().not().and(actionKey, null).getResultStream()) {

				migrateStructrAppMapping(elem, actionKey.jsonName());
				structrAppJsCount++;
			}

			// check (and fix) old event action mappings
			logger.info("Checking for event mapping implementations that need migration..");

			for (final NodeInterface elem : app.nodeQuery(StructrTraits.DOM_ELEMENT).and().not().and(eventMappingKey, null).getResultStream()) {

				migrateEventMapping(elem, eventMappingKey.jsonName());
				eventMappingCount++;
			}

			// check and fix custom actions that call methods (action => "method", method => action)
			for (final NodeInterface action : app.nodeQuery(StructrTraits.ACTION_MAPPING).and().not().and(newActionKey, null).getResultStream()) {

				if (migrateCustomEventAction(action)) {
					eventMappingCount++;
				}
			}

			tx.success();

		} catch (Throwable fex) {
			logger.warn("Unable to migrate schema relationships for event action mapping: {}", fex.getMessage());
			fex.printStackTrace();
		}

		if ((directionCount + eventMappingCount + structrAppJsCount) > 0) {
			logger.info("Migrated {} relationships, {} event mappings and {} structr-app.js settings.", directionCount, eventMappingCount, structrAppJsCount);
		}
	}

	private static void migrateStructrAppMapping(final NodeInterface elem, final String actionKeyName) throws FrameworkException {

		final Map<String, String> options = new LinkedHashMap<>();
		final Map<String, String> data    = new LinkedHashMap<>();
		final PropertyMap properties      = new PropertyMap();
		final String actionSrc            = getAndClearStringValue(elem, actionKeyName);
		final String[] parts              = actionSrc.split(":");
		final String action               = parts[0];
		final String attrs                = getAndClearStringValue(elem, "data-structr-attributes");
		final String returnUrl            = getAndClearStringValue(elem, "data-structr-return");
		final String idExpression         = getAndClearStringValue(elem, "data-structr-id");
		final boolean appendId            = getAndClearBooleanValue(elem, "data-structr-append-id");
		final boolean reload              = getAndClearBooleanValue(elem, "data-structr-reload");
		final boolean confirm             = getAndClearBooleanValue(elem, "data-structr-confirm");
		final Traits actionMappingTraits  = Traits.of(StructrTraits.ACTION_MAPPING);

		// structr-app supported click event only
		properties.put(actionMappingTraits.key("event"), "click");
		properties.put(actionMappingTraits.key("triggerElements"), List.of(elem));

		if (idExpression != null) {
			properties.put(actionMappingTraits.key("idExpression"), idExpression);
		}

		if (parts.length > 1) {
			properties.put(actionMappingTraits.key("dataType"), parts[1]);
		}

		if (StringUtils.isNotBlank(attrs)) {

			for (final String attr : attrs.split(",")) {

				final String trimmed = attr.trim();
				if (StringUtils.isNotBlank(trimmed)) {

					// in the old days, reference was by data-structr-name
					data.put(trimmed, "css(input[data-structr-name=\"" + trimmed + "\"])");
				}
			}
		}

		switch (action) {

			case "create":
				properties.put(actionMappingTraits.key("action"), "create");
				if (reload) {

					if (returnUrl != null) {

						properties.put(actionMappingTraits.key("successBehaviour"), "navigate-to-url");

						if (appendId) {

							properties.put(actionMappingTraits.key("successURL"), returnUrl + "/{result.id}");

						} else {

							properties.put(actionMappingTraits.key("successURL"), returnUrl);
						}

					} else {

						properties.put(actionMappingTraits.key("successBehaviour"), "full-page-reload");
					}
				}
				break;

			case "edit":
				// this is structr-app.js functionality which we cannot migrate :(
				logger.warn("Edit action in structr-app.js format cannot be migrated on {} {}, ignoring.", elem.getType(), elem.getUuid());
				return;

			case "delete":
				properties.put(actionMappingTraits.key("action"), "delete");
				break;

			case "login":
				properties.put(actionMappingTraits.key("action"), "login");
				break;

			case "logout":
				properties.put(actionMappingTraits.key("action"), "logout");
				break;

			default:
				properties.put(actionMappingTraits.key("action"), parts[0]);
				break;
		}

		final NodeInterface actionMapping = StructrApp.getInstance().create(StructrTraits.ACTION_MAPPING, properties);

		migrateParameters(elem, actionMapping, data);
	}

	private static boolean migrateCustomEventAction(final NodeInterface node) throws FrameworkException {

		final ActionMapping actionMapping = node.as(ActionMapping.class);
		final String action               = actionMapping.getAction();

		if (action != null) {

			if (!EventActionMappingActions.contains(action)) {

				// move unknown action name to method property
				actionMapping.setAction("method");
				actionMapping.setMethod(action);

				return true;
			}
		}

		return false;
	}

	private static void migrateEventMapping(final NodeInterface node, final String eventMappingKeyName) throws FrameworkException {

		final Map<String, String> mapping = getAndClearJsonValue(node, eventMappingKeyName);
		final PropertyMap properties      = new PropertyMap();
		final Traits actionMappingTraits  = Traits.of(StructrTraits.ACTION_MAPPING);
		final DOMElement elem             = node.as(DOMElement.class);

		logger.info("Migrating event mapping {} on {} {}", mapping, elem.getType(), elem.getUuid());

		properties.put(actionMappingTraits.key("triggerElements"), List.of(elem));

		for (final String event : mapping.keySet()) {

			final String action = mapping.get(event);

			properties.put(actionMappingTraits.key("action"), action);
			properties.put(actionMappingTraits.key("event"), event);

		}

		final String action = properties.get(actionMappingTraits.key("action"));

		final Map<String, String> settings = new LinkedHashMap<>();
		final Map<String, String> data     = new LinkedHashMap<>();

		for (final PropertyKey<String> key : elem.getDataPropertyKeys()) {

			final String keyName = key.jsonName();

			if (keyName.startsWith("_custom_html_data-")) {

				// map to attributes
				data.put(CaseHelper.dashesToCamelCase(keyName.substring(18)), node.getProperty(key));

				// remove old key
				node.removeProperty(key);
			}

			if (keyName.startsWith("data-structr-")) {

				final String value = node.getProperty(key);
				final String name  = CaseHelper.dashesToCamelCase(keyName.substring(13));

				if ("options".equals(name)) {

					properties.put(actionMappingTraits.key("options"), value);

				} else {

					// map to configuration option
					settings.put(name, value);
				}

				// remove old key
				node.removeProperty(key);
			}
		}

		// map to commands
		switch (action) {

			case "create":
				properties.put(actionMappingTraits.key("dataType"), settings.get("target"));
				break;

			case "delete":
			case "update":
				properties.put(actionMappingTraits.key("idExpression"), settings.get("target"));
				break;

			case "next-page":
				break;

			case "previous-page":
				break;

			default:
				properties.put(actionMappingTraits.key("action"), "method");
				properties.put(actionMappingTraits.key("method"), action);
				properties.put(actionMappingTraits.key("idExpression"), settings.get("target"));
		}

		if (settings.containsKey("reloadTarget")) {

			final String reloadTarget = settings.get("reloadTarget");
			final String[] parts = reloadTarget.split(":");
			final String type    = parts[0];

			switch (type) {

				case "event":
					properties.put(actionMappingTraits.key("successBehaviour"), "fire-event");
					properties.put(actionMappingTraits.key("successEvent"), parts[1]);
					break;

				case "url":
					properties.put(actionMappingTraits.key("successBehaviour"), "navigate-to-url");
					properties.put(actionMappingTraits.key("successURL"), parts[1]);
					break;

				case "none":
					properties.put(actionMappingTraits.key("successBehaviour"), "full-page-reload");
					break;

				default:

					final List<DOMElement> successTargets = new LinkedList<>();

					// first try to find elements with matching IDs etc. to link to
					for (final String target : parts[0].split("[, ]+")) {

						final DOMElement targetElement = findElementWithSelector(node, target.trim());
						if (targetElement != null) {

							successTargets.add(targetElement);
						}
					}

					if (successTargets.isEmpty()) {

						properties.put(actionMappingTraits.key("successBehaviour"), "partial-refresh");
						properties.put(actionMappingTraits.key("successPartial"), parts[0]);

					} else {

						properties.put(actionMappingTraits.key("successBehaviour"), "partial-refresh-linked");
						properties.put(actionMappingTraits.key("successTargets"), successTargets);
					}
					break;
			}
		}

		final NodeInterface actionMapping = StructrApp.getInstance().create(StructrTraits.ACTION_MAPPING, properties);

		migrateParameters(node, actionMapping, data);
	}

	private static void migrateParameters(final NodeInterface elem, final NodeInterface actionMapping, final Map<String, String> parameters) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.PARAMETER_MAPPING);

		for (final String key : parameters.keySet()) {

			final String value           = parameters.get(key);
			final PropertyMap properties = new PropertyMap();

			properties.put(traits.key("actionMapping"),    actionMapping);
			properties.put(traits.key("parameterName"),    key);

			/*
			*/

			if (value.startsWith("css(") && value.endsWith(")")) {

				final String trimmedValue     = value.substring(4, value.length() - 1);
				final DOMElement inputElement = findElementWithSelector(elem, trimmedValue);

				if (inputElement != null) {

					properties.put(traits.key("parameterType"), "user-input");
					properties.put(traits.key("inputElement"),  inputElement);

				} else {

					properties.put(traits.key("parameterType"),    "script-expression");
					properties.put(traits.key("scriptExpression"), value);
				}

			} else if (value.startsWith("name(") && value.endsWith(")")) {

				properties.put(traits.key("parameterType"), "user-input");

				final String trimmedValue     = value.substring(5, value.length() - 1);
				final DOMElement inputElement = findElementWithName(elem, trimmedValue);

				if (inputElement != null) {

					properties.put(traits.key("parameterType"), "user-input");
					properties.put(traits.key("inputElement"),  inputElement);

				} else {

					properties.put(traits.key("parameterType"),    "script-expression");
					properties.put(traits.key("scriptExpression"), value);
				}

			} else {

				properties.put(traits.key("parameterType"), "script-expression");
				properties.put(traits.key("scriptExpression"), value);
			}

			StructrApp.getInstance().create(StructrTraits.PARAMETER_MAPPING, properties);

		}
	}

	private static void updateSharedComponentFlag() {

		final PropertyKey<Boolean> key = Traits.of(StructrTraits.DOM_ELEMENT).key("hasSharedComponent");
		final App app                  = StructrApp.getInstance();
		long count                     = 0L;

		try (final Tx tx = app.tx()) {

			// check (and fix) event action mapping relationships
			logger.info("Checking hasSharedComponent flag..");

			// prefetch dom nodes with sync rels
			tx.prefetch(StructrTraits.DOM_ELEMENT, StructrTraits.DOM_ELEMENT,
				Set.of("all/INCOMING/SYNC",
					"all/OUTGOING/SYNC")
			);

			for (final NodeInterface node : app.nodeQuery(StructrTraits.DOM_ELEMENT).getResultStream()) {

				final DOMElement elem = node.as(DOMElement.class);

				if (!node.getProperty(key) && elem.getSharedComponent() != null) {
					node.setProperty(key, true);
					count++;
				}

			}

			tx.success();

		} catch (Throwable fex) {
			logger.warn("Unable to update hasSharedComponent flag: {}", fex.getMessage());
			fex.printStackTrace();
		}

		if (count > 0) {
			logger.info("Updated {} hasSharedComponent flags", count);
		}
	}

	private static boolean getAndClearBooleanValue(final NodeInterface elem, final String name) throws FrameworkException {

		final PropertyKey<Boolean> key = elem.getTraits().key(name);
		final boolean value            = Boolean.TRUE.equals(elem.getProperty(key));

		elem.removeProperty(key);

		return value;
	}

	private static String getAndClearStringValue(final NodeInterface elem, final String name) throws FrameworkException {

		final PropertyKey<String> key = elem.getTraits().key(name);
		final String value            = elem.getProperty(key);

		elem.removeProperty(key);

		return value;
	}

	private static Map<String, String> getAndClearJsonValue(final NodeInterface elem, final String name) throws FrameworkException {

		final PropertyKey<String> key = elem.getTraits().key(name);
		final String value            = elem.getProperty(key);

		elem.removeProperty(key);

		return new GsonBuilder().create().fromJson(value, Map.class);
	}

	private static DOMElement findElementWithSelector(final NodeInterface elem, final String cssSelector) {

		try {

			final Page page = elem.as(DOMNode.class).getOwnerDocument();

			for (final DOMNode node : page.getElements()) {

				if (node.is(StructrTraits.DOM_ELEMENT)) {

					final DOMElement element   = node.as(DOMElement.class);
					final Element matchElement = DOMElement.getMatchElement(element);

					if (matchElement != null && matchElement.is(cssSelector)) {
						return element;
					}
				}
			}

		} catch (Throwable t) {
			// ignore exception because we cannot do anything about it here
		}

		return null;
	}

	private static DOMElement findElementWithName(final NodeInterface node, final String name) {

		final DOMElement elem = node.as(DOMElement.class);
		final Page page       = elem.getOwnerDocument();

		for (final DOMNode domNode : page.getElements()) {

			if (domNode.is(StructrTraits.DOM_ELEMENT)) {

				final DOMElement element = domNode.as(DOMElement.class);

				if (name.equals(element.getHtmlName())) {
					return element;
				}
			}
		}

		return null;
	}

	private static void warnAboutRestQueryRepeaters() {

		final PropertyKey<String> key = Traits.of(StructrTraits.DOM_ELEMENT).key("restQuery");
		final App app                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			logger.info("Checking for REST query repeaters that need migration..");

			for (final NodeInterface elem : StructrApp.getInstance().nodeQuery(StructrTraits.DOM_ELEMENT).and().not().and(key, null).getResultStream()) {

				final String str     = elem.getProperty(key);
				final String cleaned = str.replaceAll("[\\W0-9]+", "");

				if (Character.isLowerCase(cleaned.charAt(0))) {

					logger.info("REST repeater query in {} element with UUID {} might need migration: {}. This cannot be done automatically, please check and change.", elem.getType(), elem.getUuid(), elem.getProperty(key));
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unable to check migration status for REST query repeaters: {}", fex.getMessage());
		}
	}

	private static void migrateFolderMountTarget() {

		final Traits storageConfigurationTraits = Traits.of("StorageConfiguration");
		final Traits folderTraits               = Traits.of(StructrTraits.FOLDER);
		final App app                           = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> mountedFolders = app.nodeQuery(StructrTraits.FOLDER)
				.notBlank(folderTraits.key("mountTarget"))
				.getAsList();

			if (!mountedFolders.isEmpty()) {
				logger.info("Migrating {} folders with old mountTarget property to respective storage configurations.", mountedFolders.size());
			}

			for (NodeInterface node : mountedFolders) {

				final Folder folder = node.as(Folder.class);

				final NodeInterface config = app.create("StorageConfiguration",
					new NodeAttribute<>(storageConfigurationTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), folder.getFolderPath())
				);

				app.create("StorageConfigurationEntry",
					new NodeAttribute<>(storageConfigurationTraits.key("configuration"), config),
					new NodeAttribute<>(storageConfigurationTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "mountTarget"),
					new NodeAttribute<>(storageConfigurationTraits.key("value"),         folder.getMountTarget())
				);

				folder.setProperty(folderTraits.key("storageConfiguration"), config);
				folder.setProperty(folderTraits.key("mountTarget"), null);
			}

			tx.success();
		} catch (Throwable t) {

			logger.warn("Failed to migrate mountTarget for folders.", t);
		}
	}
}
