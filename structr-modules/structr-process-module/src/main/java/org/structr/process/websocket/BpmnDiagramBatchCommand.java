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
package org.structr.process.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket command that applies a batch of BPMN diagram edits in one transaction.
 *
 * <p>The browser-side editor buffers edits locally (creates / updates / deletes)
 * until the user clicks Save. On save, the entire buffer is sent here as a single
 * message. Since the WebSocket framework wraps every command in an enclosing
 * transaction, the batch is atomic: either all changes commit together, or none
 * do and the user retries.</p>
 *
 * <p>New entities arrive with client-generated UUIDs (Structr accepts
 * caller-supplied UUIDs via the {@code id} property), so cross-references between
 * buffered entities can use those UUIDs directly: a buffered flow can reference
 * a buffered element by id without any post-commit rewrite. The editor never
 * has to "translate" temp ids to real ids.</p>
 *
 * <p>Creates are sorted server-side by type to satisfy foreign-key dependencies
 * (BpmnElement before BpmnSequenceFlow before BpmnDiShape before BpmnDiEdge).
 * Within {@code BpmnElement}, parent/child containment requires nodes whose
 * {@code parentElement} reference points to another buffered create to be
 * created after their parent: handled by an additional topological pass.</p>
 *
 * <p>After a successful commit, broadcasts a {@code BPMN_DIAGRAM_CHANGED}
 * notification to all sessions <em>except the originating one</em>, so other
 * tabs can refetch the definition. The originating tab already knows it
 * succeeded from the command's {@code FINISHED} reply.</p>
 *
 * <h3>Input format</h3>
 * <pre>
 * {
 *   command: 'BPMN_DIAGRAM_BATCH',
 *   data: {
 *     definitionId: '&lt;uuid&gt;',
 *     creates: [ { id, type, props: {...} }, ... ],
 *     updates: [ { id, props: {...} }, ... ],
 *     deletes: [ '&lt;uuid&gt;', ... ]
 *   }
 * }
 * </pre>
 */
public class BpmnDiagramBatchCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(BpmnDiagramBatchCommand.class);

	public static final String COMMAND_NAME       = "BPMN_DIAGRAM_BATCH";
	public static final String NOTIFICATION_NAME  = "BPMN_DIAGRAM_CHANGED";

	static {
		StructrWebSocket.addCommand(BpmnDiagramBatchCommand.class);
	}

	// Type ordering for create operations. Foreign keys point earlier -> later,
	// so types listed earlier are created first. Anything not listed sorts to
	// the end (insertion order is preserved within an unknown-type group).
	private static final List<String> CREATE_ORDER = List.of(
		ProcessTraits.BPMN_DEFINITIONS,
		ProcessTraits.BPMN_ELEMENT,
		ProcessTraits.BPMN_SEQUENCE_FLOW,
		ProcessTraits.BPMN_DI_DIAGRAM,
		ProcessTraits.BPMN_DI_SHAPE,
		ProcessTraits.BPMN_DI_EDGE,
		ProcessTraits.BPMN_PERFORMER,
		ProcessTraits.BPMN_TASK_LISTENER,
		ProcessTraits.BPMN_PROCESS_LISTENER,
		ProcessTraits.BPMN_GLOBAL_DEFINITION
	);

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		final String definitionId = webSocketData.getNodeDataStringValue("definitionId");
		if (definitionId == null || definitionId.isEmpty()) {
			throw new FrameworkException(422, "definitionId is required");
		}

		final NodeInterface defNode = app.getNodeById(ProcessTraits.BPMN_DEFINITIONS, definitionId);
		if (defNode == null) {
			throw new FrameworkException(404, "BpmnDefinitions " + definitionId + " not found");
		}

		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> rawCreates = listOrEmpty(webSocketData.getNodeData().get("creates"));
		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> rawUpdates = listOrEmpty(webSocketData.getNodeData().get("updates"));
		@SuppressWarnings("unchecked")
		final List<String>             rawDeletes = listOrEmpty(webSocketData.getNodeData().get("deletes"));

		// Topo-sort creates by type, then by parent-element dependency for elements.
		final List<Map<String, Object>> sortedCreates = sortCreates(rawCreates);

		// Apply creates
		for (final Map<String, Object> entry : sortedCreates) {
			applyCreate(app, securityContext, entry);
		}

		// Apply updates
		for (final Map<String, Object> entry : rawUpdates) {
			applyUpdate(app, securityContext, entry);
		}

		// Apply deletes (no special ordering: relationship cleanup is handled by
		// Structr's cascading rules per relationship-trait declaration).
		for (final String id : rawDeletes) {
			final NodeInterface node = app.getNodeById(id);
			if (node != null) {
				app.delete(node);
			}
		}

		// Broadcast definition-changed to other sessions. The version field on
		// BpmnDefinitions stays the same (UI saves mutate in place), so we ship
		// just the id; receivers refetch.
		final Map<String, Object> notifyData = new HashMap<>();
		notifyData.put("definitionId", definitionId);
		final String version = defNode.getProperty(defNode.getTraits().key(BpmnBaseNodeTraitDefinition.VERSION_PROPERTY));
		if (version != null) {
			notifyData.put("version", version);
		}
		final String originSession = securityContext.getSessionId();
		TransactionCommand.simpleBroadcast(NOTIFICATION_NAME, notifyData,
			originSession != null ? Predicate.allExcept(originSession) : Predicate.all());

		// Reply to the caller with the same payload so the client can confirm
		// the commit landed (and read the version) without a separate fetch.
		getWebSocket().send(MessageBuilder.finished()
			.callback(callback)
			.data("definitionId", definitionId)
			.data("version",      version)
			.data("ok",           Boolean.TRUE)
			.build(), true);
	}

	@Override
	public String getCommand() {
		return COMMAND_NAME;
	}

	// ----- internals -----

	private void applyCreate(final App app, final SecurityContext securityContext, final Map<String, Object> entry) throws FrameworkException {

		final String type  = (String) entry.get("type");
		final String id    = (String) entry.get("id");
		if (type == null) {
			throw new FrameworkException(422, "create entry missing 'type'");
		}
		if (id == null) {
			throw new FrameworkException(422, "create entry missing 'id'");
		}

		@SuppressWarnings("unchecked")
		final Map<String, Object> rawProps = (Map<String, Object>) entry.get("props");
		final Map<String, Object> propsCopy = (rawProps != null) ? new HashMap<>(rawProps) : new HashMap<>();
		// Inject the client-supplied UUID into the property map under 'id' so
		// CreateNodeCommand picks it up (see CreateNodeCommand.execute, where
		// a non-null idKey value is honoured and triggers UUID validation).
		propsCopy.put(GraphObjectTraitDefinition.ID_PROPERTY, id);

		final PropertyMap props = PropertyMap.inputTypeToJavaType(securityContext, type, propsCopy);
		app.create(type, props);
	}

	private void applyUpdate(final App app, final SecurityContext securityContext, final Map<String, Object> entry) throws FrameworkException {

		final String id = (String) entry.get("id");
		if (id == null) {
			throw new FrameworkException(422, "update entry missing 'id'");
		}
		final NodeInterface node = app.getNodeById(id);
		if (node == null) {
			throw new FrameworkException(404, "node " + id + " not found");
		}
		@SuppressWarnings("unchecked")
		final Map<String, Object> rawProps = (Map<String, Object>) entry.get("props");
		if (rawProps == null || rawProps.isEmpty()) {
			return;
		}
		final PropertyMap props = PropertyMap.inputTypeToJavaType(securityContext, node.getType(), rawProps);
		node.setProperties(securityContext, props);
	}

	/**
	 * Order creates so foreign-key targets land before their referrers. Two
	 * passes: first by global type order, then within {@code BpmnElement} a
	 * stable topological sort on the {@code parentElement} reference (a child
	 * sub-process element must be created after its parent if both are in this
	 * batch).
	 */
	private List<Map<String, Object>> sortCreates(final List<Map<String, Object>> creates) {

		final List<Map<String, Object>> byType = new ArrayList<>(creates);
		byType.sort(Comparator.comparingInt(this::createOrder));

		// Within BpmnElement, topo-sort by parentElement.
		final List<Map<String, Object>> result = new ArrayList<>(byType.size());
		final List<Map<String, Object>> elementBatch = new ArrayList<>();
		for (final Map<String, Object> e : byType) {
			if (ProcessTraits.BPMN_ELEMENT.equals(e.get("type"))) {
				elementBatch.add(e);
			} else {
				if (!elementBatch.isEmpty()) {
					result.addAll(topoSortElements(elementBatch));
					elementBatch.clear();
				}
				result.add(e);
			}
		}
		if (!elementBatch.isEmpty()) {
			result.addAll(topoSortElements(elementBatch));
		}
		return result;
	}

	private int createOrder(final Map<String, Object> entry) {
		final String type = (String) entry.get("type");
		final int idx     = (type != null) ? CREATE_ORDER.indexOf(type) : -1;
		return (idx >= 0) ? idx : Integer.MAX_VALUE;
	}

	/**
	 * Stable topological sort of buffered BpmnElement creates by parentElement
	 * reference. Elements whose parentElement is null, missing from the batch,
	 * or already persisted come first; their children follow. References to
	 * unknown ids (already-persisted parents from a previous save) are
	 * treated as roots for sort purposes.
	 */
	private List<Map<String, Object>> topoSortElements(final List<Map<String, Object>> batch) {

		// Build id -> entry map for quick lookup.
		final Map<String, Map<String, Object>> byId = new HashMap<>();
		for (final Map<String, Object> e : batch) {
			byId.put((String) e.get("id"), e);
		}

		final List<Map<String, Object>> result = new ArrayList<>(batch.size());
		final java.util.Set<String> emitted    = new java.util.HashSet<>();

		// Iterate up to N times; each pass emits any entry whose parent is
		// already emitted or external. Bounded by batch size.
		boolean progress = true;
		while (progress) {
			progress = false;
			for (final Map<String, Object> e : batch) {
				final String id = (String) e.get("id");
				if (emitted.contains(id)) continue;
				@SuppressWarnings("unchecked")
				final Map<String, Object> props = (Map<String, Object>) e.get("props");
				final Object parentRef = (props != null) ? props.get("parentElement") : null;
				if (parentRef == null || !byId.containsKey(parentRef.toString()) || emitted.contains(parentRef.toString())) {
					result.add(e);
					emitted.add(id);
					progress = true;
				}
			}
		}
		// Anything left over indicates a cycle. Append at the end and let
		// Structr surface the relational error rather than dropping silently.
		for (final Map<String, Object> e : batch) {
			final String id = (String) e.get("id");
			if (!emitted.contains(id)) {
				logger.warn("BPMN_DIAGRAM_BATCH: cyclic parentElement reference involving '{}' -- emitting in input order", id);
				result.add(e);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> listOrEmpty(final Object o) {
		return (o instanceof List) ? (List<T>) o : List.of();
	}
}
