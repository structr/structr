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

/**
 * Contract for BPMN diagram backends. Both the Community SVG implementation
 * and the Enterprise yFiles implementation conform to this surface.
 *
 * No backend-specific capabilities are exposed: anything one backend can do
 * via this API, the other must be able to do too. Backend-only features stay
 * inside the backend; the host UI binds only to what's defined here.
 *
 * Lifecycle:
 *   const api = new _BpmnDiagramCommunitySvg();
 *   api.mount(container, { editable: true });
 *   api.onSelect(id => host.showPropertiesFor(id));
 *   api.onElementMove((id, x, y) => host.persistMove(id, x, y));
 *   await api.load({ definitionId: '...' });
 *   // ... user interacts ...
 *   api.unmount();
 */
window._BpmnDiagramAPI = class _BpmnDiagramAPI {

	// ----- Lifecycle -----

	/**
	 * Mount the diagram into a container element. Idempotent: calling mount
	 * twice on the same instance is an error; call unmount first.
	 *
	 * @param {HTMLElement} container - Host element. The backend renders inside it.
	 * @param {{ editable?: boolean }} [options] - Editable: drag-to-move enabled
	 *   and onElementMove fires. Read-only by default.
	 */
	mount(container, options = {}) { throw new Error('not implemented'); }

	/** Tear down DOM, listeners, timers. After unmount, the instance is unusable. */
	unmount() { throw new Error('not implemented'); }

	// ----- Model -----

	/**
	 * Load a BPMN definition by UUID. The backend fetches the definition,
	 * its elements, sequence flows, and diagram (shapes + edges) via REST,
	 * then renders. Resolves when the first render is complete.
	 *
	 * @param {{ definitionId: string }} args
	 * @returns {Promise<void>}
	 */
	async load(args) { throw new Error('not implemented'); }

	/** Re-render from the current model. Use after external edits to refresh the view. */
	refresh() { throw new Error('not implemented'); }

	// ----- Selection -----

	/**
	 * Programmatically select an element (or clear selection with null).
	 * Triggers any registered onSelect handlers.
	 *
	 * @param {string|null} elementId - BpmnElement UUID, or null.
	 */
	select(elementId) { throw new Error('not implemented'); }

	/** @returns {string|null} The currently-selected BpmnElement UUID, or null. */
	getSelected() { throw new Error('not implemented'); }

	/**
	 * Register a selection-change handler. Multiple handlers allowed; all fire
	 * on every selection change. Returns an unregister function.
	 *
	 * @param {(elementId: string|null) => void} handler
	 * @returns {() => void}
	 */
	onSelect(handler) { throw new Error('not implemented'); }

	// ----- Viewport -----

	/** Fit the entire diagram to the visible viewport with a small margin. */
	fit() { throw new Error('not implemented'); }

	/**
	 * Zoom by a multiplicative factor relative to current zoom. 1.0 is no-op,
	 * 1.2 zooms in, 0.8 zooms out. Backends may clamp to sensible min/max.
	 */
	zoom(factor) { throw new Error('not implemented'); }

	/** Pan the viewport by (dx, dy) in world coordinates. */
	pan(dx, dy) { throw new Error('not implemented'); }

	// ----- Edits -----

	/**
	 * Register an element-move handler. Fires after a drag-to-move completes
	 * (pointer up), once per drag. Backend has already updated the in-memory
	 * model and re-rendered; the host is responsible for persistence.
	 *
	 * Only fires when mounted with editable=true.
	 *
	 * @param {(elementId: string, x: number, y: number) => void} handler
	 * @returns {() => void} Unregister.
	 */
	onElementMove(handler) { throw new Error('not implemented'); }

	// ----- Topology edits (CRUD) -----
	//
	// All of these mutate the in-memory model immediately and append an entry
	// to the pending-changes buffer. Nothing is persisted until savePendingChanges()
	// is called. New entities receive client-generated UUIDs that survive into
	// the database unchanged on save (Structr accepts POST with a specific UUID),
	// so cross-references between buffered entities don't need rewriting on commit.

	/**
	 * Create a new BpmnElement at world (x, y) with an automatically-generated
	 * BpmnDiShape. The shape is created internally; callers refer to the new
	 * entity by the returned element id.
	 *
	 * @param {{ type: string, name?: string, x: number, y: number, width?: number, height?: number, parentId?: string|null, props?: Object }} args
	 *   `type` is the BPMN element-type string (e.g. 'userTask', 'serviceTask',
	 *   'startEvent', 'exclusiveGateway'). `parentId` sets sub-process containment;
	 *   omit for top-level elements. `props` carries any additional BpmnElement
	 *   property writes (documentation, eventDefinitionType, ...).
	 * @returns {string} The new element's UUID.
	 */
	createElement(args) { throw new Error('not implemented'); }

	/**
	 * Update properties on an existing BpmnElement. Only fields present in
	 * `props` are written; absent fields stay as they are. Buffer merges
	 * multiple updates to the same element into one PUT on save.
	 *
	 * @param {string} elementId
	 * @param {Object} props - Sparse: e.g. { bpmnName, documentation, eventDefinitionType }.
	 */
	updateElement(elementId, props) { throw new Error('not implemented'); }

	/**
	 * Read the in-memory record of a BpmnElement (persisted or buffered).
	 * Returns a clone -- mutating it does not affect the model. Returns null
	 * if no such element exists.
	 *
	 * @param {string} elementId
	 * @returns {Object|null}
	 */
	getElement(elementId) { throw new Error('not implemented'); }

	/**
	 * Refetch a BpmnElement from the backend and replace its in-memory record.
	 * Used by the host after server-side mutations made outside the buffer
	 * path (e.g. attaching a method via a separate REST call) so the editor
	 * reflects the latest state without a full diagram reload. Fires
	 * onUpdate handlers; does NOT touch the pending-changes buffer.
	 *
	 * @param {string} elementId
	 * @returns {Promise<void>}
	 */
	refreshElement(elementId) { throw new Error('not implemented'); }

	/**
	 * Delete a BpmnElement and any incident sequence flows (and their DI
	 * counterparts). Cascading is computed in the buffer so that save can
	 * issue the deletes in dependency-safe order.
	 *
	 * @param {string} elementId
	 */
	deleteElement(elementId) { throw new Error('not implemented'); }

	/**
	 * Create a new BpmnSequenceFlow between two existing elements (either may
	 * be a buffered element with a temp UUID). Generates a corresponding
	 * BpmnDiEdge; if `waypoints` is omitted, the backend computes a default
	 * routing between the two shapes' borders.
	 *
	 * @param {{ sourceId: string, targetId: string, name?: string, condition?: string, conditionType?: string, waypoints?: Array<{x:number,y:number}> }} args
	 * @returns {string} The new sequence-flow UUID.
	 */
	createFlow(args) { throw new Error('not implemented'); }

	/**
	 * Update properties on an existing BpmnSequenceFlow.
	 *
	 * @param {string} flowId
	 * @param {Object} props - Sparse: e.g. { bpmnName, conditionExpression, conditionExpressionType }.
	 */
	updateFlow(flowId, props) { throw new Error('not implemented'); }

	/**
	 * Delete a sequence flow and its BpmnDiEdge.
	 *
	 * @param {string} flowId
	 */
	deleteFlow(flowId) { throw new Error('not implemented'); }

	// ----- Geometry edits -----

	/**
	 * Set position and/or size of an element's shape. Generalises drag-to-move:
	 * the host can use it for arrow-key nudges, alignment commands, etc.
	 * Incident edges are re-snapped automatically.
	 *
	 * @param {string} elementId
	 * @param {{ x?: number, y?: number, width?: number, height?: number }} bounds
	 */
	setShape(elementId, bounds) { throw new Error('not implemented'); }

	/**
	 * Replace the waypoint list on a flow's BpmnDiEdge. Used for manual bend
	 * editing and for re-routing after a topology change.
	 *
	 * @param {string} flowId
	 * @param {Array<{x:number,y:number}>} waypoints
	 */
	setEdgeWaypoints(flowId, waypoints) { throw new Error('not implemented'); }

	// ----- Placement mode -----
	//
	// Armed by the host when the user picks an element type from a palette.
	// While armed, the next click on the empty canvas creates an element of
	// that type at the click location (snapped to grid) and clears the armed
	// state. ESC or any other interaction cancels.

	/**
	 * Arm the next empty-canvas click to create an element of the given BPMN
	 * type. Replaces any existing armed state.
	 * @param {string} type - e.g. 'userTask', 'startEvent', 'exclusiveGateway'.
	 */
	armElementType(type) { throw new Error('not implemented'); }

	/** Cancel any armed placement. No-op if nothing is armed. */
	cancelPlacement() { throw new Error('not implemented'); }

	/**
	 * Subscribe to placement-state changes. Fires after `armElementType`,
	 * after `cancelPlacement`, and after a placement click consumes the armed
	 * state. The handler receives the currently-armed type, or null when
	 * placement is inactive.
	 *
	 * @param {(type: string|null) => void} handler
	 * @returns {() => void} Unregister.
	 */
	onPlacementChanged(handler) { throw new Error('not implemented'); }

	// ----- Pending changes (buffer) -----

	/**
	 * Snapshot of the current pending-changes buffer. Useful for the host UI
	 * to surface a count of pending edits or render a "review changes" panel.
	 * Returned object is a clone -- mutating it does not affect the buffer.
	 *
	 * @returns {{
	 *   creates: Array<{ id: string, type: string, props: Object }>,
	 *   updates: Array<{ id: string, type: string, props: Object }>,
	 *   deletes: Array<{ id: string, type: string }>
	 * }}
	 */
	getPendingChanges() { throw new Error('not implemented'); }

	/** Whether the buffer holds any unsaved change. */
	hasPendingChanges() { throw new Error('not implemented'); }

	/**
	 * Flush the buffer to the backend in a single transactional WebSocket
	 * command. On success, the buffer is cleared and the model reflects the
	 * server's post-commit state. On failure, the buffer is preserved so the
	 * user can retry.
	 *
	 * @returns {Promise<{ ok: boolean, error?: string, version?: number }>}
	 *   `version` is the new BpmnDefinitions.version, used for cross-tab
	 *   change detection.
	 */
	savePendingChanges() { throw new Error('not implemented'); }

	/** Discard the buffer. Does not refetch from the backend; the in-memory
	 *  model snaps back to its post-load (or post-last-save) state. */
	discardPendingChanges() { throw new Error('not implemented'); }

	// Client-side BPMN serialisation was removed. The host UI fetches
	// canonical XML on demand via the server-side exporter
	// (`POST /BpmnDefinitions/{id}/exportBpmn`) after saves, which is
	// the same path used by the per-row Export action. Backends
	// implementing this API don't need to provide a serialiser at all.

	// ----- Undo/redo -----
	//
	// In-memory action stack, not persisted. Cleared on every save. Each user
	// edit pushes one undoable action; backends are responsible for grouping
	// (e.g. a multi-step compound operation should appear as a single undo unit).

	undo() { throw new Error('not implemented'); }
	redo() { throw new Error('not implemented'); }
	canUndo() { throw new Error('not implemented'); }
	canRedo() { throw new Error('not implemented'); }

	// ----- Edit events -----
	//
	// Fired synchronously after the corresponding mutation lands in the buffer.
	// Multiple handlers per event; each returns an unregister function.

	/** @param {(id: string, type: string) => void} handler */
	onCreate(handler) { throw new Error('not implemented'); }

	/** @param {(id: string, type: string, changedProps: Object) => void} handler */
	onUpdate(handler) { throw new Error('not implemented'); }

	/** @param {(id: string, type: string) => void} handler */
	onDelete(handler) { throw new Error('not implemented'); }

	/**
	 * Fires whenever the buffer's pending state may have changed (any CRUD or
	 * geometry edit, undo/redo, save, discard). Callers typically use this to
	 * refresh a "dirty" indicator or recompute a save-button enabled state.
	 *
	 * @param {() => void} handler
	 */
	onPendingChanged(handler) { throw new Error('not implemented'); }

	/**
	 * Fires when the *currently-loaded* definition was changed remotely (typically
	 * by another tab of the same user that just saved its buffer). The host
	 * decides what to do -- usually offer the user a refetch, or auto-refetch
	 * silently if the local buffer is empty.
	 *
	 * @param {(args: { definitionId: string, version?: string }) => void} handler
	 * @returns {() => void} Unregister.
	 */
	onRemoteChange(handler) { throw new Error('not implemented'); }

	// ----- Highlights -----

	/**
	 * Apply a named highlight style to a set of elements. Used by monitoring
	 * views (e.g. runtime token positions, audit heatmap). Multiple highlight
	 * groups can coexist; styles are CSS classes the backend applies.
	 *
	 * @param {string[]} elementIds
	 * @param {string} style - 'active' | 'completed' | 'error' | custom name.
	 */
	highlight(elementIds, style) { throw new Error('not implemented'); }

	/** Remove all highlights. Selection is preserved. */
	clearHighlights() { throw new Error('not implemented'); }
};
