/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.process.deployment;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Deployment export/import for the design-time BPMN graph, plugged into
 * {@code DeployCommand} through {@code ProcessModule}'s module deployment hook (the same
 * extension point the flow module uses). Writes a single {@code bpmn-deployment.json} into
 * {@code modules/process/}.
 *
 * <h3>Why this exists</h3>
 *
 * <p>{@code DeployCommand}'s export set is hard-coded and knows nothing about BPMN, so before
 * this handler a deployment archive contained the generated process page but no process: every
 * BpmnDefinitions/BpmnProcess/BpmnElement was missing, every VisibilityMapping was gone, and
 * the three references that tie the UI to a process -- {@code ComponentConfiguration.boundUserTask},
 * {@code ActionMapping.controlsProcess} and {@code ActionMapping.targetsElement} -- were dropped
 * silently. The page still rendered, but every step was unconditionally visible and its buttons
 * did nothing. Nothing failed loudly, which is what made it hard to notice.</p>
 *
 * <h3>What travels</h3>
 *
 * <p>The design-time graph only ({@link #NODE_TYPES}): definitions, processes, elements, flows,
 * lanes, collaborations, participants, message flows, DI shapes/edges, performers, listeners and
 * VisibilityMappings. Runtime state -- ProcessInstance, ProcessToken, TaskInstance, ProcessTimer,
 * ProcessParameterValue -- is deliberately NOT exported: deploying half-finished instances into
 * another installation would be meaningless at best. Those types are also never deleted on
 * import, so runtime data on the target survives (but see the ordering note below).</p>
 *
 * <p>Relationships come in three flavours:</p>
 * <ul>
 * <li>{@link #REL_TYPES} -- both endpoints identified by UUID. This works because deployment
 *     preserves UUIDs on both sides: BPMN nodes are recreated here with their exported id, and
 *     Pages/DOMNodes/ComponentConfigurations/ActionMappings keep theirs through
 *     {@code DeployCommand}. It includes the UI bridges, which is the whole point.</li>
 * <li>{@link #METHOD_REL_TYPES} -- the target SchemaMethod is resolved by NAME, not UUID,
 *     because {@code StructrGlobalSchemaMethods} deletes and recreates every global method on
 *     import, giving them new UUIDs. Resolving by name is safe precisely because handler names
 *     are scoped per process, version and element (see
 *     {@link org.structr.process.bpmn.BpmnHandlerNames}) and therefore unique.</li>
 * <li>Relationships to nodes outside the deployment (a Principal on a BpmnPerformer, say) are
 *     exported too but skipped with a warning when the endpoint is absent on the target.</li>
 * </ul>
 *
 * <p>Finally, {@link #ACTION_MAPPING_PATCH_KEYS} carries the process-specific properties that
 * the process module registers on the base ActionMapping type ({@code processOperation},
 * {@code controlsProcessId}, ...). {@code DeployCommand} cannot know about them -- they only
 * exist when this module is loaded -- so the module that registers them also deploys them.
 * Without {@code processOperation} a restored button posts a "control-process" action with no
 * operation, which looks like a working button and is not.</p>
 *
 * <h3>Import never destroys node identity</h3>
 *
 * <p>Unlike every other deployment importer (list data, flows), this one does NOT delete and
 * recreate. It upserts by UUID and prunes what the archive no longer contains, because
 * deploying a new version of an application must not disturb processes that are currently
 * running: a ProcessInstance points at its BpmnProcess and a TaskInstance at its BpmnElement,
 * and recreating those nodes would silently orphan every in-flight instance and task -- a
 * production data-loss bug, not a deployment detail. See {@link #reconcile}.</p>
 *
 * <p>Three consequences worth knowing:</p>
 * <ul>
 * <li>VisibilityMappings are an exception to "updated in place", through no fault of this
 *     handler: {@code DOMNodeHASVisibilityMapping} is SOURCE_TO_TARGET, so when DeployCommand
 *     deletes the pages it is about to re-import, the mappings cascade away with them. They are
 *     then recreated here from the archive WITH THEIR ORIGINAL UUIDs, which keeps the result
 *     identical and is harmless because a mapping carries no runtime state. It is why an import
 *     report shows them as "created" even when nothing changed.</li>
 * <li>An obsolete design-time node that runtime state still refers to is KEPT rather than
 *     pruned, and reported. A definition instances are still executing outlives its removal
 *     from the application; it disappears once those instances are gone.</li>
 * <li>Pruning runs with {@code doCascadingDelete(false)}. HAS_METHOD is SOURCE_TO_TARGET, so
 *     deleting a process or element would otherwise cascade into the handler SchemaMethods
 *     {@code DeployCommand} has just recreated from {@code _globalMethods} -- deleting the
 *     user's handler code as a side effect of importing it.</li>
 * </ul>
 */
public class BpmnDeploymentHandler {

	private static final Logger logger = LoggerFactory.getLogger(BpmnDeploymentHandler.class.getName());

	public static final String DEPLOYMENT_FILE_NAME = "bpmn-deployment.json";

	/** Bumped when the file layout changes incompatibly; import refuses a version it cannot read. */
	private static final int FORMAT_VERSION = 1;

	private static final String KEY_VERSION       = "version";
	private static final String KEY_NODES         = "nodes";
	private static final String KEY_RELATIONSHIPS = "relationships";
	private static final String KEY_PATCHES       = "propertyPatches";
	private static final String KEY_TYPE          = "type";
	private static final String KEY_ID            = "id";
	private static final String KEY_SOURCE_ID     = "sourceId";
	private static final String KEY_TARGET_ID     = "targetId";
	private static final String KEY_METHOD_NAME   = "targetMethodName";
	private static final String KEY_PROPERTIES    = "properties";

	/** Design-time types. Runtime state (instances, tokens, tasks, timers) is out of scope. */
	private static final String[] NODE_TYPES = {

		ProcessTraits.BPMN_DEFINITIONS,
		ProcessTraits.BPMN_PROCESS,
		ProcessTraits.BPMN_COLLABORATION,
		ProcessTraits.BPMN_PARTICIPANT,
		ProcessTraits.BPMN_MESSAGE_FLOW,
		ProcessTraits.BPMN_LANE,
		ProcessTraits.BPMN_ELEMENT,
		ProcessTraits.BPMN_SEQUENCE_FLOW,
		ProcessTraits.BPMN_GLOBAL_DEFINITION,
		ProcessTraits.BPMN_PERFORMER,
		ProcessTraits.BPMN_TASK_LISTENER,
		ProcessTraits.BPMN_PROCESS_LISTENER,
		ProcessTraits.BPMN_DI_DIAGRAM,
		ProcessTraits.BPMN_DI_SHAPE,
		ProcessTraits.BPMN_DI_EDGE,
		ProcessTraits.VISIBILITY_MAPPING
	};

	/** Relationships whose endpoints are both identified by UUID. */
	private static final String[] REL_TYPES = {

		// BPMN internal structure
		ProcessTraits.BPMN_DEFINITIONS_HAS_PROCESS,
		ProcessTraits.BPMN_DEFINITIONS_HAS_COLLABORATION,
		ProcessTraits.BPMN_DEFINITIONS_HAS_DIAGRAM,
		ProcessTraits.BPMN_DEFINITIONS_HAS_GLOBAL_DEFINITION,
		ProcessTraits.BPMN_PROCESS_HAS_ELEMENT,
		ProcessTraits.BPMN_PROCESS_HAS_SEQUENCE_FLOW,
		ProcessTraits.BPMN_PROCESS_HAS_PROCESS_LISTENER,
		ProcessTraits.BPMN_PROCESS_HAS_LANE,
		ProcessTraits.BPMN_ELEMENT_HAS_CHILD_ELEMENT,
		ProcessTraits.BPMN_ELEMENT_HAS_CHILD_FLOW,
		ProcessTraits.BPMN_ELEMENT_HAS_PERFORMER,
		ProcessTraits.BPMN_ELEMENT_HAS_TASK_LISTENER,
		ProcessTraits.BPMN_ELEMENT_ATTACHED_TO,
		ProcessTraits.BPMN_SEQUENCE_FLOW_FROM,
		ProcessTraits.BPMN_SEQUENCE_FLOW_TO,
		ProcessTraits.BPMN_MESSAGE_FLOW_FROM,
		ProcessTraits.BPMN_MESSAGE_FLOW_TO,
		ProcessTraits.BPMN_LANE_HAS_FLOW_NODE,
		ProcessTraits.BPMN_COLLABORATION_HAS_PARTICIPANT,
		ProcessTraits.BPMN_COLLABORATION_HAS_MESSAGE_FLOW,
		ProcessTraits.BPMN_PARTICIPANT_OF_PROCESS,
		ProcessTraits.BPMN_DI_DIAGRAM_HAS_SHAPE,
		ProcessTraits.BPMN_DI_DIAGRAM_HAS_EDGE,
		ProcessTraits.BPMN_DI_SHAPE_REFERENCES_ELEMENT,
		ProcessTraits.BPMN_DI_EDGE_REFERENCES_FLOW,

		// out of the BPMN graph into deployed nodes (Page keeps its UUID, Principal may be absent)
		ProcessTraits.BPMN_PROCESS_HAS_INSTANCE_PAGE, ProcessTraits.BPMN_PERFORMER_HAS_PRINCIPAL,

		// the UI bridges -- what made a deployed process page inert without this handler
		StructrTraits.DOM_NODE_HAS_VISIBILITY_MAPPING,
		ProcessTraits.VISIBILITY_MAPPING_FOR_BPMN_PROCESS,
		ProcessTraits.VISIBILITY_MAPPING_AT_BPMN_ELEMENT,
		StructrTraits.COMPONENT_CONFIGURATION_BOUND_BPMN_ELEMENT,
		StructrTraits.ACTION_MAPPING_CONTROLS_BPMN_PROCESS,
		StructrTraits.ACTION_MAPPING_TARGETS_BPMN_ELEMENT
	};

	/** Relationships whose target is a SchemaMethod, resolved by name (see class comment). */
	private static final String[] METHOD_REL_TYPES = {

		ProcessTraits.BPMN_PROCESS_HAS_METHOD, ProcessTraits.BPMN_ELEMENT_HAS_METHOD, ProcessTraits.BPMN_TASK_LISTENER_CALLS_METHOD, ProcessTraits.BPMN_PROCESS_LISTENER_CALLS_METHOD
	};

	/** Runtime state: never exported, and a reason to keep an obsolete design-time node alive. */
	private static final Set<String> RUNTIME_TYPES = Set.of(
		ProcessTraits.PROCESS_INSTANCE,
		ProcessTraits.TASK_INSTANCE,
		ProcessTraits.PROCESS_TOKEN,
		ProcessTraits.PROCESS_TIMER,
		ProcessTraits.PROCESS_PARAMETER_VALUE
	);

	/** {@link #METHOD_REL_TYPES} as a set, for the relationship reconciliation lookups. */
	private static final Set<String> METHOD_REL_TYPE_SET = Set.of(METHOD_REL_TYPES);

	/** Process-specific keys this module registers on the base ActionMapping type. */
	private static final String[] ACTION_MAPPING_PATCH_KEYS = {

		ActionMappingTraitDefinition.PROCESS_OPERATION_PROPERTY,
		ActionMappingTraitDefinition.CONTROLS_PROCESS_ID_PROPERTY,
		ActionMappingTraitDefinition.TARGETS_ELEMENT_BPMN_ID_PROPERTY,
		ActionMappingTraitDefinition.CONTROLS_PROCESS_ID_EXPRESSION_PROPERTY
	};

	/**
	 * Not exported: bookkeeping that would make every export differ from the last one and add
	 * noise to deployment diffs, without carrying any application meaning.
	 */
	private static final Set<String> SKIPPED_NODE_PROPERTIES = Set.of("createdDate", "lastModifiedDate", "createdBy", "lastModifiedBy", "structrChangeLog");

	// ----- export -----

	public static void doExport(final Path target, final Gson gson) throws FrameworkException {

		final App app                                   = StructrApp.getInstance();
		final List<Map<String, Object>> nodes           = new LinkedList<>();
		final List<Map<String, Object>> relationships   = new LinkedList<>();
		final List<Map<String, Object>> patches         = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (final String type : NODE_TYPES) {

				for (final NodeInterface node : app.nodeQuery(type).getAsList()) {

					// a type query also yields inheriting types; each node is exported once,
					// under the type it actually has
					if (type.equals(node.getType())) {

						nodes.add(exportNode(node));
					}
				}
			}

			for (final String type : REL_TYPES) {

				for (final RelationshipInterface rel : app.relationshipQuery(type).getAsList()) {

					final Map<String, Object> entry = new TreeMap<>();

					entry.put(KEY_TYPE,      type);
					entry.put(KEY_SOURCE_ID, rel.getSourceNodeId());
					entry.put(KEY_TARGET_ID, rel.getTargetNodeId());

					relationships.add(entry);
				}
			}

			for (final String type : METHOD_REL_TYPES) {

				for (final RelationshipInterface rel : app.relationshipQuery(type).getAsList()) {

					final NodeInterface method = rel.getTargetNode();
					if (method == null) {

						continue;
					}

					final Map<String, Object> entry = new TreeMap<>();

					entry.put(KEY_TYPE,        type);
					entry.put(KEY_SOURCE_ID,   rel.getSourceNodeId());
					entry.put(KEY_METHOD_NAME, method.getName());

					relationships.add(entry);
				}
			}

			patches.addAll(exportActionMappingPatches(app));

			tx.success();
		}

		// deterministic order, so re-exporting an unchanged application produces an identical file
		nodes.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get(KEY_TYPE)))
			.thenComparing(m -> String.valueOf(m.get(KEY_ID))));

		relationships.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get(KEY_TYPE)))
			.thenComparing(m -> String.valueOf(m.get(KEY_SOURCE_ID)))
			.thenComparing(m -> String.valueOf(m.get(KEY_TARGET_ID)))
			.thenComparing(m -> String.valueOf(m.get(KEY_METHOD_NAME))));

		patches.sort(Comparator.comparing(m -> String.valueOf(m.get(KEY_ID))));

		final Map<String, Object> document = new LinkedHashMap<>();

		document.put(KEY_VERSION,       FORMAT_VERSION);
		document.put(KEY_NODES,         nodes);
		document.put(KEY_RELATIONSHIPS, relationships);
		document.put(KEY_PATCHES,       patches);

		final Path file = target.resolve(DEPLOYMENT_FILE_NAME);

		try (final Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {

			gson.toJson(document, writer);

		} catch (IOException ioex) {

			logger.warn("Unable to write {}", file, ioex);
		}

		logger.info("Exported {} BPMN node(s), {} relationship(s), {} property patch(es)", nodes.size(), relationships.size(), patches.size());
	}

	private static Map<String, Object> exportNode(final NodeInterface node) {

		final Map<String, Object> entry      = new TreeMap<>();
		final PropertyContainer<?> container = node.getPropertyContainer();

		for (final String key : container.getPropertyKeys()) {

			if (SKIPPED_NODE_PROPERTIES.contains(key)) {

				continue;
			}

			final Object value = container.getProperty(key);
			if (value != null) {

				entry.put(key, value);
			}
		}

		// the raw container may or may not carry these; both are required to recreate the node
		entry.put(KEY_ID,   node.getUuid());
		entry.put(KEY_TYPE, node.getType());

		return entry;
	}

	private static List<Map<String, Object>> exportActionMappingPatches(final App app) throws FrameworkException {

		final List<Map<String, Object>> patches = new LinkedList<>();
		final Traits traits                     = Traits.of(StructrTraits.ACTION_MAPPING);

		for (final NodeInterface actionMapping : app.nodeQuery(StructrTraits.ACTION_MAPPING).getAsList()) {

			final Map<String, Object> properties = new TreeMap<>();

			for (final String key : ACTION_MAPPING_PATCH_KEYS) {

				if (!traits.hasKey(key)) {

					continue;
				}

				final Object value = actionMapping.getProperty(traits.key(key));
				if (value != null) {

					properties.put(key, value);
				}
			}

			if (!properties.isEmpty()) {

				final Map<String, Object> entry = new TreeMap<>();

				entry.put(KEY_ID,         actionMapping.getUuid());
				entry.put(KEY_TYPE,       StructrTraits.ACTION_MAPPING);
				entry.put(KEY_PROPERTIES, properties);

				patches.add(entry);
			}
		}

		return patches;
	}

	// ----- import -----

	public static void doImport(final Path source, final Gson gson) throws FrameworkException {

		final Path file = source.resolve(DEPLOYMENT_FILE_NAME);
		if (!Files.exists(file)) {

			return;
		}

		logger.info("Reading {}..", file);

		final Map<String, Object> document;

		try (final Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {

			document = gson.fromJson(reader, Map.class);

		} catch (IOException ioex) {

			logger.warn("Unable to read {}", file, ioex);

			return;
		}

		if (document == null) {

			return;
		}

		final int version = asInt(document.get(KEY_VERSION));
		if (version > FORMAT_VERSION) {

			throw new FrameworkException(422, "Cannot import " + DEPLOYMENT_FILE_NAME + ": format version " + version
				+ " is newer than the supported version " + FORMAT_VERSION + ". Please update Structr.");
		}

		final List<Map<String, Object>> nodes         = asList(document.get(KEY_NODES));
		final List<Map<String, Object>> relationships = asList(document.get(KEY_RELATIONSHIPS));
		final List<Map<String, Object>> patches       = asList(document.get(KEY_PATCHES));
		final ImportReport report = reconcile(nodes, relationships, patches);

		report.log();
	}

	/**
	 * What an import actually did, so a deployment cannot fail quietly.
	 *
	 * <p>The bug this whole handler fixes was invisible: the page rendered, the buttons were dead,
	 * and nothing was logged. An unrestorable reference is therefore counted and reported at WARN
	 * with the reason -- a deployment that half-worked has to say so, because the alternative is
	 * someone concluding the process engine doesn't work and never mentioning it.</p>
	 */
	private static final class ImportReport {

		private int nodesCreated       = 0;
		private int nodesUpdated       = 0;
		private int nodesPruned        = 0;
		private int nodesKept          = 0;
		private int relationshipsAdded = 0;
		private int relationshipsRemoved = 0;
		private int unresolvedReferences = 0;
		private int unknownPatchKeys    = 0;

		private void log() {

			logger.info("BPMN deployment import: {} node(s) created, {} updated, {} pruned, {} kept (still in use), {} relationship(s) added, {} removed",
				nodesCreated, nodesUpdated, nodesPruned, nodesKept, relationshipsAdded, relationshipsRemoved);

			if (nodesKept > 0) {

				logger.info("{} obsolete BPMN node(s) were kept because running process instances still refer to them; they are removed once those instances are gone", nodesKept);
			}

			if (unresolvedReferences > 0) {

				logger.warn("{} process reference(s) could not be restored -- the deployed application is INCOMPLETE: pages bound to a process may show all steps at once, "
					+ "and process-control buttons may do nothing. See the warnings above for the individual references.", unresolvedReferences);
			}

			if (unknownPatchKeys > 0) {

				logger.warn("{} process property/properties from the archive are unknown to this installation -- it may be older than the one that produced the export", unknownPatchKeys);
			}
		}
	}

	/**
	 * Bring the design-time graph in line with the archive WITHOUT destroying node identity: a
	 * node that already exists is updated in place, never deleted and recreated.
	 *
	 * <p>This is the whole point of the import being written this way. Deploying a new version of
	 * an application onto a live installation must not disturb processes that are currently
	 * running: a ProcessInstance points at its BpmnProcess and a TaskInstance at its BpmnElement,
	 * and those relationships live on nodes this importer touches. Recreating the nodes -- what
	 * every other deployment importer does, and what this one did first -- would silently orphan
	 * every in-flight instance and task. Upserting by UUID keeps them attached.</p>
	 *
	 * <p>It also makes re-import naturally version-aware. A source instance that re-imports a
	 * changed BPMN file keeps the old version and adds a new one, so the archive contains both:
	 * the already-deployed version matches by UUID and is left alone (instances on it keep
	 * running), and the new version arrives as new nodes.</p>
	 */
	private static ImportReport reconcile(final List<Map<String, Object>> nodes, final List<Map<String, Object>> relationships, final List<Map<String, Object>> patches) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();

		context.setDoTransactionNotifications(false);
		context.setIgnoreMissingNodesInDeserialization(true);

		// HAS_METHOD is SOURCE_TO_TARGET: without this, deleting an obsolete process or element
		// would cascade into the handler SchemaMethods DeployCommand has just recreated from
		// _globalMethods -- deleting the user's handler code as a side effect of importing it.
		context.setDoCascadingDelete(false);

		final App app             = StructrApp.getInstance(context);
		final ImportReport report = new ImportReport();

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			final Set<String> importedIds = upsertNodes(app, context, nodes, report);

			pruneNodes(app, importedIds, report);
			reconcileRelationships(app, relationships, report);

			for (final Map<String, Object> entry : patches) {

				applyPatch(app, entry, report);
			}

			tx.success();

		} finally {

			context.setIgnoreMissingNodesInDeserialization(false);
		}

		return report;
	}

	/**
	 * Create the nodes that are new and update the ones that already exist, keyed by UUID.
	 * Returns every id seen in the archive, which {@link #pruneNodes} needs.
	 */
	private static Set<String> upsertNodes(final App app, final SecurityContext context, final List<Map<String, Object>> nodes, final ImportReport report) throws FrameworkException {

		final Set<String> importedIds = new LinkedHashSet<>();

		for (final Map<String, Object> entry : nodes) {

			final String type = (String) entry.get(KEY_TYPE);
			final String id   = (String) entry.get(KEY_ID);

			if (type == null || id == null) {

				continue;
			}

			importedIds.add(id);

			final PropertyMap map      = PropertyMap.inputTypeToJavaType(context, type, new LinkedHashMap<>(entry));
			final NodeInterface exists = app.getNodeById(id);

			if (exists == null) {

				app.create(type, map);
				report.nodesCreated++;

			} else if (type.equals(exists.getType())) {

				// update in place -- the node keeps its identity, and with it every
				// relationship that runtime state holds on it
				exists.setProperties(context, map);
				report.nodesUpdated++;

			} else {

				report.unresolvedReferences++;
				logger.warn("Not importing BPMN node {}: a node with that id already exists with type {} (archive says {})", id, exists.getType(), type);
			}
		}

		return importedIds;
	}

	/**
	 * Delete managed nodes the archive no longer contains -- unless runtime state still refers to
	 * them, in which case they are kept and reported. A process definition that instances are
	 * still executing outlives its removal from the application; dropping it would break exactly
	 * the running processes this importer is careful not to disturb.
	 */
	private static void pruneNodes(final App app, final Set<String> importedIds, final ImportReport report) throws FrameworkException {

		for (final String type : NODE_TYPES) {

			for (final NodeInterface node : app.nodeQuery(type).getAsList()) {

				if (!type.equals(node.getType()) || importedIds.contains(node.getUuid())) {

					continue;
				}

				if (isReferencedByRuntimeState(node)) {

					report.nodesKept++;
					continue;
				}

				app.delete(node);
				report.nodesPruned++;
			}
		}

	}

	/** Whether any relationship of this node leads to runtime state (an instance, task, token, timer). */
	private static boolean isReferencedByRuntimeState(final NodeInterface node) {

		for (final RelationshipInterface rel : node.getRelationships()) {

			final NodeInterface other = (rel.getSourceNode() == node) ? rel.getTargetNode() : rel.getSourceNode();
			if (other == null) {

				continue;
			}

			if (RUNTIME_TYPES.contains(other.getType())) {

				return true;
			}
		}

		return false;
	}

	/**
	 * Make the managed relationships match the archive: add what is missing, remove what the
	 * archive no longer has. Relationships to runtime state are not managed here and are left
	 * untouched -- they are not in {@link #REL_TYPES}.
	 */
	private static void reconcileRelationships(final App app, final List<Map<String, Object>> relationships, final ImportReport report) throws FrameworkException {

		final Map<String, Map<String, Object>> desired = new LinkedHashMap<>();

		for (final Map<String, Object> entry : relationships) {

			final String key = relationshipKey((String) entry.get(KEY_TYPE), (String) entry.get(KEY_SOURCE_ID), (String) entry.get(KEY_TARGET_ID), (String) entry.get(KEY_METHOD_NAME));
			if (key != null) {

				desired.put(key, entry);
			}
		}

		final Set<String> existing = new LinkedHashSet<>();

		for (final String type : allManagedRelationshipTypes()) {

			for (final RelationshipInterface rel : app.relationshipQuery(type).getAsList()) {

				final NodeInterface target = rel.getTargetNode();
				final boolean byName       = METHOD_REL_TYPE_SET.contains(type);
				final String key           = relationshipKey(type, rel.getSourceNodeId(), byName ? null : rel.getTargetNodeId(), (byName && target != null) ? target.getName() : null);

				if (key == null) {

					continue;
				}

				if (desired.containsKey(key)) {

					existing.add(key);

				} else {

					app.delete(rel);
					report.relationshipsRemoved++;
				}
			}
		}

		for (final Map.Entry<String, Map<String, Object>> entry : desired.entrySet()) {

			if (!existing.contains(entry.getKey())) {

				createRelationship(app, entry.getValue(), report);
			}
		}
	}

	private static String relationshipKey(final String type, final String sourceId, final String targetId, final String methodName) {

		if (type == null || sourceId == null) {

			return null;
		}

		return type + "|" + sourceId + "|" + ((methodName != null) ? "name:" + methodName : targetId);
	}

	private static List<String> allManagedRelationshipTypes() {

		final List<String> types = new LinkedList<>();

		types.addAll(List.of(REL_TYPES));
		types.addAll(List.of(METHOD_REL_TYPES));

		return types;
	}

	private static void createRelationship(final App app, final Map<String, Object> entry, final ImportReport report) throws FrameworkException {

		final String type       = (String) entry.get(KEY_TYPE);
		final String sourceId   = (String) entry.get(KEY_SOURCE_ID);
		final String methodName = (String) entry.get(KEY_METHOD_NAME);
		final String targetId   = (String) entry.get(KEY_TARGET_ID);

		if (type == null || sourceId == null) {

			return;
		}

		final NodeInterface sourceNode = app.getNodeById(sourceId);
		if (sourceNode == null) {

			report.unresolvedReferences++;
			logger.warn("Unable to import BPMN relationship {}: source node {} not found", type, sourceId);

			return;
		}

		final NodeInterface targetNode = (methodName != null) ? findMethodByName(app, methodName) : app.getNodeById(targetId);
		if (targetNode == null) {

			report.unresolvedReferences++;
			logger.warn("Unable to import BPMN relationship {} from {}: target {} not found", type, sourceId, (methodName != null) ? "method '" + methodName + "'" : "node " + targetId);

			return;
		}

		app.create(sourceNode, targetNode, type);
		report.relationshipsAdded++;
	}

	/**
	 * The handler method named {@code name} in the global namespace. Global schema methods are
	 * recreated with new UUIDs during a deployment import, so the name is the only stable
	 * handle -- which is sound because handler names are process-, version- and element-scoped
	 * (see BpmnHandlerNames).
	 */
	private static NodeInterface findMethodByName(final App app, final String name) throws FrameworkException {

		final Traits traits                            = Traits.of(StructrTraits.SCHEMA_METHOD);
		final PropertyKey<String> nameKey              = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey<NodeInterface> schemaNodeKey = traits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY);

		for (final NodeInterface candidate : app.nodeQuery(StructrTraits.SCHEMA_METHOD).key(nameKey, name).getResultStream()) {

			if (candidate.getProperty(schemaNodeKey) == null) {

				return candidate;
			}
		}

		return null;
	}

	private static void applyPatch(final App app, final Map<String, Object> entry, final ImportReport report) throws FrameworkException {

		final String id                      = (String) entry.get(KEY_ID);
		final String type                    = (String) entry.get(KEY_TYPE);
		final Map<String, Object> properties = asMap(entry.get(KEY_PROPERTIES));

		if (id == null || type == null || properties.isEmpty()) {

			return;
		}

		final NodeInterface node = app.getNodeById(id);
		if (node == null) {

			report.unresolvedReferences++;
			logger.warn("Unable to apply process property patch: {} {} not found", type, id);

			return;
		}

		final Traits traits = node.getTraits();

		for (final Map.Entry<String, Object> property : properties.entrySet()) {

			if (!traits.hasKey(property.getKey())) {

				report.unknownPatchKeys++;
				logger.warn("Unable to apply process property patch to {} {}: unknown key '{}'", type, id, property.getKey());
				continue;
			}

			node.setProperty(traits.key(property.getKey()), property.getValue());
		}
	}

	// ----- json helpers (Gson hands us untyped maps) -----

	private static List<Map<String, Object>> asList(final Object raw) {

		if (raw instanceof List<?> list) {

			final List<Map<String, Object>> result = new LinkedList<>();

			for (final Object element : list) {

				result.add(asMap(element));
			}

			return result;
		}

		return List.of();
	}

	private static Map<String, Object> asMap(final Object raw) {

		if (raw instanceof Map<?, ?> map) {

			final Map<String, Object> result = new LinkedHashMap<>();

			for (final Map.Entry<?, ?> entry : map.entrySet()) {

				result.put(String.valueOf(entry.getKey()), entry.getValue());
			}

			return result;
		}

		return Map.of();
	}

	private static int asInt(final Object raw) {

		if (raw instanceof Number number) {

			return number.intValue();
		}

		return 0;
	}
}
