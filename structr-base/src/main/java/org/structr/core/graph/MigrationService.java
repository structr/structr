/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.api.DatabaseFeature;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.web.entity.Folder;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.traits.definitions.*;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;

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
		"method",
		"flow",
		"control-process"
	);

	private static final Set<String> FQCNBlacklist = Set.of(
		"org.structr.web.property.ContentPathProperty",
		"org.structr.core.entity.Favoritable$FavoriteContentProperty",
		"org.structr.core.entity.Favoritable$FavoriteContextProperty",
		"org.structr.core.entity.Favoritable$FavoriteContentTypeProperty"
	);

	private static final Set<String> SchemaPropertyMigrationBlacklist = Set.of(
		"AbstractFile.isMounted",
		"AbstractFile.name",
		"AbstractFile.nextSiblingId",
		"AbstractFile.parentId",
		"Audio._html_mediagroup",
		"Content.content",
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
		"DOMNode.childrenIds",
		"DOMNode.pageId",
		"DOMNode.parentId",
		"DOMNode.sortedChildren",
		"DOMNode.syncedNodesIds",
		"DOMNode.xpathQuery",
		"DOMElement._html_id",
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
		"PaymentNode.state",
		"Person.twitterName",
		"Principal.currentAccessToken",
		"Principal.customPermissionQueryAccessControl",
		"Principal.customPermissionQueryDelete",
		"Principal.customPermissionQueryRead",
		"Principal.customPermissionQueryWrite",
		"Principal.twoFactorCode",
		"Textarea._html_maxlenght",
		"User.twitterName",
		"Video._html_mediagroup",

		// 4.2 migration
		"DataFeed.description",
		"DataFeed.feedType",
		"FeedItem.checksum",
		"FeedItem.pubDate",
		"FeedItem.updatedDate",
		"FeedItem.url",
		"FeedItem.version",
		"Image.tnMid",
		"Image.tnSmall",
		"LinkSource.linkableId",
		"Linkable.linkingElementsIds",
		"Localization.imported",
		"Mailbox.mailProtocol",
		"RemoteDocument.version",
		"Template.content",
		"VideoFile.duration",
		"VideoFile.height",
		"VideoFile.sampleRate",
		"VideoFile.width",
		"Widget.isPageTemplate",
		"Widget.treePath",
		"XMPPClient.presenceMode",
		"XMPPRequest.requestType"
	);

	public static final Set<String> StaticTypeMigrationBlacklist = Set.of(
		"ConceptGroup", "ConceptGroupLabel", "ContentContainer", "ContentItem",
		"CustomConceptAttribute", "CustomNote", "CustomTermAttribute", "Note",
		"SimpleNonPreferredTerm", "StructuredDocument", "StructuredTextNode",
		"Thesaurus", "ThesaurusArray", "ThesaurusTerm", "VersionHistory",
		"Definition", "MetadataNode", "NodeLabel", "ThesaurusConcept",
		"Favoritable", "Indexable", "IndexedWord", "JavaScriptSource",
		"MinifiedCssFile", "MinifiedJavaScriptFile", "LDAPGroup",
		"LDAPUser", "PaymentItemNode", "PaymentNode", "Person"
	);

	public static void execute() {

		//if (!Services.isTesting() && Services.getInstance().hasExclusiveDatabaseAccess()) {
		if (Services.getInstance().hasExclusiveDatabaseAccess()) {

			migrateStaticSchema();
			migratePrincipalToPrincipalInterface();
			migrateFolderMountTarget();
			migrateEventActionMapping();
			migrateActionMappingTargetsToRelationships();
			cleanStaleActionMappingTargets();
			migrateMailTemplates();
			updateSharedComponentFlag();
			if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {
				migrateRestQueryRepeaters();
				migrateActionMappingControlsToProcess();
				migrateVisibilityMappingForToProcess();
			}
			warnAboutWrongNotionProperties();
		}
	}

	public static boolean typeShouldBeRemoved(final String name) {

		return MigrationService.StaticTypeMigrationBlacklist.contains(name);
	}

	public static boolean propertyShouldBeRemoved(final SchemaProperty property) {

		final AbstractSchemaNode parent  = property.getSchemaNode();
		final String propertyName        = property.getName();
		final String propertyType        = property.getPropertyType().toString().toLowerCase();
		final String fqcn                = property.getFqcn();

		return propertyShouldBeRemoved(property, parent.getClassName(), propertyName, propertyType, fqcn);
	}

	public static boolean propertyShouldBeRemoved(final SchemaProperty property, final String type, final String name, final String propertyType, final String fqcn) {

		if (MigrationService.SchemaPropertyMigrationBlacklist.contains(type + "." + name)) {
			return true;
		}

		// check if property already exists in the static schema
		if (Traits.exists(type)) {

			final Traits traits = Traits.of(type);
			if (traits.hasKey(name) && !traits.key(name).isDynamic()) {

				if (property != null) {

					final PropertyKey key = traits.key(name);

					if (property.isIndexed() != key.isIndexed()) {

						logger.info("Allowing {} to override {} property to change indexing flag.", type, name);
						return false;
					}

					if (property.isFulltext() != key.isFulltextIndexed()) {

						logger.info("Allowing {} to override {} property to change fulltext indexing flag.", type, name);
						return false;
					}

					if (property.isUnique() != key.isUnique()) {

						logger.info("Allowing {} to override {} property to change uniqueness constraint flag.", type, name);
						return false;
					}

					if (property.getFormat() != null && !property.getFormat().equals(key.format())) {

						logger.info("Allowing {} to override {} property to change format constraint.", type, name);
						return false;
					}
				}

				return true;
			}
		}

		// check if property has been blacklisted
		if ("custom".equals(propertyType)) {

			return fqcn != null && FQCNBlacklist.contains(fqcn);
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
		return "java".equals(codeType);
	}

	// ----- private methods -----
	private static void migrateStaticSchema() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			// check (and fix) principal nodes
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

					// remove empty or blacklisted schema nodes
					if (typeShouldBeRemoved(schemaNode.getName()) || (Iterables.isEmpty(schemaNode.getSchemaProperties()) && Iterables.isEmpty(schemaNode.getSchemaMethods()))) {

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
			for (final String name : relationshipNodeNames) {

				final NodeInterface rel1 = app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).name(name).getFirst();
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
			final Traits actionMappingTraits                                          = Traits.of(StructrTraits.ACTION_MAPPING);
			final PropertyKey<Iterable<NodeInterface>> triggerElementsKey             = actionMappingTraits.key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY);
			final PropertyKey<Iterable<NodeInterface>> successTargetsKey              = actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY);
			final PropertyKey<Iterable<NodeInterface>> failureTargetsKey              = actionMappingTraits.key(ActionMappingTraitDefinition.FAILURE_TARGETS_PROPERTY);
			final PropertyKey<Iterable<NodeInterface>> successNotificationElementsKey = actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATION_ELEMENTS_PROPERTY);
			final PropertyKey<Iterable<NodeInterface>> failureNotificationElementsKey = actionMappingTraits.key(ActionMappingTraitDefinition.FAILURE_NOTIFICATION_ELEMENTS_PROPERTY);

			final Traits parameterMappingTraits              = Traits.of(StructrTraits.PARAMETER_MAPPING);
			final PropertyKey<NodeInterface> inputElementKey = parameterMappingTraits.key(ParameterMappingTraitDefinition.INPUT_ELEMENT_PROPERTY);

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

			final Traits domElementTraits                 = Traits.of(StructrTraits.DOM_ELEMENT);
			final PropertyKey<String> reloadTargetKey     = new StringProperty(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);
			final PropertyKey<String> actionKey           = new StringProperty("data-structr-action");
			final PropertyKey<String> successBehaviourKey = actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY);
			final PropertyKey<String> newActionKey        = actionMappingTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY);
			final PropertyKey<String> methodKey           = actionMappingTraits.key(ActionMappingTraitDefinition.METHOD_PROPERTY);
			final PropertyKey<String> eventMappingKey     = domElementTraits.key(DOMElementTraitDefinition.EVENT_MAPPING_PROPERTY);

			// check (and fix if possible) structr-app.js implementations
			for (final NodeInterface elem : app.nodeQuery(StructrTraits.DOM_ELEMENT).and().not().key(actionKey, null).getResultStream()) {

				migrateStructrAppMapping(elem, actionKey.jsonName());
				structrAppJsCount++;
			}

			// check (and fix) old event action mappings
			for (final NodeInterface elem : app.nodeQuery(StructrTraits.DOM_ELEMENT).and().not().key(eventMappingKey, null).getResultStream()) {

				migrateEventMapping(elem, eventMappingKey.jsonName());
				eventMappingCount++;
			}

			// check and fix custom actions that call methods (action => "method", method => action)
			for (final NodeInterface action : app.nodeQuery(StructrTraits.ACTION_MAPPING).and().not().key(newActionKey, null).getResultStream()) {

				if (migrateCustomEventAction(action)) {
					eventMappingCount++;
				}
			}

			// check and fix custom actions that miss successBehaviour or targetBehaviour
			for (final NodeInterface action : app.nodeQuery(StructrTraits.ACTION_MAPPING).key(successBehaviourKey, null).getResultStream()) {

				if (migrateReloadBehaviourAction(action)) {
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

	/**
	 * Migrate ActionMapping nodes from string-only target references to graph
	 * relationships. For each ActionMapping with non-empty {@code method} /
	 * {@code flow} / {@code dataType} string properties, look up the corresponding
	 * SchemaMethod / FlowContainer / SchemaNode by name and write the relationship
	 * if not already set. The strings remain in place (kept as the authoring
	 * surface and as a fallback for unresolvable targets).
	 *
	 * <p>This re-runs harmlessly: nodes with relationships already set are skipped
	 * by the "if not already populated" check. Failed lookups (target node missing)
	 * are logged at debug level and leave the relationship null, matching runtime
	 * fallback behaviour.</p>
	 */
	private static void migrateActionMappingTargetsToRelationships() {

		final App app   = StructrApp.getInstance();
		int resolved    = 0;
		int unresolved  = 0;

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			final Traits actionMappingTraits = Traits.of(StructrTraits.ACTION_MAPPING);
			final PropertyKey<String> methodKey            = actionMappingTraits.key(ActionMappingTraitDefinition.METHOD_PROPERTY);
			final PropertyKey<String> flowKey              = actionMappingTraits.key(ActionMappingTraitDefinition.FLOW_PROPERTY);
			final PropertyKey<String> dataTypeKey          = actionMappingTraits.key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY);
			final PropertyKey<NodeInterface> methodNodeKey   = actionMappingTraits.key(ActionMappingTraitDefinition.METHOD_NODE_PROPERTY);
			final PropertyKey<NodeInterface> flowNodeKey     = actionMappingTraits.key(ActionMappingTraitDefinition.FLOW_NODE_PROPERTY);
			final PropertyKey<NodeInterface> dataTypeNodeKey = actionMappingTraits.key(ActionMappingTraitDefinition.DATA_TYPE_NODE_PROPERTY);

			final Traits schemaMethodTraits = Traits.of(StructrTraits.SCHEMA_METHOD);
			final PropertyKey<NodeInterface> schemaMethodSchemaNodeKey = schemaMethodTraits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY);

			for (final NodeInterface eam : app.nodeQuery(StructrTraits.ACTION_MAPPING).getResultStream()) {

				final String methodName   = eam.getProperty(methodKey);
				final String flowName     = eam.getProperty(flowKey);
				final String dataTypeName = eam.getProperty(dataTypeKey);

				// dataType -> SchemaNode
				NodeInterface resolvedSchemaNode = eam.getProperty(dataTypeNodeKey);
				if (resolvedSchemaNode == null && StringUtils.isNotBlank(dataTypeName)) {
					resolvedSchemaNode = app.nodeQuery(StructrTraits.SCHEMA_NODE).name(dataTypeName).getFirst();
					if (resolvedSchemaNode != null) {
						eam.setProperty(dataTypeNodeKey, resolvedSchemaNode);
						resolved++;
					} else {
						logger.warn("MigrationService: ActionMapping {} references dataType '{}' but no SchemaNode with that name exists. Relationship not set; verify the type name and resave or use the UI selector once available.", eam.getUuid(), dataTypeName);
						unresolved++;
					}
				}

				// flow -> FlowContainer
				if (eam.getProperty(flowNodeKey) == null && StringUtils.isNotBlank(flowName)) {
					final NodeInterface resolvedFlow = app.nodeQuery(StructrTraits.FLOW_CONTAINER).name(flowName).getFirst();
					if (resolvedFlow != null) {
						eam.setProperty(flowNodeKey, resolvedFlow);
						resolved++;
					} else {
						logger.warn("MigrationService: ActionMapping {} references flow '{}' but no FlowContainer with that name exists. Relationship not set; verify the flow name and resave or use the UI selector once available.", eam.getUuid(), flowName);
						unresolved++;
					}
				}

				// method -> SchemaMethod (scoped by dataType when present, else top-level only)
				if (eam.getProperty(methodNodeKey) == null && StringUtils.isNotBlank(methodName)) {

					NodeInterface resolvedMethod = null;
					if (resolvedSchemaNode != null) {

						// Tier 1: explicit dataType, search methods on that type only
						for (final NodeInterface candidate : app.nodeQuery(StructrTraits.SCHEMA_METHOD).name(methodName).getResultStream()) {
							final NodeInterface parent = candidate.getProperty(schemaMethodSchemaNodeKey);
							if (parent != null && parent.getUuid().equals(resolvedSchemaNode.getUuid())) {
								resolvedMethod = candidate;
								break;
							}
						}

					} else {

						// Tier 2: no dataType, accept only a top-level (parent-less) SchemaMethod.
						// Methods on specific types are NOT auto-resolved without a declared dataType:
						// inferring by name uniqueness would weaken the App Graph as a validation surface.
						for (final NodeInterface candidate : app.nodeQuery(StructrTraits.SCHEMA_METHOD).name(methodName).getResultStream()) {
							final NodeInterface parent = candidate.getProperty(schemaMethodSchemaNodeKey);
							if (parent == null) {
								resolvedMethod = candidate;
								break;
							}
						}
					}

					if (resolvedMethod != null) {
						eam.setProperty(methodNodeKey, resolvedMethod);
						resolved++;
					} else {
						// Diagnostic: collect candidate types to help the user fix the binding.
						final List<String> candidateTypes = new ArrayList<>();
						for (final NodeInterface candidate : app.nodeQuery(StructrTraits.SCHEMA_METHOD).name(methodName).getResultStream()) {
							final NodeInterface parent = candidate.getProperty(schemaMethodSchemaNodeKey);
							candidateTypes.add(parent != null ? parent.getName() : "(top-level)");
						}

						// Distinguish Java-method references (built-in engine methods like claim,
						// complete, signalEvent, terminate, ...) from genuinely missing methods.
						// Java methods are valid runtime targets but have no SchemaMethod node, so
						// the relationship stays null by design: they are platform code, not data.
						// The runtime dispatcher (Methods.resolveMethod) finds them via idExpression.
						final List<String> javaMethodTypes = findTypesWithJavaMethod(methodName);

						if (resolvedSchemaNode != null) {
							logger.warn("MigrationService: ActionMapping {} references method '{}' on type '{}', but no SchemaMethod with that name exists on that type. Candidate types where the method exists: {}. Relationship not set; verify the binding manually.", eam.getUuid(), methodName, dataTypeName, candidateTypes);
							unresolved++;
						} else if (candidateTypes.isEmpty() && javaMethodTypes.isEmpty()) {
							logger.warn("MigrationService: ActionMapping {} references method '{}' but no SchemaMethod with that name exists anywhere in the schema. Relationship not set; verify the method name and resave.", eam.getUuid(), methodName);
							unresolved++;
						} else if (candidateTypes.isEmpty() && !javaMethodTypes.isEmpty()) {
							// Built-in Java method (claim / complete / signalEvent / ...). The binding
							// is valid: runtime resolution via idExpression finds the Java method.
							// Log INFO so the user knows we deliberately skipped the relationship.
							logger.info("MigrationService: ActionMapping {} references built-in Java method '{}' on types {}. No SchemaMethod node; relationship intentionally left null. Runtime dispatch via idExpression resolves the call.", eam.getUuid(), methodName, javaMethodTypes);
						} else {
							// dataType not declared, no top-level match, but SchemaMethod nodes exist on
							// specific types. Don't guess by name uniqueness: the user must pick the target
							// explicitly via dataType (or, eventually, the UI selector).
							logger.warn("MigrationService: ActionMapping {} references method '{}' without a dataType and no top-level SchemaMethod with that name exists. Candidate types where the method is defined: {}. Relationship not set; declare a dataType or use the UI selector once available to bind the target explicitly.", eam.getUuid(), methodName, candidateTypes);
							unresolved++;
						}
					}
				}
			}

			tx.success();

		} catch (Throwable fex) {
			logger.warn("Unable to migrate ActionMapping target relationships: {}", fex.getMessage());
			fex.printStackTrace();
		}

		if (resolved > 0) {
			logger.info("Migrated {} ActionMapping target string(s) to graph relationships.", resolved);
		}
		if (unresolved > 0) {
			logger.warn("MigrationService: {} ActionMapping target string(s) could not be resolved automatically. See WARN messages above for details and required fixes.", unresolved);
		}
	}

	/**
	 * One-time hygiene pass that clears string properties (and via the OnModification
	 * lifecycle, the corresponding relationships) on ActionMapping nodes when those
	 * strings are not relevant to the node's current {@code action}. Stale strings can
	 * accumulate across edit cycles when the editor changes {@code action} from one
	 * value to another without clearing previously-set fields. Once the EAM editor is
	 * updated to clear irrelevant fields on action change, this cleanup becomes a
	 * one-shot historical fix; until then it runs on every server start and is
	 * idempotent (does nothing once data is clean).
	 *
	 * <p>Action-to-fields mapping (only the listed fields are kept; others are
	 * cleared):
	 * <ul>
	 *   <li>{@code create}: keeps {@code dataType}.</li>
	 *   <li>{@code method}: keeps {@code method} and {@code dataType} (used as a
	 *       static-method type qualifier when set).</li>
	 *   <li>{@code flow}: keeps {@code flow}.</li>
	 *   <li>{@code control-process}: clears {@code method} and {@code flow};
	 *       keeps {@code dataType} (the {@code completeWithSubject}
	 *       operation uses it to pick the SchemaNode type that a userTask
	 *       creates as the process subject); keeps the process-control
	 *       properties ({@code controlsProcess}, {@code targetsElement},
	 *       {@code processOperation}) which are not part of this cleanup
	 *       pass (they have no legacy string counterparts).</li>
	 *   <li>All other declared actions ({@code update}, {@code delete}, sign-in
	 *       family, page-navigation family, child/html-manipulation family): clear
	 *       all three of {@code method}, {@code flow}, {@code dataType}.</li>
	 *   <li>Unknown / null / non-canonical {@code action}: leave alone (we cannot
	 *       safely decide what is stale for incomplete or legacy nodes).</li>
	 * </ul>
	 */
	private static void cleanStaleActionMappingTargets() {

		// Whitelist per action: which of {method, flow, dataType} are relevant.
		// Note: control-process has its own dedicated properties (controlsProcess,
		// targetsElement, processOperation) which are not part of this cleanup;
		// `method` and `flow` are unused by it. `dataType` IS used by the
		// `completeWithSubject` operation as the SchemaNode type to instantiate
		// as the new process subject -- keep it so the editor's selection
		// survives a server restart.
		final Set<String> METHOD_RELEVANT    = Set.of("method");
		final Set<String> FLOW_RELEVANT      = Set.of("flow");
		final Set<String> DATA_TYPE_RELEVANT = Set.of("create", "method", "control-process");

		final App app   = StructrApp.getInstance();
		int cleaned     = 0;

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			final Traits actionMappingTraits = Traits.of(StructrTraits.ACTION_MAPPING);
			final PropertyKey<String> actionKey   = actionMappingTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY);
			final PropertyKey<String> methodKey   = actionMappingTraits.key(ActionMappingTraitDefinition.METHOD_PROPERTY);
			final PropertyKey<String> flowKey     = actionMappingTraits.key(ActionMappingTraitDefinition.FLOW_PROPERTY);
			final PropertyKey<String> dataTypeKey = actionMappingTraits.key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY);

			for (final NodeInterface eam : app.nodeQuery(StructrTraits.ACTION_MAPPING).getResultStream()) {

				final String action = eam.getProperty(actionKey);

				// Skip nodes whose action is unknown or unset: we cannot safely decide
				// what is stale for incomplete or legacy data.
				if (StringUtils.isBlank(action) || !EventActionMappingActions.contains(action)) {
					continue;
				}

				boolean changed = false;

				if (!METHOD_RELEVANT.contains(action) && StringUtils.isNotBlank(eam.getProperty(methodKey))) {
					logger.info("Cleaning stale 'method' on ActionMapping {} (action='{}'): was '{}'", eam.getUuid(), action, eam.getProperty(methodKey));
					eam.setProperty(methodKey, null);
					changed = true;
				}
				if (!FLOW_RELEVANT.contains(action) && StringUtils.isNotBlank(eam.getProperty(flowKey))) {
					logger.info("Cleaning stale 'flow' on ActionMapping {} (action='{}'): was '{}'", eam.getUuid(), action, eam.getProperty(flowKey));
					eam.setProperty(flowKey, null);
					changed = true;
				}
				if (!DATA_TYPE_RELEVANT.contains(action) && StringUtils.isNotBlank(eam.getProperty(dataTypeKey))) {
					logger.info("Cleaning stale 'dataType' on ActionMapping {} (action='{}'): was '{}'", eam.getUuid(), action, eam.getProperty(dataTypeKey));
					eam.setProperty(dataTypeKey, null);
					changed = true;
				}

				if (changed) {
					cleaned++;
				}
			}

			tx.success();

		} catch (Throwable fex) {
			logger.warn("Unable to clean stale ActionMapping target strings: {}", fex.getMessage());
			fex.printStackTrace();
		}

		if (cleaned > 0) {
			logger.info("Cleaned stale targets on {} ActionMapping node(s). Corresponding relationships were also cleared by the OnModification lifecycle.", cleaned);
		}
	}

	/**
	 * Find every registered type that has a method (Java or SchemaMethod-backed)
	 * with the given name. Caller-side context: this is invoked only when no
	 * SchemaMethod node with the name exists in the database, so any hits here
	 * must be Java methods (built-in engine code) rather than SchemaMethod-backed.
	 *
	 * <p>The result distinguishes "this name resolves to built-in engine code at
	 * runtime" (relationship intentionally null because Java methods are not graph
	 * nodes) from "this name doesn't resolve anywhere" (real user error). Returns
	 * the type names where the method exists; empty if nowhere.</p>
	 */
	private static List<String> findTypesWithJavaMethod(final String methodName) {

		final List<String> hits = new ArrayList<>();
		if (methodName == null || methodName.isEmpty()) {
			return hits;
		}

		try {
			for (final String typeName : Traits.getAllTypes()) {
				try {
					final Traits t = Traits.of(typeName);
					if (t == null) continue;
					final org.structr.core.api.AbstractMethod m = org.structr.core.api.Methods.resolveMethod(t, methodName);
					if (m != null) {
						hits.add(typeName);
					}
				} catch (Exception inner) {
					// Type-level lookup failures should not break the migration.
				}
			}
		} catch (Exception ex) {
			logger.debug("findTypesWithJavaMethod failed for '{}': {}", methodName, ex.getMessage());
		}

		return hits;
	}

	private static void migrateMailTemplates() {

		final App app = StructrApp.getInstance();

		String[][] renamedKeys = {
			{ "CONFIRM_REGISTRATION_SUCCESS_PATH",    "CONFIRM_REGISTRATION_TARGET_PAGE" },
			{ "CONFIRM_REGISTRATION_FAILURE_PATH",    "CONFIRM_REGISTRATION_ERROR_PAGE" },
			{ "CONFIRM_REGISTRATION_TARGET_PATH_KEY", "CONFIRM_REGISTRATION_TARGET_PAGE_KEY" },
			{ "RESET_PASSWORD_TARGET_PATH",           "RESET_PASSWORD_TARGET_PAGE" },
			{ "RESET_PASSWORD_TARGET_PATH_KEY",       "RESET_PASSWORD_TARGET_PAGE_KEY" }
		};

		try (final Tx tx = app.tx()) {

			for (final String[] renamedKey : renamedKeys) {
				final String oldKey = renamedKey[0];
				final String newKey = renamedKey[1];

				for (final NodeInterface mailTemplate : app.nodeQuery(StructrTraits.MAIL_TEMPLATE).getResultStream()) {
					if (oldKey.equals(mailTemplate.getName())) {
						mailTemplate.setName(newKey);

						logger.info("Updated mail template {} to {} (value: {})", oldKey, newKey, mailTemplate.getProperty(Traits.key("MailTemplate", "text")));
					}
				}
			}


			tx.success();

		} catch (Throwable fex) {
			logger.warn("Unable to migrate mail templates: {}", fex.getMessage());
			fex.printStackTrace();
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
		final String idExpression         = getAndClearStringValue(elem, DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY);
		final boolean appendId            = getAndClearBooleanValue(elem, "data-structr-append-id");
		final boolean reload              = getAndClearBooleanValue(elem, "data-structr-reload");
		final boolean confirm             = getAndClearBooleanValue(elem, "data-structr-confirm");
		final Traits actionMappingTraits  = Traits.of(StructrTraits.ACTION_MAPPING);

		// structr-app supported click event only
		properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
		properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(elem));

		if (idExpression != null) {
			properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY), idExpression);
		}

		if (parts.length > 1) {
			properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), parts[1]);
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
				properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
				if (reload) {

					if (returnUrl != null) {

						properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "navigate-to-url");

						if (appendId) {

							properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_URL_PROPERTY), returnUrl + "/{result.id}");

						} else {

							properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_URL_PROPERTY), returnUrl);
						}

					} else {

						properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "full-page-reload");
					}
				}
				break;

			case "edit":
				// this is structr-app.js functionality which we cannot migrate :(
				logger.warn("Edit action in structr-app.js format cannot be migrated on {} {}, ignoring.", elem.getType(), elem.getUuid());
				return;

			case "delete":
				properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), "delete");
				break;

			case "login":
				properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), "login");
				break;

			case "logout":
				properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), "logout");
				break;

			default:
				properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), parts[0]);
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

	private static boolean migrateReloadBehaviourAction(final NodeInterface node) throws FrameworkException {

		final ActionMapping actionMapping = node.as(ActionMapping.class);
		final String successBehaviour     = actionMapping.getSuccessBehaviour();
		final String failureBehaviour     = actionMapping.getFailureBehaviour();
		boolean hasChanges                = false;

		if (StringUtils.isBlank(successBehaviour)) {

			actionMapping.setSuccessBehaviour("full-page-reload");
			hasChanges = true;
		}

		if (StringUtils.isBlank(failureBehaviour)) {

			actionMapping.setFailureBehaviour("none");
			hasChanges = true;
		}

		return hasChanges;
	}

	private static void migrateEventMapping(final NodeInterface node, final String eventMappingKeyName) throws FrameworkException {

		final Map<String, String> mapping = getAndClearJsonValue(node, eventMappingKeyName);
		final PropertyMap properties      = new PropertyMap();
		final Traits actionMappingTraits  = Traits.of(StructrTraits.ACTION_MAPPING);
		final DOMElement elem             = node.as(DOMElement.class);

		logger.info("Migrating event mapping {} on {} {}", mapping, elem.getType(), elem.getUuid());

		properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(elem));

		for (final String event : mapping.keySet()) {

			final String action = mapping.get(event);

			properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), action);
			properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.EVENT_PROPERTY), event);

		}

		final String action = properties.get(actionMappingTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY));

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

				Object value       = node.getProperty(key);
				final String name  = CaseHelper.dashesToCamelCase(keyName.substring(13));

				// convert on the fly
				if (value != null) {

					if ("options".equals(name)) {

						properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.OPTIONS_PROPERTY), value.toString());

					} else {

						// map to configuration option
						settings.put(name, value.toString());
					}
				}

				// remove old key
				node.removeProperty(key);
			}
		}

		// map to commands
		switch (action) {

			case "create":
				properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), settings.get("target"));
				break;

			case "delete":
			case "update":
				properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY), settings.get("target"));
				break;

			case "next-page":
				break;

			case "previous-page":
				break;

			default:
				properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), "method");
				properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.METHOD_PROPERTY), action);
				properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY), settings.get("target"));
		}

		if (settings.containsKey("reloadTarget")) {

			final String reloadTarget = settings.get("reloadTarget");
			final String[] parts = reloadTarget.split(":");
			final String type    = parts[0];

			switch (type) {

				case "event":
					properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "fire-event");
					properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_EVENT_PROPERTY), parts[1]);
					break;

				case "url":
					properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "navigate-to-url");
					properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_URL_PROPERTY), parts[1]);
					break;

				case "none":
					properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "full-page-reload");
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

						properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "partial-refresh");
						properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_PARTIAL_PROPERTY), parts[0]);

					} else {

						properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "partial-refresh-linked");
						properties.put(actionMappingTraits.key(ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY), successTargets);
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

			properties.put(traits.key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY),    actionMapping);
			properties.put(traits.key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY),    key);

			/*
			*/

			if (value.startsWith("css(") && value.endsWith(")")) {

				final String trimmedValue     = value.substring(4, value.length() - 1);
				final DOMElement inputElement = findElementWithSelector(elem, trimmedValue);

				if (inputElement != null) {

					properties.put(traits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "user-input");
					properties.put(traits.key(ParameterMappingTraitDefinition.INPUT_ELEMENT_PROPERTY),  inputElement);

				} else {

					properties.put(traits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY),    "script-expression");
					properties.put(traits.key(ParameterMappingTraitDefinition.SCRIPT_EXPRESSION_PROPERTY), value);
				}

			} else if (value.startsWith("name(") && value.endsWith(")")) {

				properties.put(traits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "user-input");

				final String trimmedValue     = value.substring(5, value.length() - 1);
				final DOMElement inputElement = findElementWithName(elem, trimmedValue);

				if (inputElement != null) {

					properties.put(traits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "user-input");
					properties.put(traits.key(ParameterMappingTraitDefinition.INPUT_ELEMENT_PROPERTY),  inputElement);

				} else {

					properties.put(traits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY),    "script-expression");
					properties.put(traits.key(ParameterMappingTraitDefinition.SCRIPT_EXPRESSION_PROPERTY), value);
				}

			} else {

				properties.put(traits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "script-expression");
				properties.put(traits.key(ParameterMappingTraitDefinition.SCRIPT_EXPRESSION_PROPERTY), value);
			}

			StructrApp.getInstance().create(StructrTraits.PARAMETER_MAPPING, properties);

		}
	}

	private static void updateSharedComponentFlag() {

		final PropertyKey<Boolean> key = Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HAS_SHARED_COMPONENT_PROPERTY);
		final App app                  = StructrApp.getInstance();
		long count                     = 0L;

		try (final Tx tx = app.tx()) {

			// prefetch dom nodes with sync rels
			tx.prefetch(StructrTraits.DOM_NODE, StructrTraits.DOM_NODE,
				Set.of("all/INCOMING/SYNC",
					"all/OUTGOING/SYNC")
			);

			// check (and fix) event action mapping relationships
			for (final NodeInterface node : app.nodeQuery(StructrTraits.DOM_NODE).getResultStream()) {

				final DOMNode elem = node.as(DOMNode.class);

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

	private static void warnAboutWrongNotionProperties() {

		final PropertyKey<String> typeKey   = Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY);
		final PropertyKey<String> formatKey = Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.FORMAT_PROPERTY);
		final App app                       = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface property : StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_PROPERTY).key(typeKey, "Notion").getResultStream()) {

				final SchemaProperty schemaProperty = property.as(SchemaProperty.class);
				final AbstractSchemaNode type       = schemaProperty.getSchemaNode();
				final String format                 = property.getProperty(formatKey);

				if (format != null && format.contains("Property")) {

					logger.info("Notion property {}.{} might need migration because the format string contains 'Property'. This cannot be done automatically, please check and change: {}", type.getName(), property.getName(), format);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unable to check migration status for REST query repeaters: {}", fex.getMessage());
		}
	}

	private static void migrateRestQueryRepeaters() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final List<GraphObject> objects = Iterables.toList(app.query("MATCH (n:DOMNode) WHERE n.restQuery IS NOT null RETURN n", Map.of()));

			for (final GraphObject object : objects) {

				final PropertyContainer propertyContainer = object.getPropertyContainer();
				final String restQuery                    = (String) propertyContainer.getProperty("restQuery");
				final String functionQuery                = (String) propertyContainer.getProperty("functionQuery");

				if (StringUtils.isEmpty(functionQuery) && StringUtils.isNotEmpty(restQuery)) {

					logger.info("MIGRATING rest QUERY in {} {} to functionQuery.", object.getType(), object.getUuid());
					propertyContainer.setProperty("functionQuery", "{\n\t/* Migrated REST query, please fix! */\n\t/* " + restQuery + " */\n\n\t$.log('Repeater in ', $.this.type, ' with UUID ', $.this.id, ' needs manual migration of restQuery to functionQuery!');\n}");
					propertyContainer.removeProperty("restQuery");

				} else {

					logger.info("NOT migrating rest QUERY in {} {} because functionQuery is non-empty!", object.getType(), object.getUuid());
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unable to migrate REST query repeaters: {}", fex.getMessage());
		}
	}

	/**
	 * Repoint legacy {@code ActionMapping -[CONTROLS]-> BpmnDefinitions} edges to
	 * the contained {@code BpmnProcess}. Before the multi-process refactor the
	 * CONTROLS rel targeted the definitions file; now it targets one specific
	 * BpmnProcess (definitions can hold multiple processes via collaborations).
	 *
	 * <p>The Neo4j label ("CONTROLS") is unchanged; only the trait's declared
	 * target type was switched, so the framework no longer surfaces the legacy
	 * edges via {@code ActionMapping.getControlsProcess()}. We find them through
	 * direct Cypher, delete the stale edges, and re-set the property through
	 * the typed API so the new rel gets the framework metadata it needs.</p>
	 *
	 * <p>Target-selection rules per source BpmnDefinitions:
	 * <ul>
	 *   <li>Single child BpmnProcess: pick it.</li>
	 *   <li>Multiple children, exactly one marked {@code processIsExecutable}:
	 *       pick that one.</li>
	 *   <li>Otherwise: leave the source ActionMapping unset and log a warning
	 *       so the author can fix it via the EAM editor.</li>
	 * </ul></p>
	 */
	private static void migrateActionMappingControlsToProcess() {

		final App app = StructrApp.getInstance();
		int repointed = 0;
		int skipped   = 0;

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			final List<GraphObject> staleDefs = Iterables.toList(app.query(
				"MATCH (:ActionMapping)-[:CONTROLS]->(bd:BpmnDefinitions) RETURN DISTINCT bd",
				Map.of()
			));

			if (staleDefs.isEmpty()) {
				tx.success();
				return;
			}

			logger.info("MigrationService: found {} BpmnDefinitions with legacy ActionMapping CONTROLS edges, repointing to BpmnProcess.", staleDefs.size());

			final PropertyKey<NodeInterface> controlsProcessKey = Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.CONTROLS_PROCESS_PROPERTY);

			for (final GraphObject bdObj : staleDefs) {

				if (!(bdObj instanceof NodeInterface)) continue;
				final NodeInterface bd = (NodeInterface) bdObj;
				final String bdId      = bd.getUuid();

				// Pick the target BpmnProcess: single child, or unique executable.
				final Iterable<NodeInterface> processes = bd.getProperty(bd.getTraits().key("processes"));
				final List<NodeInterface> all  = new ArrayList<>();
				final List<NodeInterface> exec = new ArrayList<>();
				if (processes != null) {
					for (final NodeInterface p : processes) {
						all.add(p);
						final Boolean isExec = p.getProperty(p.getTraits().key("processIsExecutable"));
						if (Boolean.TRUE.equals(isExec)) exec.add(p);
					}
				}

				NodeInterface target = null;
				if (all.size() == 1) {
					target = all.get(0);
				} else if (exec.size() == 1) {
					target = exec.get(0);
				}

				// Find the ActionMappings that currently CONTROL this definitions.
				final List<GraphObject> ams = Iterables.toList(app.query(
					"MATCH (am:ActionMapping)-[:CONTROLS]->(:BpmnDefinitions {id:$bdId}) RETURN DISTINCT am",
					Map.of("bdId", bdId)
				));

				// Drop the legacy edges first (trait change hides them from
				// the typed API; we delete via Cypher).
				app.query(
					"MATCH (:ActionMapping)-[r:CONTROLS]->(:BpmnDefinitions {id:$bdId}) DELETE r",
					Map.of("bdId", bdId)
				);

				if (target == null) {
					logger.warn("MigrationService: BpmnDefinitions {} has {} BpmnProcess children ({} marked executable); cannot decide which to repoint to. {} ActionMapping(s) left without a controlsProcess -- fix via the EAM editor.", bdId, all.size(), exec.size(), ams.size());
					skipped += ams.size();
					continue;
				}

				for (final GraphObject amObj : ams) {
					if (!(amObj instanceof NodeInterface)) continue;
					final NodeInterface am = (NodeInterface) amObj;
					am.setProperty(controlsProcessKey, target);
					repointed++;
				}
			}

			logger.info("MigrationService: ActionMapping CONTROLS rels: repointed={}, skipped={}.", repointed, skipped);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unable to migrate ActionMapping CONTROLS rels to BpmnProcess: {}", fex.getMessage());
		}
	}

	/**
	 * Repoint legacy VisibilityMapping FOR rels from BpmnDefinitions to the
	 * matching BpmnProcess child. Mirrors {@link #migrateActionMappingControlsToProcess()}.
	 */
	private static void migrateVisibilityMappingForToProcess() {

		final App app = StructrApp.getInstance();
		int repointed = 0;
		int skipped   = 0;

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			final List<GraphObject> staleDefs = Iterables.toList(app.query(
				"MATCH (:VisibilityMapping)-[:FOR]->(bd:BpmnDefinitions) RETURN DISTINCT bd",
				Map.of()
			));

			if (staleDefs.isEmpty()) {
				tx.success();
				return;
			}

			logger.info("MigrationService: found {} BpmnDefinitions with legacy VisibilityMapping FOR edges, repointing to BpmnProcess.", staleDefs.size());

			final PropertyKey<NodeInterface> boundProcessKey = Traits.of("VisibilityMapping").key("boundProcess");

			for (final GraphObject bdObj : staleDefs) {

				if (!(bdObj instanceof NodeInterface)) continue;
				final NodeInterface bd = (NodeInterface) bdObj;
				final String bdId      = bd.getUuid();

				final Iterable<NodeInterface> processes = bd.getProperty(bd.getTraits().key("processes"));
				final List<NodeInterface> all  = new ArrayList<>();
				final List<NodeInterface> exec = new ArrayList<>();
				if (processes != null) {
					for (final NodeInterface p : processes) {
						all.add(p);
						final Boolean isExec = p.getProperty(p.getTraits().key("processIsExecutable"));
						if (Boolean.TRUE.equals(isExec)) exec.add(p);
					}
				}

				NodeInterface target = null;
				if (all.size() == 1) {
					target = all.get(0);
				} else if (exec.size() == 1) {
					target = exec.get(0);
				}

				final List<GraphObject> vms = Iterables.toList(app.query(
					"MATCH (vm:VisibilityMapping)-[:FOR]->(:BpmnDefinitions {id:$bdId}) RETURN DISTINCT vm",
					Map.of("bdId", bdId)
				));

				app.query(
					"MATCH (:VisibilityMapping)-[r:FOR]->(:BpmnDefinitions {id:$bdId}) DELETE r",
					Map.of("bdId", bdId)
				);

				if (target == null) {
					logger.warn("MigrationService: BpmnDefinitions {} has {} BpmnProcess children ({} marked executable); cannot decide which to repoint to. {} VisibilityMapping(s) left without a boundProcess; fix via the VM editor.", bdId, all.size(), exec.size(), vms.size());
					skipped += vms.size();
					continue;
				}

				for (final GraphObject vmObj : vms) {
					if (!(vmObj instanceof NodeInterface)) continue;
					final NodeInterface vm = (NodeInterface) vmObj;
					vm.setProperty(boundProcessKey, target);
					repointed++;
				}
			}

			logger.info("MigrationService: VisibilityMapping FOR rels: repointed={}, skipped={}.", repointed, skipped);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unable to migrate VisibilityMapping FOR rels to BpmnProcess: {}", fex.getMessage());
		}
	}

	private static void migrateFolderMountTarget() {

		final Traits storageConfigurationTraits      = Traits.of(StructrTraits.STORAGE_CONFIGURATION);
		final Traits storageConfigurationEntryTraits = Traits.of(StructrTraits.STORAGE_CONFIGURATION_ENTRY);
		final Traits folderTraits                    = Traits.of(StructrTraits.FOLDER);
		final App app                                = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> mountedFolders = app.nodeQuery(StructrTraits.FOLDER)
				.notBlank(folderTraits.key(FolderTraitDefinition.MOUNT_TARGET_PROPERTY))
				.getAsList();

			if (!mountedFolders.isEmpty()) {
				logger.info("Migrating {} folders with old mountTarget property to respective storage configurations.", mountedFolders.size());
			}

			for (NodeInterface node : mountedFolders) {

				final Folder folder = node.as(Folder.class);

				final NodeInterface config = app.create(StructrTraits.STORAGE_CONFIGURATION,
						new NodeAttribute<>(storageConfigurationTraits.key(StorageConfigurationTraitDefinition.NAME_PROPERTY), folder.getFolderPath()),
						new NodeAttribute<>(storageConfigurationTraits.key(StorageConfigurationTraitDefinition.PROVIDER_PROPERTY), "org.structr.storage.providers.local.LocalFSStorageProvider")
				);

				app.create(StructrTraits.STORAGE_CONFIGURATION_ENTRY,
					new NodeAttribute<>(storageConfigurationEntryTraits.key(StorageConfigurationEntryTraitDefinition.CONFIGURATION_PROPERTY), config),
					new NodeAttribute<>(storageConfigurationEntryTraits.key(StorageConfigurationEntryTraitDefinition.NAME_PROPERTY),          "mountTarget"),
					new NodeAttribute<>(storageConfigurationEntryTraits.key(StorageConfigurationEntryTraitDefinition.VALUE_PROPERTY),         folder.getMountTarget())
				);

				folder.setProperty(folderTraits.key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), config);
				folder.setProperty(folderTraits.key(FolderTraitDefinition.MOUNT_TARGET_PROPERTY), null);
			}

			tx.success();
		} catch (Throwable t) {

			logger.warn("Failed to migrate mountTarget for folders.", t);
		}
	}
}
