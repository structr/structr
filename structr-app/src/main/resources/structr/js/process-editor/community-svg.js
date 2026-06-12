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
 * Community SVG implementation of _BpmnDiagramAPI. Plain DOM + SVG, no
 * dependencies. Coordinates come from the imported BpmnDiDiagram (shapes +
 * edges); drag-to-move persists back via REST. Pan/zoom is SVG viewBox
 * manipulation.
 */
window._BpmnDiagramCommunitySvg = class _BpmnDiagramCommunitySvg extends window._BpmnDiagramAPI {

	static SVG_NS = 'http://www.w3.org/2000/svg';

	// Sensible viewport bounds. World coords come from BPMN file (~hundreds of units).
	static MIN_ZOOM = 0.1;
	static MAX_ZOOM = 5.0;

	// Grid step in world units. Used both for the visible background grid and
	// as the snap-to-grid step during drag.
	static GRID_STEP = 10;

	// Width of the vertical title strip inside a participant pool / lane.
	// Pools nest one row of lanes inside their content area; the lanes start
	// at pool.x + POOL_STRIP_WIDTH. Each lane in turn has its own narrower
	// label strip on the left.
	static POOL_STRIP_WIDTH = 30;
	static LANE_STRIP_WIDTH = 20;

	constructor() {
		super();

		this._container       = null;
		this._svg             = null;
		this._viewport        = null;       // <g> that pans/zooms; everything is inside it
		this._editable        = false;

		// Model state. Buffered creates are folded into these maps too -- so
		// rendering doesn't care whether an entity is persisted or pending.
		this._definition      = null;       // { id, processName, ... }
		this._diagramId       = null;       // BpmnDiDiagram UUID (for new shapes/edges)
		this._elements        = new Map();  // node UUID  -> element node JSON
		this._flows           = new Map();  // node UUID  -> flow node JSON
		this._shapes          = new Map();  // bpmnElementRef (bpmnId) -> shape JSON
		this._edges           = new Map();  // bpmnElementRef (flow bpmnId) -> edge JSON

		// Collaboration (multi-process) state. Optional: single-process files
		// have no collaboration. Participants render as labelled pools and
		// messageFlows as dashed cross-pool edges.
		this._collaboration   = null;       // { id, bpmnId } or null
		this._participants    = new Map();  // node UUID  -> participant JSON (bpmnName, process, ...)
		this._messageFlows    = new Map();  // node UUID  -> messageFlow JSON (sourceRefId, targetRefId, ...)
		// bpmnId -> participant/messageFlow JSON, for fast DI -> entity lookup.
		this._participantsByBpmnId = new Map();
		this._messageFlowsByBpmnId = new Map();

		// Lane state. Lanes subdivide a participant pool horizontally and
		// each lane carries a name + the bpmnIds of the flow elements that
		// belong inside it. Pure layout: the engine ignores them.
		this._lanes           = new Map();  // node UUID -> lane JSON
		this._lanesByBpmnId   = new Map();  // bpmnId   -> lane JSON

		// View state
		this._zoom            = 1.0;
		this._panX            = 0;
		this._panY            = 0;
		this._selectedId      = null;       // selected element id (mutually exclusive with _selectedFlowId)
		this._selectedFlowId  = null;       // selected sequence-flow id, when an edge is selected instead of a shape
		this._highlightGroups = new Map();  // style -> Set<elementId>

		// Subscribers
		this._selectHandlers       = [];
		this._elementMoveHandlers  = [];
		this._createHandlers       = [];
		this._updateHandlers       = [];
		this._deleteHandlers       = [];
		this._pendingChangedHandlers = [];
		this._remoteChangeHandlers = [];

		// Cross-tab listener registration. The dispatcher in websocket.js calls
		// every function in window._BpmnDiagramRemoteListeners on each
		// BPMN_DIAGRAM_CHANGED notification; we add/remove our bound handler
		// on mount/unmount.
		this._remoteListenerBound = null;

		// Drag state
		this._dragState       = null;       // { kind:'pan'|'element', startX, startY, ... }

		// Definition currently loaded
		this._definitionId    = null;

		// Pending-changes buffer. Three sections:
		//   creates: Map<id, { type, props }> -- new entities not yet on server
		//   updates: Map<id, props>           -- partial updates (sparse)
		//   deletes: Set<id>                  -- ids to be deleted on save
		// `id` is always a node UUID. References between buffered entities use
		// either real UUIDs or buffered-create UUIDs (interchangeable since
		// Structr accepts client-supplied UUIDs on POST).
		this._buffer = { creates: new Map(), updates: new Map(), deletes: new Set() };

		// Undo/redo stacks. Each entry is { apply, revert } closures that
		// reverse a single user action. Cleared on save.
		this._undoStack = [];
		this._redoStack = [];

		// Recording flag. When true, mutation methods append to undo via the
		// closure they receive; we suspend during undo/redo to avoid recursion.
		this._recordingUndo = true;

		// Placement mode (armed by host's palette). When non-null, the next
		// empty-canvas click creates an element of this type and clears the
		// armed state.
		this._pendingPlacement   = null;
		this._placementHandlers  = [];
	}

	// ===== Buffer helpers =====

	_bufferKey() {
		return this._definitionId ? `bpmn-diagram-buffer:${this._definitionId}` : null;
	}

	_serializeBuffer() {
		// Plain-JSON form for localStorage / User-property persistence.
		const out = { version: 1, creates: {}, updates: {}, deletes: [] };
		for (const [id, entry] of this._buffer.creates) out.creates[id] = entry;
		for (const [id, props] of this._buffer.updates) out.updates[id] = props;
		out.deletes = Array.from(this._buffer.deletes);
		return out;
	}

	_writeBufferToLocalStorage() {
		const key = this._bufferKey();
		if (!key) return;
		const json = JSON.stringify(this._serializeBuffer());
		try {
			if (typeof LSWrapper !== 'undefined' && LSWrapper.setItem) LSWrapper.setItem(key, json);
			else localStorage.setItem(key, json);
		} catch (_) {}
	}

	_clearBufferLocalStorage() {
		const key = this._bufferKey();
		if (!key) return;
		try {
			if (typeof LSWrapper !== 'undefined' && LSWrapper.removeItem) LSWrapper.removeItem(key);
			else localStorage.removeItem(key);
		} catch (_) {}
	}

	_loadBufferFromLocalStorage() {
		const key = this._bufferKey();
		if (!key) return;
		let raw = null;
		try {
			raw = (typeof LSWrapper !== 'undefined' && LSWrapper.getItem) ? LSWrapper.getItem(key) : localStorage.getItem(key);
		} catch (_) { raw = null; }
		if (!raw) return;
		let obj;
		try { obj = JSON.parse(raw); } catch (_) { return; }
		if (!obj || obj.version !== 1) return;
		for (const id of Object.keys(obj.creates ?? {})) this._buffer.creates.set(id, obj.creates[id]);
		for (const id of Object.keys(obj.updates ?? {})) this._buffer.updates.set(id, obj.updates[id]);
		for (const id of (obj.deletes ?? [])) this._buffer.deletes.add(id);
	}

	/** Apply a sparse update to the buffer for a known-persisted entity. */
	_bufferUpdate(id, props) {
		if (this._buffer.creates.has(id)) {
			// Entity is buffered-new: merge into the create's props.
			Object.assign(this._buffer.creates.get(id).props, props);
		} else {
			const existing = this._buffer.updates.get(id) || {};
			this._buffer.updates.set(id, Object.assign(existing, props));
		}
		this._writeBufferToLocalStorage();
		this._fireOnPendingChanged();
	}

	/** Add a create entry to the buffer. */
	_bufferCreate(id, type, props) {
		this._buffer.creates.set(id, { type, props });
		this._writeBufferToLocalStorage();
		this._fireOnPendingChanged();
	}

	/** Mark a persisted (or buffered) entity for deletion. Cancels pending creates/updates for the same id. */
	_bufferDelete(id) {
		if (this._buffer.creates.has(id)) {
			// Was a buffered new entity: drop it entirely, no need to delete on server.
			this._buffer.creates.delete(id);
		} else {
			this._buffer.updates.delete(id);
			this._buffer.deletes.add(id);
		}
		this._writeBufferToLocalStorage();
		this._fireOnPendingChanged();
	}

	hasPendingChanges() {
		return this._buffer.creates.size > 0 || this._buffer.updates.size > 0 || this._buffer.deletes.size > 0;
	}

	getPendingChanges() {
		// Return a clone so callers can't mutate internal state.
		const creates = [];
		for (const [id, entry] of this._buffer.creates) {
			creates.push({ id, type: entry.type, props: { ...entry.props } });
		}
		const updates = [];
		for (const [id, props] of this._buffer.updates) {
			const type = this._typeOfPersistedId(id);
			updates.push({ id, type, props: { ...props } });
		}
		const deletes = [];
		for (const id of this._buffer.deletes) {
			deletes.push({ id, type: this._typeOfPersistedId(id) });
		}
		return { creates, updates, deletes };
	}

	_typeOfPersistedId(id) {
		// Best-effort lookup: match against in-memory model. If the entity has
		// already been removed from the in-memory model (e.g. for a delete),
		// the type is unknown -- the server can still process the delete.
		for (const [, el] of this._elements) if (el.id === id) return 'BpmnElement';
		for (const [, fl] of this._flows)    if (fl.id === id) return 'BpmnSequenceFlow';
		for (const [, sh] of this._shapes)   if (sh.id === id) return 'BpmnDiShape';
		for (const [, ed] of this._edges)    if (ed.id === id) return 'BpmnDiEdge';
		return null;
	}

	/**
	 * Flush the buffer in a single transactional WebSocket command.
	 * Resolves with { ok, version?, error? }.
	 */
	async savePendingChanges() {
		if (!this.hasPendingChanges()) return { ok: true };
		const payload = this.getPendingChanges();
		return await new Promise((resolve) => {
			const obj = {
				command:   'BPMN_DIAGRAM_BATCH',
				sessionId: (typeof Structr !== 'undefined' && Structr.getSessionId) ? Structr.getSessionId() : null,
				data: {
					definitionId: this._definitionId,
					creates:      payload.creates,
					updates:      payload.updates,
					deletes:      payload.deletes.map(d => d.id),
				}
			};
			try {
				StructrWS.sendObj(obj, (resp) => {
					if (resp && resp.ok === true) {
						// Buffer + undo cleared; localStorage emptied; pending UI refreshed.
						this._buffer.creates.clear();
						this._buffer.updates.clear();
						this._buffer.deletes.clear();
						this._undoStack.length = 0;
						this._redoStack.length = 0;
						this._clearBufferLocalStorage();
						this._fireOnPendingChanged();
						resolve({ ok: true, version: resp.version });
					} else {
						resolve({ ok: false, error: resp?.message || 'unknown error' });
					}
				});
			} catch (e) {
				resolve({ ok: false, error: e.message });
			}
		});
	}

	async discardPendingChanges() {
		// Clear buffer + undo state and refetch the persisted definition so the
		// in-memory model snaps back to the server's view. Updates and deletes
		// to persisted entities can't be reverted any other way -- we mutated
		// the in-memory entries in place and don't snapshot original values
		// per-edit.
		this._buffer.creates.clear();
		this._buffer.updates.clear();
		this._buffer.deletes.clear();
		this._undoStack.length = 0;
		this._redoStack.length = 0;
		this._clearBufferLocalStorage();
		this._fireOnPendingChanged();
		if (this._definitionId) {
			await this.load({ definitionId: this._definitionId });
		} else {
			this.refresh();
		}
	}

	// ===== Undo / Redo =====

	_pushUndo(action) {
		if (!this._recordingUndo) return;
		this._undoStack.push(action);
		this._redoStack.length = 0; // user action invalidates the redo stack
		// Fire onPendingChanged so the host's updateDirty re-evaluates
		// canUndo / canRedo. Without this, the toolbar Undo button stays
		// disabled even after an action that pushed an undo entry,
		// because _bufferUpdate (which fires onPendingChanged) runs
		// BEFORE _pushUndo at every call site -- by the time the host
		// looks at canUndo, the stack is still empty.
		this._fireOnPendingChanged();
	}

	undo() {
		const a = this._undoStack.pop();
		if (!a) return;
		this._recordingUndo = false;
		try { a.revert(); } finally { this._recordingUndo = true; }
		this._redoStack.push(a);
		this._fireOnPendingChanged();
	}

	redo() {
		const a = this._redoStack.pop();
		if (!a) return;
		this._recordingUndo = false;
		try { a.apply(); } finally { this._recordingUndo = true; }
		this._undoStack.push(a);
		this._fireOnPendingChanged();
	}

	canUndo() { return this._undoStack.length > 0; }
	canRedo() { return this._redoStack.length > 0; }

	// ===== Event subscriber helpers =====

	_subscribe(list, handler) {
		list.push(handler);
		return () => {
			const i = list.indexOf(handler);
			if (i >= 0) list.splice(i, 1);
		};
	}
	onCreate(handler)         { return this._subscribe(this._createHandlers, handler); }
	onUpdate(handler)         { return this._subscribe(this._updateHandlers, handler); }
	onDelete(handler)         { return this._subscribe(this._deleteHandlers, handler); }
	onPendingChanged(handler) { return this._subscribe(this._pendingChangedHandlers, handler); }
	onRemoteChange(handler)   { return this._subscribe(this._remoteChangeHandlers, handler); }

	_fireOnPendingChanged() {
		for (const h of this._pendingChangedHandlers) {
			try { h(); } catch (e) { console.error(e); }
		}
	}

	// ===== ID generators =====

	_uuid() {
		// Structr UUIDs are 32 hex chars without dashes -- crypto.randomUUID
		// returns the dashed canonical form, so strip them.
		if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID().replace(/-/g, '');
		// Fallback: not collision-safe but works in older environments. The
		// template is already 32 chars without dashes.
		return 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
			const r = Math.random() * 16 | 0;
			return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
		});
	}

	_genBpmnId(typePrefix) {
		// Short random suffix; collision is checked server-side on save.
		const suffix = Math.random().toString(36).slice(2, 8);
		return `${typePrefix}_${suffix}`;
	}

	_typePrefixForBpmnElementType(t) {
		if (!t) return 'Element';
		if (t.endsWith('Event'))             return 'Event';
		if (t.endsWith('Gateway'))           return 'Gateway';
		if (t === 'sequenceFlow')            return 'Flow';
		if (t === 'subProcess')              return 'SubProcess';
		if (t === 'dataObjectReference' || t === 'dataObject') return 'DataObject';
		if (t === 'dataStoreReference')      return 'DataStore';
		// userTask, serviceTask, scriptTask, manualTask, businessRuleTask, send/receive, ...
		return 'Task';
	}

	// ===== Lifecycle =====

	mount(container, options = {}) {
		if (this._container) throw new Error('already mounted; call unmount first');

		this._container = container;
		this._editable  = !!options.editable;

		this._container.classList.add('bpmn-diagram-host');
		this._container.style.position = 'relative';
		this._container.style.overflow = 'hidden';
		this._container.style.userSelect = 'none';

		// Re-fit when the host resizes (dialog can be resized / maximised by
		// the user). ResizeObserver fires on layout changes; we just refit so
		// the diagram stays visible at any size.
		this._resizeObserver = new ResizeObserver(() => {
			if (this._shapes.size > 0) this.fit();
		});
		this._resizeObserver.observe(this._container);

		// Root SVG. Width/height fill the container; viewBox controls world view.
		this._svg = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'svg');
		this._svg.setAttribute('width',  '100%');
		this._svg.setAttribute('height', '100%');
		this._svg.style.display = 'block';
		this._svg.style.background = '#fafafa';

		// Defs: arrowhead marker for sequence flows, drop-shadow filter for
		// shape bodies, and a grid pattern for the background.
		const defs = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'defs');
		defs.innerHTML = `
			<marker id="bpmn-arrow" viewBox="0 0 10 10" refX="10" refY="5"
			        markerWidth="8" markerHeight="8" orient="auto-start-reverse">
				<path d="M 0 0 L 10 5 L 0 10 z" fill="#333"></path>
			</marker>
			<marker id="bpmn-msgflow-start" viewBox="0 0 10 10" refX="5" refY="5"
			        markerWidth="8" markerHeight="8" orient="auto">
				<circle cx="5" cy="5" r="3.5" fill="#fff" stroke="#333" stroke-width="1"></circle>
			</marker>
			<marker id="bpmn-msgflow-end" viewBox="0 0 10 10" refX="9" refY="5"
			        markerWidth="10" markerHeight="10" orient="auto-start-reverse">
				<path d="M 0 0 L 10 5 L 0 10" fill="none" stroke="#333" stroke-width="1.2"></path>
			</marker>
			<filter id="bpmn-shadow" x="-30%" y="-30%" width="160%" height="160%">
				<feDropShadow dx="0" dy="2" stdDeviation="2.5" flood-color="#000" flood-opacity="0.30"></feDropShadow>
			</filter>
			<pattern id="bpmn-grid" width="${_BpmnDiagramCommunitySvg.GRID_STEP}" height="${_BpmnDiagramCommunitySvg.GRID_STEP}" patternUnits="userSpaceOnUse">
				<path d="M ${_BpmnDiagramCommunitySvg.GRID_STEP} 0 L 0 0 0 ${_BpmnDiagramCommunitySvg.GRID_STEP}" fill="none" stroke="#e5e5e5" stroke-width="0.5"></path>
			</pattern>
		`;
		this._svg.appendChild(defs);

		// Viewport group: holds everything that pans/zooms. Grid sits at the
		// back; shapes/edges layer on top.
		this._viewport = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'g');
		this._viewport.setAttribute('class', 'bpmn-viewport');
		this._svg.appendChild(this._viewport);

		this._container.appendChild(this._svg);

		// Pan + zoom + selection-clear on background.
		this._svg.addEventListener('pointerdown', (e) => this._onPointerDown(e));
		this._svg.addEventListener('wheel',       (e) => this._onWheel(e), { passive: false });

		// Capture-phase pointerdown listener: runs BEFORE any per-shape
		// stopPropagation handler, so it sees the true pointer target. Used
		// to record whether a click started on the bare background.
		// We can't decide that in the click handler itself: shape interactions
		// call setPointerCapture(svg), which retargets the synthesized click
		// event to the svg even though the pointer originated on a shape. So
		// the click handler can't distinguish "real bg click" from "shape
		// click whose click event was retargeted" without this hint.
		this._svg.addEventListener('pointerdown', (e) => {
			this._clickStartedOnBg = (e.target === this._svg || e.target === this._viewport);
		}, true);

		// Click outside any element clears selection -- but only when the
		// pointerdown actually hit the background.
		this._svg.addEventListener('click', (e) => {
			if (!this._clickStartedOnBg) return;
			if (e.target === this._svg || e.target === this._viewport) {
				if (this._selectedFlowId) this.selectFlow(null);
				else                      this.select(null);
			}
		});

		// Cross-tab notification listener: register a closure that filters by
		// definitionId and forwards to onRemoteChange subscribers. Stored on
		// `this` so unmount can remove it from the global list.
		this._remoteListenerBound = (args) => {
			if (!args || !args.definitionId) return;
			if (args.definitionId !== this._definitionId) return;
			for (const h of this._remoteChangeHandlers) {
				try { h(args); } catch (e) { console.error(e); }
			}
		};
		if (!Array.isArray(window._BpmnDiagramRemoteListeners)) {
			window._BpmnDiagramRemoteListeners = [];
		}
		window._BpmnDiagramRemoteListeners.push(this._remoteListenerBound);
	}

	unmount() {
		if (!this._container) return;
		if (this._resizeObserver) {
			try { this._resizeObserver.disconnect(); } catch (_) {}
			this._resizeObserver = null;
		}
		if (this._remoteListenerBound && Array.isArray(window._BpmnDiagramRemoteListeners)) {
			const i = window._BpmnDiagramRemoteListeners.indexOf(this._remoteListenerBound);
			if (i >= 0) window._BpmnDiagramRemoteListeners.splice(i, 1);
		}
		this._remoteListenerBound = null;
		this._container.classList.remove('bpmn-diagram-host');
		while (this._container.firstChild) this._container.removeChild(this._container.firstChild);
		this._container = null;
		this._svg = null;
		this._viewport = null;
		this._dragState = null;
		this._selectHandlers          = [];
		this._elementMoveHandlers     = [];
		this._createHandlers          = [];
		this._updateHandlers          = [];
		this._deleteHandlers          = [];
		this._pendingChangedHandlers  = [];
		this._remoteChangeHandlers    = [];
	}

	// ===== Model =====

	async load(args) {
		const { definitionId } = args || {};
		if (!definitionId) throw new Error('load: definitionId required');

		// Fetch the definition (elements + flows come back fully via the ui view).
		// Diagrams come back as stubs at this depth because Structr's per-call
		// depth budget is exhausted by the 15+14 element/flow expansions, so we
		// pull shapes/edges with separate, narrower queries.
		const defUrl = `${Structr.rootUrl}BpmnDefinitions/${definitionId}/ui`;
		const defRes = await fetch(defUrl, { credentials: 'same-origin' });
		if (!defRes.ok) throw new Error(`load: ${defRes.status} ${defRes.statusText}`);
		const defBody = await defRes.json();
		let def = defBody.result;
		if (Array.isArray(def)) def = def[0];
		if (!def) throw new Error('load: empty result for ' + definitionId);

		// Identify the diagram we need shapes/edges for. The BPMN spec allows
		// multiple diagrams per definition; pick the first.
		const diagramStub = (def.diagrams ?? [])[0];
		const diagramId   = diagramStub?.id;

		this._definition   = def;
		this._definitionId = definitionId;
		this._diagramId    = diagramId ?? null;
		this._elements.clear();
		this._flows.clear();
		this._shapes.clear();
		this._edges.clear();
		this._collaboration = null;
		this._participants.clear();
		this._messageFlows.clear();
		this._participantsByBpmnId.clear();
		this._messageFlowsByBpmnId.clear();
		this._lanes.clear();
		this._lanesByBpmnId.clear();
		this._buffer.creates.clear();
		this._buffer.updates.clear();
		this._buffer.deletes.clear();
		this._undoStack.length = 0;
		this._redoStack.length = 0;

		// Walk every BpmnProcess child of this BpmnDefinitions and accumulate
		// elements + flows. The /ui view of a definition returns processes as
		// stubs; we re-fetch each process to read its (process-level) name +
		// processId, then pull elements / flows by process id. Multi-process
		// (collaboration) files concatenate across all processes; rendering
		// is currently a flat layer (pool drawing for participants is a
		// follow-up).
		this._processes = [];
		for (const procStub of (def.processes ?? [])) {
			if (!procStub?.id) continue;
			try {
				const procRes = await fetch(`${Structr.rootUrl}BpmnProcess/${procStub.id}/ui`, { credentials: 'same-origin' });
				const procBody = await procRes.json();
				let proc = procBody.result;
				if (Array.isArray(proc)) proc = proc[0];
				if (proc) this._processes.push(proc);
			} catch (_) { /* ignore -- empty processes list still renders */ }
		}
		// Default "active" process for new-element creation: the first one.
		// Multi-process pools could later let the user pick by clicking in.
		this._activeProcessId = this._processes[0]?.id ?? null;

		// Per-process queries for elements / sequence flows / lanes + the
		// per-diagram queries for shapes / edges. Each in its own /ui-view
		// call so the output-depth budget isn't shared.
		const elementFetches = this._processes.map(p =>
			fetch(`${Structr.rootUrl}BpmnElement/ui?process=${p.id}`,       { credentials: 'same-origin' }).then(r => r.json()));
		const flowFetches    = this._processes.map(p =>
			fetch(`${Structr.rootUrl}BpmnSequenceFlow/ui?process=${p.id}`,  { credentials: 'same-origin' }).then(r => r.json()));
		const laneFetches    = this._processes.map(p =>
			fetch(`${Structr.rootUrl}BpmnLane/ui?process=${p.id}`,          { credentials: 'same-origin' }).then(r => r.json()));
		const shapesP = diagramId ? fetch(`${Structr.rootUrl}BpmnDiShape/ui?diagram=${diagramId}`, { credentials: 'same-origin' }).then(r => r.json()) : Promise.resolve({ result: [] });
		const edgesP  = diagramId ? fetch(`${Structr.rootUrl}BpmnDiEdge/ui?diagram=${diagramId}`,  { credentials: 'same-origin' }).then(r => r.json()) : Promise.resolve({ result: [] });

		// Optional collaboration: BpmnDefinitions.collaboration is 0..1. The
		// /ui view returns it as a stub; we re-fetch with /ui to get the
		// participants + messageFlows lists, then per-collaboration queries
		// for the participant + messageFlow nodes themselves.
		const collabStub = def.collaboration;
		const collabId   = collabStub?.id ?? null;
		const collabP    = collabId ? fetch(`${Structr.rootUrl}BpmnCollaboration/${collabId}/ui`,                  { credentials: 'same-origin' }).then(r => r.json()) : Promise.resolve({ result: null });
		const partsP     = collabId ? fetch(`${Structr.rootUrl}BpmnParticipant/ui?collaboration=${collabId}`,      { credentials: 'same-origin' }).then(r => r.json()) : Promise.resolve({ result: [] });
		const msgsP      = collabId ? fetch(`${Structr.rootUrl}BpmnMessageFlow/ui?collaboration=${collabId}`,      { credentials: 'same-origin' }).then(r => r.json()) : Promise.resolve({ result: [] });

		const [shapesBody, edgesBody, collabBody, partsBody, msgsBody, ...rest] = await Promise.all(
			[shapesP, edgesP, collabP, partsP, msgsP, ...elementFetches, ...flowFetches, ...laneFetches]);
		const elementBodies = rest.slice(0,                            this._processes.length);
		const flowBodies    = rest.slice(this._processes.length,       this._processes.length * 2);
		const laneBodies    = rest.slice(this._processes.length * 2,   this._processes.length * 3);

		for (const body of elementBodies) {
			for (const el of (body.result ?? [])) this._elements.set(el.id, el);
		}
		for (const body of flowBodies) {
			for (const fl of (body.result ?? [])) this._flows.set(fl.id, fl);
		}
		for (const body of laneBodies) {
			for (const ln of (body.result ?? [])) {
				this._lanes.set(ln.id, ln);
				if (ln.bpmnId) this._lanesByBpmnId.set(ln.bpmnId, ln);
			}
		}
		for (const sh of (shapesBody.result   ?? [])) {
			if (sh.bpmnElementRef) this._shapes.set(sh.bpmnElementRef, sh);
		}
		for (const ed of (edgesBody.result    ?? [])) {
			if (ed.bpmnElementRef) this._edges.set(ed.bpmnElementRef, ed);
		}

		// Collaboration: stash the entity stub plus its participants and
		// messageFlows. The bpmnId-keyed maps let DI-shape rendering tell
		// "this shape is a participant pool" from "this shape is an element"
		// in O(1).
		let collabResult = collabBody?.result ?? null;
		if (Array.isArray(collabResult)) collabResult = collabResult[0];
		this._collaboration = collabResult || null;
		for (const p of (partsBody.result ?? [])) {
			this._participants.set(p.id, p);
			if (p.bpmnId) this._participantsByBpmnId.set(p.bpmnId, p);
		}
		for (const m of (msgsBody.result ?? [])) {
			this._messageFlows.set(m.id, m);
			if (m.bpmnId) this._messageFlowsByBpmnId.set(m.bpmnId, m);
		}

		// Normalize gateway diamonds to a minimum visible size. Many BPMN
		// exporters emit gateways at 50x50, which leaves no room for an
		// inline label. Bumping to 80x80 (preserving centre) gives the
		// diamond an interior big enough for a wrapped label at standard
		// font size. The change lives in memory until Save flushes it.
		const GATEWAY_MIN = 80;
		for (const [, sh] of this._shapes) {
			const elementId = this._findElementIdByBpmnId(sh.bpmnElementRef);
			const element   = elementId ? this._elements.get(elementId) : null;
			const elemType  = element?.bpmnElementType ?? '';
			if (!elemType.endsWith('Gateway')) continue;
			if (sh.boundsWidth >= GATEWAY_MIN && sh.boundsHeight >= GATEWAY_MIN) continue;
			const cx = sh.boundsX + sh.boundsWidth  / 2;
			const cy = sh.boundsY + sh.boundsHeight / 2;
			sh.boundsWidth  = GATEWAY_MIN;
			sh.boundsHeight = GATEWAY_MIN;
			sh.boundsX = cx - GATEWAY_MIN / 2;
			sh.boundsY = cy - GATEWAY_MIN / 2;
			if (sh.id) this._bufferUpdate(sh.id, { boundsX: sh.boundsX, boundsY: sh.boundsY, boundsWidth: sh.boundsWidth, boundsHeight: sh.boundsHeight });
		}

		// IMPORTED LAYOUT IS PRESERVED AS-IS.
		// The gateway-min normalisation above is the only forced layout
		// edit on load: 50x50 gateways have no room for a wrapped label.
		// All other auto-correct passes (axis-cluster, cardinal-snap,
		// parallel-spread) were experimental and produced more layout
		// regressions than fixes; they're disabled. The helper methods
		// are kept in source for future targeted experiments. Users who
		// want a layout cleanup invoke `tidy()` from the toolbar.

		// Restore any pending changes not yet committed to the backend.
		this._loadBufferFromLocalStorage();
		this._applyLoadedBuffer();

		console.log(`[bpmn-diagram] elements=${this._elements.size} flows=${this._flows.size} shapes=${this._shapes.size} edges=${this._edges.size} participants=${this._participants.size} messageFlows=${this._messageFlows.size} lanes=${this._lanes.size}`);

		this.refresh();
		this.fit();
	}

	refresh() {
		if (!this._viewport) return;

		// Wipe and re-draw from the model. Simple and correct; v2 can diff if
		// performance demands.
		while (this._viewport.firstChild) this._viewport.removeChild(this._viewport.firstChild);

		// Grid background: a large rect tiled with the grid pattern, sized to
		// generously cover the world bounds. Sits behind shapes/edges so it
		// doesn't intercept pointer events on content.
		this._renderGrid();

		// Z-order:
		//   1. participant pools (background, behind everything else)
		//   2. lanes (pool subdivisions, sit on top of pools)
		//   3. sequence-flow edges
		//   4. element shapes (tasks, gateways, events)
		//   5. message-flow edges (dashed, drawn on top so they cross pools cleanly)
		//
		// Skip entries whose bpmnElementRef can't be resolved -- defence in
		// depth against orphan DI from older imports.
		for (const [, shape] of this._shapes) {
			if (shape.bpmnElementRef && this._participantsByBpmnId.has(shape.bpmnElementRef)) {
				this._renderParticipantPool(shape);
			}
		}
		for (const [, shape] of this._shapes) {
			if (shape.bpmnElementRef && this._lanesByBpmnId.has(shape.bpmnElementRef)) {
				this._renderLane(shape);
			}
		}
		for (const [, edge]  of this._edges)  {
			if (edge.bpmnElementRef && this._findFlowByBpmnId(edge.bpmnElementRef)) {
				this._renderEdge(edge);
			}
		}
		for (const [, shape] of this._shapes) {
			if (shape.bpmnElementRef && this._findElementIdByBpmnId(shape.bpmnElementRef)) {
				this._renderShape(shape);
			}
		}
		for (const [, edge]  of this._edges)  {
			if (edge.bpmnElementRef && this._messageFlowsByBpmnId.has(edge.bpmnElementRef)) {
				this._renderMessageFlowEdge(edge);
			}
		}

		this._applyHighlights();
		this._applySelection();
		this._applyTransform();
	}

	_renderGrid() {
		// Compute world bounds of all shapes and pad generously, so panning
		// well past the diagram still shows grid.
		let minX = 0, minY = 0, maxX = 1000, maxY = 1000;
		if (this._shapes.size > 0) {
			minX = Infinity; minY = Infinity; maxX = -Infinity; maxY = -Infinity;
			for (const [, sh] of this._shapes) {
				minX = Math.min(minX, sh.boundsX);
				minY = Math.min(minY, sh.boundsY);
				maxX = Math.max(maxX, sh.boundsX + sh.boundsWidth);
				maxY = Math.max(maxY, sh.boundsY + sh.boundsHeight);
			}
		}
		const pad = 1000;
		const rect = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'rect');
		rect.setAttribute('x',      minX - pad);
		rect.setAttribute('y',      minY - pad);
		rect.setAttribute('width',  (maxX - minX) + 2 * pad);
		rect.setAttribute('height', (maxY - minY) + 2 * pad);
		rect.setAttribute('fill',   'url(#bpmn-grid)');
		rect.setAttribute('pointer-events', 'none');
		this._viewport.appendChild(rect);
	}

	// ===== Selection =====

	select(elementId) {
		if (this._selectedId === elementId && this._selectedFlowId === null) return;
		this._selectedId     = elementId || null;
		this._selectedFlowId = null;
		this._applySelection();
		for (const h of this._selectHandlers) {
			try { h(this._selectedId); } catch (e) { console.error(e); }
		}
	}

	getSelected() { return this._selectedId; }

	onSelect(handler) {
		this._selectHandlers.push(handler);
		return () => {
			const i = this._selectHandlers.indexOf(handler);
			if (i >= 0) this._selectHandlers.splice(i, 1);
		};
	}

	/**
	 * Select an edge (sequence flow). Mutually exclusive with element
	 * selection: any element selection is cleared. Triggers bend-handle
	 * rendering on the selected edge.
	 */
	selectFlow(flowId) {
		if (this._selectedFlowId === flowId && this._selectedId === null) return;
		this._selectedFlowId = flowId || null;
		this._selectedId     = null;
		this._applySelection();
		// Element-select handlers fire with null so hosts that key off
		// "anything selected? -> show side panel" hide their element panel.
		for (const h of this._selectHandlers) {
			try { h(null); } catch (e) { console.error(e); }
		}
	}

	getSelectedFlow() { return this._selectedFlowId; }

	// ===== Viewport =====

	fit() {
		if (!this._svg || this._shapes.size === 0) return;

		// World bounds across all shapes.
		let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
		for (const [, sh] of this._shapes) {
			minX = Math.min(minX, sh.boundsX);
			minY = Math.min(minY, sh.boundsY);
			maxX = Math.max(maxX, sh.boundsX + sh.boundsWidth);
			maxY = Math.max(maxY, sh.boundsY + sh.boundsHeight);
		}
		// Edges may extend beyond shapes; include their waypoints.
		for (const [, ed] of this._edges) {
			for (const w of this._parseWaypoints(ed)) {
				minX = Math.min(minX, w.x); minY = Math.min(minY, w.y);
				maxX = Math.max(maxX, w.x); maxY = Math.max(maxY, w.y);
			}
		}
		if (!Number.isFinite(minX)) return;

		const margin = 40;
		minX -= margin; minY -= margin; maxX += margin; maxY += margin;

		const worldW = maxX - minX;
		const worldH = maxY - minY;
		const rect   = this._svg.getBoundingClientRect();
		const scale  = Math.min(rect.width / worldW, rect.height / worldH);

		this._zoom = Math.max(_BpmnDiagramCommunitySvg.MIN_ZOOM, Math.min(_BpmnDiagramCommunitySvg.MAX_ZOOM, scale));
		// Center.
		this._panX = (rect.width  - worldW * this._zoom) / 2 - minX * this._zoom;
		this._panY = (rect.height - worldH * this._zoom) / 2 - minY * this._zoom;
		this._applyTransform();
	}

	zoom(factor) {
		const next = this._zoom * factor;
		this._zoom = Math.max(_BpmnDiagramCommunitySvg.MIN_ZOOM, Math.min(_BpmnDiagramCommunitySvg.MAX_ZOOM, next));
		this._applyTransform();
	}

	pan(dx, dy) {
		this._panX += dx;
		this._panY += dy;
		this._applyTransform();
	}

	// ===== Edits =====

	onElementMove(handler) {
		this._elementMoveHandlers.push(handler);
		return () => {
			const i = this._elementMoveHandlers.indexOf(handler);
			if (i >= 0) this._elementMoveHandlers.splice(i, 1);
		};
	}

	// ----- Topology edits -----

	createElement(args) {
		const { type, name = null, x, y, width, height, parentId = null, props: extraProps = {} } = args || {};
		if (!type)            throw new Error('createElement: type required');
		if (typeof x !== 'number' || typeof y !== 'number') throw new Error('createElement: x, y required');

		// Parse "bpmnElementType:eventDefinitionType" syntax used by the
		// palette for typed events (e.g. "startEvent:messageEventDefinition").
		// The colon-prefix is split off and stored as eventDefinitionType
		// so the renderer's marker glyph picks it up.
		let elementType  = type;
		let eventDefType = null;
		const colonIdx   = type.indexOf(':');
		if (colonIdx > 0) {
			elementType  = type.substring(0, colonIdx);
			eventDefType = type.substring(colonIdx + 1);
		}

		const elementId = this._uuid();
		const shapeId   = this._uuid();
		const bpmnId    = this._genBpmnId(this._typePrefixForBpmnElementType(elementType));
		const isEvent   = elementType.endsWith('Event');
		const isGateway = elementType.endsWith('Gateway');
		const isData    = elementType === 'dataObjectReference' || elementType === 'dataStoreReference' || elementType === 'dataObject';
		const w = (typeof width  === 'number') ? width  : (isEvent ? 36 : (isGateway ? 80 : (isData ? 50 : 100)));
		const h = (typeof height === 'number') ? height : (isEvent ? 36 : (isGateway ? 80 : (isData ? 50 : 80)));

		// In-memory model
		const element = {
			id: elementId, type: 'BpmnElement',
			bpmnId, bpmnElementType: elementType, bpmnName: name, name: name || bpmnId,
			process: this._activeProcessId ? { id: this._activeProcessId } : null,
			parentElement: parentId ? { id: parentId } : null,
			...(eventDefType ? { eventDefinitionType: eventDefType } : {}),
			...extraProps
		};
		const shape = {
			id: shapeId, type: 'BpmnDiShape',
			bpmnElementRef: bpmnId,
			boundsX: x, boundsY: y, boundsWidth: w, boundsHeight: h,
			diagram: this._diagramId ? { id: this._diagramId } : null,
		};
		this._elements.set(elementId, element);
		this._shapes.set(bpmnId, shape);

		// Buffer entries
		const elementProps = { bpmnId, bpmnElementType: elementType, process: this._activeProcessId };
		if (name)         elementProps.bpmnName            = name;
		if (parentId)     elementProps.parentElement       = parentId;
		if (eventDefType) elementProps.eventDefinitionType = eventDefType;
		Object.assign(elementProps, extraProps);
		this._bufferCreate(elementId, 'BpmnElement', elementProps);

		const shapeProps = { bpmnElementRef: bpmnId, boundsX: x, boundsY: y, boundsWidth: w, boundsHeight: h };
		if (this._diagramId) shapeProps.diagram = this._diagramId;
		this._bufferCreate(shapeId, 'BpmnDiShape', shapeProps);

		this._pushUndo({
			apply: () => {
				this._elements.set(elementId, element);
				this._shapes.set(bpmnId, shape);
				this._bufferCreate(elementId, 'BpmnElement', elementProps);
				this._bufferCreate(shapeId,   'BpmnDiShape', shapeProps);
				this.refresh();
				for (const h of this._createHandlers) { try { h(elementId, 'BpmnElement'); } catch (e) { console.error(e); } }
			},
			revert: () => {
				this._elements.delete(elementId);
				this._shapes.delete(bpmnId);
				this._buffer.creates.delete(elementId);
				this._buffer.creates.delete(shapeId);
				this._writeBufferToLocalStorage();
				this.refresh();
				for (const h of this._deleteHandlers) { try { h(elementId, 'BpmnElement'); } catch (e) { console.error(e); } }
			},
		});

		this.refresh();
		// Auto-select the new element so the side panel populates and the
		// connect handle appears -- no extra click required.
		this.select(elementId);
		for (const h of this._createHandlers) { try { h(elementId, 'BpmnElement'); } catch (e) { console.error(e); } }
		return elementId;
	}

	getElement(elementId) {
		const el = this._elements.get(elementId);
		return el ? { ...el } : null;
	}

	/**
	 * Return shallow copies of every BpmnProcess loaded with the active
	 * BpmnDefinitions. A single definition may host multiple processes
	 * (collaboration: pool A + pool B), so callers that surface a "process
	 * settings" UI must accommodate the whole list rather than picking
	 * the first.
	 */
	getProcesses() {
		return (this._processes || []).map(p => ({ ...p }));
	}

	/**
	 * Mutate properties of a BpmnProcess (e.g. processName,
	 * processIsExecutable, defaultAssigneeFromInitiator). Mirrors
	 * `updateElement` for BpmnElement: in-memory mutation + buffer
	 * write + single undo entry + update-handler notification. Save is
	 * deferred to the editor's flush, same as every other edit.
	 */
	updateProcess(processId, props) {
		const proc = (this._processes || []).find(p => p.id === processId);
		if (!proc) throw new Error(`updateProcess: ${processId} not in model`);
		const before = {};
		for (const k of Object.keys(props)) before[k] = proc[k];
		Object.assign(proc, props);
		this._bufferUpdate(processId, props);

		this._pushUndo({
			apply:  () => { Object.assign(proc, props);  this._bufferUpdate(processId, props);  for (const h of this._updateHandlers) { try { h(processId, 'BpmnProcess', props); } catch (e) { console.error(e); } } },
			revert: () => { Object.assign(proc, before); this._bufferUpdate(processId, before); for (const h of this._updateHandlers) { try { h(processId, 'BpmnProcess', before); } catch (e) { console.error(e); } } },
		});
		for (const h of this._updateHandlers) { try { h(processId, 'BpmnProcess', props); } catch (e) { console.error(e); } }
	}

	async refreshElement(elementId) {
		if (!elementId) return;
		const res = await fetch(`${Structr.rootUrl}BpmnElement/${elementId}/ui`, { credentials: 'same-origin', cache: 'no-store' });
		if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
		const body = await res.json();
		let fresh = body.result;
		if (Array.isArray(fresh)) fresh = fresh[0];
		if (!fresh || !fresh.id) return;
		this._elements.set(fresh.id, fresh);
		// Repaint so visual state derived from the element (e.g. the method-count badge)
		// updates. updateElement() does this; refreshElement() previously only mutated the
		// model and fired handlers, so the SVG never redrew.
		this.refresh();
		for (const h of this._updateHandlers) {
			try { h(fresh.id, 'BpmnElement', {}); } catch (e) { console.error(e); }
		}
	}

	updateElement(elementId, props) {
		const element = this._elements.get(elementId);
		if (!element) throw new Error(`updateElement: ${elementId} not in model`);
		const before = {};
		for (const k of Object.keys(props)) before[k] = element[k];
		Object.assign(element, props);
		this._bufferUpdate(elementId, props);

		this._pushUndo({
			apply:  () => { Object.assign(element, props);  this._bufferUpdate(elementId, props);  this.refresh(); for (const h of this._updateHandlers) { try { h(elementId, 'BpmnElement', props); } catch (e) { console.error(e); } } },
			revert: () => { Object.assign(element, before); this._bufferUpdate(elementId, before); this.refresh(); for (const h of this._updateHandlers) { try { h(elementId, 'BpmnElement', before); } catch (e) { console.error(e); } } },
		});
		this.refresh();
		for (const h of this._updateHandlers) { try { h(elementId, 'BpmnElement', props); } catch (e) { console.error(e); } }
	}

	deleteElement(elementId) {
		const element = this._elements.get(elementId);
		if (!element) return;
		const bpmnId = element.bpmnId;
		const shape  = bpmnId ? this._shapes.get(bpmnId) : null;
		// Incident flows: source or target == bpmnId.
		const incidentFlows = [];
		const incidentEdges = [];
		for (const [, fl] of this._flows) {
			if (fl.sourceRefId === bpmnId || fl.targetRefId === bpmnId) {
				incidentFlows.push(fl);
				const ed = this._edges.get(fl.bpmnId);
				if (ed) incidentEdges.push(ed);
			}
		}

		// In-memory removal
		this._elements.delete(elementId);
		if (shape && bpmnId) this._shapes.delete(bpmnId);
		for (const fl of incidentFlows) this._flows.delete(fl.id);
		for (const ed of incidentEdges) {
			for (const [k, e] of this._edges) if (e.id === ed.id) { this._edges.delete(k); break; }
		}

		// Buffer
		this._bufferDelete(elementId);
		if (shape && shape.id) this._bufferDelete(shape.id);
		for (const fl of incidentFlows) if (fl.id) this._bufferDelete(fl.id);
		for (const ed of incidentEdges) if (ed.id) this._bufferDelete(ed.id);

		this._pushUndo({
			apply: () => {
				this.deleteElement(elementId); // re-applies the same compound delete
			},
			revert: () => {
				this._elements.set(elementId, element);
				if (shape && bpmnId) this._shapes.set(bpmnId, shape);
				for (const fl of incidentFlows) this._flows.set(fl.id, fl);
				for (const ed of incidentEdges) if (ed.bpmnElementRef) this._edges.set(ed.bpmnElementRef, ed);
				// Remove from deletes set
				this._buffer.deletes.delete(elementId);
				if (shape && shape.id) this._buffer.deletes.delete(shape.id);
				for (const fl of incidentFlows) if (fl.id) this._buffer.deletes.delete(fl.id);
				for (const ed of incidentEdges) if (ed.id) this._buffer.deletes.delete(ed.id);
				this._writeBufferToLocalStorage();
				this.refresh();
				for (const h of this._createHandlers) { try { h(elementId, 'BpmnElement'); } catch (e) { console.error(e); } }
			},
		});
		this.refresh();
		for (const h of this._deleteHandlers) { try { h(elementId, 'BpmnElement'); } catch (e) { console.error(e); } }
	}

	createFlow(args) {
		const { sourceId, targetId, name = null, condition = null, conditionType = null, waypoints = null } = args || {};
		if (!sourceId || !targetId) throw new Error('createFlow: sourceId and targetId required');
		const source = this._elements.get(sourceId);
		const target = this._elements.get(targetId);
		if (!source || !target) throw new Error('createFlow: source/target not in model');

		const flowId   = this._uuid();
		const edgeId   = this._uuid();
		const flowBpmnId = this._genBpmnId('Flow');

		// Default routing: Manhattan (orthogonal) — exits source on the side
		// facing the target, optional mid-elbow, enters target on the
		// opposite side. Falls back to a 2-point straight line when shapes
		// are aligned along the relevant axis.
		const sShape = this._shapes.get(source.bpmnId);
		const tShape = this._shapes.get(target.bpmnId);
		let wps = waypoints;
		if (!wps && sShape && tShape) {
			wps = this._routeOrthogonal(sShape, tShape);
		}
		if (!wps) wps = [];

		const flow = {
			id: flowId, type: 'BpmnSequenceFlow',
			bpmnId: flowBpmnId, bpmnName: name,
			sourceRefId: source.bpmnId, targetRefId: target.bpmnId,
			sourceElement: { id: sourceId }, targetElement: { id: targetId },
			process: this._activeProcessId ? { id: this._activeProcessId } : null,
			conditionExpression: condition, conditionExpressionType: conditionType,
		};
		const edge = {
			id: edgeId, type: 'BpmnDiEdge',
			bpmnElementRef: flowBpmnId,
			waypoints: JSON.stringify(wps),
			diagram: this._diagramId ? { id: this._diagramId } : null,
		};
		this._flows.set(flowId, flow);
		this._edges.set(flowBpmnId, edge);

		const flowProps = {
			bpmnId: flowBpmnId,
			sourceRefId: source.bpmnId, targetRefId: target.bpmnId,
			sourceElement: sourceId, targetElement: targetId,
			process: this._activeProcessId,
		};
		if (name)          flowProps.bpmnName                = name;
		if (condition)     flowProps.conditionExpression     = condition;
		if (conditionType) flowProps.conditionExpressionType = conditionType;
		this._bufferCreate(flowId, 'BpmnSequenceFlow', flowProps);

		const edgeProps = { bpmnElementRef: flowBpmnId, waypoints: JSON.stringify(wps) };
		if (this._diagramId) edgeProps.diagram = this._diagramId;
		this._bufferCreate(edgeId, 'BpmnDiEdge', edgeProps);

		this._pushUndo({
			apply: () => {
				this._flows.set(flowId, flow);
				this._edges.set(flowBpmnId, edge);
				this._bufferCreate(flowId, 'BpmnSequenceFlow', flowProps);
				this._bufferCreate(edgeId, 'BpmnDiEdge', edgeProps);
				this.refresh();
				for (const h of this._createHandlers) { try { h(flowId, 'BpmnSequenceFlow'); } catch (e) { console.error(e); } }
			},
			revert: () => {
				this._flows.delete(flowId);
				this._edges.delete(flowBpmnId);
				this._buffer.creates.delete(flowId);
				this._buffer.creates.delete(edgeId);
				this._writeBufferToLocalStorage();
				this.refresh();
				for (const h of this._deleteHandlers) { try { h(flowId, 'BpmnSequenceFlow'); } catch (e) { console.error(e); } }
			},
		});
		this.refresh();
		for (const h of this._createHandlers) { try { h(flowId, 'BpmnSequenceFlow'); } catch (e) { console.error(e); } }
		return flowId;
	}

	updateFlow(flowId, props) {
		const flow = this._flows.get(flowId);
		if (!flow) throw new Error(`updateFlow: ${flowId} not in model`);
		const before = {};
		for (const k of Object.keys(props)) before[k] = flow[k];
		Object.assign(flow, props);
		this._bufferUpdate(flowId, props);
		this._pushUndo({
			apply:  () => { Object.assign(flow, props);  this._bufferUpdate(flowId, props);  this.refresh(); for (const h of this._updateHandlers) { try { h(flowId, 'BpmnSequenceFlow', props); } catch (e) { console.error(e); } } },
			revert: () => { Object.assign(flow, before); this._bufferUpdate(flowId, before); this.refresh(); for (const h of this._updateHandlers) { try { h(flowId, 'BpmnSequenceFlow', before); } catch (e) { console.error(e); } } },
		});
		this.refresh();
		for (const h of this._updateHandlers) { try { h(flowId, 'BpmnSequenceFlow', props); } catch (e) { console.error(e); } }
	}

	deleteFlow(flowId) {
		const flow = this._flows.get(flowId);
		if (!flow) return;
		const flowBpmnId = flow.bpmnId;
		const edge = flowBpmnId ? this._edges.get(flowBpmnId) : null;

		this._flows.delete(flowId);
		if (edge && flowBpmnId) this._edges.delete(flowBpmnId);

		this._bufferDelete(flowId);
		if (edge && edge.id) this._bufferDelete(edge.id);

		this._pushUndo({
			apply:  () => { this.deleteFlow(flowId); },
			revert: () => {
				this._flows.set(flowId, flow);
				if (edge && flowBpmnId) this._edges.set(flowBpmnId, edge);
				this._buffer.deletes.delete(flowId);
				if (edge && edge.id) this._buffer.deletes.delete(edge.id);
				this._writeBufferToLocalStorage();
				this.refresh();
				for (const h of this._createHandlers) { try { h(flowId, 'BpmnSequenceFlow'); } catch (e) { console.error(e); } }
			},
		});
		this.refresh();
		for (const h of this._deleteHandlers) { try { h(flowId, 'BpmnSequenceFlow'); } catch (e) { console.error(e); } }
	}

	// ----- Geometry edits -----

	setShape(elementId, bounds) {
		const element = this._elements.get(elementId);
		if (!element) throw new Error(`setShape: element ${elementId} not in model`);
		const shape = this._shapes.get(element.bpmnId);
		if (!shape) throw new Error(`setShape: no shape for element ${elementId}`);
		const before = { boundsX: shape.boundsX, boundsY: shape.boundsY, boundsWidth: shape.boundsWidth, boundsHeight: shape.boundsHeight };

		const next = {};
		if (typeof bounds.x      === 'number') { shape.boundsX      = bounds.x;      next.boundsX = bounds.x; }
		if (typeof bounds.y      === 'number') { shape.boundsY      = bounds.y;      next.boundsY = bounds.y; }
		if (typeof bounds.width  === 'number') { shape.boundsWidth  = bounds.width;  next.boundsWidth  = bounds.width; }
		if (typeof bounds.height === 'number') { shape.boundsHeight = bounds.height; next.boundsHeight = bounds.height; }
		this._refreshIncidentEdges(shape);
		if (shape.id) this._bufferUpdate(shape.id, next);

		this._pushUndo({
			apply: () => {
				Object.assign(shape, next);
				this._refreshIncidentEdges(shape);
				if (shape.id) this._bufferUpdate(shape.id, next);
				this.refresh();
				for (const h of this._updateHandlers) { try { h(elementId, 'BpmnElement', next); } catch (e) { console.error(e); } }
			},
			revert: () => {
				Object.assign(shape, before);
				this._refreshIncidentEdges(shape);
				if (shape.id) this._bufferUpdate(shape.id, before);
				this.refresh();
				for (const h of this._updateHandlers) { try { h(elementId, 'BpmnElement', before); } catch (e) { console.error(e); } }
			},
		});
		this.refresh();
		for (const h of this._updateHandlers) { try { h(elementId, 'BpmnElement', next); } catch (e) { console.error(e); } }
	}

	setEdgeWaypoints(flowId, waypoints) {
		const flow = this._flows.get(flowId);
		if (!flow) throw new Error(`setEdgeWaypoints: flow ${flowId} not in model`);
		const edge = this._edges.get(flow.bpmnId);
		if (!edge) throw new Error(`setEdgeWaypoints: no edge for flow ${flowId}`);
		const beforeStr = (typeof edge.waypoints === 'string') ? edge.waypoints : JSON.stringify(edge.waypoints || []);
		const nextStr   = JSON.stringify(waypoints);
		edge.waypoints  = nextStr;
		if (edge.id) this._bufferUpdate(edge.id, { waypoints: nextStr });
		this._pushUndo({
			apply:  () => { edge.waypoints = nextStr;   if (edge.id) this._bufferUpdate(edge.id, { waypoints: nextStr   }); this.refresh(); },
			revert: () => { edge.waypoints = beforeStr; if (edge.id) this._bufferUpdate(edge.id, { waypoints: beforeStr }); this.refresh(); },
		});
		this.refresh();
	}

	/**
	 * Auto-route an existing edge using the Manhattan (orthogonal) router.
	 * Replaces all waypoints, snaps endpoints to the shapes' borders, and
	 * pushes a single undo entry. Useful as a "Reroute" action on selected
	 * edges or to tidy up after large layout changes.
	 */
	relayoutEdge(flowId) {
		const flow = this._flows.get(flowId);
		if (!flow) throw new Error(`relayoutEdge: flow ${flowId} not in model`);
		const sourceId = flow.sourceElement?.id ?? flow.sourceElement;
		const targetId = flow.targetElement?.id ?? flow.targetElement;
		const source   = sourceId ? this._elements.get(sourceId) : null;
		const target   = targetId ? this._elements.get(targetId) : null;
		if (!source || !target) return;
		const sShape   = this._shapes.get(source.bpmnId);
		const tShape   = this._shapes.get(target.bpmnId);
		if (!sShape || !tShape) return;
		const wps = this._routeOrthogonal(sShape, tShape);
		this.setEdgeWaypoints(flowId, wps);
	}

	/**
	 * Tidy up the layout: snap every element shape's CENTRE to the grid,
	 * snap pool / lane bounds to the grid, inset lanes inside their pools'
	 * content area, and re-snap edge endpoints for shapes that actually
	 * moved. Buffers all changes so the next save persists them, and pushes
	 * a single undo entry so the whole tidy is one ctrl-Z.
	 *
	 * Imported layouts are preserved as-is on load -- this is the
	 * "clean up my diagram" button users invoke explicitly.
	 */
	tidy() {
		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		const inset = _BpmnDiagramCommunitySvg.POOL_STRIP_WIDTH;

		// Snapshot before-state for every shape we may touch, so a single
		// undo entry restores the whole tidy.
		const shapeSnaps = [];
		const edgeSnaps  = [];
		for (const [, sh] of this._shapes) {
			shapeSnaps.push({
				shape: sh,
				before: {
					boundsX: sh.boundsX, boundsY: sh.boundsY,
					boundsWidth: sh.boundsWidth, boundsHeight: sh.boundsHeight,
				},
			});
		}
		for (const [, ed] of this._edges) {
			edgeSnaps.push({ edge: ed, before: ed.waypoints });
		}

		const moved = new Set();
		for (const [, sh] of this._shapes) {
			const isPool = this._participantsByBpmnId.has(sh.bpmnElementRef);
			const isLane = this._lanesByBpmnId.has(sh.bpmnElementRef);
			// Pool and lane bounds intentionally NOT snapped to grid:
			// adjacent lanes are stacked vertically and their flush-ness
			// (lane B.y == lane A.y + A.height) only holds if they're
			// rounded together, not independently. Snapping them in
			// isolation produces a 10-px gap or overlap whenever the
			// rounding directions differ. Pool / lane sizing is the
			// author's call -- leave it alone.
			if (isPool || isLane) continue;
			// Boundary events ride on their host activity's perimeter
			// (BPMN attachedToRef). Snapping their centre to the grid
			// independently of the host pulls them off the perimeter
			// whenever the host's centre and the boundary's centre
			// disagree on rounding. They get translated by the host's
			// delta further down, so skip them here.
			const elForShape = this._elementByBpmnId(sh.bpmnElementRef);
			if (elForShape && this._attachedToBpmnIdOf(elForShape)) continue;
			const x0 = sh.boundsX, y0 = sh.boundsY;
			const cxSnapped = Math.round((x0 + sh.boundsWidth  / 2) / step) * step;
			const cySnapped = Math.round((y0 + sh.boundsHeight / 2) / step) * step;
			sh.boundsX = cxSnapped - sh.boundsWidth  / 2;
			sh.boundsY = cySnapped - sh.boundsHeight / 2;
			if (sh.boundsX !== x0 || sh.boundsY !== y0) {
				if (sh.bpmnElementRef) moved.add(sh.bpmnElementRef);
			}
		}

		// Inset lanes inside their pool's content area.
		for (const [, poolShape] of this._shapes) {
			if (!this._participantsByBpmnId.has(poolShape.bpmnElementRef)) continue;
			for (const laneShape of this._laneShapesForPool(poolShape)) {
				const x0 = laneShape.boundsX, w0 = laneShape.boundsWidth;
				const tx = poolShape.boundsX + inset;
				const tw = Math.max(step, poolShape.boundsWidth - inset);
				if (x0 === tx && w0 === tw) continue;
				laneShape.boundsX     = tx;
				laneShape.boundsWidth = tw;
				if (laneShape.bpmnElementRef) moved.add(laneShape.bpmnElementRef);
			}
		}

		// Horizontal-axis clustering: shapes connected by sequence flows
		// in the same lane with overlapping y bboxes get pulled onto a
		// common centre y. Supersedes the previous gateway-only y-align
		// pass: works for any pair (event-task, gateway-task, etc.) and
		// uses lanes as the canonical "row grouping" signal.
		const axisHResult = this._autoAlignHorizontalAxes();
		for (const id of axisHResult.moved) moved.add(id);

		// Vertical-axis clustering: shapes connected by ANY flow (sequence
		// or message) with overlapping x bboxes get pulled onto a common
		// centre x. Handles cross-pool messageFlows (Pay/Receive) and
		// vertical sequence flows (gateway -> 60 minutes timer) alike.
		const axisVResult = this._autoAlignVerticalAxes();
		for (const id of axisVResult.moved) moved.add(id);

		// Translate boundary events along with their host activity so
		// `attachedToRef` is honoured: when Tidy nudges a host onto a
		// shared axis, any boundaries riding on its perimeter follow by
		// the same delta. Computed against shapeSnaps' before-state so
		// every host's net delta is applied exactly once, regardless of
		// how many passes touched it.
		for (const snap of shapeSnaps) {
			const host = snap.shape;
			const dx = host.boundsX - snap.before.boundsX;
			const dy = host.boundsY - snap.before.boundsY;
			if (dx === 0 && dy === 0) continue;
			const attached = this._attachedShapesFor(host.bpmnElementRef);
			for (const a of attached) {
				a.shape.boundsX += dx;
				a.shape.boundsY += dy;
				if (a.shape.bpmnElementRef) moved.add(a.shape.bpmnElementRef);
			}
		}

		// Re-snap incident edges only for shapes that actually shifted.
		for (const [, sh] of this._shapes) {
			if (sh.bpmnElementRef && moved.has(sh.bpmnElementRef)) {
				this._refreshIncidentEdges(sh);
			}
		}

		// Drop redundant interior bends on every edge (collinear or
		// duplicate waypoints). Cluster passes often make pre-authored
		// zig-zags unnecessary; pruning gives visibly straight runs
		// instead of stair-step waypoints sitting on a common axis.
		// Mutates edge.waypoints in place; the edgeSnaps captured above
		// already hold the before-state, so the change is rolled into
		// the same undo entry as the rest of Tidy.
		this._pruneRedundantBends();

		// Port / slot re-assignment is intentionally NOT part of Tidy.
		// Endpoint placement is now manual in the Community editor (drag
		// the endpoint handle of a selected edge onto a cardinal slot).
		// Auto-redistribute was useful before manual re-attachment
		// existed, but with explicit slot picking it just moves
		// authored endpoints around behind the user's back.
		// `_redistributeConnections` is kept in source for possible
		// future targeted use, but no longer called from `tidy()`.

		// Build the after-state and write to buffer.
		const shapeChanges = shapeSnaps
			.map(s => ({
				shape: s.shape,
				before: s.before,
				after: {
					boundsX: s.shape.boundsX, boundsY: s.shape.boundsY,
					boundsWidth: s.shape.boundsWidth, boundsHeight: s.shape.boundsHeight,
				},
			}))
			.filter(c =>
				c.before.boundsX !== c.after.boundsX || c.before.boundsY !== c.after.boundsY
				|| c.before.boundsWidth !== c.after.boundsWidth
				|| c.before.boundsHeight !== c.after.boundsHeight);
		const edgeChanges = edgeSnaps
			.map(s => ({ edge: s.edge, before: s.before, after: s.edge.waypoints }))
			.filter(c => c.before !== c.after);

		if (shapeChanges.length === 0 && edgeChanges.length === 0) {
			this.refresh();
			return;  // already tidy
		}

		for (const c of shapeChanges) if (c.shape.id) this._bufferUpdate(c.shape.id, c.after);
		for (const ec of edgeChanges) if (ec.edge.id) this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
		this._pushUndo({
			apply:  () => {
				for (const c of shapeChanges)  { Object.assign(c.shape, c.after);  if (c.shape.id) this._bufferUpdate(c.shape.id, c.after); }
				for (const ec of edgeChanges)  { ec.edge.waypoints = ec.after;  if (ec.edge.id) this._bufferUpdate(ec.edge.id, { waypoints: ec.after }); }
				this.refresh();
			},
			revert: () => {
				for (const c of shapeChanges)  { Object.assign(c.shape, c.before); if (c.shape.id) this._bufferUpdate(c.shape.id, c.before); }
				for (const ec of edgeChanges)  { ec.edge.waypoints = ec.before; if (ec.edge.id) this._bufferUpdate(ec.edge.id, { waypoints: ec.before }); }
				this.refresh();
			},
		});
		this.refresh();
	}

	// ----- Manhattan / orthogonal routing -----
	//
	// Picks the source and target shapes' "exit" / "entry" sides based on
	// their relative position, then builds a 2- or 4-point orthogonal path
	// between them. Snaps endpoints to grid so handles line up cleanly with
	// the snap step used by drag and resize.

	_routeOrthogonal(sShape, tShape) {
		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		const snap = (v) => Math.round(v / step) * step;

		const sCx = sShape.boundsX + sShape.boundsWidth  / 2;
		const sCy = sShape.boundsY + sShape.boundsHeight / 2;
		const tCx = tShape.boundsX + tShape.boundsWidth  / 2;
		const tCy = tShape.boundsY + tShape.boundsHeight / 2;
		const dx  = tCx - sCx;
		const dy  = tCy - sCy;

		// Pick exit/entry sides based on which axis dominates. This produces
		// the natural "left-to-right" routing when source is left of target,
		// and "top-down" when source is above target.
		const horizontal = Math.abs(dx) >= Math.abs(dy);
		const exitSide   = horizontal ? (dx > 0 ? 'right'  : 'left')  : (dy > 0 ? 'bottom' : 'top');
		const entrySide  = horizontal ? (dx > 0 ? 'left'   : 'right') : (dy > 0 ? 'top'    : 'bottom');

		const sP = this._sidePoint(sShape, exitSide);
		const tP = this._sidePoint(tShape, entrySide);

		// Two-point straight line if the exit and entry already line up.
		if (horizontal && Math.abs(sP.y - tP.y) < 0.5) {
			return [{ x: snap(sP.x), y: snap(sP.y) }, { x: snap(tP.x), y: snap(tP.y) }];
		}
		if (!horizontal && Math.abs(sP.x - tP.x) < 0.5) {
			return [{ x: snap(sP.x), y: snap(sP.y) }, { x: snap(tP.x), y: snap(tP.y) }];
		}

		// Four-point Z-route: exit out, jog to mid axis, enter from opposite.
		if (horizontal) {
			const midX = snap((sP.x + tP.x) / 2);
			return [
				{ x: snap(sP.x), y: snap(sP.y) },
				{ x: midX,       y: snap(sP.y) },
				{ x: midX,       y: snap(tP.y) },
				{ x: snap(tP.x), y: snap(tP.y) },
			];
		} else {
			const midY = snap((sP.y + tP.y) / 2);
			return [
				{ x: snap(sP.x), y: snap(sP.y) },
				{ x: snap(sP.x), y: midY       },
				{ x: snap(tP.x), y: midY       },
				{ x: snap(tP.x), y: snap(tP.y) },
			];
		}
	}

	/**
	 * Midpoint of a shape's named side. Used by the orthogonal router so the
	 * line exits/enters at the centre of an edge instead of a corner.
	 */
	_sidePoint(shape, side) {
		const x1 = shape.boundsX,                            y1 = shape.boundsY;
		const x2 = shape.boundsX + shape.boundsWidth,        y2 = shape.boundsY + shape.boundsHeight;
		const cx = (x1 + x2) / 2,                            cy = (y1 + y2) / 2;
		switch (side) {
			case 'left':   return { x: x1, y: cy };
			case 'right':  return { x: x2, y: cy };
			case 'top':    return { x: cx, y: y1 };
			case 'bottom': return { x: cx, y: y2 };
		}
		return { x: cx, y: cy };
	}

	/**
	 * Point at parametric position `t` (0..1) along a named side of a
	 * shape's bounding box. Used by the connection-point redistribute pass:
	 * each side hosts n evenly-spaced slots at t = (i+0.5)/n for i=0..n-1.
	 */
	_pointAtSlot(shape, side, t) {
		const x1 = shape.boundsX,                     y1 = shape.boundsY;
		const x2 = x1 + shape.boundsWidth,            y2 = y1 + shape.boundsHeight;
		switch (side) {
			case 'top':    return { x: x1 + t * (x2 - x1), y: y1 };
			case 'bottom': return { x: x1 + t * (x2 - x1), y: y2 };
			case 'left':   return { x: x1,                 y: y1 + t * (y2 - y1) };
			case 'right':  return { x: x2,                 y: y1 + t * (y2 - y1) };
		}
		return { x: (x1 + x2) / 2, y: (y1 + y2) / 2 };
	}

	/**
	 * Redistribute every shape's incident edge endpoints into evenly-
	 * spaced slots per side, ordered to minimise crossings.
	 *
	 * Algorithm per shape:
	 *   1. For each incident edge, compute the "ideal side" by comparing
	 *      the dx/dy from this shape's centre to the OTHER endpoint.
	 *   2. Bucket edges by ideal side.
	 *   3. Sort each side's edges by the other endpoint's coordinate
	 *      along the side axis (otherEnd.x for top/bottom; otherEnd.y
	 *      for left/right). Leftmost target gets the leftmost slot ->
	 *      no crossings between siblings on the same side.
	 *   4. Allocate slots at t = (i+0.5)/n for the n edges on that side.
	 *   5. Update the edge's endpoint that touches this shape; propagate
	 *      orthogonal alignment to the adjacent bend if it shared an axis
	 *      with the old endpoint.
	 *
	 * Pools, lanes, and self-loops are skipped. Both sequence flows and
	 * message flows participate.
	 */
	_redistributeConnections() {
		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		const snap = (v) => Math.round(v / step) * step;

		// Build a per-shape list of incident edges.
		const incidentByBpmnId = new Map();   // bpmnId -> [{edge, isSource}, ...]
		for (const [, ed] of this._edges) {
			const seq = this._findFlowByBpmnId(ed.bpmnElementRef);
			const msg = !seq ? this._messageFlowsByBpmnId.get(ed.bpmnElementRef) : null;
			const flow = seq || msg;
			if (!flow) continue;
			const src = flow.sourceRefId, tgt = flow.targetRefId;
			if (!src || !tgt || src === tgt) continue;  // self-loop: skip
			if (!incidentByBpmnId.has(src)) incidentByBpmnId.set(src, []);
			if (!incidentByBpmnId.has(tgt)) incidentByBpmnId.set(tgt, []);
			incidentByBpmnId.get(src).push({ edge: ed, isSource: true });
			incidentByBpmnId.get(tgt).push({ edge: ed, isSource: false });
		}

		for (const [bpmnId, list] of incidentByBpmnId) {
			const shape = this._shapes.get(bpmnId);
			if (!shape) continue;
			// Skip pools and lanes -- they don't host flow connections.
			if (this._participantsByBpmnId.has(bpmnId)) continue;
			if (this._lanesByBpmnId.has(bpmnId))        continue;

			const cx = shape.boundsX + shape.boundsWidth  / 2;
			const cy = shape.boundsY + shape.boundsHeight / 2;

			// Cardinal-only shapes (gateways, events): the visible body
			// tapers to a point at each side midpoint, so off-centre slots
			// look detached. With <=4 incident edges we assign each to a
			// distinct cardinal side; with more edges we fall back to the
			// n-slot per-side layout used for tasks.
			const elementId   = this._findElementIdByBpmnId(bpmnId);
			const element     = elementId ? this._elements.get(elementId) : null;
			const elementType = element?.bpmnElementType ?? '';
			const isCardinal  = elementType.endsWith('Gateway')
				|| elementType.endsWith('Event')
				|| elementType === 'startEvent'
				|| elementType === 'endEvent';

			// Bucket each incident by chosen side, recording the anchor
			// (other endpoint coord projected along the chosen side) for
			// sorting. Reads waypoints fresh on each iteration so updates
			// from earlier shapes feed the next one's side picks.
			const sides = { top: [], bottom: [], left: [], right: [] };

			if (isCardinal) {
				// For each edge, bucket by the DIRECTION to its other
				// endpoint -- the cardinal facing the neighbour. A split
				// gateway whose source authored every arrow at the top
				// vertex still gets its branches assigned to top / right
				// / bottom / left based on where the targets actually
				// sit. Multiple edges going the same direction stack at
				// the same cardinal (gateways have 4 ports; "all arrows
				// going right exit from the right port" is the standard
				// authoring model). Closest-side bucketing was tried
				// first but preserved authored stacking even when the
				// source had clearly miscategorised the side.
				for (const inc of list) {
					const wps = this._parseWaypoints(inc.edge);
					if (wps.length < 2) continue;
					const otherEnd = inc.isSource ? wps[wps.length - 1] : wps[0];
					const dx = otherEnd.x - cx;
					const dy = otherEnd.y - cy;
					const side = (Math.abs(dx) >= Math.abs(dy))
						? (dx > 0 ? 'right'  : 'left')
						: (dy > 0 ? 'bottom' : 'top');
					sides[side].push({
						...inc,
						otherEnd,
						// Anchor unused since cardinal shapes always snap
						// to t=0.5; kept for shape consistency with the
						// non-cardinal branch.
						anchor: 0,
					});
				}
			} else {
				// Tasks (rectangles): bucket by CURRENT closest side, not
				// by direction to the other endpoint. Preserves the
				// author's choice of side -- a "loop back" edge that exits
				// the bottom stays on the bottom, even when its target
				// happens to sit to the left of the shape geometrically.
				const x1 = shape.boundsX,                 y1 = shape.boundsY;
				const x2 = x1 + shape.boundsWidth,        y2 = y1 + shape.boundsHeight;
				for (const inc of list) {
					const wps = this._parseWaypoints(inc.edge);
					if (wps.length < 2) continue;
					const idx = inc.isSource ? 0 : (wps.length - 1);
					const ep  = wps[idx];

					const dT = Math.abs(ep.y - y1);
					const dB = Math.abs(ep.y - y2);
					const dL = Math.abs(ep.x - x1);
					const dR = Math.abs(ep.x - x2);
					const minD = Math.min(dT, dB, dL, dR);
					let side = 'top';
					if (minD === dB) side = 'bottom';
					else if (minD === dL) side = 'left';
					else if (minD === dR) side = 'right';

					const otherEnd = inc.isSource ? wps[wps.length - 1] : wps[0];
					const anchor = (side === 'top' || side === 'bottom') ? otherEnd.x : otherEnd.y;
					sides[side].push({ ...inc, otherEnd, anchor });
				}
			}

			for (const [side, items] of Object.entries(sides)) {
				if (items.length === 0) continue;
				// Simple n-slot spread: every item on a side gets its own
				// distinct slot, sorted by the other-endpoint's coordinate
				// along the side axis to minimise crossings. Cardinal
				// shapes (diamonds / circles) ignore the slot index and
				// always anchor at the side centre (cardinal vertex).
				items.sort((a, b) => a.anchor - b.anchor);
				const n = items.length;
				for (let i = 0; i < n; i++) {
					const item = items[i];
					const t = isCardinal ? 0.5 : (i + 0.5) / n;
					const newPt = this._pointAtSlot(shape, side, t);
					newPt.x = snap(newPt.x);
					newPt.y = snap(newPt.y);

					const wps = this._parseWaypoints(item.edge);
					if (wps.length < 2) continue;
					const idx = item.isSource ? 0 : (wps.length - 1);
					const old = wps[idx];

					// Propagate orthogonal alignment to the adjacent bend so
					// the segment touching the shape stays orthogonal whenever
					// it was before. Mutate the bend object in place rather
					// than replacing it, so the two axis tests compose cleanly.
					if (wps.length > 2) {
						const adjIdx = item.isSource ? 1 : (wps.length - 2);
						const adj = wps[adjIdx];
						if (Math.abs(adj.x - old.x) < 0.5) adj.x = newPt.x;
						if (Math.abs(adj.y - old.y) < 0.5) adj.y = newPt.y;
					}
					wps[idx] = newPt;
					item.edge.waypoints = JSON.stringify(wps);
				}
			}
		}
	}

	// ----- Placement mode -----

	armElementType(type) {
		if (!type) { this.cancelPlacement(); return; }
		this._pendingPlacement = String(type);
		this._updatePlacementCursor();
		this._installBoundaryPlacementHover();
		this._firePlacementChanged();
	}

	cancelPlacement() {
		if (this._pendingPlacement === null) return;
		this._pendingPlacement = null;
		this._updatePlacementCursor();
		this._uninstallBoundaryPlacementHover();
		this._clearBoundaryPlacementOverlay();
		this._firePlacementChanged();
	}

	/**
	 * While a `boundaryEvent` type is armed in the palette, track the
	 * pointer over the canvas and highlight the host shape currently
	 * under the cursor (plus its 4 cardinal slot dots). Reuses the
	 * endpoint-drop visual vocabulary (`.bpmn-endpoint-target` /
	 * `.bpmn-endpoint-slot[-active]`) so the editor speaks one
	 * "snap to a slot of this shape" language. No-op when the armed
	 * type isn't a boundary, or when not editable.
	 */
	_installBoundaryPlacementHover() {
		if (!this._editable || !this._svg) return;
		if (!this._isBoundaryElementType(this._pendingPlacement)) return;
		if (this._boundaryHoverBound) return;  // already installed
		this._boundaryHoverBound = (ev) => {
			if (!this._isBoundaryElementType(this._pendingPlacement)) {
				this._clearBoundaryPlacementOverlay();
				return;
			}
			const host = this._boundaryHostAtScreen(ev.clientX, ev.clientY);
			if (host) {
				const cursor = this._screenToWorld(ev.clientX, ev.clientY);
				this._updateEndpointDropTargets(host.elementId, cursor);
				// Crosshair = "click to drop here": cursor matches the
				// rest of placement mode when over a valid host.
				this._svg.style.cursor = 'crosshair';
			} else {
				this._clearBoundaryPlacementOverlay();
				// Not-allowed = "this is not a valid drop target": tells
				// the user empty-canvas / non-activity shapes are off
				// limits before they click.
				this._svg.style.cursor = 'not-allowed';
			}
		};
		this._svg.addEventListener('pointermove', this._boundaryHoverBound);
	}

	_uninstallBoundaryPlacementHover() {
		if (this._svg && this._boundaryHoverBound) {
			this._svg.removeEventListener('pointermove', this._boundaryHoverBound);
		}
		this._boundaryHoverBound = null;
	}

	/**
	 * Show a one-shot warning toast when the user clicks somewhere a
	 * boundary cannot be placed (empty canvas, gateway, event, data
	 * shape, lane, pool, ...). Uses Structr's `WarningMessage` global
	 * defined in init.js -- silently no-ops when unavailable so the
	 * editor still functions outside the host page.
	 */
	_warnBoundaryNeedsHost() {
		if (typeof WarningMessage === 'undefined') return;
		try {
			new WarningMessage()
				.text('Boundary events must be attached to a task or sub-process. Click on an activity shape.')
				.show();
		} catch (_) { /* swallow: never block placement on toast failure */ }
	}

	/**
	 * Tear down the host-highlight + slot-dot overlay used by both the
	 * boundary palette hover and the boundary drag. Mirrors the cleanup
	 * already done at the end of `_onEndpointDragUp`.
	 */
	_clearBoundaryPlacementOverlay() {
		if (!this._viewport) return;
		for (const g of this._viewport.querySelectorAll('.bpmn-shape')) {
			g.classList.remove('bpmn-endpoint-target');
		}
		for (const n of this._viewport.querySelectorAll('.bpmn-endpoint-slot, .bpmn-endpoint-slot-active')) {
			n.remove();
		}
	}

	onPlacementChanged(handler) { return this._subscribe(this._placementHandlers, handler); }

	_firePlacementChanged() {
		for (const h of this._placementHandlers) {
			try { h(this._pendingPlacement); } catch (e) { console.error(e); }
		}
	}

	_updatePlacementCursor() {
		if (!this._svg) return;
		this._svg.style.cursor = (this._pendingPlacement !== null) ? 'crosshair' : '';
	}

	/** Convert client (screen) coordinates to world coordinates inside the viewport. */
	_screenToWorld(clientX, clientY) {
		const rect = this._svg.getBoundingClientRect();
		return {
			x: (clientX - rect.left - this._panX) / this._zoom,
			y: (clientY - rect.top  - this._panY) / this._zoom,
		};
	}

	// ----- Connect handle (drag-to-create-flow) -----

	/**
	 * Find the BpmnElement id at (clientX, clientY), if any. Walks up from
	 * the DOM hit-test result to the nearest .bpmn-shape ancestor.
	 */
	_elementIdAtScreen(clientX, clientY) {
		const node = document.elementFromPoint(clientX, clientY);
		if (!node) return null;
		const g = node.closest ? node.closest('.bpmn-shape') : null;
		return g?.dataset?.elementId || null;
	}

	/**
	 * Render the connect handle ("+") next to the currently-selected shape.
	 * Called from _applySelection so the handle follows selection changes.
	 * Removes any previous handle. No-op when not editable or nothing is
	 * selected.
	 */
	_renderConnectHandle() {
		if (!this._viewport) return;
		const prev = this._viewport.querySelector('.bpmn-connect-handle');
		if (prev) prev.remove();
		if (!this._editable) return;
		if (!this._selectedId) return;

		const element = this._elements.get(this._selectedId);
		if (!element || !element.bpmnId) return;
		const shape = this._shapes.get(element.bpmnId);
		if (!shape) return;

		// Place handle just outside the shape's right edge, vertically centred.
		const cx = shape.boundsX + shape.boundsWidth + 14;
		const cy = shape.boundsY + shape.boundsHeight / 2;

		const g = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'g');
		g.setAttribute('class', 'bpmn-connect-handle');
		g.style.cursor = 'crosshair';

		const t = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'title');
		t.textContent = 'Drag to another element to create a sequence flow';
		g.appendChild(t);

		const c = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'circle');
		c.setAttribute('cx', cx);
		c.setAttribute('cy', cy);
		c.setAttribute('r', 9);
		c.setAttribute('fill', '#3498db');
		c.setAttribute('stroke', '#fff');
		c.setAttribute('stroke-width', '1.5');
		g.appendChild(c);

		// Plus glyph drawn as two stroked lines centred exactly on (cx, cy).
		// SVG <text>'s "+" character alignment varies by font; using
		// strokes lets us pin the cross-point precisely.
		const arm = 4;
		const plus = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'path');
		plus.setAttribute('d', `M ${cx - arm} ${cy} L ${cx + arm} ${cy} M ${cx} ${cy - arm} L ${cx} ${cy + arm}`);
		plus.setAttribute('stroke', '#fff');
		plus.setAttribute('stroke-width', '2');
		plus.setAttribute('stroke-linecap', 'round');
		plus.setAttribute('fill', 'none');
		plus.setAttribute('pointer-events', 'none');
		g.appendChild(plus);

		g.addEventListener('pointerdown', (e) => this._onConnectHandlePointerDown(e, this._selectedId));
		this._viewport.appendChild(g);
	}

	_onConnectHandlePointerDown(e, sourceId) {
		if (!this._editable) return;
		e.stopPropagation();
		const sourceEl = this._elements.get(sourceId);
		if (!sourceEl) return;
		const sShape = this._shapes.get(sourceEl.bpmnId);
		if (!sShape) return;

		// Anchor the preview line at the source shape's right edge.
		const startX = sShape.boundsX + sShape.boundsWidth;
		const startY = sShape.boundsY + sShape.boundsHeight / 2;
		const preview = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'line');
		preview.setAttribute('class', 'bpmn-flow-preview');
		preview.setAttribute('x1', startX);
		preview.setAttribute('y1', startY);
		preview.setAttribute('x2', startX);
		preview.setAttribute('y2', startY);
		preview.setAttribute('stroke', '#3498db');
		preview.setAttribute('stroke-width', '1.5');
		preview.setAttribute('stroke-dasharray', '4 3');
		preview.setAttribute('pointer-events', 'none');
		this._viewport.appendChild(preview);

		this._dragState = { kind: 'flow', sourceId, preview };
		this._svg.setPointerCapture(e.pointerId);
		this._svg.addEventListener('pointermove', this._flowMoveBound = (ev) => this._onFlowDragMove(ev));
		this._svg.addEventListener('pointerup',   this._flowUpBound   = (ev) => this._onFlowDragUp(ev));
	}

	_onFlowDragMove(e) {
		if (!this._dragState || this._dragState.kind !== 'flow') return;
		const w = this._screenToWorld(e.clientX, e.clientY);
		this._dragState.preview.setAttribute('x2', w.x);
		this._dragState.preview.setAttribute('y2', w.y);
	}

	_onFlowDragUp(e) {
		this._svg.removeEventListener('pointermove', this._flowMoveBound);
		this._svg.removeEventListener('pointerup',   this._flowUpBound);
		try { this._svg.releasePointerCapture(e.pointerId); } catch (_) {}
		const ds = this._dragState;
		this._dragState = null;
		if (!ds || ds.kind !== 'flow') return;

		ds.preview.remove();

		const targetId = this._elementIdAtScreen(e.clientX, e.clientY);
		if (targetId && targetId !== ds.sourceId) {
			try { this.createFlow({ sourceId: ds.sourceId, targetId }); }
			catch (err) { console.error('createFlow failed', err); }
		}
	}

	// ----- Bend-point editing -----
	//
	// When an edge is selected (`_selectedFlowId`), draw small handles at:
	//   * each interior waypoint -- filled square. Drag to move; double-
	//     click to remove.
	//   * each midpoint between consecutive waypoints -- smaller hollow
	//     square. Drag to insert a new waypoint at that mid-segment
	//     position and start dragging it immediately.
	// Endpoints (first / last waypoints) are NOT exposed: they're owned by
	// the source/target shapes and snap automatically when shapes move.

	_renderBendHandles() {
		if (!this._viewport) return;
		const prev = this._viewport.querySelectorAll('.bpmn-bend-handle, .bpmn-mid-handle, .bpmn-endpoint-handle');
		for (const n of prev) n.remove();
		if (!this._editable || !this._selectedFlowId) return;

		const lookup = this._lookupFlow(this._selectedFlowId);
		if (!lookup) return;
		const { flow } = lookup;
		const edge = this._edgeForFlow(flow);
		if (!edge) return;
		const wps = this._parseWaypoints(edge);
		if (wps.length < 2) return;

		const ns = _BpmnDiagramCommunitySvg.SVG_NS;

		// Endpoint handles: filled circles at first/last waypoints.
		// Drag to move the connection point along the same shape's
		// border, or onto a different shape to re-attach the flow.
		for (const idx of [0, wps.length - 1]) {
			const wp = wps[idx];
			const isSource = (idx === 0);
			const r = 6;
			const h = document.createElementNS(ns, 'circle');
			h.setAttribute('class', 'bpmn-endpoint-handle');
			h.setAttribute('cx', wp.x);
			h.setAttribute('cy', wp.y);
			h.setAttribute('r', r);
			h.setAttribute('fill',   '#3498db');
			h.setAttribute('stroke', '#fff');
			h.setAttribute('stroke-width', '1.5');
			h.style.cursor = 'move';
			h.dataset.endpoint = isSource ? 'source' : 'target';
			h.addEventListener('pointerdown', (e) => this._onEndpointHandlePointerDown(e, edge, flow, isSource));
			this._viewport.appendChild(h);
		}

		// Interior waypoint handles: solid squares, draggable.
		for (let i = 1; i < wps.length - 1; i++) {
			const wp = wps[i];
			const r  = 5;
			const h  = document.createElementNS(ns, 'rect');
			h.setAttribute('class', 'bpmn-bend-handle');
			h.setAttribute('x', wp.x - r);
			h.setAttribute('y', wp.y - r);
			h.setAttribute('width',  r * 2);
			h.setAttribute('height', r * 2);
			h.setAttribute('fill',   '#3498db');
			h.setAttribute('stroke', '#fff');
			h.setAttribute('stroke-width', '1');
			h.style.cursor = 'move';
			h.dataset.waypointIndex = String(i);
			h.addEventListener('pointerdown', (e) => this._onBendHandlePointerDown(e, i));
			h.addEventListener('dblclick',    (e) => { e.stopPropagation(); this._removeWaypoint(i); });
			this._viewport.appendChild(h);
		}

		// Mid-segment "ghost" handles: hollow smaller squares; click to add.
		for (let i = 0; i < wps.length - 1; i++) {
			const a = wps[i], b = wps[i + 1];
			const mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2;
			const r  = 4;
			const h  = document.createElementNS(ns, 'rect');
			h.setAttribute('class', 'bpmn-mid-handle');
			h.setAttribute('x', mx - r);
			h.setAttribute('y', my - r);
			h.setAttribute('width',  r * 2);
			h.setAttribute('height', r * 2);
			h.setAttribute('fill',   '#fff');
			h.setAttribute('stroke', '#3498db');
			h.setAttribute('stroke-width', '1');
			h.setAttribute('opacity', '0.85');
			h.style.cursor = 'crosshair';
			h.dataset.segmentIndex = String(i);
			h.addEventListener('pointerdown', (e) => this._onMidHandlePointerDown(e, i, mx, my));
			this._viewport.appendChild(h);
		}
	}

	/**
	 * Begin dragging an edge endpoint. The user can drop it anywhere on
	 * the same shape (moves the connection point on the same shape) or
	 * on a different element shape (re-attaches the flow). Drop on
	 * empty space reverts.
	 *
	 * Works for both sequence flows and message flows; the flow's
	 * `sourceRefId` / `targetRefId` and `sourceElement` / `targetElement`
	 * are updated when re-attached.
	 */
	_onEndpointHandlePointerDown(e, edge, flow, isSource) {
		if (!this._editable) return;
		e.stopPropagation();

		const wps = this._parseWaypoints(edge);
		if (wps.length < 2) return;
		const fixedIdx = isSource ? wps.length - 1 : 0;
		const fixedPt  = wps[fixedIdx];

		// Live preview: dashed line from the FIXED endpoint to the cursor.
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const preview = document.createElementNS(ns, 'line');
		preview.setAttribute('class', 'bpmn-endpoint-preview');
		preview.setAttribute('x1', fixedPt.x);
		preview.setAttribute('y1', fixedPt.y);
		preview.setAttribute('x2', wps[isSource ? 0 : wps.length - 1].x);
		preview.setAttribute('y2', wps[isSource ? 0 : wps.length - 1].y);
		preview.setAttribute('stroke', '#3498db');
		preview.setAttribute('stroke-width', '1.5');
		preview.setAttribute('stroke-dasharray', '4 3');
		preview.setAttribute('pointer-events', 'none');
		this._viewport.appendChild(preview);

		this._dragState = {
			kind: 'endpoint',
			edge,
			flow,
			isSource,
			fixedPt,
			preview,
			before: {
				waypoints:     edge.waypoints,
				sourceRefId:   flow.sourceRefId,
				targetRefId:   flow.targetRefId,
				sourceElement: flow.sourceElement,
				targetElement: flow.targetElement,
			},
		};
		this._svg.setPointerCapture(e.pointerId);
		this._svg.addEventListener('pointermove', this._endpointMoveBound = (ev) => this._onEndpointDragMove(ev));
		this._svg.addEventListener('pointerup',   this._endpointUpBound   = (ev) => this._onEndpointDragUp(ev));
	}

	_onEndpointDragMove(e) {
		if (!this._dragState || this._dragState.kind !== 'endpoint') return;
		const w = this._screenToWorld(e.clientX, e.clientY);
		this._dragState.preview.setAttribute('x2', w.x);
		this._dragState.preview.setAttribute('y2', w.y);

		// Find element under cursor and update slot indicators. Slot
		// targets are explicit (the four cardinals); the user picks one
		// rather than relying on auto-positioning. Drop will snap to
		// whichever cardinal is closest to the cursor.
		const elementId = this._elementIdAtScreen(e.clientX, e.clientY);
		this._updateEndpointDropTargets(elementId, w);
	}

	/**
	 * Show the discrete connection slots on the hovered shape so the
	 * user knows exactly where the endpoint will land. Highlights the
	 * one closest to the cursor as the active drop target.
	 */
	_updateEndpointDropTargets(elementId, cursor) {
		// Tear down previous indicators.
		for (const n of (this._viewport?.querySelectorAll('.bpmn-endpoint-slot, .bpmn-endpoint-slot-active') || [])) {
			n.remove();
		}
		for (const g of (this._viewport?.querySelectorAll('.bpmn-shape') || [])) {
			g.classList.remove('bpmn-endpoint-target');
		}
		if (!elementId) return;
		const element = this._elements.get(elementId);
		if (!element || !element.bpmnId) return;
		const shape = this._shapes.get(element.bpmnId);
		if (!shape) return;

		// Highlight the shape body itself.
		for (const g of this._viewport.querySelectorAll('.bpmn-shape')) {
			if (g.dataset.elementId === elementId) g.classList.add('bpmn-endpoint-target');
		}

		// Render the four cardinal slots.
		const cardinals = this._cardinalsOf(shape);
		let closest = null, closestDist = Infinity;
		for (const c of cardinals) {
			const d = Math.hypot(c.x - cursor.x, c.y - cursor.y);
			if (d < closestDist) { closestDist = d; closest = c; }
		}
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		for (const c of cardinals) {
			const isActive = (c === closest);
			const dot = document.createElementNS(ns, 'circle');
			dot.setAttribute('class', isActive ? 'bpmn-endpoint-slot-active' : 'bpmn-endpoint-slot');
			dot.setAttribute('cx', c.x);
			dot.setAttribute('cy', c.y);
			dot.setAttribute('r', isActive ? 6 : 4);
			dot.setAttribute('fill',   isActive ? '#3498db' : '#fff');
			dot.setAttribute('stroke', '#3498db');
			dot.setAttribute('stroke-width', isActive ? '2' : '1');
			dot.setAttribute('pointer-events', 'none');
			this._viewport.appendChild(dot);
		}
	}

	/** Discrete connection slots for a shape: the four cardinal vertices
	 *  (top / right / bottom / left side midpoints). Used for explicit
	 *  endpoint placement during manual edge re-attachment. */
	_cardinalsOf(shape) {
		const cx = shape.boundsX + shape.boundsWidth  / 2;
		const cy = shape.boundsY + shape.boundsHeight / 2;
		return [
			{ x: cx,                                     y: shape.boundsY                       },  // top
			{ x: shape.boundsX + shape.boundsWidth,      y: cy                                  },  // right
			{ x: cx,                                     y: shape.boundsY + shape.boundsHeight  },  // bottom
			{ x: shape.boundsX,                          y: cy                                  },  // left
		];
	}

	_onEndpointDragUp(e) {
		this._svg.removeEventListener('pointermove', this._endpointMoveBound);
		this._svg.removeEventListener('pointerup',   this._endpointUpBound);
		try { this._svg.releasePointerCapture(e.pointerId); } catch (_) {}
		const ds = this._dragState;
		this._dragState = null;
		// Clear any drop-target highlight + slot dots from drag.
		for (const g of (this._viewport?.querySelectorAll('.bpmn-shape') || [])) {
			g.classList.remove('bpmn-endpoint-target');
		}
		for (const n of (this._viewport?.querySelectorAll('.bpmn-endpoint-slot, .bpmn-endpoint-slot-active') || [])) {
			n.remove();
		}
		if (!ds || ds.kind !== 'endpoint') return;

		ds.preview.remove();

		// Identify the shape under the drop point. Only element shapes
		// count -- pools / lanes are layout containers, not connection
		// targets. `_elementIdAtScreen` already filters to .bpmn-shape
		// groups (which are only set for element shapes).
		const targetElementId = this._elementIdAtScreen(e.clientX, e.clientY);
		if (!targetElementId) {
			// Drop on empty space -> revert. No model state changed yet.
			this.refresh();
			return;
		}
		const targetElement = this._elements.get(targetElementId);
		if (!targetElement || !targetElement.bpmnId) { this.refresh(); return; }
		const targetShape = this._shapes.get(targetElement.bpmnId);
		if (!targetShape) { this.refresh(); return; }

		// Snap to whichever cardinal slot is closest to the drop point.
		// Explicit slot picking gives the user precise manual control --
		// no auto-positioning, no surprise side choices.
		const cursor    = this._screenToWorld(e.clientX, e.clientY);
		const cardinals = this._cardinalsOf(targetShape);
		let newEndpoint = cardinals[0];
		let closestDist = Infinity;
		for (const c of cardinals) {
			const d = Math.hypot(c.x - cursor.x, c.y - cursor.y);
			if (d < closestDist) { closestDist = d; newEndpoint = c; }
		}

		const wps = this._parseWaypoints(ds.edge);
		const dragIdx = ds.isSource ? 0 : (wps.length - 1);
		wps[dragIdx] = newEndpoint;
		ds.edge.waypoints = JSON.stringify(wps);

		// Re-attach the flow if the drop landed on a DIFFERENT shape.
		const newBpmnId   = targetElement.bpmnId;
		const oldRefId    = ds.isSource ? ds.before.sourceRefId : ds.before.targetRefId;
		const reattached  = (newBpmnId !== oldRefId);
		if (reattached) {
			if (ds.isSource) {
				ds.flow.sourceRefId   = newBpmnId;
				ds.flow.sourceElement = { id: targetElementId };
			} else {
				ds.flow.targetRefId   = newBpmnId;
				ds.flow.targetElement = { id: targetElementId };
			}
		}

		// Buffer everything: edge waypoints + (optionally) flow re-attach.
		if (ds.edge.id) this._bufferUpdate(ds.edge.id, { waypoints: ds.edge.waypoints });
		if (reattached) {
			const flowProps = ds.isSource
				? { sourceRefId: newBpmnId, sourceElement: targetElementId }
				: { targetRefId: newBpmnId, targetElement: targetElementId };
			this._bufferUpdate(ds.flow.id, flowProps);
		}

		const after = {
			waypoints:     ds.edge.waypoints,
			sourceRefId:   ds.flow.sourceRefId,
			targetRefId:   ds.flow.targetRefId,
			sourceElement: ds.flow.sourceElement,
			targetElement: ds.flow.targetElement,
		};
		const writeFlowBuffer = (s) => {
			if (!reattached) return;
			const props = ds.isSource
				? { sourceRefId: s.sourceRefId, sourceElement: s.sourceElement?.id ?? s.sourceElement }
				: { targetRefId: s.targetRefId, targetElement: s.targetElement?.id ?? s.targetElement };
			this._bufferUpdate(ds.flow.id, props);
		};
		this._pushUndo({
			apply: () => {
				ds.edge.waypoints     = after.waypoints;
				ds.flow.sourceRefId   = after.sourceRefId;
				ds.flow.targetRefId   = after.targetRefId;
				ds.flow.sourceElement = after.sourceElement;
				ds.flow.targetElement = after.targetElement;
				if (ds.edge.id) this._bufferUpdate(ds.edge.id, { waypoints: after.waypoints });
				writeFlowBuffer(after);
				this.refresh();
			},
			revert: () => {
				ds.edge.waypoints     = ds.before.waypoints;
				ds.flow.sourceRefId   = ds.before.sourceRefId;
				ds.flow.targetRefId   = ds.before.targetRefId;
				ds.flow.sourceElement = ds.before.sourceElement;
				ds.flow.targetElement = ds.before.targetElement;
				if (ds.edge.id) this._bufferUpdate(ds.edge.id, { waypoints: ds.before.waypoints });
				writeFlowBuffer(ds.before);
				this.refresh();
			},
		});
		this.refresh();
	}

	/** Begin dragging an existing interior waypoint. */
	_onBendHandlePointerDown(e, index) {
		if (!this._editable || !this._selectedFlowId) return;
		e.stopPropagation();
		const lookup = this._lookupFlow(this._selectedFlowId);
		const edge = lookup ? this._edgeForFlow(lookup.flow) : null;
		if (!edge) return;
		const wps = this._parseWaypoints(edge);
		if (index < 1 || index >= wps.length - 1) return;
		const before = JSON.stringify(wps);
		this._dragState = {
			kind:  'bend',
			edge,
			index,
			waypoints: wps,
			before,
		};
		this._svg.setPointerCapture(e.pointerId);
		this._svg.addEventListener('pointermove', this._bendMoveBound = (ev) => this._onBendDragMove(ev));
		this._svg.addEventListener('pointerup',   this._bendUpBound   = (ev) => this._onBendDragUp(ev));
	}

	/**
	 * Insert a new waypoint at the midpoint of a segment and immediately
	 * begin dragging it. Mirrors the way most BPMN editors let you "pull"
	 * a new bend out of an edge.
	 */
	_onMidHandlePointerDown(e, segIndex, mx, my) {
		if (!this._editable || !this._selectedFlowId) return;
		e.stopPropagation();
		const lookup = this._lookupFlow(this._selectedFlowId);
		const edge = lookup ? this._edgeForFlow(lookup.flow) : null;
		if (!edge) return;
		const wps = this._parseWaypoints(edge);
		const before = JSON.stringify(wps);
		// Insert the new waypoint right after segIndex.
		wps.splice(segIndex + 1, 0, { x: mx, y: my });
		this._dragState = {
			kind:  'bend',
			edge,
			index: segIndex + 1,
			waypoints: wps,
			before,
		};
		this._svg.setPointerCapture(e.pointerId);
		this._svg.addEventListener('pointermove', this._bendMoveBound = (ev) => this._onBendDragMove(ev));
		this._svg.addEventListener('pointerup',   this._bendUpBound   = (ev) => this._onBendDragUp(ev));
	}

	_onBendDragMove(e) {
		if (!this._dragState || this._dragState.kind !== 'bend') return;
		const w = this._screenToWorld(e.clientX, e.clientY);
		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		const nx = Math.round(w.x / step) * step;
		const ny = Math.round(w.y / step) * step;
		this._dragState.waypoints[this._dragState.index] = { x: nx, y: ny };
		this._dragState.edge.waypoints = JSON.stringify(this._dragState.waypoints);
		this.refresh();
	}

	_onBendDragUp(e) {
		this._svg.removeEventListener('pointermove', this._bendMoveBound);
		this._svg.removeEventListener('pointerup',   this._bendUpBound);
		try { this._svg.releasePointerCapture(e.pointerId); } catch (_) {}
		const ds = this._dragState;
		this._dragState = null;
		if (!ds || ds.kind !== 'bend') return;

		const edge   = ds.edge;
		const before = ds.before;
		const after  = edge.waypoints;
		if (before === after) return;
		if (edge.id) this._bufferUpdate(edge.id, { waypoints: after });
		this._pushUndo({
			apply:  () => { edge.waypoints = after;  if (edge.id) this._bufferUpdate(edge.id, { waypoints: after  }); this.refresh(); },
			revert: () => { edge.waypoints = before; if (edge.id) this._bufferUpdate(edge.id, { waypoints: before }); this.refresh(); },
		});
	}

	/** Remove an interior waypoint by index (1..length-2). Recorded for undo. */
	_removeWaypoint(index) {
		if (!this._editable || !this._selectedFlowId) return;
		const lookup = this._lookupFlow(this._selectedFlowId);
		const edge = lookup ? this._edgeForFlow(lookup.flow) : null;
		if (!edge) return;
		const wps = this._parseWaypoints(edge);
		if (index < 1 || index >= wps.length - 1) return;
		const before = edge.waypoints;
		wps.splice(index, 1);
		const after  = JSON.stringify(wps);
		edge.waypoints = after;
		if (edge.id) this._bufferUpdate(edge.id, { waypoints: after });
		this._pushUndo({
			apply:  () => { edge.waypoints = after;  if (edge.id) this._bufferUpdate(edge.id, { waypoints: after  }); this.refresh(); },
			revert: () => { edge.waypoints = before; if (edge.id) this._bufferUpdate(edge.id, { waypoints: before }); this.refresh(); },
		});
		this.refresh();
	}

	// ----- Resize handles -----
	//
	// When a non-pool element shape is selected, draw 8 resize handles:
	// 4 corners (nw / ne / se / sw) for arbitrary resize, 4 edges (n / s /
	// e / w) for single-axis resize. Handles snap to the grid step. We do
	// NOT enforce a minimum aspect ratio for events / gateways here -- the
	// load-time gateway-min normalisation applies on next open.

	_renderResizeHandles() {
		if (!this._viewport) return;
		const prev = this._viewport.querySelectorAll('.bpmn-resize-handle');
		for (const n of prev) n.remove();
		if (!this._editable || !this._selectedId) return;

		const element = this._elements.get(this._selectedId);
		if (!element || !element.bpmnId) return;
		const shape = this._shapes.get(element.bpmnId);
		if (!shape) return;
		// Don't show resize handles on participant pools (they're collaboration
		// containers, edited via dedicated tools later).
		if (this._participantsByBpmnId.has(shape.bpmnElementRef)) return;

		const x  = shape.boundsX, y = shape.boundsY;
		const w  = shape.boundsWidth, h = shape.boundsHeight;
		const handles = [
			{ side: 'nw', x: x,         y: y,         cursor: 'nwse-resize' },
			{ side: 'n',  x: x + w / 2, y: y,         cursor: 'ns-resize'   },
			{ side: 'ne', x: x + w,     y: y,         cursor: 'nesw-resize' },
			{ side: 'e',  x: x + w,     y: y + h / 2, cursor: 'ew-resize'   },
			{ side: 'se', x: x + w,     y: y + h,     cursor: 'nwse-resize' },
			{ side: 's',  x: x + w / 2, y: y + h,     cursor: 'ns-resize'   },
			{ side: 'sw', x: x,         y: y + h,     cursor: 'nesw-resize' },
			{ side: 'w',  x: x,         y: y + h / 2, cursor: 'ew-resize'   },
		];
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const r  = 4;
		for (const hh of handles) {
			const node = document.createElementNS(ns, 'rect');
			node.setAttribute('class', 'bpmn-resize-handle');
			node.setAttribute('x', hh.x - r);
			node.setAttribute('y', hh.y - r);
			node.setAttribute('width',  r * 2);
			node.setAttribute('height', r * 2);
			node.setAttribute('fill',   '#fff');
			node.setAttribute('stroke', '#3498db');
			node.setAttribute('stroke-width', '1.5');
			node.style.cursor = hh.cursor;
			node.dataset.side = hh.side;
			node.addEventListener('pointerdown', (e) => this._onResizeHandlePointerDown(e, shape, hh.side));
			this._viewport.appendChild(node);
		}
	}

	_onResizeHandlePointerDown(e, shape, side) {
		if (!this._editable) return;
		e.stopPropagation();

		// Snapshot incident-edge waypoints so a single undo restores both
		// the shape bounds and the edge endpoints we'll snap to the new
		// border. Includes both sequence flows and message flows.
		const edgesSnap = this._snapshotIncidentEdges(shape);
		// Same lockstep treatment as element drag: any boundary events
		// attached via `attachedToRef` need their centres re-projected
		// from the old perimeter to the new one (preserving side and
		// parametric offset), and their own incident edges translated.
		const attachedSnap = this._attachedShapesFor(shape.bpmnElementRef).map(a => ({
			element:   a.element,
			shape:     a.shape,
			boundsX0:  a.shape.boundsX,
			boundsY0:  a.shape.boundsY,
			edgesSnap: this._snapshotIncidentEdges(a.shape),
		}));

		this._dragState = {
			kind:  'resize',
			shape,
			side,
			startX:  e.clientX,
			startY:  e.clientY,
			boundsX0:      shape.boundsX,
			boundsY0:      shape.boundsY,
			boundsWidth0:  shape.boundsWidth,
			boundsHeight0: shape.boundsHeight,
			edgesSnap,
			attachedSnap,
		};
		this._svg.setPointerCapture(e.pointerId);
		this._svg.addEventListener('pointermove', this._resizeMoveBound = (ev) => this._onResizeDragMove(ev));
		this._svg.addEventListener('pointerup',   this._resizeUpBound   = (ev) => this._onResizeDragUp(ev));
	}

	_onResizeDragMove(e) {
		if (!this._dragState || this._dragState.kind !== 'resize') return;
		const ds = this._dragState;
		const dx = (e.clientX - ds.startX) / this._zoom;
		const dy = (e.clientY - ds.startY) / this._zoom;
		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		const MIN  = 20;  // hard floor; events look sane at 20 too

		// Compute new top-left + dimensions per dragged side. Negative
		// width / height (drag past the opposite edge) is clamped to MIN.
		let x = ds.boundsX0,        y = ds.boundsY0;
		let w = ds.boundsWidth0,    h = ds.boundsHeight0;
		const right  = ds.boundsX0 + ds.boundsWidth0;
		const bottom = ds.boundsY0 + ds.boundsHeight0;
		if (ds.side.includes('e')) w = Math.max(MIN, ds.boundsWidth0  + dx);
		if (ds.side.includes('s')) h = Math.max(MIN, ds.boundsHeight0 + dy);
		if (ds.side.includes('w')) {
			const nx = Math.min(right - MIN, ds.boundsX0 + dx);
			x = nx;
			w = right - nx;
		}
		if (ds.side.includes('n')) {
			const ny = Math.min(bottom - MIN, ds.boundsY0 + dy);
			y = ny;
			h = bottom - ny;
		}
		// Snap bounds to grid step so handles align with the drag-snap grid.
		const xs = Math.round(x / step) * step;
		const ys = Math.round(y / step) * step;
		const ws = Math.max(MIN, Math.round(w / step) * step);
		const hs = Math.max(MIN, Math.round(h / step) * step);
		ds.shape.boundsX = xs; ds.shape.boundsY = ys;
		ds.shape.boundsWidth = ws; ds.shape.boundsHeight = hs;
		// Project incident edge endpoints from old bounds to new bounds,
		// preserving each endpoint's side and parametric offset along it.
		// Reads from the pointerdown snapshot so each tick is idempotent
		// regardless of how many fired before; otherwise repeated calls
		// would compound rounding and parallel edges would still collapse.
		const oldHostBounds = { x: ds.boundsX0, y: ds.boundsY0, w: ds.boundsWidth0, h: ds.boundsHeight0 };
		this._projectIncidentEdgeEndpoints(ds.shape, ds.edgesSnap, oldHostBounds);
		// Re-project attached boundary centres onto the new perimeter and
		// translate each boundary's incident edges by the resulting delta.
		// Idempotent: reads from the pointerdown snapshot and recomputes
		// from the same fixed old-bounds, so per-frame compounding is
		// avoided (matches the host-edge projection pattern above).
		this._projectAttachedBoundaryCentres(ds.shape, oldHostBounds, ds.attachedSnap || []);
		for (const att of (ds.attachedSnap || [])) {
			const dxAtt = att.shape.boundsX - att.boundsX0;
			const dyAtt = att.shape.boundsY - att.boundsY0;
			this._translateIncidentEdgeEndpoints(att.shape, att.edgesSnap, dxAtt, dyAtt);
		}
		this.refresh();
	}

	_onResizeDragUp(e) {
		this._svg.removeEventListener('pointermove', this._resizeMoveBound);
		this._svg.removeEventListener('pointerup',   this._resizeUpBound);
		try { this._svg.releasePointerCapture(e.pointerId); } catch (_) {}
		const ds = this._dragState;
		this._dragState = null;
		if (!ds || ds.kind !== 'resize') return;

		const sh   = ds.shape;
		const before = { boundsX: ds.boundsX0, boundsY: ds.boundsY0, boundsWidth: ds.boundsWidth0, boundsHeight: ds.boundsHeight0 };
		const after  = { boundsX: sh.boundsX,  boundsY: sh.boundsY,  boundsWidth: sh.boundsWidth,  boundsHeight: sh.boundsHeight  };
		const noChange = before.boundsX === after.boundsX && before.boundsY === after.boundsY
			&& before.boundsWidth === after.boundsWidth && before.boundsHeight === after.boundsHeight;
		if (noChange) return;
		const edgeChanges = ds.edgesSnap.map(s => ({ edge: s.edge, before: s.waypointsBefore, after: s.edge.waypoints }));
		// Attached-boundary moves: per-boundary before/after for shape
		// bounds + incident-edge waypoints, all rolled into the same
		// resize undo entry so one ctrl-Z restores host + boundaries.
		const attachedChanges = (ds.attachedSnap || []).map(att => ({
			shape:       att.shape,
			before:      { boundsX: att.boundsX0, boundsY: att.boundsY0 },
			after:       { boundsX: att.shape.boundsX, boundsY: att.shape.boundsY },
			edgeChanges: att.edgesSnap.map(s => ({ edge: s.edge, before: s.waypointsBefore, after: s.edge.waypoints })),
		}));

		if (sh.id) this._bufferUpdate(sh.id, after);
		for (const ec of edgeChanges) {
			if (ec.edge.id && ec.before !== ec.after) {
				this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
			}
		}
		for (const ac of attachedChanges) {
			if (ac.shape.id) this._bufferUpdate(ac.shape.id, ac.after);
			for (const ec of ac.edgeChanges) {
				if (ec.edge.id && ec.before !== ec.after) {
					this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
				}
			}
		}
		this._pushUndo({
			apply: () => {
				Object.assign(sh, after);
				for (const ec of edgeChanges) ec.edge.waypoints = ec.after;
				if (sh.id) this._bufferUpdate(sh.id, after);
				for (const ec of edgeChanges) if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
				for (const ac of attachedChanges) {
					Object.assign(ac.shape, ac.after);
					if (ac.shape.id) this._bufferUpdate(ac.shape.id, ac.after);
					for (const ec of ac.edgeChanges) {
						ec.edge.waypoints = ec.after;
						if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
					}
				}
				this.refresh();
			},
			revert: () => {
				Object.assign(sh, before);
				for (const ec of edgeChanges) ec.edge.waypoints = ec.before;
				if (sh.id) this._bufferUpdate(sh.id, before);
				for (const ec of edgeChanges) if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.before });
				for (const ac of attachedChanges) {
					Object.assign(ac.shape, ac.before);
					if (ac.shape.id) this._bufferUpdate(ac.shape.id, ac.before);
					for (const ec of ac.edgeChanges) {
						ec.edge.waypoints = ec.before;
						if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.before });
					}
				}
				this.refresh();
			},
		});
	}

	// ----- Loaded-buffer application -----

	_applyLoadedBuffer() {
		// Apply buffered creates first (instantiate synthetic in-memory entries).
		const touchedShapeIds = new Set();
		for (const [id, entry] of this._buffer.creates) {
			const props = entry.props || {};
			const synth = { id, type: entry.type, ...props };
			switch (entry.type) {
				case 'BpmnElement':       this._elements.set(id, synth); break;
				case 'BpmnSequenceFlow':  this._flows.set(id, synth); break;
				case 'BpmnDiShape':       if (synth.bpmnElementRef) { this._shapes.set(synth.bpmnElementRef, synth); touchedShapeIds.add(id); } break;
				case 'BpmnDiEdge':        if (synth.bpmnElementRef) this._edges.set(synth.bpmnElementRef, synth); break;
			}
		}
		// Apply buffered updates by id. Track which shape ids we touched so
		// we know which edges need re-snapping; shapes that weren't in the
		// buffer keep their authored waypoints (avoids collapsing parallel
		// message flows whose endpoints share the same shape).
		for (const [id, props] of this._buffer.updates) {
			let target = null, isShape = false;
			for (const [, sh] of this._shapes)   if (sh.id === id) { target = sh; isShape = true; break; }
			if (!target) for (const [, ed] of this._edges)    if (ed.id === id) { target = ed; break; }
			if (!target) for (const [, el] of this._elements) if (el.id === id) { target = el; break; }
			if (!target) for (const [, fl] of this._flows)    if (fl.id === id) { target = fl; break; }
			if (target) {
				Object.assign(target, props);
				// Only flag the shape for edge-resnap when its bounds actually
				// changed -- non-bounds property updates (labels, lane refs)
				// don't move the endpoints.
				if (isShape && (
					'boundsX' in props || 'boundsY' in props
					|| 'boundsWidth' in props || 'boundsHeight' in props
				)) {
					touchedShapeIds.add(id);
				}
			}
		}
		// Apply buffered deletes.
		for (const id of this._buffer.deletes) {
			for (const [k, el] of this._elements) if (el.id === id) { this._elements.delete(k); break; }
			for (const [k, fl] of this._flows)    if (fl.id === id) { this._flows.delete(k);    break; }
			for (const [k, sh] of this._shapes)   if (sh.id === id) { this._shapes.delete(k);   break; }
			for (const [k, ed] of this._edges)    if (ed.id === id) { this._edges.delete(k);    break; }
		}
		// Re-snap incident edges only for shapes whose bounds the buffer
		// changed. Empty buffer => no-op, so a fresh import preserves the
		// source's authored waypoints exactly.
		if (touchedShapeIds.size > 0) {
			for (const [, sh] of this._shapes) {
				if (sh.id && touchedShapeIds.has(sh.id)) this._refreshIncidentEdges(sh);
			}
		}
	}
	// Client-side BPMN serialisation was removed: the host UI now fetches
	// canonical XML from the server-side `BpmnExporter` via
	// `POST /BpmnDefinitions/{id}/exportBpmn` on every save. See
	// `getBpmnXml` removal in the editor commit history for the previous
	// lossy implementation. The API surface no longer exposes a client
	// serialiser; callers wanting the XML go through the REST method.



	// ===== Highlights =====

	highlight(elementIds, style) {
		if (!style) return;
		this._highlightGroups.set(style, new Set(elementIds || []));
		this._applyHighlights();
	}

	clearHighlights() {
		this._highlightGroups.clear();
		this._applyHighlights();
	}

	// ===== Internals: rendering =====

	_renderShape(shape) {
		const elementId  = this._findElementIdByBpmnId(shape.bpmnElementRef);
		const element    = elementId ? this._elements.get(elementId) : null;
		const elementType = element?.bpmnElementType ?? 'unknown';
		const cx = shape.boundsX + shape.boundsWidth  / 2;
		const cy = shape.boundsY + shape.boundsHeight / 2;

		// Events render with labels OUTSIDE the shape because the circles are
		// too small to host a readable label inside. Tasks/gateways render
		// labels inside. Data shapes (data store / data object) render with
		// labels OUTSIDE too -- their bodies have non-rectangular geometry
		// that crowds inline text.
		const isEvent   = elementType.endsWith('Event') || elementType === 'startEvent' || elementType === 'endEvent';
		const isGateway = elementType.endsWith('Gateway');
		const isDataStore  = elementType === 'dataStoreReference';
		const isDataObject = elementType === 'dataObject' || elementType === 'dataObjectReference';
		const isData       = isDataStore || isDataObject;

		const g = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'g');
		g.setAttribute('class', `bpmn-shape bpmn-shape-${elementType}`);
		if (elementId) g.dataset.elementId = elementId;
		if (shape.id)  g.dataset.shapeId   = shape.id;

		// SVG <title> as the *first* child of <g> renders as the native
		// browser tooltip on hover. Attaching later doesn't always work in
		// Chromium / WebKit, so we add it first.
		const titleText = element?.bpmnName || element?.bpmnId || shape.bpmnElementRef || '';
		if (titleText) {
			const title = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'title');
			title.textContent = titleText;
			g.appendChild(title);
		}

		// Shape body by element type.
		let body;
		if (isEvent) {
			body = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'circle');
			body.setAttribute('cx', cx);
			body.setAttribute('cy', cy);
			body.setAttribute('r',  Math.min(shape.boundsWidth, shape.boundsHeight) / 2);
		} else if (isGateway) {
			const w = shape.boundsWidth / 2, h = shape.boundsHeight / 2;
			body = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'polygon');
			body.setAttribute('points', `${cx},${cy - h} ${cx + w},${cy} ${cx},${cy + h} ${cx - w},${cy}`);
		} else if (isDataStore) {
			// Cylinder: vertical sides, full ellipse at top, half ellipse at bottom.
			body = this._buildDataStorePath(shape);
		} else if (isDataObject) {
			// Page with a folded top-right corner. Body is the page outline;
			// the fold triangle is drawn as a separate path appended below.
			body = this._buildDataObjectPath(shape);
		} else {
			// Tasks (userTask, serviceTask, scriptTask, manualTask, businessRuleTask, ...)
			body = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'rect');
			body.setAttribute('x',  shape.boundsX);
			body.setAttribute('y',  shape.boundsY);
			body.setAttribute('width',  shape.boundsWidth);
			body.setAttribute('height', shape.boundsHeight);
			body.setAttribute('rx', 8);
			body.setAttribute('ry', 8);
		}
		body.setAttribute('class', 'bpmn-shape-body');
		body.setAttribute('fill',   '#fff');
		body.setAttribute('stroke', '#333');
		body.setAttribute('stroke-width', elementType.startsWith('boundary') ? '1.5' : '2');
		if (elementType.startsWith('boundary')) body.setAttribute('stroke-dasharray', '4 3');
		// Subtle drop shadow via SVG filter (defined in <defs>).
		body.setAttribute('filter', 'url(#bpmn-shadow)');
		g.appendChild(body);

		// Detail glyphs that sit on top of the body but inherit the shape's
		// click target (pointer-events: none).
		if (isGateway) {
			// Gateway type marker: small glyph centred in the diamond. Sizes are
			// fractions of the shape's smaller side so markers scale with the box.
			// Spec markers are: exclusive=X, parallel=+, inclusive=O, complex=*,
			// eventBased=pentagon-with-circle.
			this._renderGatewayMarker(g, shape, elementType, cx, cy);
		}
		if (isEvent && element?.eventDefinitionType) {
			// Event-definition overlay: clock for timer, envelope for message,
			// lightning for error, triangle for signal, etc. Drawn at ~60% of
			// the inner radius so there's white space around the glyph.
			this._renderEventDefinitionMarker(g, shape, element.eventDefinitionType, elementType, cx, cy);
		}
		if (isDataStore) {
			// Top "lid" arc -- the front half of the top ellipse, drawn after
			// the body so it sits cleanly over the side outline.
			this._appendDataStoreLid(g, shape);
		}
		if (isDataObject) {
			// Fold triangle in the top-right corner of the page glyph.
			this._appendDataObjectFold(g, shape);
		}

		// Label: BPMN name if present, falls back to bpmnId. Tasks and gateways
		// render their label *inside* the shape; events render *below*.
		// Boundary events have machine-style bpmnIds like "Boundary_Review_Remind";
		// when no human bpmnName is set, humanise the fallback so wrapping
		// produces readable lines ("Review Remind" rather than the ID).
		// Auto-generated UUID-shaped bpmnIds (e.g. "id-add7903c-fc54-..."),
		// emitted by tools that don't author readable identifiers, are
		// suppressed when no human name is set: the shape renders without
		// a label rather than dragging a 30+ char hex blob across the canvas.
		const AUTO_BPMN_ID_PATTERN = /^id-[0-9a-fA-F-]{16,}$/;
		const isBoundary = elementType === 'boundaryEvent';
		const hasName    = !!element?.bpmnName;
		const fallbackId = element?.bpmnId || shape.bpmnElementRef || '';
		const fallbackUsable = fallbackId && !AUTO_BPMN_ID_PATTERN.test(fallbackId);
		const rawLabel   = hasName ? element.bpmnName : (fallbackUsable ? fallbackId : '');
		const labelText  = (isBoundary && !hasName)
			? rawLabel.replace(/^Boundary_/, '').replace(/_/g, ' ')
			: rawLabel;
		// Events label below their circle; data shapes do too because their
		// non-rectangular interiors crowd inline text.
		const labelOutside = isEvent || isData;
		if (labelText) {
			// Wrap text into multiple lines so long labels fit inside small
			// shapes / under tight events. Char limit is approximate -- SVG
			// text doesn't expose easy measurement at render time without
			// going through getBBox after-the-fact.
			//   * Boundary events: tight clusters, aggressive wrap.
			//   * Gateways: diamond bodies, aggressive wrap.
			//   * Other events (start/end/intermediate): label sits below.
			//   * Tasks: scale by box width.
			const maxChars = (isBoundary || isGateway)
				? 7
				: (labelOutside ? 18 : Math.max(10, Math.floor(shape.boundsWidth / 6)));
			const fontSize   = 10;
			const lines      = this._wrapLabel(labelText, maxChars);
			const lineHeight = fontSize + 1; // small leading

			const txt = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'text');
			txt.setAttribute('text-anchor', 'middle');
			txt.setAttribute('class', 'bpmn-shape-label');
			txt.setAttribute('font-size', `${fontSize}`);
			txt.setAttribute('font-family', 'sans-serif');
			txt.setAttribute('pointer-events', 'none');
			// White paint-order halo so the label stays readable when it sits
			// over an edge in busy diagrams.
			txt.setAttribute('paint-order', 'stroke');
			txt.setAttribute('stroke', '#fafafa');
			txt.setAttribute('stroke-width', '3');
			txt.setAttribute('stroke-linejoin', 'round');

			let startY;
			if (labelOutside) {
				// Below the shape's bottom edge with a small gap.
				startY = shape.boundsY + shape.boundsHeight + 8;
				txt.setAttribute('dominant-baseline', 'hanging');
			} else {
				// Vertically centre the line block around (cx, cy).
				startY = cy - ((lines.length - 1) * lineHeight) / 2;
				txt.setAttribute('dominant-baseline', 'middle');
			}
			txt.setAttribute('x', cx);
			txt.setAttribute('y', startY);

			lines.forEach((line, i) => {
				const tspan = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'tspan');
				tspan.setAttribute('x', cx);
				tspan.setAttribute('dy', i === 0 ? '0' : `${lineHeight}`);
				tspan.textContent = line;
				txt.appendChild(tspan);
			});
			g.appendChild(txt);
		}

		// Method-count badge: shapes whose BpmnElement has attached
		// SchemaMethods get a small numbered marker at the top-right corner.
		// pointer-events are off so the badge doesn't intercept clicks meant
		// for the shape body.
		const methodCount = Array.isArray(element?.methods) ? element.methods.length : 0;
		if (methodCount > 0) {
			let bx, by;
			if (isEvent) {
				const r   = Math.min(shape.boundsWidth, shape.boundsHeight) / 2;
				const off = r * Math.SQRT1_2;
				bx = cx + off;
				by = cy - off;
			} else if (isGateway) {
				// Halfway along the diamond's upper-right edge.
				bx = cx + shape.boundsWidth  / 4;
				by = cy - shape.boundsHeight / 4;
			} else {
				bx = shape.boundsX + shape.boundsWidth;
				by = shape.boundsY;
			}
			const badge = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'g');
			badge.setAttribute('class', 'bpmn-method-badge');
			badge.setAttribute('pointer-events', 'none');
			const circ = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'circle');
			circ.setAttribute('cx', bx);
			circ.setAttribute('cy', by);
			circ.setAttribute('r',  8);
			circ.setAttribute('fill',   '#1abc9c');
			circ.setAttribute('stroke', '#fff');
			circ.setAttribute('stroke-width', '1.5');
			badge.appendChild(circ);
			const ct = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'text');
			ct.setAttribute('x', bx);
			ct.setAttribute('y', by);
			ct.setAttribute('text-anchor', 'middle');
			ct.setAttribute('dominant-baseline', 'central');
			ct.setAttribute('font-size', '10');
			ct.setAttribute('font-family', 'sans-serif');
			ct.setAttribute('font-weight', 'bold');
			ct.setAttribute('fill', '#fff');
			ct.textContent = String(methodCount);
			badge.appendChild(ct);
			g.appendChild(badge);
		}

		// Pointer interaction: in editable mode, pointerdown begins a drag and
		// pointerup decides "click vs drag" based on whether the pointer moved.
		// We don't use 'click' here because setPointerCapture during drag
		// redirects the synthesised click event away from the shape -- so we
		// detect "click" as "pointerup with no movement" inside the drag flow.
		if (this._editable) {
			g.addEventListener('pointerdown', (e) => this._onShapePointerDown(e, shape, elementId));
		} else {
			g.addEventListener('click', (e) => {
				e.stopPropagation();
				if (elementId) this.select(elementId);
			});
		}

		this._viewport.appendChild(g);
	}

	_renderEdge(edge) {
		const waypoints = this._parseWaypoints(edge);
		if (waypoints.length < 2) return;

		const flow   = this._findFlowByBpmnId(edge.bpmnElementRef);
		const flowId = flow ? this._findFlowIdByBpmnId(edge.bpmnElementRef) : null;

		// Edges render as a <g> with two stacked polylines: a wide invisible
		// "hit" overlay for clicking, plus the visible thin polyline. This is
		// needed because a 1.5-px stroke is uncomfortably hard to click.
		const g = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'g');
		g.setAttribute('class', 'bpmn-edge-group');
		if (flowId)             g.dataset.flowId  = flowId;
		if (edge.bpmnElementRef) g.dataset.flowRef = edge.bpmnElementRef;

		const points = waypoints.map(w => `${w.x},${w.y}`).join(' ');

		const hit = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'polyline');
		hit.setAttribute('points', points);
		hit.setAttribute('fill', 'none');
		hit.setAttribute('stroke', 'transparent');
		hit.setAttribute('stroke-width', '12');
		hit.setAttribute('class', 'bpmn-edge-hit');
		hit.style.cursor = this._editable ? 'pointer' : '';
		g.appendChild(hit);

		const path = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'polyline');
		path.setAttribute('points', points);
		path.setAttribute('fill', 'none');
		path.setAttribute('stroke', '#333');
		path.setAttribute('stroke-width', '1.5');
		path.setAttribute('marker-end', 'url(#bpmn-arrow)');
		path.setAttribute('class', 'bpmn-edge');
		path.setAttribute('pointer-events', 'none');
		if (edge.bpmnElementRef) path.dataset.flowRef = edge.bpmnElementRef;
		g.appendChild(path);

		if (this._editable && flowId) {
			g.addEventListener('click', (e) => {
				e.stopPropagation();
				this.selectFlow(flowId);
			});
		}

		// Flow name label (e.g. "Yes" / "No" on the decision gateway, or
		// "More than 2 days" on the auto-approve outflow). Rendered at the
		// polyline's geometric midpoint with a small perpendicular offset
		// so the text sits above the line rather than on top of it.
		if (flow && flow.bpmnName) this._appendEdgeLabel(g, waypoints, flow.bpmnName);

		this._viewport.appendChild(g);
	}

	/**
	 * Render an edge label (flow name) at the geometric midpoint of the
	 * waypoint polyline. The midpoint is the point at exactly half of the
	 * polyline's total length, picked by walking segments. The label is
	 * pushed a few pixels perpendicular to the local segment direction so
	 * it sits beside the line rather than under it; the side it lands on
	 * (above / below / left / right of the line) is chosen by the segment
	 * orientation, but always leaning "up" relative to the viewport to
	 * match BPMN authoring tools' conventions.
	 *
	 * White paint-order halo (3px stroke in #fafafa) so the label stays
	 * legible when other elements pass underneath -- same trick the shape
	 * labels use.
	 */
	_appendEdgeLabel(g, waypoints, label) {
		if (!label || !waypoints || waypoints.length < 2) return;

		// Total length + per-segment lengths.
		const segLens = [];
		let total = 0;
		for (let i = 0; i < waypoints.length - 1; i++) {
			const dx = waypoints[i + 1].x - waypoints[i].x;
			const dy = waypoints[i + 1].y - waypoints[i].y;
			const len = Math.hypot(dx, dy);
			segLens.push(len);
			total += len;
		}
		if (total <= 0) return;

		// Walk segments until we cross the half-length mark; interpolate
		// inside that segment for the exact midpoint.
		const target = total / 2;
		let acc = 0;
		let segIdx = 0;
		let mx = waypoints[0].x;
		let my = waypoints[0].y;
		for (segIdx = 0; segIdx < segLens.length; segIdx++) {
			if (acc + segLens[segIdx] >= target) {
				const segLen = segLens[segIdx];
				const t = segLen > 0 ? (target - acc) / segLen : 0;
				mx = waypoints[segIdx].x + t * (waypoints[segIdx + 1].x - waypoints[segIdx].x);
				my = waypoints[segIdx].y + t * (waypoints[segIdx + 1].y - waypoints[segIdx].y);
				break;
			}
			acc += segLens[segIdx];
		}

		// Perpendicular offset away from the line. For horizontal segments
		// the label lifts above (negative y); for vertical segments it sits
		// to the right of the line (positive x). The 8-px offset matches
		// the visual weight of the shape labels.
		const OFFSET = 8;
		const dx = waypoints[segIdx + 1].x - waypoints[segIdx].x;
		const dy = waypoints[segIdx + 1].y - waypoints[segIdx].y;
		const segLen = Math.hypot(dx, dy);
		let ox = 0, oy = -OFFSET;
		if (segLen > 0) {
			// Perpendicular vector (rotate (dx,dy) by -90deg): (-dy, dx).
			// In SVG coords (y-down), -dy is "up" for a right-going segment.
			ox = -dy / segLen * OFFSET;
			oy =  dx / segLen * OFFSET;
			// Bias upward: ensure oy <= 0 so the label leans up regardless
			// of the segment's direction-of-travel sign.
			if (oy > 0) { ox = -ox; oy = -oy; }
		}

		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const text = document.createElementNS(ns, 'text');
		text.setAttribute('x', mx + ox);
		text.setAttribute('y', my + oy);
		text.setAttribute('class', 'bpmn-edge-label');
		text.setAttribute('text-anchor', 'middle');
		text.setAttribute('dominant-baseline', 'middle');
		text.setAttribute('font-size', '10');
		text.setAttribute('font-family', 'sans-serif');
		text.setAttribute('fill', '#333');
		text.setAttribute('pointer-events', 'none');
		text.setAttribute('paint-order', 'stroke');
		text.setAttribute('stroke', '#fafafa');
		text.setAttribute('stroke-width', '3');
		text.setAttribute('stroke-linejoin', 'round');
		text.textContent = label;
		g.appendChild(text);
	}

	/**
	 * Reverse-lookup helper: BpmnDiEdge.bpmnElementRef -> flow node UUID.
	 * Only finds sequence flows (BpmnSequenceFlow); messageFlows have their
	 * own map (`_messageFlowsByBpmnId`).
	 */
	_findFlowIdByBpmnId(bpmnId) {
		for (const [id, fl] of this._flows) if (fl.bpmnId === bpmnId) return id;
		return null;
	}

	/**
	 * Draw the BPMN gateway marker glyph centred in the diamond.
	 * Marker is appended to the parent <g> so it inherits the shape's
	 * selection / highlight state. pointer-events are off so the glyph
	 * doesn't intercept clicks meant for the diamond body underneath.
	 */
	_renderGatewayMarker(parent, shape, elementType, cx, cy) {
		const r = Math.min(shape.boundsWidth, shape.boundsHeight) * 0.22;
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const wrap = document.createElementNS(ns, 'g');
		wrap.setAttribute('class', 'bpmn-gateway-marker');
		wrap.setAttribute('pointer-events', 'none');
		wrap.setAttribute('fill', 'none');
		wrap.setAttribute('stroke', '#333');
		wrap.setAttribute('stroke-width', '2');
		wrap.setAttribute('stroke-linecap', 'round');

		switch (elementType) {
			case 'exclusiveGateway': {
				// X glyph: two crossed diagonals.
				const d = `M ${cx - r} ${cy - r} L ${cx + r} ${cy + r} M ${cx + r} ${cy - r} L ${cx - r} ${cy + r}`;
				const p = document.createElementNS(ns, 'path');
				p.setAttribute('d', d);
				p.setAttribute('stroke-width', '3');
				wrap.appendChild(p);
				break;
			}
			case 'parallelGateway': {
				// + glyph: vertical and horizontal bars meeting at centre.
				const d = `M ${cx} ${cy - r} L ${cx} ${cy + r} M ${cx - r} ${cy} L ${cx + r} ${cy}`;
				const p = document.createElementNS(ns, 'path');
				p.setAttribute('d', d);
				p.setAttribute('stroke-width', '4');
				wrap.appendChild(p);
				break;
			}
			case 'inclusiveGateway': {
				// O glyph: hollow circle centred in the diamond.
				const c = document.createElementNS(ns, 'circle');
				c.setAttribute('cx', cx);
				c.setAttribute('cy', cy);
				c.setAttribute('r',  r);
				c.setAttribute('stroke-width', '2.5');
				wrap.appendChild(c);
				break;
			}
			case 'complexGateway': {
				// * glyph: 4 lines through centre at 0, 45, 90, 135 degrees.
				const a = r * Math.SQRT1_2;
				const d = [
					`M ${cx - r} ${cy} L ${cx + r} ${cy}`,
					`M ${cx} ${cy - r} L ${cx} ${cy + r}`,
					`M ${cx - a} ${cy - a} L ${cx + a} ${cy + a}`,
					`M ${cx + a} ${cy - a} L ${cx - a} ${cy + a}`,
				].join(' ');
				const p = document.createElementNS(ns, 'path');
				p.setAttribute('d', d);
				p.setAttribute('stroke-width', '2.5');
				wrap.appendChild(p);
				break;
			}
			case 'eventBasedGateway': {
				// Outer thin circle, inner thicker pentagon-style polygon. The
				// BPMN convention layers a pentagon (5-pointed star outline)
				// inside two concentric circles; we approximate with one outer
				// ring + a pentagon.
				const outer = document.createElementNS(ns, 'circle');
				outer.setAttribute('cx', cx);
				outer.setAttribute('cy', cy);
				outer.setAttribute('r',  r * 1.25);
				outer.setAttribute('stroke-width', '1');
				wrap.appendChild(outer);
				const inner = document.createElementNS(ns, 'circle');
				inner.setAttribute('cx', cx);
				inner.setAttribute('cy', cy);
				inner.setAttribute('r',  r);
				inner.setAttribute('stroke-width', '1');
				wrap.appendChild(inner);
				// Pentagon
				const pts = [];
				for (let i = 0; i < 5; i++) {
					const ang = -Math.PI / 2 + i * (2 * Math.PI / 5);
					pts.push(`${cx + r * 0.7 * Math.cos(ang)},${cy + r * 0.7 * Math.sin(ang)}`);
				}
				const pent = document.createElementNS(ns, 'polygon');
				pent.setAttribute('points', pts.join(' '));
				pent.setAttribute('stroke-width', '1.5');
				wrap.appendChild(pent);
				break;
			}
			default:
				return;  // unknown gateway type -> no marker
		}
		parent.appendChild(wrap);
	}

	/**
	 * Draw the BPMN event-definition glyph centred inside the event circle.
	 * Glyphs are sized at ~25% of the smaller side and stay inside the inner
	 * radius. Pointer-events are off so the glyph doesn't intercept clicks
	 * meant for the event body underneath.
	 *
	 * Throwing variants (intermediateThrowEvent / endEvent) get filled
	 * glyphs; catching variants get hollow ones, matching the BPMN
	 * "throw vs catch" visual convention. We don't read the throw/catch
	 * variant explicitly: bpmnElementType encodes it in the element name.
	 */
	_renderEventDefinitionMarker(parent, shape, eventDefType, elementType, cx, cy) {
		const r  = Math.min(shape.boundsWidth, shape.boundsHeight) * 0.28;
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		// Throwing events (intermediateThrowEvent / endEvent) render their
		// glyph filled black; catching events (start, intermediateCatch,
		// boundary) leave the glyph hollow.
		const isThrowing = /Throw|^endEvent$/.test(elementType || '');
		const fill   = isThrowing ? '#333' : '#fff';
		const stroke = '#333';

		const wrap = document.createElementNS(ns, 'g');
		wrap.setAttribute('class', `bpmn-event-marker bpmn-event-marker-${eventDefType}`);
		wrap.setAttribute('pointer-events', 'none');

		switch (eventDefType) {
			case 'timerEventDefinition': {
				// Clock face: outer circle + 12/3/6/9 ticks + two hands at 12 and 2.
				const ring = document.createElementNS(ns, 'circle');
				ring.setAttribute('cx', cx);
				ring.setAttribute('cy', cy);
				ring.setAttribute('r',  r);
				ring.setAttribute('fill',   '#fff');
				ring.setAttribute('stroke', stroke);
				ring.setAttribute('stroke-width', '1.2');
				wrap.appendChild(ring);
				// Tick marks at 12, 3, 6, 9 o'clock; short bars from 0.85r to r.
				const ticks = document.createElementNS(ns, 'path');
				const t1 = r * 0.85, t2 = r;
				ticks.setAttribute('d', [
					`M ${cx} ${cy - t2} L ${cx} ${cy - t1}`,
					`M ${cx} ${cy + t1} L ${cx} ${cy + t2}`,
					`M ${cx - t2} ${cy} L ${cx - t1} ${cy}`,
					`M ${cx + t1} ${cy} L ${cx + t2} ${cy}`,
				].join(' '));
				ticks.setAttribute('stroke', stroke);
				ticks.setAttribute('stroke-width', '1');
				wrap.appendChild(ticks);
				// Hands: short hand straight up, long hand to ~2 o'clock.
				const hands = document.createElementNS(ns, 'path');
				const ang   = -Math.PI / 3;       // 2 o'clock
				const lx    = cx + r * 0.65 * Math.cos(ang);
				const ly    = cy + r * 0.65 * Math.sin(ang);
				hands.setAttribute('d', `M ${cx} ${cy} L ${cx} ${cy - r * 0.5} M ${cx} ${cy} L ${lx} ${ly}`);
				hands.setAttribute('stroke', stroke);
				hands.setAttribute('stroke-width', '1.2');
				hands.setAttribute('stroke-linecap', 'round');
				wrap.appendChild(hands);
				break;
			}
			case 'messageEventDefinition': {
				// Envelope: rectangle with a triangular flap meeting at the centre.
				const w = r * 1.6, h = r * 1.1;
				const x = cx - w / 2, y = cy - h / 2;
				const env = document.createElementNS(ns, 'rect');
				env.setAttribute('x', x);
				env.setAttribute('y', y);
				env.setAttribute('width',  w);
				env.setAttribute('height', h);
				env.setAttribute('fill',   fill);
				env.setAttribute('stroke', isThrowing ? '#fff' : stroke);
				env.setAttribute('stroke-width', '1.2');
				wrap.appendChild(env);
				// V-flap from top corners to centre.
				const flap = document.createElementNS(ns, 'polyline');
				flap.setAttribute('points', `${x},${y} ${cx},${y + h * 0.55} ${x + w},${y}`);
				flap.setAttribute('fill', 'none');
				flap.setAttribute('stroke', isThrowing ? '#fff' : stroke);
				flap.setAttribute('stroke-width', '1.2');
				flap.setAttribute('stroke-linejoin', 'round');
				wrap.appendChild(flap);
				break;
			}
			case 'errorEventDefinition': {
				// Lightning bolt: a 4-point zigzag shape.
				const w = r * 1.4, h = r * 1.6;
				const x = cx - w / 2, y = cy - h / 2;
				const pts = [
					`${x},${y + h * 0.55}`,
					`${x + w * 0.4},${y}`,
					`${x + w * 0.45},${y + h * 0.45}`,
					`${x + w},${y + h * 0.45}`,
					`${x + w * 0.6},${y + h}`,
					`${x + w * 0.55},${y + h * 0.55}`,
				].join(' ');
				const bolt = document.createElementNS(ns, 'polygon');
				bolt.setAttribute('points', pts);
				bolt.setAttribute('fill',   fill);
				bolt.setAttribute('stroke', stroke);
				bolt.setAttribute('stroke-width', '1');
				bolt.setAttribute('stroke-linejoin', 'round');
				wrap.appendChild(bolt);
				break;
			}
			case 'signalEventDefinition': {
				// Triangle pointing up.
				const a = r * 1.05;
				const pts = [
					`${cx},${cy - a}`,
					`${cx + a * 0.866},${cy + a * 0.5}`,
					`${cx - a * 0.866},${cy + a * 0.5}`,
				].join(' ');
				const tri = document.createElementNS(ns, 'polygon');
				tri.setAttribute('points', pts);
				tri.setAttribute('fill',   fill);
				tri.setAttribute('stroke', stroke);
				tri.setAttribute('stroke-width', '1.2');
				tri.setAttribute('stroke-linejoin', 'round');
				wrap.appendChild(tri);
				break;
			}
			case 'escalationEventDefinition': {
				// Up-arrow chevron.
				const a = r * 1.1;
				const pts = [
					`${cx},${cy - a}`,
					`${cx + a * 0.7},${cy + a * 0.5}`,
					`${cx},${cy + a * 0.05}`,
					`${cx - a * 0.7},${cy + a * 0.5}`,
				].join(' ');
				const arrow = document.createElementNS(ns, 'polygon');
				arrow.setAttribute('points', pts);
				arrow.setAttribute('fill',   fill);
				arrow.setAttribute('stroke', stroke);
				arrow.setAttribute('stroke-width', '1.2');
				arrow.setAttribute('stroke-linejoin', 'round');
				wrap.appendChild(arrow);
				break;
			}
			case 'terminateEventDefinition': {
				// Solid disk filling most of the circle.
				const c = document.createElementNS(ns, 'circle');
				c.setAttribute('cx', cx);
				c.setAttribute('cy', cy);
				c.setAttribute('r',  r);
				c.setAttribute('fill', stroke);
				wrap.appendChild(c);
				break;
			}
			default:
				return;  // unknown event-def type -> no marker
		}
		parent.appendChild(wrap);
	}

	/**
	 * Build the SVG <path> outline for a BPMN data-store reference --
	 * the body of the cylinder (vertical sides + half-ellipse at the
	 * bottom). The top is left open so the lid ellipse drawn separately
	 * by `_appendDataStoreLid` covers it cleanly. Matches the palette
	 * glyph for visual consistency between the toolbar and canvas.
	 */
	_buildDataStorePath(shape) {
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const x  = shape.boundsX, y = shape.boundsY;
		const w  = shape.boundsWidth, h = shape.boundsHeight;
		const ry = Math.max(4, Math.min(h * 0.12, w * 0.18));
		// Body path: left side, bottom half-ellipse, right side. Open at
		// the top -- fill auto-closes with a horizontal line at y+ry,
		// which the lid ellipse then covers.
		const d = [
			`M ${x} ${y + ry}`,
			`L ${x} ${y + h - ry}`,
			`A ${w / 2} ${ry} 0 0 0 ${x + w} ${y + h - ry}`,
			`L ${x + w} ${y + ry}`,
		].join(' ');
		const path = document.createElementNS(ns, 'path');
		path.setAttribute('d', d);
		return path;
	}

	/** Append the lid ellipse (full circle on top of the cylinder) and
	 *  a couple of "stack of disks" hint arcs underneath. Matches the
	 *  palette glyph: full ellipse for the lid, two thin arcs for the
	 *  stack below. */
	_appendDataStoreLid(parent, shape) {
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const x  = shape.boundsX, y = shape.boundsY;
		const w  = shape.boundsWidth, h = shape.boundsHeight;
		const ry = Math.max(4, Math.min(h * 0.12, w * 0.18));

		// Top lid: full ellipse, white-filled so it covers the open top
		// of the body path. The bottom half of the ellipse sits inside
		// the body and is hidden by the body fill.
		const lid = document.createElementNS(ns, 'ellipse');
		lid.setAttribute('cx', x + w / 2);
		lid.setAttribute('cy', y + ry);
		lid.setAttribute('rx', w / 2);
		lid.setAttribute('ry', ry);
		lid.setAttribute('fill',   '#fff');
		lid.setAttribute('stroke', '#333');
		lid.setAttribute('stroke-width', '1.5');
		lid.setAttribute('pointer-events', 'none');
		parent.appendChild(lid);

		// Two thin "disk" arcs below the lid for the stack-of-disks hint.
		for (let i = 1; i <= 2; i++) {
			const dy = ry * 1.6 * i;
			const arc = document.createElementNS(ns, 'path');
			arc.setAttribute('d', `M ${x} ${y + ry + dy} A ${w / 2} ${ry} 0 0 0 ${x + w} ${y + ry + dy}`);
			arc.setAttribute('fill', 'none');
			arc.setAttribute('stroke', '#333');
			arc.setAttribute('stroke-width', '0.8');
			arc.setAttribute('pointer-events', 'none');
			parent.appendChild(arc);
		}
	}

	/** Build the SVG <path> outline for a BPMN data object: a rectangle
	 *  with a folded top-right corner. Returns the path node. */
	_buildDataObjectPath(shape) {
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const x  = shape.boundsX, y = shape.boundsY;
		const w  = shape.boundsWidth, h = shape.boundsHeight;
		const fold = Math.min(w, h) * 0.22;
		const d = [
			`M ${x} ${y}`,
			`L ${x + w - fold} ${y}`,
			`L ${x + w} ${y + fold}`,
			`L ${x + w} ${y + h}`,
			`L ${x} ${y + h}`,
			`Z`,
		].join(' ');
		const path = document.createElementNS(ns, 'path');
		path.setAttribute('d', d);
		return path;
	}

	/** Append the corner-fold triangle on top of the data-object body so it
	 *  reads as a folded sheet of paper. */
	_appendDataObjectFold(parent, shape) {
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const x  = shape.boundsX, y = shape.boundsY;
		const w  = shape.boundsWidth, h = shape.boundsHeight;
		const fold = Math.min(w, h) * 0.22;
		const tri = document.createElementNS(ns, 'path');
		tri.setAttribute('d', `M ${x + w - fold} ${y} L ${x + w - fold} ${y + fold} L ${x + w} ${y + fold}`);
		tri.setAttribute('fill', 'none');
		tri.setAttribute('stroke', '#333');
		tri.setAttribute('stroke-width', '1');
		tri.setAttribute('pointer-events', 'none');
		parent.appendChild(tri);
	}

	/**
	 * Render a BpmnLane as a pool subdivision: a rectangle drawn at the
	 * lane's DI bounds, with a thin vertical title strip on the left for
	 * the rotated lane name. Lanes sit on top of their parent pool but
	 * below element shapes so they don't intercept clicks meant for
	 * tasks / gateways / events.
	 */
	_renderLane(shape) {
		const lane  = this._lanesByBpmnId.get(shape.bpmnElementRef);
		const label = (lane && (lane.bpmnName || lane.name)) || '';

		const x = shape.boundsX, y = shape.boundsY;
		const w = shape.boundsWidth, h = shape.boundsHeight;
		const stripW = _BpmnDiagramCommunitySvg.LANE_STRIP_WIDTH;

		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const g  = document.createElementNS(ns, 'g');
		g.setAttribute('class', 'bpmn-lane');
		if (lane?.id) g.dataset.laneId = lane.id;
		if (shape.id) g.dataset.shapeId = shape.id;
		g.setAttribute('pointer-events', 'none');  // never intercept element clicks

		const outer = document.createElementNS(ns, 'rect');
		outer.setAttribute('x', x);
		outer.setAttribute('y', y);
		outer.setAttribute('width',  w);
		outer.setAttribute('height', h);
		outer.setAttribute('fill',   'none');
		outer.setAttribute('stroke', '#888');
		outer.setAttribute('stroke-width', '1');
		g.appendChild(outer);

		// Vertical title strip divider on the left.
		const strip = document.createElementNS(ns, 'line');
		strip.setAttribute('x1', x + stripW);
		strip.setAttribute('y1', y);
		strip.setAttribute('x2', x + stripW);
		strip.setAttribute('y2', y + h);
		strip.setAttribute('stroke', '#888');
		strip.setAttribute('stroke-width', '1');
		g.appendChild(strip);

		if (label) {
			const cx = x + stripW / 2;
			const cy = y + h / 2;
			const text = document.createElementNS(ns, 'text');
			text.setAttribute('x', cx);
			text.setAttribute('y', cy);
			text.setAttribute('text-anchor',       'middle');
			text.setAttribute('dominant-baseline', 'middle');
			text.setAttribute('transform', `rotate(-90 ${cx} ${cy})`);
			text.setAttribute('class', 'bpmn-lane-label');
			text.setAttribute('font-size', '11');
			text.setAttribute('fill',      '#555');
			text.textContent = label;
			g.appendChild(text);
		}

		this._viewport.appendChild(g);

		// Move + resize affordances: rendered as siblings of the lane g so
		// they can have pointer-events:auto without losing the "clicks pass
		// through the lane body" property the body itself relies on.
		if (this._editable) {
			this._appendContainerMoveHandle(shape, 'lane');
			this._appendContainerResizeHandle(shape, 'lane');
		}
	}

	/**
	 * Render a BpmnParticipant as a labelled pool: a tall rectangle with a
	 * vertical title strip on the left holding the rotated participant name.
	 * BPMN convention is for pools to *contain* their process's flow elements
	 * visually -- we don't enforce that geometrically; we just paint the pool
	 * at the import-time bounds and let the elements layer above it.
	 */
	_renderParticipantPool(shape) {
		const participant = this._participantsByBpmnId.get(shape.bpmnElementRef);
		const label = (participant && (participant.bpmnName || participant.name)) || '';

		const x = shape.boundsX, y = shape.boundsY;
		const w = shape.boundsWidth, h = shape.boundsHeight;
		const stripW = _BpmnDiagramCommunitySvg.POOL_STRIP_WIDTH;

		const g = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'g');
		g.setAttribute('class', 'bpmn-participant-pool');
		if (participant?.id) g.dataset.participantId = participant.id;
		if (shape.id)        g.dataset.shapeId       = shape.id;
		g.setAttribute('pointer-events', 'none');  // pools shouldn't intercept clicks meant for elements

		// Outer rectangle.
		const outer = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'rect');
		outer.setAttribute('x', x);
		outer.setAttribute('y', y);
		outer.setAttribute('width',  w);
		outer.setAttribute('height', h);
		outer.setAttribute('fill',   '#ffffff');
		outer.setAttribute('stroke', '#666');
		outer.setAttribute('stroke-width', '1');
		g.appendChild(outer);

		// Vertical title strip divider.
		const strip = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'line');
		strip.setAttribute('x1', x + stripW);
		strip.setAttribute('y1', y);
		strip.setAttribute('x2', x + stripW);
		strip.setAttribute('y2', y + h);
		strip.setAttribute('stroke', '#666');
		strip.setAttribute('stroke-width', '1');
		g.appendChild(strip);

		// Rotated label centred in the title strip.
		if (label) {
			const cx = x + stripW / 2;
			const cy = y + h / 2;
			const text = document.createElementNS(_BpmnDiagramCommunitySvg.SVG_NS, 'text');
			text.setAttribute('x', cx);
			text.setAttribute('y', cy);
			text.setAttribute('text-anchor', 'middle');
			text.setAttribute('dominant-baseline', 'middle');
			text.setAttribute('transform', `rotate(-90 ${cx} ${cy})`);
			text.setAttribute('class', 'bpmn-participant-label');
			text.setAttribute('font-size', '12');
			text.setAttribute('fill', '#333');
			text.textContent = label;
			g.appendChild(text);
		}

		this._viewport.appendChild(g);

		// Move + resize affordances: same pattern as lanes.
		if (this._editable) {
			this._appendContainerMoveHandle(shape, 'pool');
			this._appendContainerResizeHandle(shape, 'pool');
		}
	}

	/**
	 * Append a transparent click-target over the title strip of a pool /
	 * lane. Dragging it moves the whole container plus everything visually
	 * inside it. The strip is the BPMN-conventional grab area for pools and
	 * lanes (it's where the rotated label sits, so users instinctively
	 * reach for it).
	 */
	_appendContainerMoveHandle(shape, kind) {
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const stripW = (kind === 'pool')
			? _BpmnDiagramCommunitySvg.POOL_STRIP_WIDTH
			: _BpmnDiagramCommunitySvg.LANE_STRIP_WIDTH;

		const h = document.createElementNS(ns, 'rect');
		h.setAttribute('class', `bpmn-container-move-handle bpmn-container-move-${kind}`);
		h.setAttribute('x', shape.boundsX);
		h.setAttribute('y', shape.boundsY);
		h.setAttribute('width',  stripW);
		h.setAttribute('height', shape.boundsHeight);
		h.setAttribute('fill', 'transparent');
		h.style.cursor = 'move';
		if (shape.id) h.dataset.shapeId = shape.id;
		h.addEventListener('pointerdown', (e) => this._onContainerMovePointerDown(e, shape, kind));
		this._viewport.appendChild(h);
	}

	_onContainerMovePointerDown(e, shape, kind) {
		if (!this._editable) return;
		e.stopPropagation();

		// Compute the set of shapes that move together with the container:
		//   pool: the pool itself, all its lane shapes, and every element
		//         shape whose centre falls inside the pool's bounds at
		//         drag start.
		//   lane: the lane itself, plus every element shape whose centre
		//         falls inside the lane's bounds. Lanes don't drag the
		//         enclosing pool (a sibling lane would be left behind).
		const x1 = shape.boundsX, y1 = shape.boundsY;
		const x2 = x1 + shape.boundsWidth, y2 = y1 + shape.boundsHeight;
		const movingShapes = [shape];
		if (kind === 'pool') {
			for (const ls of this._laneShapesForPool(shape)) movingShapes.push(ls);
		}
		for (const [, sh] of this._shapes) {
			if (sh === shape) continue;
			if (this._participantsByBpmnId.has(sh.bpmnElementRef)) continue;
			if (this._lanesByBpmnId.has(sh.bpmnElementRef))        continue;
			const cx = sh.boundsX + sh.boundsWidth  / 2;
			const cy = sh.boundsY + sh.boundsHeight / 2;
			if (cx >= x1 && cx <= x2 && cy >= y1 && cy <= y2) {
				movingShapes.push(sh);
			}
		}

		// Snapshot before-state for each moving shape (positions only).
		const shapeSnaps = movingShapes.map(s => ({
			shape: s,
			boundsX0: s.boundsX,
			boundsY0: s.boundsY,
		}));

		// Snapshot every edge incident to a moving shape, classified by
		// whether both endpoints are moving (fully-inside) or just one
		// (cross). Both kinds are translated by the container delta on
		// each move tick -- fully-inside translates every waypoint;
		// cross translates only the inside endpoint and propagates the
		// orthogonal-aligned adjacent bend. Reading from this snapshot
		// (instead of the live edge state) keeps each tick idempotent.
		const movingBpmnIds = new Set(movingShapes.map(s => s.bpmnElementRef).filter(Boolean));
		const edgesSnap = [];
		for (const [, ed] of this._edges) {
			const seq = this._findFlowByBpmnId(ed.bpmnElementRef);
			const msg = !seq ? this._messageFlowsByBpmnId.get(ed.bpmnElementRef) : null;
			const flow = seq || msg;
			if (!flow) continue;
			const srcIn = movingBpmnIds.has(flow.sourceRefId);
			const tgtIn = movingBpmnIds.has(flow.targetRefId);
			if (srcIn || tgtIn) {
				edgesSnap.push({
					edge: ed,
					waypointsBefore: ed.waypoints,
					srcIn,
					tgtIn,
				});
			}
		}

		this._dragState = {
			kind: 'container-move',
			containerKind: kind,
			startX:  e.clientX,
			startY:  e.clientY,
			shapeSnaps,
			edgesSnap,
		};
		this._svg.setPointerCapture(e.pointerId);
		this._svg.addEventListener('pointermove', this._containerMoveMoveBound = (ev) => this._onContainerMoveMove(ev));
		this._svg.addEventListener('pointerup',   this._containerMoveUpBound   = (ev) => this._onContainerMoveUp(ev));
	}

	_onContainerMoveMove(e) {
		if (!this._dragState || this._dragState.kind !== 'container-move') return;
		const ds   = this._dragState;
		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		// Snap the delta to the grid step so the whole group stays aligned.
		const dx = Math.round(((e.clientX - ds.startX) / this._zoom) / step) * step;
		const dy = Math.round(((e.clientY - ds.startY) / this._zoom) / step) * step;

		// Apply the snapped delta to every moving shape. boundsX0/Y0 hold
		// the pre-drag position so re-applying with a new delta is idempotent
		// across move ticks.
		for (const s of ds.shapeSnaps) {
			s.shape.boundsX = s.boundsX0 + dx;
			s.shape.boundsY = s.boundsY0 + dy;
		}

		// Translate every snapshotted edge by the container delta. Fully-
		// inside edges (both endpoints moving) shift every waypoint;
		// cross edges (one endpoint moving) shift only the inside endpoint
		// and the orthogonal-aligned adjacent bend. The translation is
		// computed from the snapshot, so the result is idempotent across
		// move ticks and parallel edges keep their authored offsets.
		for (const es of ds.edgesSnap) {
			const wpsBefore = this._parseWaypoints({ waypoints: es.waypointsBefore });
			if (wpsBefore.length < 2) continue;
			if (es.srcIn && es.tgtIn) {
				const wps = wpsBefore.map(w => ({ x: w.x + dx, y: w.y + dy }));
				es.edge.waypoints = JSON.stringify(wps);
				continue;
			}
			const wps = wpsBefore.map(w => ({ x: w.x, y: w.y }));
			if (es.srcIn) {
				const old = wpsBefore[0];
				wps[0] = { x: old.x + dx, y: old.y + dy };
				if (wps.length > 2) {
					const adj = wpsBefore[1];
					if (Math.abs(adj.x - old.x) < 0.5) wps[1].x = adj.x + dx;
					if (Math.abs(adj.y - old.y) < 0.5) wps[1].y = adj.y + dy;
				}
			} else if (es.tgtIn) {
				const last = wpsBefore.length - 1;
				const old  = wpsBefore[last];
				wps[last]  = { x: old.x + dx, y: old.y + dy };
				if (wps.length > 2) {
					const adj = wpsBefore[last - 1];
					if (Math.abs(adj.x - old.x) < 0.5) wps[last - 1].x = adj.x + dx;
					if (Math.abs(adj.y - old.y) < 0.5) wps[last - 1].y = adj.y + dy;
				}
			}
			es.edge.waypoints = JSON.stringify(wps);
		}

		this.refresh();
	}

	_onContainerMoveUp(e) {
		this._svg.removeEventListener('pointermove', this._containerMoveMoveBound);
		this._svg.removeEventListener('pointerup',   this._containerMoveUpBound);
		try { this._svg.releasePointerCapture(e.pointerId); } catch (_) {}
		const ds = this._dragState;
		this._dragState = null;
		if (!ds || ds.kind !== 'container-move') return;

		// Build before/after snapshots for every shape that actually moved.
		const shapeChanges = ds.shapeSnaps
			.map(s => ({
				shape: s.shape,
				before: { boundsX: s.boundsX0,        boundsY: s.boundsY0 },
				after:  { boundsX: s.shape.boundsX,   boundsY: s.shape.boundsY },
			}))
			.filter(c => c.before.boundsX !== c.after.boundsX || c.before.boundsY !== c.after.boundsY);
		if (shapeChanges.length === 0) return;

		// All edges (both fully-inside and cross) were snapshotted at
		// pointerdown and translated in-place each move tick. Build
		// before/after pairs for the undo entry from the same snapshot.
		const edgeChanges = ds.edgesSnap
			.map(es => ({
				edge: es.edge,
				before: es.waypointsBefore,
				after:  es.edge.waypoints,
			}))
			.filter(ec => ec.before !== ec.after);

		for (const c of shapeChanges) {
			if (c.shape.id) this._bufferUpdate(c.shape.id, c.after);
		}
		for (const ec of edgeChanges) {
			if (ec.edge.id && ec.before !== ec.after) {
				this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
			}
		}
		this._pushUndo({
			apply:  () => {
				for (const c of shapeChanges) {
					Object.assign(c.shape, c.after);
					if (c.shape.id) this._bufferUpdate(c.shape.id, c.after);
				}
				for (const ec of edgeChanges) {
					ec.edge.waypoints = ec.after;
					if (ec.edge.id) this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
				}
				this.refresh();
			},
			revert: () => {
				for (const c of shapeChanges) {
					Object.assign(c.shape, c.before);
					if (c.shape.id) this._bufferUpdate(c.shape.id, c.before);
				}
				for (const ec of edgeChanges) {
					ec.edge.waypoints = ec.before;
					if (ec.edge.id) this._bufferUpdate(ec.edge.id, { waypoints: ec.before });
				}
				this.refresh();
			},
		});
	}

	/**
	 * Append a small bottom-right resize handle for a layout container
	 * (pool or lane). Always visible -- pools and lanes don't surface in
	 * normal selection because they have pointer-events disabled, so the
	 * handle is the only way to grab them. Both axes are dragged together
	 * (SE-resize) regardless of `kind`; the kind is recorded on the drag
	 * state so dragend / undo know which collection of bpmn-* classes to
	 * touch when refreshing.
	 */
	_appendContainerResizeHandle(shape, kind) {
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const x  = shape.boundsX + shape.boundsWidth;
		const y  = shape.boundsY + shape.boundsHeight;
		const r  = 6;

		const h = document.createElementNS(ns, 'rect');
		h.setAttribute('class', `bpmn-container-resize-handle bpmn-container-resize-${kind}`);
		h.setAttribute('x', x - r);
		h.setAttribute('y', y - r);
		h.setAttribute('width',  r * 2);
		h.setAttribute('height', r * 2);
		h.setAttribute('fill',   '#fff');
		h.setAttribute('stroke', kind === 'pool' ? '#666' : '#888');
		h.setAttribute('stroke-width', '1.5');
		// Hover-only visibility: invisible at rest, full opacity on hover.
		// Pointer-events stay on so the corner is still grabbable -- users
		// learn the affordance via the cursor change.
		h.setAttribute('opacity', '0');
		h.addEventListener('pointerenter', () => h.setAttribute('opacity', '1'));
		h.addEventListener('pointerleave', () => {
			// Keep visible while a drag is in flight even if the pointer
			// leaves the original handle bounds.
			if (!this._dragState || this._dragState.kind !== 'container-resize') {
				h.setAttribute('opacity', '0');
			}
		});
		h.style.cursor = 'nwse-resize';
		if (shape.id) h.dataset.shapeId = shape.id;
		h.addEventListener('pointerdown', (e) => this._onContainerResizePointerDown(e, shape, kind));
		this._viewport.appendChild(h);
	}

	_onContainerResizePointerDown(e, shape, kind) {
		if (!this._editable) return;
		e.stopPropagation();
		// Snapshot the lane DI shapes so a pool resize can write their new
		// boundsX / boundsWidth to the buffer at dragend (and restore them
		// on undo). Pools have no flows attached, so no edge snapshot.
		const laneSnaps = (kind === 'pool')
			? this._laneShapesForPool(shape).map(ls => ({
				shape: ls,
				boundsX0: ls.boundsX,
				boundsWidth0: ls.boundsWidth,
			}))
			: [];
		this._dragState = {
			kind:  'container-resize',
			shape,
			containerKind: kind,
			startX:  e.clientX,
			startY:  e.clientY,
			boundsX0:      shape.boundsX,
			boundsY0:      shape.boundsY,
			boundsWidth0:  shape.boundsWidth,
			boundsHeight0: shape.boundsHeight,
			laneSnaps,
		};
		this._svg.setPointerCapture(e.pointerId);
		this._svg.addEventListener('pointermove', this._containerResizeMoveBound = (ev) => this._onContainerResizeMove(ev));
		this._svg.addEventListener('pointerup',   this._containerResizeUpBound   = (ev) => this._onContainerResizeUp(ev));
	}

	_onContainerResizeMove(e) {
		if (!this._dragState || this._dragState.kind !== 'container-resize') return;
		const ds   = this._dragState;
		const dx   = (e.clientX - ds.startX) / this._zoom;
		const dy   = (e.clientY - ds.startY) / this._zoom;
		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		// Pools enforce a roomy minimum because they hold elements; lanes a
		// smaller one (they only hold a labelled band).
		const MIN_W = ds.containerKind === 'pool' ? 200 : 100;
		const MIN_H = ds.containerKind === 'pool' ?  80 :  40;
		const w = Math.max(MIN_W, Math.round((ds.boundsWidth0  + dx) / step) * step);
		const h = Math.max(MIN_H, Math.round((ds.boundsHeight0 + dy) / step) * step);
		ds.shape.boundsWidth  = w;
		ds.shape.boundsHeight = h;
		// Pool resize: keep its lanes flush. They share the pool's x and
		// width by BPMN convention; we don't touch their y or height (the
		// stacking layout is the author's call).
		if (ds.containerKind === 'pool') {
			this._propagatePoolWidthToLanes(ds.shape);
		}
		this.refresh();
	}

	_onContainerResizeUp(e) {
		this._svg.removeEventListener('pointermove', this._containerResizeMoveBound);
		this._svg.removeEventListener('pointerup',   this._containerResizeUpBound);
		try { this._svg.releasePointerCapture(e.pointerId); } catch (_) {}
		const ds = this._dragState;
		this._dragState = null;
		if (!ds || ds.kind !== 'container-resize') return;

		const sh     = ds.shape;
		const before = { boundsWidth: ds.boundsWidth0, boundsHeight: ds.boundsHeight0 };
		const after  = { boundsWidth: sh.boundsWidth,  boundsHeight: sh.boundsHeight };
		if (before.boundsWidth === after.boundsWidth && before.boundsHeight === after.boundsHeight) return;

		// Lane width changes propagated during the drag: capture them as
		// before/after pairs so the same compound undo entry restores them.
		const laneChanges = ds.laneSnaps.map(ls => ({
			shape: ls.shape,
			before: { boundsX: ls.boundsX0, boundsWidth: ls.boundsWidth0 },
			after:  { boundsX: ls.shape.boundsX, boundsWidth: ls.shape.boundsWidth },
		}));

		if (sh.id) this._bufferUpdate(sh.id, after);
		for (const lc of laneChanges) {
			if (lc.shape.id) this._bufferUpdate(lc.shape.id, lc.after);
		}
		this._pushUndo({
			apply:  () => {
				Object.assign(sh, after);
				if (sh.id) this._bufferUpdate(sh.id, after);
				for (const lc of laneChanges) {
					Object.assign(lc.shape, lc.after);
					if (lc.shape.id) this._bufferUpdate(lc.shape.id, lc.after);
				}
				this.refresh();
			},
			revert: () => {
				Object.assign(sh, before);
				if (sh.id) this._bufferUpdate(sh.id, before);
				for (const lc of laneChanges) {
					Object.assign(lc.shape, lc.before);
					if (lc.shape.id) this._bufferUpdate(lc.shape.id, lc.before);
				}
				this.refresh();
			},
		});
	}

	/**
	 * Find the DI shapes of all lanes that belong to the participant whose
	 * pool DI shape is `poolShape`. Walks pool -> participant -> process ->
	 * lanes, then resolves each lane's bpmnId back to its DI shape. Returns
	 * an empty array for pools without a wired participant or lanes.
	 */
	_laneShapesForPool(poolShape) {
		const out = [];
		const part = this._participantsByBpmnId.get(poolShape?.bpmnElementRef);
		const procId = part?.process?.id ?? null;
		if (!procId) return out;
		for (const [, lane] of this._lanes) {
			if ((lane.process?.id ?? null) !== procId) continue;
			if (!lane.bpmnId) continue;
			const laneShape = this._shapes.get(lane.bpmnId);
			if (laneShape) out.push(laneShape);
		}
		return out;
	}

	/** Make every lane shape in `poolShape`'s pool nest cleanly inside the
	 *  pool's content area: lane.x = pool.x + POOL_STRIP_WIDTH, lane.width =
	 *  pool.width - POOL_STRIP_WIDTH. Heights and y-positions are preserved
	 *  (lanes stack vertically; the author owns that layout). Used both
	 *  during pool drag-resize and at import time to clean up authored
	 *  slop. */
	_propagatePoolWidthToLanes(poolShape) {
		const inset = _BpmnDiagramCommunitySvg.POOL_STRIP_WIDTH;
		for (const laneShape of this._laneShapesForPool(poolShape)) {
			laneShape.boundsX     = poolShape.boundsX + inset;
			laneShape.boundsWidth = Math.max(0, poolShape.boundsWidth - inset);
		}
	}

	/**
	 * Walk every participant pool and inset its lanes inside the pool's
	 * content area (right of the pool's title strip). Idempotent: re-
	 * running with already-aligned shapes is a no-op. Buffers any actual
	 * shifts so the cleanup persists when the user saves.
	 */
	_normaliseLaneWidthsToPools() {
		const inset = _BpmnDiagramCommunitySvg.POOL_STRIP_WIDTH;
		for (const [, poolShape] of this._shapes) {
			if (!this._participantsByBpmnId.has(poolShape.bpmnElementRef)) continue;
			const targetX = poolShape.boundsX + inset;
			const targetW = Math.max(0, poolShape.boundsWidth - inset);
			for (const laneShape of this._laneShapesForPool(poolShape)) {
				const x0 = laneShape.boundsX, w0 = laneShape.boundsWidth;
				if (x0 === targetX && w0 === targetW) continue;
				laneShape.boundsX     = targetX;
				laneShape.boundsWidth = targetW;
				if (laneShape.id) {
					this._bufferUpdate(laneShape.id, {
						boundsX: laneShape.boundsX, boundsWidth: laneShape.boundsWidth,
					});
				}
			}
		}
	}

	/**
	 * Render a BpmnMessageFlow as a dashed cross-pool edge with a small
	 * hollow circle at the source and an open arrow at the target. Painted
	 * on top of the shape layer so it crosses pools cleanly.
	 */
	_renderMessageFlowEdge(edge) {
		const waypoints = this._parseWaypoints(edge);
		if (waypoints.length < 2) return;

		const msgFlow = this._messageFlowsByBpmnId.get(edge.bpmnElementRef);
		const msgFlowId = msgFlow ? msgFlow.id : null;

		// MessageFlow edges render with a wider invisible hit overlay so
		// the dashed line is clickable; same pattern as sequence flows.
		const ns = _BpmnDiagramCommunitySvg.SVG_NS;
		const g = document.createElementNS(ns, 'g');
		g.setAttribute('class', 'bpmn-edge-group bpmn-edge-group-msgflow');
		if (msgFlowId)            g.dataset.flowId = msgFlowId;
		if (edge.bpmnElementRef) g.dataset.flowRef = edge.bpmnElementRef;

		const points = waypoints.map(w => `${w.x},${w.y}`).join(' ');

		const hit = document.createElementNS(ns, 'polyline');
		hit.setAttribute('points', points);
		hit.setAttribute('fill', 'none');
		hit.setAttribute('stroke', 'transparent');
		hit.setAttribute('stroke-width', '12');
		hit.setAttribute('class', 'bpmn-edge-hit');
		hit.style.cursor = this._editable ? 'pointer' : '';
		g.appendChild(hit);

		const path = document.createElementNS(ns, 'polyline');
		path.setAttribute('points', points);
		path.setAttribute('fill', 'none');
		path.setAttribute('stroke', '#333');
		path.setAttribute('stroke-width', '1.2');
		path.setAttribute('stroke-dasharray', '6 4');
		path.setAttribute('marker-start', 'url(#bpmn-msgflow-start)');
		path.setAttribute('marker-end',   'url(#bpmn-msgflow-end)');
		path.setAttribute('class', 'bpmn-edge bpmn-message-flow');
		path.setAttribute('pointer-events', 'none');
		if (edge.bpmnElementRef) path.dataset.messageFlowRef = edge.bpmnElementRef;
		g.appendChild(path);

		if (this._editable && msgFlowId) {
			g.addEventListener('click', (e) => {
				e.stopPropagation();
				this.selectFlow(msgFlowId);
			});
		}

		// MessageFlow name label: same convention as sequence flows.
		if (msgFlow && msgFlow.bpmnName) this._appendEdgeLabel(g, waypoints, msgFlow.bpmnName);

		this._viewport.appendChild(g);
	}

	/**
	 * Look up a flow object by its node id, regardless of whether it's a
	 * sequence flow or a message flow. Returns the flow and a "kind" tag
	 * the caller can use when buffering changes back to the server.
	 */
	_lookupFlow(flowId) {
		const seq = this._flows.get(flowId);
		if (seq) return { flow: seq, kind: 'sequence' };
		const msg = this._messageFlows.get(flowId);
		if (msg) return { flow: msg, kind: 'message' };
		return null;
	}

	/**
	 * The DI edge for a given flow. Sequence flow -> _edges keyed by flow's
	 * bpmnId; message flow -> same map (BpmnDiEdges work for both).
	 */
	_edgeForFlow(flow) {
		return flow?.bpmnId ? this._edges.get(flow.bpmnId) : null;
	}

	_parseWaypoints(edge) {
		// Waypoints are stored as a JSON string per BpmnDiEdge schema.
		if (!edge.waypoints) return [];
		try {
			const parsed = typeof edge.waypoints === 'string' ? JSON.parse(edge.waypoints) : edge.waypoints;
			return parsed.map(w => ({ x: Number(w.x), y: Number(w.y) }));
		} catch (e) {
			console.warn('bpmn diagram: bad waypoints', edge, e);
			return [];
		}
	}

	/**
	 * Greedy word-wrap into lines bounded by an approximate character count.
	 * Splits on whitespace; hyphens are treated as soft-break points (the
	 * hyphen stays on the preceding line). Long unbreakable tokens (BPMN
	 * identifiers like "Task_NotifyApproved") that have no whitespace and no
	 * hyphens are kept on a single line rather than chopped mid-identifier.
	 */
	_wrapLabel(text, maxChars) {
		if (!text) return [];
		// Insert a soft break after each hyphen so hyphenated terms can wrap.
		const normalized = String(text).replace(/-(\S)/g, '- $1');
		const words = normalized.split(/\s+/).filter(Boolean);
		if (words.length === 0) return [];
		const lines = [];
		let current = '';
		for (const w of words) {
			if (!current) { current = w; continue; }
			// Concat without the inserted space when the previous word ended in
			// a hyphen, so "Auto- approve?" round-trips back to "Auto-" + "approve?".
			const sep = current.endsWith('-') ? '' : ' ';
			if ((current.length + sep.length + w.length) <= maxChars) {
				current += sep + w;
			} else {
				lines.push(current);
				current = w;
			}
		}
		if (current) lines.push(current);
		return lines;
	}

	_findElementIdByBpmnId(bpmnId) {
		// Shape.bpmnElementRef is the bpmnId (e.g. "Task_Submit"); we need the
		// node UUID for selection events. Reverse-lookup once per render.
		for (const [id, el] of this._elements) {
			if (el.bpmnId === bpmnId) return id;
		}
		return null;
	}

	_applyTransform() {
		if (!this._viewport) return;
		this._viewport.setAttribute('transform', `translate(${this._panX},${this._panY}) scale(${this._zoom})`);
	}

	_applySelection() {
		if (!this._viewport) return;
		for (const g of this._viewport.querySelectorAll('.bpmn-shape')) {
			if (g.dataset.elementId === this._selectedId) g.classList.add('bpmn-selected');
			else g.classList.remove('bpmn-selected');
		}
		// Highlight the selected edge by recolouring its visible polyline.
		// Hit overlays stay transparent; only the .bpmn-edge polyline takes
		// the selection colour.
		for (const eg of this._viewport.querySelectorAll('.bpmn-edge-group')) {
			const sel  = (eg.dataset.flowId === this._selectedFlowId);
			const path = eg.querySelector('.bpmn-edge');
			if (path) {
				path.setAttribute('stroke',       sel ? '#3498db' : '#333');
				path.setAttribute('stroke-width', sel ? '2'       : '1.5');
			}
		}
		this._renderConnectHandle();
		this._renderBendHandles();
		this._renderResizeHandles();
	}

	_applyHighlights() {
		if (!this._viewport) return;
		for (const g of this._viewport.querySelectorAll('.bpmn-shape')) {
			// Strip any prior highlight classes; reapply current set.
			for (const cls of Array.from(g.classList)) {
				if (cls.startsWith('bpmn-highlight-')) g.classList.remove(cls);
			}
			for (const [style, ids] of this._highlightGroups) {
				if (ids.has(g.dataset.elementId)) g.classList.add(`bpmn-highlight-${style}`);
			}
		}
	}

	// ===== Internals: interaction =====

	_onPointerDown(e) {
		// Background drag = pan. Element drag is handled by per-shape listener.
		if (e.target !== this._svg && e.target !== this._viewport) return;

		// Placement mode: an empty-canvas pointer-down creates an element at
		// the click location and consumes the armed state. Suppress pan so
		// we don't translate the viewport instead.
		if (this._editable && this._pendingPlacement) {
			// Boundary events MUST be attached to a host activity. An
			// empty-canvas click while a boundary type is armed is
			// rejected: keep the placement live so the next click can
			// land on a real activity. (Detach is delete-only, so we
			// also reject creation in detached state.)
			if (this._isBoundaryElementType(this._pendingPlacement)) {
				this._warnBoundaryNeedsHost();
				e.preventDefault();
				return;
			}
			const w  = this._screenToWorld(e.clientX, e.clientY);
			const step = _BpmnDiagramCommunitySvg.GRID_STEP;
			// Snap centre to grid; default-size element back-computed.
			const cx = Math.round(w.x / step) * step;
			const cy = Math.round(w.y / step) * step;
			const type = this._pendingPlacement;
			// Strip the optional ":eventDefinitionType" suffix when sizing.
			const baseType  = type.includes(':') ? type.substring(0, type.indexOf(':')) : type;
			const isEvent   = baseType.endsWith('Event')   || baseType === 'startEvent' || baseType === 'endEvent';
			const isGateway = baseType.endsWith('Gateway');
			const isData    = baseType === 'dataObjectReference' || baseType === 'dataStoreReference' || baseType === 'dataObject';
			const width  = isEvent ? 36 : (isGateway ? 80 : (isData ? 50 : 100));
			const height = isEvent ? 36 : (isGateway ? 80 : (isData ? 50 : 80));
			try {
				this.createElement({ type, x: cx - width / 2, y: cy - height / 2, width, height });
			} catch (err) {
				console.error('createElement failed', err);
			}
			this.cancelPlacement();
			e.preventDefault();
			return;
		}

		this._dragState = {
			kind:   'pan',
			startX: e.clientX,
			startY: e.clientY,
			panX0:  this._panX,
			panY0:  this._panY,
		};
		this._svg.setPointerCapture(e.pointerId);
		this._svg.addEventListener('pointermove', this._panMoveBound = (ev) => this._onPanMove(ev));
		this._svg.addEventListener('pointerup',   this._panUpBound   = (ev) => this._onPanUp(ev));
		e.preventDefault();
	}

	_onPanMove(e) {
		if (!this._dragState || this._dragState.kind !== 'pan') return;
		this._panX = this._dragState.panX0 + (e.clientX - this._dragState.startX);
		this._panY = this._dragState.panY0 + (e.clientY - this._dragState.startY);
		this._applyTransform();
	}

	_onPanUp(e) {
		this._svg.removeEventListener('pointermove', this._panMoveBound);
		this._svg.removeEventListener('pointerup',   this._panUpBound);
		try { this._svg.releasePointerCapture(e.pointerId); } catch (_) {}
		this._dragState = null;
	}

	_onWheel(e) {
		e.preventDefault();
		const factor = e.deltaY < 0 ? 1.1 : 1 / 1.1;
		// Zoom around the cursor: keep the world point under the cursor fixed.
		const rect = this._svg.getBoundingClientRect();
		const cx = e.clientX - rect.left;
		const cy = e.clientY - rect.top;
		const worldX = (cx - this._panX) / this._zoom;
		const worldY = (cy - this._panY) / this._zoom;
		this.zoom(factor);
		this._panX = cx - worldX * this._zoom;
		this._panY = cy - worldY * this._zoom;
		this._applyTransform();
	}

	_onShapePointerDown(e, shape, elementId) {
		if (!this._editable) return;
		// Palette boundary placement: clicking a valid host while a
		// boundaryEvent type is armed creates a boundary on that host
		// at the nearest cardinal slot, with `attachedToRef` set. No
		// shape selection / drag for this case -- consume and return.
		if (this._pendingPlacement && this._isBoundaryElementType(this._pendingPlacement)) {
			e.stopPropagation();
			const host = this._boundaryHostAtScreen(e.clientX, e.clientY);
			if (host) {
				const cursor = this._screenToWorld(e.clientX, e.clientY);
				const slot   = this._nearestCardinalSlot(host.shape, cursor);
				const type   = this._pendingPlacement;
				// Default size of a boundary event: 36x36 (events).
				const w = 36, h = 36;
				try {
					// `attachedTo` is the typed relationship to the host
					// element. The buffer-create writes the host's UUID;
					// the in-memory element gets a stub `{ id, bpmnId }`
					// patched in below so `_attachedToBpmnIdOf` resolves
					// without a separate /ui round-trip.
					// `cancelActivity` stays in bpmnAttributes JSON: it's a
					// single boolean attribute and doesn't merit a typed
					// property of its own.
					const newId = this.createElement({
						type, x: slot.x - w / 2, y: slot.y - h / 2, width: w, height: h,
						props: {
							attachedTo:     host.element.id,
							bpmnAttributes: JSON.stringify({ cancelActivity: 'true' }),
						},
					});
					const boundaryEl = newId ? this._elements.get(newId) : null;
					if (boundaryEl) {
						boundaryEl.attachedTo = { id: host.element.id, bpmnId: host.element.bpmnId };
						boundaryEl._cachedAttachedToRef = host.element.bpmnId;
					}
					// If existing siblings already occupy this side, spread
					// the side's full set evenly. Pushed as a separate undo
					// entry so the create + redistribute decompose cleanly:
					// one Cmd-Z reverts the spread, a second deletes the new
					// boundary.
					this._redistributeBoundariesOnSideWithUndo(host.element.bpmnId, slot.side);
				} catch (err) {
					console.error('createElement (boundary) failed', err);
				}
				this._clearBoundaryPlacementOverlay();
				this.cancelPlacement();
			} else {
				// Click on a non-host shape (gateway, event, data) while a
				// boundary type is armed: warn and keep placement live so
				// the user can click a real activity next.
				this._warnBoundaryNeedsHost();
			}
			e.preventDefault();
			return;
		}

		e.stopPropagation();
		// Select on pointerdown rather than waiting for a "click" decision at
		// pointerup. The click-vs-drag heuristic missed too many real clicks
		// (a 2-3px pointer jitter on a touchpad flips `moved` to true and the
		// select branch is skipped). Selection is independent of moving --
		// safe to set up front.
		if (elementId) this.select(elementId);
		// Snapshot incident edges' waypoints so undo can restore them and so
		// dragend can record their new state in the buffer. Includes both
		// sequence flows and message flows -- they share the BpmnDiEdge
		// representation and both need their endpoints re-snapped.
		const edgesSnap = this._snapshotIncidentEdges(shape);
		// Boundary events attached to this host (BPMN `attachedToRef`)
		// must move together with the host: snapshot their bounds and
		// incident edges so the live drag can translate them in lockstep
		// and dragend can write each one to the buffer + undo entry.
		const attachedSnap = this._attachedShapesFor(shape.bpmnElementRef).map(a => ({
			element:   a.element,
			shape:     a.shape,
			boundsX0:  a.shape.boundsX,
			boundsY0:  a.shape.boundsY,
			edgesSnap: this._snapshotIncidentEdges(a.shape),
		}));
		// Boundary detection: when the dragged shape is itself a boundary
		// event we override the free-position drag with slot snapping
		// against valid hosts. Capture the original `attachedToRef` up
		// front so dragend can detect a re-attach (host change).
		const element            = elementId ? this._elements.get(elementId) : null;
		const elementType        = element?.bpmnElementType ?? '';
		const isBoundary         = elementType === 'boundaryEvent';
		const originalHostBpmnId = isBoundary ? this._attachedToBpmnIdOf(element) : null;
		// Origin-host siblings (other boundaries on the same host): if
		// the drag leaves a side, the remaining siblings need to spread
		// out. Snapshot them up front so dragend can diff for the undo
		// entry.
		const originSiblingsSnap = (isBoundary && originalHostBpmnId)
			? this._attachedShapesFor(originalHostBpmnId)
				.filter(a => a.element.id !== elementId)
				.map(a => ({
					element:  a.element,
					shape:    a.shape,
					boundsX0: a.shape.boundsX,
					boundsY0: a.shape.boundsY,
					edgesSnap: this._snapshotIncidentEdges(a.shape),
				}))
			: [];
		this._dragState = {
			kind:      'element',
			elementId: elementId,
			shape:     shape,
			startX:    e.clientX,
			startY:    e.clientY,
			boundsX0:  shape.boundsX,
			boundsY0:  shape.boundsY,
			edgesSnap,
			attachedSnap,
			isBoundary,
			originalHostBpmnId,
			currentHostBpmnId:   originalHostBpmnId,  // mutates as drag crosses hosts
			currentHostElementId: null,               // populated by drag-move feedback
			originSiblingsSnap,
			moved:     false,
		};
		this._svg.setPointerCapture(e.pointerId);
		this._svg.addEventListener('pointermove', this._elementMoveBound = (ev) => this._onElementDragMove(ev));
		this._svg.addEventListener('pointerup',   this._elementUpBound   = (ev) => this._onElementDragUp(ev));
	}

	_onElementDragMove(e) {
		if (!this._dragState || this._dragState.kind !== 'element') return;
		const dx = (e.clientX - this._dragState.startX) / this._zoom;
		const dy = (e.clientY - this._dragState.startY) / this._zoom;
		// Only flip 'moved' once the pointer has travelled past a small slop
		// threshold; otherwise a small involuntary jitter on click would be
		// treated as a drag and prevent selection.
		const px = Math.abs(e.clientX - this._dragState.startX);
		const py = Math.abs(e.clientY - this._dragState.startY);
		if (!this._dragState.moved && (px > 3 || py > 3)) {
			this._dragState.moved = true;
		}
		if (!this._dragState.moved) return;
		const sh   = this._dragState.shape;

		// Boundary-event drag: snap to the nearest cardinal slot of a
		// valid host instead of free positioning. If the cursor leaves
		// every host, fall back to the original host so the boundary
		// can't be detached by drag (per UX: detach = delete only).
		if (this._dragState.isBoundary) {
			const cursor = this._screenToWorld(e.clientX, e.clientY);
			const hit    = this._boundaryHostAtScreen(e.clientX, e.clientY, this._dragState.elementId);
			let host = hit;
			if (!host) {
				// Fall back to original host so the boundary stays anchored.
				const origBpmnId = this._dragState.originalHostBpmnId;
				const origShape  = origBpmnId ? this._shapes.get(origBpmnId) : null;
				const origEl     = origBpmnId ? this._elementByBpmnId(origBpmnId) : null;
				if (origShape && origEl) {
					host = { element: origEl, shape: origShape, elementId: origEl.id };
				}
			}
			if (host) {
				const slot = this._nearestCardinalSlot(host.shape, cursor);
				sh.boundsX = slot.x - sh.boundsWidth  / 2;
				sh.boundsY = slot.y - sh.boundsHeight / 2;
				this._dragState.currentHostBpmnId    = host.element.bpmnId;
				this._dragState.currentHostElementId = host.elementId;
				this._updateEndpointDropTargets(host.elementId, cursor);
			}
			// Re-snap the boundary's own incident edges (e.g. the timer's
			// outgoing escalation flow) by the actual delta applied.
			const dxApplied = sh.boundsX - this._dragState.boundsX0;
			const dyApplied = sh.boundsY - this._dragState.boundsY0;
			this._translateIncidentEdgeEndpoints(sh, this._dragState.edgesSnap, dxApplied, dyApplied);
			this.refresh();
			return;
		}

		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		// Snap the shape's CENTRE to the grid, then back-compute the
		// top-left bounds. This lines up shapes of different sizes around
		// the same axis lines and matches the load-time auto-snap rule.
		const cxNew = Math.round(((this._dragState.boundsX0 + dx) + sh.boundsWidth  / 2) / step) * step;
		const cyNew = Math.round(((this._dragState.boundsY0 + dy) + sh.boundsHeight / 2) / step) * step;
		sh.boundsX = cxNew - sh.boundsWidth  / 2;
		sh.boundsY = cyNew - sh.boundsHeight / 2;
		// Translate incident edge endpoints by the same delta the shape
		// moved, so each endpoint keeps its offset along the shape side.
		// Parallel edges (e.g. two opposite-direction messageFlows between
		// the same two shapes) stay visibly separate during the drag.
		const dxApplied = sh.boundsX - this._dragState.boundsX0;
		const dyApplied = sh.boundsY - this._dragState.boundsY0;
		this._translateIncidentEdgeEndpoints(sh, this._dragState.edgesSnap, dxApplied, dyApplied);
		// Boundary events ride on the host's perimeter (BPMN attachedToRef):
		// translate each by the same delta so the relative position is
		// preserved, and re-snap their own incident edges (e.g. the timer
		// boundary's `Flow_Escalate` outgoing flow).
		for (const att of this._dragState.attachedSnap) {
			att.shape.boundsX = att.boundsX0 + dxApplied;
			att.shape.boundsY = att.boundsY0 + dyApplied;
			this._translateIncidentEdgeEndpoints(att.shape, att.edgesSnap, dxApplied, dyApplied);
		}
		this.refresh();
	}

	_onElementDragUp(e) {
		this._svg.removeEventListener('pointermove', this._elementMoveBound);
		this._svg.removeEventListener('pointerup',   this._elementUpBound);
		try { this._svg.releasePointerCapture(e.pointerId); } catch (_) {}
		const ds = this._dragState;
		this._dragState = null;
		// Tear down boundary-drag visual feedback regardless of how the
		// drag ended.
		if (ds && ds.isBoundary) this._clearBoundaryPlacementOverlay();
		if (!ds || ds.kind !== 'element') return;

		// Selection happened up front in _onShapePointerDown, so a no-move
		// release is just a click that's already done its job.
		if (!ds.moved) return;

		// Move ended -- write shape position and incident-edge waypoints to
		// the buffer, push a single compound undo entry covering both, and
		// notify the host so it can update its "unsaved changes" indicator.
		const sh     = ds.shape;
		const before = { boundsX: ds.boundsX0, boundsY: ds.boundsY0 };

		// ----- Boundary slot redistribution -----
		//
		// When a boundary lands on a side that already has siblings, all
		// boundaries on that side spread evenly along it (no pile-ups).
		// When a boundary leaves a side, the remaining siblings on that
		// side also redistribute (they may have been distributed under
		// the now-departed boundary's presence).
		//
		// Affected (host, side) pairs are at most two: the side the
		// boundary started on, and the side it ended on. Same-host
		// in-place drags collapse to one pair.
		//
		// Sibling state is snapshotted into `siblingSnaps` so dragend's
		// undo entry can revert every one of them, not just the dragged
		// boundary itself. Origin-host siblings are pre-snapshotted in
		// pointerdown; destination-host siblings (when the host changed)
		// are snapshotted lazily here, immediately before redistribution
		// mutates them, so their pre-redistribution bounds are correct.
		const siblingSnaps = new Map();   // shape -> { shape, boundsX0, boundsY0, edgesSnap }
		for (const s of (ds.originSiblingsSnap || [])) siblingSnaps.set(s.shape, s);
		const ensureSibSnap = (shape) => {
			let snap = siblingSnaps.get(shape);
			if (!snap) {
				snap = {
					shape,
					boundsX0: shape.boundsX,
					boundsY0: shape.boundsY,
					edgesSnap: this._snapshotIncidentEdges(shape),
				};
				siblingSnaps.set(shape, snap);
			}
			return snap;
		};

		if (ds.isBoundary) {
			const affected = new Set();   // "hostBpmnId|side"
			// Origin side (computed against the boundary's PRE-drag centre).
			if (ds.originalHostBpmnId) {
				const origHost = this._shapes.get(ds.originalHostBpmnId);
				if (origHost) {
					const oldCx = ds.boundsX0 + sh.boundsWidth  / 2;
					const oldCy = ds.boundsY0 + sh.boundsHeight / 2;
					affected.add(`${ds.originalHostBpmnId}|${this._sideOfPointOnHost(origHost, oldCx, oldCy)}`);
				}
			}
			// Destination side (boundary's CURRENT centre after slot snap).
			if (ds.currentHostBpmnId) {
				const destHost = this._shapes.get(ds.currentHostBpmnId);
				if (destHost) {
					const newCx = sh.boundsX + sh.boundsWidth  / 2;
					const newCy = sh.boundsY + sh.boundsHeight / 2;
					affected.add(`${ds.currentHostBpmnId}|${this._sideOfPointOnHost(destHost, newCx, newCy)}`);
				}
			}
			for (const key of affected) {
				const sep = key.lastIndexOf('|');
				const hostBpmnId = key.substring(0, sep);
				const side       = key.substring(sep + 1);
				const hostShape  = this._shapes.get(hostBpmnId);
				if (!hostShape) continue;
				// Members currently on this (host, side) -- includes the
				// dragged boundary if it ended up here.
				const members = this._attachedShapesFor(hostBpmnId)
					.filter(a => this._sideOfPointOnHost(hostShape,
						a.shape.boundsX + a.shape.boundsWidth  / 2,
						a.shape.boundsY + a.shape.boundsHeight / 2) === side)
					.map(a => ({ shape: a.shape }));
				if (members.length < 2) continue;  // single member: midpoint already correct
				// Snapshot every sibling we're about to mutate so undo
				// can restore them. The dragged boundary's own bounds
				// are tracked separately via `before` / `after`.
				for (const m of members) {
					if (m.shape !== sh) ensureSibSnap(m.shape);
				}
				this._redistributeBoundariesOnSide(hostShape, side, members);
			}
		}

		// `after` and `edgeChanges` MUST be captured AFTER redistribution:
		// the dragged boundary's slot may have shifted to fit the spread,
		// and its incident edges have been translated accordingly.
		// Translate the dragged boundary's incident edges by the final
		// delta from the pointerdown snapshot (idempotent re-translate).
		if (ds.isBoundary) {
			const dxFinal = sh.boundsX - ds.boundsX0;
			const dyFinal = sh.boundsY - ds.boundsY0;
			this._translateIncidentEdgeEndpoints(sh, ds.edgesSnap, dxFinal, dyFinal);
		}
		const after  = { boundsX: sh.boundsX,  boundsY: sh.boundsY };
		const edgeChanges = ds.edgesSnap.map(s => ({ edge: s.edge, before: s.waypointsBefore, after: s.edge.waypoints }));

		// Build sibling-shape changes (redistribution movement) plus
		// translate each sibling's incident edges by its own delta so
		// they follow the new slot. Skip siblings that didn't actually
		// move (covers same-host re-snap to the same slot).
		const siblingShapeChanges = [];
		for (const [, snap] of siblingSnaps) {
			const dxs = snap.shape.boundsX - snap.boundsX0;
			const dys = snap.shape.boundsY - snap.boundsY0;
			if (dxs === 0 && dys === 0) continue;
			this._translateIncidentEdgeEndpoints(snap.shape, snap.edgesSnap, dxs, dys);
			siblingShapeChanges.push({
				shape:       snap.shape,
				before:      { boundsX: snap.boundsX0,        boundsY: snap.boundsY0 },
				after:       { boundsX: snap.shape.boundsX,   boundsY: snap.shape.boundsY },
				edgeChanges: snap.edgesSnap.map(es => ({ edge: es.edge, before: es.waypointsBefore, after: es.edge.waypoints })),
			});
		}
		// Attached boundary events: collect before/after for each one's
		// shape bounds and its incident edges, so the same undo entry
		// covers the whole "host + boundary children" move.
		const attachedChanges = (ds.attachedSnap || []).map(att => ({
			shape:       att.shape,
			before:      { boundsX: att.boundsX0,         boundsY: att.boundsY0 },
			after:       { boundsX: att.shape.boundsX,    boundsY: att.shape.boundsY },
			edgeChanges: att.edgesSnap.map(s => ({ edge: s.edge, before: s.waypointsBefore, after: s.edge.waypoints })),
		}));

		// Boundary re-attach: detect a change in attachedTo and stage a
		// corresponding typed-relationship swap on the boundary element.
		// Buffer-write sends the host UUID (REST translates to a graph
		// edge); the in-memory stub is mirrored as `{id, bpmnId}` so the
		// editor's _attachedToBpmnIdOf can resolve without a /ui round
		// trip. Apply / revert closures swap the stub + cached bpmnId.
		let attachedRefChange = null;
		if (ds.isBoundary && ds.elementId
				&& ds.currentHostBpmnId && ds.originalHostBpmnId
				&& ds.currentHostBpmnId !== ds.originalHostBpmnId) {
			const boundaryEl    = this._elements.get(ds.elementId);
			const oldHostEl     = this._elementByBpmnId(ds.originalHostBpmnId);
			const newHostEl     = this._elementByBpmnId(ds.currentHostBpmnId);
			attachedRefChange = {
				elementId:  ds.elementId,
				element:    boundaryEl,
				beforeStub: oldHostEl ? { id: oldHostEl.id, bpmnId: oldHostEl.bpmnId } : null,
				afterStub:  newHostEl ? { id: newHostEl.id, bpmnId: newHostEl.bpmnId } : null,
				beforeId:   oldHostEl?.id ?? null,
				afterId:    newHostEl?.id ?? null,
				oldHost:    ds.originalHostBpmnId,
				newHost:    ds.currentHostBpmnId,
			};
		}

		if (sh.id) this._bufferUpdate(sh.id, after);
		for (const ec of edgeChanges) {
			if (ec.edge.id && ec.before !== ec.after) {
				this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
			}
		}
		for (const ac of attachedChanges) {
			if (ac.shape.id) this._bufferUpdate(ac.shape.id, ac.after);
			for (const ec of ac.edgeChanges) {
				if (ec.edge.id && ec.before !== ec.after) {
					this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
				}
			}
		}
		// Boundary-redistribution sibling moves: same buffer-write + undo
		// pattern as attachedChanges, so all of host + dragged + siblings
		// commit and revert as one operation.
		for (const sc of siblingShapeChanges) {
			if (sc.shape.id) this._bufferUpdate(sc.shape.id, sc.after);
			for (const ec of sc.edgeChanges) {
				if (ec.edge.id && ec.before !== ec.after) {
					this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
				}
			}
		}
		if (attachedRefChange) {
			// Buffer-update the typed `attachedTo` relationship. REST takes
			// a UUID string for an EndNode property and creates / replaces
			// the graph edge atomically. Also patches the in-memory stub
			// so the next _attachedToBpmnIdOf reads the new host without
			// touching the network.
			if (attachedRefChange.element) {
				attachedRefChange.element.attachedTo = attachedRefChange.afterStub;
				attachedRefChange.element._cachedAttachedToRef = attachedRefChange.newHost;
			}
			this._bufferUpdate(attachedRefChange.elementId, { attachedTo: attachedRefChange.afterId });
		}

		// Lane reassignment: if the element's centre now sits inside a
		// different lane's bounds, update the element's `lane` property.
		// BPMN allows at most one lane per element, so the OneToMany
		// cardinality cleanly swaps any old assignment when written.
		const element = ds.elementId ? this._elements.get(ds.elementId) : null;
		const oldLaneId = element?.lane?.id ?? element?.lane ?? null;
		const newLaneId = element ? this._laneIdAtCentre(sh) : null;
		const laneChanged = (newLaneId !== oldLaneId);
		if (laneChanged && element) {
			// Mutate in-memory model (lane stub) so subsequent reads see it.
			element.lane = newLaneId ? { id: newLaneId } : null;
			this._bufferUpdate(ds.elementId, { lane: newLaneId });
		}

		this._pushUndo({
			apply: () => {
				Object.assign(sh, after);
				for (const ec of edgeChanges) ec.edge.waypoints = ec.after;
				if (sh.id) this._bufferUpdate(sh.id, after);
				for (const ec of edgeChanges) if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
				// Re-apply attached boundary moves in lockstep with the host.
				for (const ac of attachedChanges) {
					Object.assign(ac.shape, ac.after);
					if (ac.shape.id) this._bufferUpdate(ac.shape.id, ac.after);
					for (const ec of ac.edgeChanges) {
						ec.edge.waypoints = ec.after;
						if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
					}
				}
				// Re-apply boundary slot redistribution.
				for (const sc of siblingShapeChanges) {
					Object.assign(sc.shape, sc.after);
					if (sc.shape.id) this._bufferUpdate(sc.shape.id, sc.after);
					for (const ec of sc.edgeChanges) {
						ec.edge.waypoints = ec.after;
						if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
					}
				}
				if (attachedRefChange) {
					if (attachedRefChange.element) {
						attachedRefChange.element.attachedTo = attachedRefChange.afterStub;
						attachedRefChange.element._cachedAttachedToRef = attachedRefChange.newHost;
					}
					this._bufferUpdate(attachedRefChange.elementId, { attachedTo: attachedRefChange.afterId });
				}
				if (laneChanged && element) {
					element.lane = newLaneId ? { id: newLaneId } : null;
					this._bufferUpdate(ds.elementId, { lane: newLaneId });
				}
				this.refresh();
				for (const h of this._updateHandlers) { try { h(ds.elementId, 'BpmnElement', after); } catch (e) { console.error(e); } }
			},
			revert: () => {
				Object.assign(sh, before);
				for (const ec of edgeChanges) ec.edge.waypoints = ec.before;
				if (sh.id) this._bufferUpdate(sh.id, before);
				for (const ec of edgeChanges) if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.before });
				// Revert attached boundary moves in lockstep with the host.
				for (const ac of attachedChanges) {
					Object.assign(ac.shape, ac.before);
					if (ac.shape.id) this._bufferUpdate(ac.shape.id, ac.before);
					for (const ec of ac.edgeChanges) {
						ec.edge.waypoints = ec.before;
						if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.before });
					}
				}
				// Revert boundary slot redistribution.
				for (const sc of siblingShapeChanges) {
					Object.assign(sc.shape, sc.before);
					if (sc.shape.id) this._bufferUpdate(sc.shape.id, sc.before);
					for (const ec of sc.edgeChanges) {
						ec.edge.waypoints = ec.before;
						if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.before });
					}
				}
				if (attachedRefChange) {
					if (attachedRefChange.element) {
						attachedRefChange.element.attachedTo = attachedRefChange.beforeStub;
						attachedRefChange.element._cachedAttachedToRef = attachedRefChange.oldHost;
					}
					this._bufferUpdate(attachedRefChange.elementId, { attachedTo: attachedRefChange.beforeId });
				}
				if (laneChanged && element) {
					element.lane = oldLaneId ? { id: oldLaneId } : null;
					this._bufferUpdate(ds.elementId, { lane: oldLaneId });
				}
				this.refresh();
				for (const h of this._updateHandlers) { try { h(ds.elementId, 'BpmnElement', before); } catch (e) { console.error(e); } }
			},
		});
		for (const h of this._elementMoveHandlers) {
			try { h(ds.elementId, sh.boundsX, sh.boundsY); }
			catch (err) { console.error(err); }
		}
		for (const h of this._updateHandlers) { try { h(ds.elementId, 'BpmnElement', after); } catch (e) { console.error(e); } }
	}

	/**
	 * Find the BpmnLane (if any) whose DI shape contains the given shape's
	 * centre point. Returns the lane node UUID, or null when the centre
	 * isn't inside any lane (single-pool processes, or dragged outside
	 * all bands). When lanes overlap (rare authoring), the smallest-area
	 * one wins so nested lane sets resolve to the innermost band.
	 */
	_laneIdAtCentre(shape) {
		const cx = shape.boundsX + shape.boundsWidth  / 2;
		const cy = shape.boundsY + shape.boundsHeight / 2;
		let best = null;
		let bestArea = Infinity;
		for (const [, sh] of this._shapes) {
			const lane = this._lanesByBpmnId.get(sh.bpmnElementRef);
			if (!lane) continue;
			if (cx < sh.boundsX || cx > sh.boundsX + sh.boundsWidth)  continue;
			if (cy < sh.boundsY || cy > sh.boundsY + sh.boundsHeight) continue;
			const area = sh.boundsWidth * sh.boundsHeight;
			if (area < bestArea) { bestArea = area; best = lane.id; }
		}
		return best;
	}

	/**
	 * Snapshot the current waypoints of every BpmnDiEdge incident to the
	 * given shape -- whether it represents a sequence flow OR a message
	 * flow. Returned as `[{edge, waypointsBefore}, ...]` so dragend can
	 * compute before/after pairs for the undo stack and the buffer.
	 */
	_snapshotIncidentEdges(shape) {
		const out = [];
		const elemBpmnId = shape?.bpmnElementRef;
		if (!elemBpmnId) return out;
		for (const [, ed] of this._edges) {
			const seq  = this._findFlowByBpmnId(ed.bpmnElementRef);
			const msg  = !seq ? this._messageFlowsByBpmnId.get(ed.bpmnElementRef) : null;
			const flow = seq || msg;
			if (!flow) continue;
			if (flow.sourceRefId === elemBpmnId || flow.targetRefId === elemBpmnId) {
				out.push({ edge: ed, waypointsBefore: ed.waypoints });
			}
		}
		return out;
	}

	/**
	 * Cluster shapes that are "on the same horizontal line" and snap each
	 * cluster's members to a common centre y (the cluster's mode of
	 * grid-rounded centres). Pulls events / gateways / off-axis tasks
	 * onto their connected neighbours' line.
	 *
	 * "Same line" rule (this is the trigger for clustering two shapes
	 * connected by a sequence flow):
	 *
	 *   1. They must be in the SAME LANE (or both have no lane). Lanes
	 *      define the canonical horizontal grouping in BPMN; flows
	 *      crossing lanes go to a different visual row by design.
	 *
	 *   2. Their bounding-box y-ranges must OVERLAP by more than
	 *      MIN_Y_OVERLAP px. Default 0 (any overlap counts). Tunable:
	 *      raise to require a stricter "same row" threshold (e.g.,
	 *      `min(h_a, h_b) / 2` to demand half-shape overlap).
	 *
	 * Pools, lanes, self-loops, and messageFlows are excluded from
	 * cluster building.
	 *
	 * The function only mutates the in-memory shape and edge state.
	 * Buffer / undo handling is the caller's responsibility (Tidy
	 * snapshots before, calls this, then derives the diff). Returns
	 * the count of shapes that shifted.
	 */
	_autoAlignHorizontalAxes() {
		// Tunable threshold: minimum required y-overlap as a fraction
		// of the SMALLER shape's height. 0.5 = "shapes must share at
		// least half of the smaller shape's vertical extent". This
		// scales naturally across event/task/gateway sizes and prevents
		// transitive clustering through a junction shape (e.g. a gateway
		// centred between two branches has ~30 px overlap with each
		// branch but min/2 ~= 40 px, so neither branch clusters with
		// the gateway and the branches don't transitively merge).
		//   0    = any overlap counts (most permissive).
		//   0.5  = half the smaller shape (current default).
		//   1.0  = smaller shape fully contained within larger.
		const MIN_OVERLAP_FRACTION = 0.5;
		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		const moved = new Set();   // bpmnIds of shapes whose y shifted

		// Union-find via parent map.
		const parent = new Map();
		const find = (id) => {
			if (!parent.has(id)) parent.set(id, id);
			let p = parent.get(id);
			while (p !== id) {
				const gp = parent.get(p);
				if (gp === undefined) break;
				parent.set(id, gp);
				id = p;
				p = gp;
			}
			return p;
		};
		const union = (a, b) => {
			const pa = find(a), pb = find(b);
			if (pa !== pb) parent.set(pa, pb);
		};

		for (const [, flow] of this._flows) {
			const sBpmnId = flow.sourceRefId, tBpmnId = flow.targetRefId;
			if (!sBpmnId || !tBpmnId || sBpmnId === tBpmnId) continue;
			if (this._participantsByBpmnId.has(sBpmnId) || this._participantsByBpmnId.has(tBpmnId)) continue;
			if (this._lanesByBpmnId.has(sBpmnId)        || this._lanesByBpmnId.has(tBpmnId))        continue;
			const sShape = this._shapes.get(sBpmnId);
			const tShape = this._shapes.get(tBpmnId);
			if (!sShape || !tShape) continue;

			// Lane constraint: shapes in different lanes don't cluster.
			// Both null (no lane) is treated as "same lane" -- pools
			// without a laneSet act as one group.
			const sElemId = this._findElementIdByBpmnId(sBpmnId);
			const tElemId = this._findElementIdByBpmnId(tBpmnId);
			const sElem = sElemId ? this._elements.get(sElemId) : null;
			const tElem = tElemId ? this._elements.get(tElemId) : null;
			const sLane = sElem?.lane?.id ?? null;
			const tLane = tElem?.lane?.id ?? null;
			if (sLane !== tLane) continue;

			// Y-range overlap scaled by smaller height. Strictly greater
			// than the threshold, so adjacent rows that just touch
			// (overlap = 0) don't cluster, and neither do the two
			// branches off a centred gateway (overlap < min(h)/2).
			const sBottom = sShape.boundsY + sShape.boundsHeight;
			const tBottom = tShape.boundsY + tShape.boundsHeight;
			const overlap = Math.min(sBottom, tBottom) - Math.max(sShape.boundsY, tShape.boundsY);
			const minH    = Math.min(sShape.boundsHeight, tShape.boundsHeight);
			if (overlap <= MIN_OVERLAP_FRACTION * minH) continue;

			union(sBpmnId, tBpmnId);
		}

		// Group cluster members by their root.
		const clusters = new Map();
		for (const id of parent.keys()) {
			const root = find(id);
			if (!clusters.has(root)) clusters.set(root, new Set());
			clusters.get(root).add(id);
		}

		let endpointFixes = 0;
		for (const members of clusters.values()) {
			if (members.size < 2) continue;
			// Pick the cluster's TARGET y as the grid-rounded MODE of
			// member centres, not the median. The mode preserves the
			// "majority axis" exactly and pulls outliers onto it; median
			// could land halfway between two evenly-split groups, leaving
			// nobody on a clean axis after grid rounding. Ties break by
			// the value closest to the cluster's mean (avoids snapping
			// to a far-away minority).
			const counts = new Map();
			let sum = 0, n = 0;
			for (const bpmnId of members) {
				const sh = this._shapes.get(bpmnId);
				if (!sh) continue;
				const cyR = Math.round((sh.boundsY + sh.boundsHeight / 2) / step) * step;
				counts.set(cyR, (counts.get(cyR) || 0) + 1);
				sum += sh.boundsY + sh.boundsHeight / 2;
				n++;
			}
			if (n === 0) continue;
			const meanCy = sum / n;
			let targetCy = null, bestCount = -1, bestDist = Infinity;
			for (const [y, c] of counts) {
				const dist = Math.abs(y - meanCy);
				if (c > bestCount || (c === bestCount && dist < bestDist)) {
					bestCount = c;
					bestDist  = dist;
					targetCy  = y;
				}
			}

			for (const bpmnId of members) {
				const sh = this._shapes.get(bpmnId);
				if (!sh) continue;
				const cy = sh.boundsY + sh.boundsHeight / 2;
				const dy = targetCy - cy;
				if (Math.abs(dy) < 0.5) continue;

				// Snapshot incident edges first so the translate helper
				// can reproduce the move on each waypoint -- otherwise
				// shapes processed earlier in the cluster would have
				// already shifted their endpoints relative to this one.
				const edgesSnap = this._snapshotIncidentEdges(sh);
				sh.boundsY += dy;
				this._translateIncidentEdgeEndpoints(sh, edgesSnap, 0, dy);
				if (sh.bpmnElementRef) moved.add(sh.bpmnElementRef);
			}

			// Per-shape translation handles edges that exit the cluster, but
			// edges INSIDE the cluster need a second pass: the source-side
			// translate fires when the source moves, but if the target's dy
			// is 0 (already on the target axis) the target-side endpoint
			// keeps the *original* y the source authored. The line ends up
			// diagonal between two shapes that both sit on the cluster axis.
			//
			// Snap both endpoints of every within-cluster edge onto target_cy.
			// Adjacent bends that were axis-aligned with the old endpoint
			// follow on the y axis so multi-bend orthogonal routes stay
			// orthogonal. Middle bends are left alone -- the author's
			// authored shape persists.
			for (const [, ed] of this._edges) {
				const seq = this._findFlowByBpmnId(ed.bpmnElementRef);
				const msg = !seq ? this._messageFlowsByBpmnId.get(ed.bpmnElementRef) : null;
				const flow = seq || msg;
				if (!flow) continue;
				if (!members.has(flow.sourceRefId) || !members.has(flow.targetRefId)) continue;

				const wps = this._parseWaypoints(ed);
				if (wps.length < 2) continue;
				let changed = false;

				const oldStartY = wps[0].y;
				if (Math.abs(oldStartY - targetCy) > 0.5) {
					wps[0].y = targetCy;
					if (wps.length > 2) {
						const next = wps[1];
						if (Math.abs(next.y - oldStartY) < 0.5) next.y = targetCy;
					}
					changed = true;
				}
				const last = wps.length - 1;
				const oldEndY = wps[last].y;
				if (Math.abs(oldEndY - targetCy) > 0.5) {
					wps[last].y = targetCy;
					if (wps.length > 2) {
						const prev = wps[last - 1];
						if (Math.abs(prev.y - oldEndY) < 0.5) prev.y = targetCy;
					}
					changed = true;
				}

				if (changed) {
					ed.waypoints = JSON.stringify(wps);
					endpointFixes++;
				}
			}
		}
		// Caller (Tidy) folds the moved set into its own tracker so that
		// _refreshIncidentEdges + redistribute can run on the merged
		// post-cluster state.
		return { moved, endpointFixes };
	}

	/**
	 * Vertical-axis counterpart of _autoAlignHorizontalAxes. Two shapes
	 * connected by ANY flow (sequence or message) cluster if their
	 * x bbox ranges overlap. Snaps cluster members to a common centre
	 * x (mode of grid-rounded centres) and pulls within-cluster edge
	 * endpoints onto that x axis.
	 *
	 * Differences from horizontal:
	 *   * Both flow types participate -- vertical alignment is the
	 *     natural rule for cross-pool messageFlows (which by definition
	 *     run mostly vertical between pools).
	 *   * No lane constraint -- vertical chains commonly cross lanes
	 *     (a sequence flow going down through multiple bands inside a
	 *     pool is normal).
	 *
	 * Caller (Tidy) handles buffer / undo. Returns { moved, endpointFixes }.
	 */
	_autoAlignVerticalAxes() {
		// Mirrors _autoAlignHorizontalAxes: minimum x-overlap as a
		// fraction of the SMALLER shape's width.
		const MIN_OVERLAP_FRACTION = 0.5;
		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		const moved = new Set();

		const parent = new Map();
		const find = (id) => {
			if (!parent.has(id)) parent.set(id, id);
			let p = parent.get(id);
			while (p !== id) {
				const gp = parent.get(p);
				if (gp === undefined) break;
				parent.set(id, gp);
				id = p;
				p = gp;
			}
			return p;
		};
		const union = (a, b) => {
			const pa = find(a), pb = find(b);
			if (pa !== pb) parent.set(pa, pb);
		};

		// Cluster pairs connected by ANY flow with x-overlap.
		const allFlows = [];
		for (const [, f] of this._flows)         allFlows.push(f);
		for (const [, f] of this._messageFlows)  allFlows.push(f);
		for (const flow of allFlows) {
			const sBpmnId = flow.sourceRefId, tBpmnId = flow.targetRefId;
			if (!sBpmnId || !tBpmnId || sBpmnId === tBpmnId) continue;
			if (this._participantsByBpmnId.has(sBpmnId) || this._participantsByBpmnId.has(tBpmnId)) continue;
			if (this._lanesByBpmnId.has(sBpmnId)        || this._lanesByBpmnId.has(tBpmnId))        continue;
			const sShape = this._shapes.get(sBpmnId);
			const tShape = this._shapes.get(tBpmnId);
			if (!sShape || !tShape) continue;

			const sRight = sShape.boundsX + sShape.boundsWidth;
			const tRight = tShape.boundsX + tShape.boundsWidth;
			const overlap = Math.min(sRight, tRight) - Math.max(sShape.boundsX, tShape.boundsX);
			const minW    = Math.min(sShape.boundsWidth, tShape.boundsWidth);
			if (overlap <= MIN_OVERLAP_FRACTION * minW) continue;

			union(sBpmnId, tBpmnId);
		}

		const clusters = new Map();
		for (const id of parent.keys()) {
			const root = find(id);
			if (!clusters.has(root)) clusters.set(root, new Set());
			clusters.get(root).add(id);
		}

		let endpointFixes = 0;
		for (const members of clusters.values()) {
			if (members.size < 2) continue;

			const counts = new Map();
			let sum = 0, n = 0;
			for (const bpmnId of members) {
				const sh = this._shapes.get(bpmnId);
				if (!sh) continue;
				const cxR = Math.round((sh.boundsX + sh.boundsWidth / 2) / step) * step;
				counts.set(cxR, (counts.get(cxR) || 0) + 1);
				sum += sh.boundsX + sh.boundsWidth / 2;
				n++;
			}
			if (n === 0) continue;
			const meanCx = sum / n;
			let targetCx = null, bestCount = -1, bestDist = Infinity;
			for (const [x, c] of counts) {
				const dist = Math.abs(x - meanCx);
				if (c > bestCount || (c === bestCount && dist < bestDist)) {
					bestCount = c;
					bestDist  = dist;
					targetCx  = x;
				}
			}

			for (const bpmnId of members) {
				const sh = this._shapes.get(bpmnId);
				if (!sh) continue;
				const cx = sh.boundsX + sh.boundsWidth / 2;
				const dx = targetCx - cx;
				if (Math.abs(dx) < 0.5) continue;

				const edgesSnap = this._snapshotIncidentEdges(sh);
				sh.boundsX += dx;
				this._translateIncidentEdgeEndpoints(sh, edgesSnap, dx, 0);
				if (sh.bpmnElementRef) moved.add(sh.bpmnElementRef);
			}

			// Within-cluster edge endpoints snap to target_cx.
			for (const [, ed] of this._edges) {
				const seq = this._findFlowByBpmnId(ed.bpmnElementRef);
				const msg = !seq ? this._messageFlowsByBpmnId.get(ed.bpmnElementRef) : null;
				const flow = seq || msg;
				if (!flow) continue;
				if (!members.has(flow.sourceRefId) || !members.has(flow.targetRefId)) continue;

				const wps = this._parseWaypoints(ed);
				if (wps.length < 2) continue;
				let changed = false;

				const oldStartX = wps[0].x;
				if (Math.abs(oldStartX - targetCx) > 0.5) {
					wps[0].x = targetCx;
					if (wps.length > 2) {
						const next = wps[1];
						if (Math.abs(next.x - oldStartX) < 0.5) next.x = targetCx;
					}
					changed = true;
				}
				const last = wps.length - 1;
				const oldEndX = wps[last].x;
				if (Math.abs(oldEndX - targetCx) > 0.5) {
					wps[last].x = targetCx;
					if (wps.length > 2) {
						const prev = wps[last - 1];
						if (Math.abs(prev.x - oldEndX) < 0.5) prev.x = targetCx;
					}
					changed = true;
				}

				if (changed) {
					ed.waypoints = JSON.stringify(wps);
					endpointFixes++;
				}
			}
		}

		return { moved, endpointFixes };
	}

	/**
	 * Remove redundant interior waypoints from every edge: drop a waypoint
	 * if it duplicates the previous KEPT waypoint, or if it sits on the
	 * straight line between the previous KEPT waypoint and the next
	 * waypoint (perpendicular distance under 1 px). First and last
	 * waypoints are always kept: they're the snapped endpoints.
	 *
	 * Run after the axis-cluster passes during Tidy. Cluster passes pull
	 * shapes onto common axes, which often makes pre-existing zig-zags
	 * unnecessary, and the endpoint-translation step can leave a handful
	 * of collinear interior bends behind. Pruning produces visibly
	 * cleaner straight runs without changing the topology of any path.
	 *
	 * Returns the number of waypoints removed. Caller is responsible for
	 * snapshotting edge.waypoints into the Tidy undo entry; we only
	 * mutate `edge.waypoints` in place.
	 */
	_pruneRedundantBends() {
		const EPS_DUP   = 0.5;   // 0.5 px counts as duplicate
		const EPS_COLIN = 1.0;   // 1.0 px perpendicular = collinear
		let pruned = 0;
		for (const [, ed] of this._edges) {
			const wps = this._parseWaypoints(ed);
			if (wps.length < 3) continue;

			const cleaned = [wps[0]];
			for (let i = 1; i < wps.length - 1; i++) {
				const curr = wps[i];
				const prev = cleaned[cleaned.length - 1];
				const next = wps[i + 1];

				// Duplicate of the previous kept waypoint.
				if (Math.abs(curr.x - prev.x) < EPS_DUP && Math.abs(curr.y - prev.y) < EPS_DUP) {
					pruned++;
					continue;
				}
				// Collinear with prev / next: perpendicular distance from
				// curr to the line through prev,next.
				const dx = next.x - prev.x;
				const dy = next.y - prev.y;
				const segLen = Math.hypot(dx, dy);
				if (segLen < EPS_DUP) {
					// prev and next coincide: curr is redundant unless it
					// also coincides (already handled above).
					pruned++;
					continue;
				}
				const cross = Math.abs((curr.x - prev.x) * dy - (curr.y - prev.y) * dx);
				const perpDist = cross / segLen;
				if (perpDist < EPS_COLIN) {
					pruned++;
					continue;
				}
				cleaned.push(curr);
			}
			cleaned.push(wps[wps.length - 1]);

			if (cleaned.length !== wps.length) {
				ed.waypoints = JSON.stringify(cleaned);
			}
		}
		return pruned;
	}

	/**
	 * For every shape with 2 or more incident edge endpoints on the same
	 * side, spread them into evenly-spaced slots along that side. Sides
	 * with a single edge are left alone -- the cluster-align and cardinal-
	 * snap passes own those.
	 *
	 * Side classification uses the endpoint's CURRENT position (which
	 * side of the bbox it sits closest to), not the direction to the
	 * other endpoint. That keeps authored side choices intact: an edge
	 * that exits the left side stays on the left side, even if the
	 * direction to its target would technically put it on the bottom.
	 *
	 * Slot order minimises crossings: within a side, edges are sorted by
	 * the OTHER endpoint's coord along the side axis (otherEnd.x for
	 * top/bottom, otherEnd.y for left/right) so leftmost-target gets the
	 * leftmost slot. Both sequence flows and message flows participate.
	 *
	 * Returns the count of endpoints actually moved. Self-loops, pools,
	 * and lanes are skipped.
	 */
	_autoSpreadParallelEndpoints() {
		const SIDE_TOL = 1.5;        // px tolerance for "is on this side"
		const step = _BpmnDiagramCommunitySvg.GRID_STEP;
		const snap = (v) => Math.round(v / step) * step;

		// Build a per-shape list of incident edges (same as Tidy's redistribute).
		const incidentByBpmnId = new Map();
		for (const [, ed] of this._edges) {
			const seq = this._findFlowByBpmnId(ed.bpmnElementRef);
			const msg = !seq ? this._messageFlowsByBpmnId.get(ed.bpmnElementRef) : null;
			const flow = seq || msg;
			if (!flow) continue;
			const src = flow.sourceRefId, tgt = flow.targetRefId;
			if (!src || !tgt || src === tgt) continue;
			if (!incidentByBpmnId.has(src)) incidentByBpmnId.set(src, []);
			if (!incidentByBpmnId.has(tgt)) incidentByBpmnId.set(tgt, []);
			incidentByBpmnId.get(src).push({ edge: ed, isSource: true });
			incidentByBpmnId.get(tgt).push({ edge: ed, isSource: false });
		}

		let moved = 0;
		for (const [bpmnId, list] of incidentByBpmnId) {
			const shape = this._shapes.get(bpmnId);
			if (!shape) continue;
			if (this._participantsByBpmnId.has(bpmnId)) continue;
			if (this._lanesByBpmnId.has(bpmnId))        continue;

			const x1 = shape.boundsX,                 y1 = shape.boundsY;
			const x2 = x1 + shape.boundsWidth,        y2 = y1 + shape.boundsHeight;

			// Bucket by CURRENT closest side. Tolerance lets endpoints
			// authored a px or two off the border still register as on
			// that side; far-off endpoints get whichever side is closest.
			const sides = { top: [], bottom: [], left: [], right: [] };
			for (const inc of list) {
				const wps = this._parseWaypoints(inc.edge);
				if (wps.length < 2) continue;
				const idx = inc.isSource ? 0 : (wps.length - 1);
				const ep  = wps[idx];

				const dT = Math.abs(ep.y - y1);
				const dB = Math.abs(ep.y - y2);
				const dL = Math.abs(ep.x - x1);
				const dR = Math.abs(ep.x - x2);
				const minD = Math.min(dT, dB, dL, dR);
				let side = 'top';
				if (minD === dB) side = 'bottom';
				else if (minD === dL) side = 'left';
				else if (minD === dR) side = 'right';

				const otherEnd = inc.isSource ? wps[wps.length - 1] : wps[0];
				const anchor = (side === 'top' || side === 'bottom') ? otherEnd.x : otherEnd.y;
				sides[side].push({ ...inc, otherEnd, anchor, currentEp: ep });
			}

			for (const [side, items] of Object.entries(sides)) {
				if (items.length < 2) continue;     // single edge: leave it where it is
				items.sort((a, b) => a.anchor - b.anchor);
				const n = items.length;
				for (let i = 0; i < n; i++) {
					const item = items[i];
					const t    = (i + 0.5) / n;
					const newPt = this._pointAtSlot(shape, side, t);
					newPt.x = snap(newPt.x);
					newPt.y = snap(newPt.y);
					if (Math.abs(newPt.x - item.currentEp.x) < 0.5
						&& Math.abs(newPt.y - item.currentEp.y) < 0.5) continue;

					const wps = this._parseWaypoints(item.edge);
					if (wps.length < 2) continue;
					const idx = item.isSource ? 0 : (wps.length - 1);
					const old = wps[idx];

					if (wps.length > 2) {
						const adjIdx = item.isSource ? 1 : (wps.length - 2);
						const adj = wps[adjIdx];
						if (Math.abs(adj.x - old.x) < 0.5) adj.x = newPt.x;
						if (Math.abs(adj.y - old.y) < 0.5) adj.y = newPt.y;
					}
					wps[idx] = newPt;
					item.edge.waypoints = JSON.stringify(wps);
					if (item.edge.id) this._bufferUpdate(item.edge.id, { waypoints: item.edge.waypoints });
					moved++;
				}
			}
		}
		return moved;
	}

	/**
	 * Snap incident edge endpoints onto cardinal vertices on events and
	 * gateways. Each edge's exit/entry side is picked by the DIRECTION
	 * to its other endpoint, NOT by where the source authored the
	 * endpoint -- so a clearly right-bound arrow goes to the right
	 * vertex even if the source put it slightly above the top vertex.
	 *
	 * Multiple edges may share the same cardinal vertex (BPMN gateways
	 * conventionally have 4 ports; "all arrows going right exit from
	 * the right port, all arrows going down exit from the bottom port"
	 * is the standard authoring model). We do NOT re-assign overflow to
	 * different sides -- if 3 edges all exit right, they stack at the
	 * right vertex.
	 *
	 * Adjacent bends that were axis-aligned with the old endpoint shift
	 * along the matching axis so orthogonal segments stay orthogonal.
	 */
	_autoSnapCardinalEndpoints() {
		let snapped = 0;
		for (const [, sh] of this._shapes) {
			const elementId = this._findElementIdByBpmnId(sh.bpmnElementRef);
			if (!elementId) continue;
			const element = this._elements.get(elementId);
			const type = element?.bpmnElementType ?? '';
			const isCardinal = type.endsWith('Gateway')
				|| type.endsWith('Event')
				|| type === 'startEvent'
				|| type === 'endEvent';
			if (!isCardinal) continue;

			const cx = sh.boundsX + sh.boundsWidth  / 2;
			const cy = sh.boundsY + sh.boundsHeight / 2;
			const x1 = sh.boundsX, x2 = sh.boundsX + sh.boundsWidth;
			const y1 = sh.boundsY, y2 = sh.boundsY + sh.boundsHeight;
			const cardinalForSide = {
				top:    { x: cx, y: y1 },
				right:  { x: x2, y: cy },
				bottom: { x: cx, y: y2 },
				left:   { x: x1, y: cy },
			};

			for (const [, ed] of this._edges) {
				const seq  = this._findFlowByBpmnId(ed.bpmnElementRef);
				const msg  = !seq ? this._messageFlowsByBpmnId.get(ed.bpmnElementRef) : null;
				const flow = seq || msg;
				if (!flow) continue;
				const isSource = flow.sourceRefId === sh.bpmnElementRef;
				const isTarget = flow.targetRefId === sh.bpmnElementRef;
				if (!isSource && !isTarget) continue;

				const wps = this._parseWaypoints(ed);
				if (wps.length < 2) continue;
				const idx = isSource ? 0 : (wps.length - 1);
				const old = wps[idx];

				// Direction-based side: where SHOULD the edge exit, given
				// the other endpoint's position relative to this shape's
				// centre? This overrides where the source authored it,
				// because BPMN convention is "exit on the side facing the
				// neighbour" and authoring tools sometimes get this wrong.
				const otherEnd = isSource ? wps[wps.length - 1] : wps[0];
				const dx = otherEnd.x - cx;
				const dy = otherEnd.y - cy;
				let side;
				if (Math.abs(dx) >= Math.abs(dy)) {
					side = dx > 0 ? 'right'  : 'left';
				} else {
					side = dy > 0 ? 'bottom' : 'top';
				}
				const target = cardinalForSide[side];

				if (Math.abs(old.x - target.x) < 0.5
					&& Math.abs(old.y - target.y) < 0.5) continue;

				if (wps.length > 2) {
					const adjIdx = isSource ? 1 : (wps.length - 2);
					const adj = wps[adjIdx];
					if (Math.abs(adj.x - old.x) < 0.5) adj.x = target.x;
					if (Math.abs(adj.y - old.y) < 0.5) adj.y = target.y;
				}
				wps[idx] = { x: target.x, y: target.y };
				ed.waypoints = JSON.stringify(wps);
				if (ed.id) this._bufferUpdate(ed.id, { waypoints: ed.waypoints });
				snapped++;
			}
		}
		return snapped;
	}

	/**
	 * Translate the endpoint(s) of every snapshotted edge incident to
	 * `shape` by the same (dx, dy) the shape moved. Preserves each
	 * endpoint's relative offset along its shape side -- so two parallel
	 * messageFlows between the same shapes (e.g. money / receipt that
	 * sit at slightly different x) stay visibly separate during a drag,
	 * instead of collapsing onto the side midpoint.
	 *
	 * Always reads from the snapshot (taken at pointerdown), never from
	 * the live edge state -- so each move tick is idempotent regardless
	 * of how many ticks have fired before.
	 *
	 * Self-loops (shape is both source and target) translate every
	 * waypoint by the delta. Adjacent bends that were axis-aligned with
	 * the moving endpoint shift along the matching axis so orthogonal
	 * segments stay orthogonal.
	 */
	_translateIncidentEdgeEndpoints(shape, edgesSnap, dx, dy) {
		if (!shape || !edgesSnap) return;
		const elemBpmnId = shape.bpmnElementRef;
		if (!elemBpmnId) return;
		for (const snap of edgesSnap) {
			const ed   = snap.edge;
			const wpsBefore = this._parseWaypoints({ waypoints: snap.waypointsBefore });
			if (wpsBefore.length < 2) continue;
			const seq  = this._findFlowByBpmnId(ed.bpmnElementRef);
			const msg  = !seq ? this._messageFlowsByBpmnId.get(ed.bpmnElementRef) : null;
			const flow = seq || msg;
			if (!flow) continue;
			const isSource = flow.sourceRefId === elemBpmnId;
			const isTarget = flow.targetRefId === elemBpmnId;
			if (!isSource && !isTarget) continue;

			// Self-loop: translate all waypoints by the delta.
			if (isSource && isTarget) {
				const wps = wpsBefore.map(w => ({ x: w.x + dx, y: w.y + dy }));
				ed.waypoints = JSON.stringify(wps);
				continue;
			}

			const wps = wpsBefore.map(w => ({ x: w.x, y: w.y }));
			if (isSource) {
				const old = wpsBefore[0];
				wps[0] = { x: old.x + dx, y: old.y + dy };
				if (wps.length > 2) {
					const adj = wpsBefore[1];
					if (Math.abs(adj.x - old.x) < 0.5) wps[1].x = adj.x + dx;
					if (Math.abs(adj.y - old.y) < 0.5) wps[1].y = adj.y + dy;
				}
			}
			if (isTarget) {
				const last = wpsBefore.length - 1;
				const old  = wpsBefore[last];
				wps[last]  = { x: old.x + dx, y: old.y + dy };
				if (wps.length > 2) {
					const adj = wpsBefore[last - 1];
					if (Math.abs(adj.x - old.x) < 0.5) wps[last - 1].x = adj.x + dx;
					if (Math.abs(adj.y - old.y) < 0.5) wps[last - 1].y = adj.y + dy;
				}
			}
			ed.waypoints = JSON.stringify(wps);
		}
	}

	/**
	 * Project the endpoints of every snapshotted edge incident to `shape`
	 * from `oldBounds` onto the shape's current bounds, preserving each
	 * endpoint's side AND its parametric position along that side.
	 *
	 * Used by resize: the shape's size changes but the endpoint should
	 * stay anchored at the same fractional offset on the same side --
	 * so two parallel edges that exited at e.g. 30% and 70% along the
	 * bottom side stay distinct after the resize, instead of collapsing
	 * to the new bottom midpoint.
	 *
	 * `oldBounds = { x, y, w, h }`. Endpoints that fall on no recognisable
	 * side (within 1px) fall back to a midpoint snap so we never produce
	 * NaN / disconnected lines.
	 */
	_projectIncidentEdgeEndpoints(shape, edgesSnap, oldBounds) {
		if (!shape || !edgesSnap || !oldBounds) return;
		const elemBpmnId = shape.bpmnElementRef;
		if (!elemBpmnId) return;

		const project = (pt) => {
			// Identify which side of old bounds the endpoint sits on. Use a
			// 1px tolerance for floating-point and authored-rounding slop.
			const eps = 1;
			const oxR = oldBounds.x + oldBounds.w;
			const oyB = oldBounds.y + oldBounds.h;
			const nxR = shape.boundsX + shape.boundsWidth;
			const nyB = shape.boundsY + shape.boundsHeight;
			const onTop    = Math.abs(pt.y - oldBounds.y) <= eps;
			const onBottom = Math.abs(pt.y - oyB)        <= eps;
			const onLeft   = Math.abs(pt.x - oldBounds.x) <= eps;
			const onRight  = Math.abs(pt.x - oxR)        <= eps;
			// Prefer horizontal sides when both vertical and horizontal match
			// (corner case): picks the longer side, which usually matches the
			// authored intent for tasks/events.
			if (onTop) {
				const t = oldBounds.w > 0 ? (pt.x - oldBounds.x) / oldBounds.w : 0.5;
				return { x: shape.boundsX + t * shape.boundsWidth, y: shape.boundsY };
			}
			if (onBottom) {
				const t = oldBounds.w > 0 ? (pt.x - oldBounds.x) / oldBounds.w : 0.5;
				return { x: shape.boundsX + t * shape.boundsWidth, y: nyB };
			}
			if (onLeft) {
				const t = oldBounds.h > 0 ? (pt.y - oldBounds.y) / oldBounds.h : 0.5;
				return { x: shape.boundsX, y: shape.boundsY + t * shape.boundsHeight };
			}
			if (onRight) {
				const t = oldBounds.h > 0 ? (pt.y - oldBounds.y) / oldBounds.h : 0.5;
				return { x: nxR, y: shape.boundsY + t * shape.boundsHeight };
			}
			// Off-side fallback: project to the closest side midpoint of new bounds.
			return this._snapToShapeBorder(shape, pt);
		};

		for (const snap of edgesSnap) {
			const ed = snap.edge;
			const wpsBefore = this._parseWaypoints({ waypoints: snap.waypointsBefore });
			if (wpsBefore.length < 2) continue;
			const seq  = this._findFlowByBpmnId(ed.bpmnElementRef);
			const msg  = !seq ? this._messageFlowsByBpmnId.get(ed.bpmnElementRef) : null;
			const flow = seq || msg;
			if (!flow) continue;
			const isSource = flow.sourceRefId === elemBpmnId;
			const isTarget = flow.targetRefId === elemBpmnId;
			if (!isSource && !isTarget) continue;

			const wps = wpsBefore.map(w => ({ x: w.x, y: w.y }));
			if (isSource) {
				const old = wpsBefore[0];
				const nu  = project(old);
				wps[0] = nu;
				if (wps.length > 2) {
					// Preserve orthogonal alignment with the adjacent bend by
					// dragging it along whichever axis it shared with the old
					// endpoint (matches the move-time logic).
					const adj = wpsBefore[1];
					if (Math.abs(adj.x - old.x) < 0.5) wps[1].x = nu.x;
					if (Math.abs(adj.y - old.y) < 0.5) wps[1].y = nu.y;
				}
			}
			if (isTarget) {
				const last = wpsBefore.length - 1;
				const old  = wpsBefore[last];
				const nu   = project(old);
				wps[last]  = nu;
				if (wps.length > 2) {
					const adj = wpsBefore[last - 1];
					if (Math.abs(adj.x - old.x) < 0.5) wps[last - 1].x = nu.x;
					if (Math.abs(adj.y - old.y) < 0.5) wps[last - 1].y = nu.y;
				}
			}
			ed.waypoints = JSON.stringify(wps);
		}
	}

	/**
	 * Re-project boundary-event centres from the host's old perimeter
	 * onto its new perimeter, preserving the side they sit on AND the
	 * parametric position along that side. Mirrors the edge-endpoint
	 * projection used during host resize, but for boundary shapes
	 * rather than edge endpoints.
	 *
	 * Boundary centres are expected to lie on the host's perimeter.
	 * Side classification picks the closest of the four sides (top /
	 * bottom / left / right) of `oldHostBounds` -- if a boundary has
	 * been authored slightly inside or outside the perimeter, it still
	 * snaps to the most plausible side rather than failing.
	 *
	 * Reads each boundary's pre-resize centre from the snapshot's
	 * `boundsX0` / `boundsY0` and writes the new top-left bounds back
	 * to `att.shape`. Idempotent under repeated calls during a drag
	 * because every read is from the snapshot, never the live shape.
	 */
	_projectAttachedBoundaryCentres(hostShape, oldHostBounds, attachedSnap) {
		if (!hostShape || !oldHostBounds || !attachedSnap || attachedSnap.length === 0) return;
		const ox  = oldHostBounds.x,  oy  = oldHostBounds.y;
		const ow  = oldHostBounds.w,  oh  = oldHostBounds.h;
		const oxR = ox + ow,          oyB = oy + oh;
		const nx  = hostShape.boundsX,     ny  = hostShape.boundsY;
		const nw  = hostShape.boundsWidth, nh  = hostShape.boundsHeight;
		const nxR = nx + nw,               nyB = ny + nh;

		const clamp01 = (v) => Math.min(1, Math.max(0, v));

		for (const att of attachedSnap) {
			const sh = att.shape;
			// Boundary centre BEFORE the host resize started. The boundary's
			// own width / height don't change during a host resize, so they
			// can be read from the live shape.
			const cxOld = att.boundsX0 + sh.boundsWidth  / 2;
			const cyOld = att.boundsY0 + sh.boundsHeight / 2;

			// Closest side of the OLD host. Absolute distance so a boundary
			// authored slightly outside the perimeter still resolves.
			const dTop    = Math.abs(cyOld - oy);
			const dBottom = Math.abs(cyOld - oyB);
			const dLeft   = Math.abs(cxOld - ox);
			const dRight  = Math.abs(cxOld - oxR);
			const minD    = Math.min(dTop, dBottom, dLeft, dRight);

			let cxNew, cyNew;
			if (minD === dTop) {
				const t = ow > 0 ? clamp01((cxOld - ox) / ow) : 0.5;
				cxNew = nx + t * nw;
				cyNew = ny;
			} else if (minD === dBottom) {
				const t = ow > 0 ? clamp01((cxOld - ox) / ow) : 0.5;
				cxNew = nx + t * nw;
				cyNew = nyB;
			} else if (minD === dLeft) {
				const t = oh > 0 ? clamp01((cyOld - oy) / oh) : 0.5;
				cxNew = nx;
				cyNew = ny + t * nh;
			} else {
				const t = oh > 0 ? clamp01((cyOld - oy) / oh) : 0.5;
				cxNew = nxR;
				cyNew = ny + t * nh;
			}

			sh.boundsX = cxNew - sh.boundsWidth  / 2;
			sh.boundsY = cyNew - sh.boundsHeight / 2;
		}
	}

	_refreshIncidentEdges(shape) {
		// When a shape moves, snap the endpoint waypoint of any incident edge
		// to the shape's bounding-box border, and propagate orthogonal
		// alignment to adjacent bend points so the line stays cleanly
		// horizontal/vertical where the author originally drew it that way.
		// Middle waypoints that aren't axis-aligned with the old endpoint
		// are left untouched. Both sequence flows AND message flows are
		// re-snapped: they share the same DI representation and resolve to
		// element bpmnIds for source/target.
		//
		// NOTE: this collapses parallel edges to the shape's side midpoint
		// (it loses each endpoint's offset along the side). For interactive
		// moves, prefer `_translateIncidentEdgeEndpoints`; for resize use
		// `_projectIncidentEdgeEndpoints`. This stays for the buffer-replay
		// path and other "re-anchor to a clean exit" cleanups.
		if (!shape) return;
		const elemBpmnId = shape.bpmnElementRef;
		if (!elemBpmnId) return;

		for (const [, edge] of this._edges) {
			const wps = this._parseWaypoints(edge);
			if (wps.length < 2) continue;
			// Resolve the edge's logical flow (sequence flow or message flow)
			// so we know whether THIS shape is its source or target. The two
			// flow kinds use the same `sourceRefId` / `targetRefId` schema;
			// we just have to pick the right map.
			const seqFlow = this._findFlowByBpmnId(edge.bpmnElementRef);
			const msgFlow = !seqFlow ? this._messageFlowsByBpmnId.get(edge.bpmnElementRef) : null;
			const flow    = seqFlow || msgFlow;
			if (!flow) continue;
			const isSource = flow.sourceRefId === elemBpmnId;
			const isTarget = flow.targetRefId === elemBpmnId;
			if (!isSource && !isTarget) continue;

			if (isSource) {
				const oldStart = wps[0];
				const newStart = this._snapToShapeBorder(shape, wps[1]);
				// Propagate orthogonal alignment to the adjacent waypoint
				// only when it's a *middle* bend (length > 2). For a 2-point
				// edge the "next" waypoint IS the other endpoint, and shifting
				// it would disconnect the line from its target.
				if (wps.length > 2) {
					const next = wps[1];
					if (Math.abs(next.x - oldStart.x) < 0.5) next.x = newStart.x;
					if (Math.abs(next.y - oldStart.y) < 0.5) next.y = newStart.y;
				}
				wps[0] = newStart;
			}
			if (isTarget) {
				const lastIdx  = wps.length - 1;
				const oldEnd   = wps[lastIdx];
				const newEnd   = this._snapToShapeBorder(shape, wps[lastIdx - 1]);
				if (wps.length > 2) {
					const prev = wps[lastIdx - 1];
					if (Math.abs(prev.x - oldEnd.x) < 0.5) prev.x = newEnd.x;
					if (Math.abs(prev.y - oldEnd.y) < 0.5) prev.y = newEnd.y;
				}
				wps[lastIdx]   = newEnd;
			}
			edge.waypoints = JSON.stringify(wps);
		}
	}

	/**
	 * Pick an endpoint location on the shape's bounding-box border that
	 * faces the given reference point. Exits at the midpoint of the chosen
	 * side -- so the arrow leaves the shape cleanly from the centre of its
	 * top/bottom/left/right edge instead of from a corner.
	 *
	 * Side selection compares |dx| / halfW against |dy| / halfH so the
	 * decision is aspect-aware: a tall narrow rectangle prefers horizontal
	 * exits even for moderately steep reference angles.
	 */
	_snapToShapeBorder(shape, refPoint) {
		const x1 = shape.boundsX, y1 = shape.boundsY;
		const x2 = x1 + shape.boundsWidth, y2 = y1 + shape.boundsHeight;
		const cx = (x1 + x2) / 2, cy = (y1 + y2) / 2;
		if (!refPoint) return { x: cx, y: cy };
		const dx = refPoint.x - cx;
		const dy = refPoint.y - cy;
		if (dx === 0 && dy === 0) return { x: cx, y: cy };
		const halfW = (x2 - x1) / 2, halfH = (y2 - y1) / 2;
		const horizontalDominates = Math.abs(dx) * halfH > Math.abs(dy) * halfW;
		if (horizontalDominates) {
			return { x: dx > 0 ? x2 : x1, y: cy };
		}
		return { x: cx, y: dy > 0 ? y2 : y1 };
	}

	_findFlowByBpmnId(flowBpmnId) {
		for (const [, fl] of this._flows) if (fl.bpmnId === flowBpmnId) return fl;
		return null;
	}

	/**
	 * Look up an element by its BPMN id (the round-trip identifier from
	 * the source XML, e.g. "Task_Review"). The element map is keyed by
	 * the Structr UUID; this is a linear scan but called only on demand
	 * during boundary-event resolution.
	 */
	_elementByBpmnId(bpmnId) {
		if (!bpmnId) return null;
		for (const [, el] of this._elements) {
			if (el.bpmnId === bpmnId) return el;
		}
		return null;
	}

	/**
	 * Resolve the host activity's bpmnId for a boundary event. Prefers
	 * the typed `attachedTo` relationship populated by the importer
	 * (post-graph migration); falls back to the legacy `attachedToRef`
	 * field embedded in `bpmnAttributes` JSON for diagrams imported
	 * before the relationship existed.
	 *
	 * Cached on the element as `_cachedAttachedToRef` so repeat lookups
	 * during a drag don't re-parse the JSON every frame. The cache
	 * MUST be invalidated whenever the editor mutates `attachedTo` or
	 * `bpmnAttributes` (set to `undefined` to force recomputation).
	 *
	 * Note: `attachedToRef` only carries meaning on boundary events,
	 * but we don't filter by element type here: the cache is set
	 * unconditionally and the field is only ever populated on boundary
	 * events, so callers that don't pre-filter still get null on
	 * non-boundary elements.
	 */
	_attachedToBpmnIdOf(element) {
		if (!element) return null;
		if (element._cachedAttachedToRef !== undefined) return element._cachedAttachedToRef;
		let ref = null;
		// Typed relationship (preferred). Stub may carry bpmnId directly,
		// or only an id which we resolve through the loaded element map.
		const at = element.attachedTo;
		if (at && typeof at === 'object') {
			if (typeof at.bpmnId === 'string' && at.bpmnId) {
				ref = at.bpmnId;
			} else if (typeof at.id === 'string') {
				const host = this._elements.get(at.id);
				if (host && host.bpmnId) ref = host.bpmnId;
			}
		}
		// Legacy: attachedToRef in bpmnAttributes JSON. Only used until the
		// diagram is re-imported post-trait-change; importer rewrites the
		// JSON to drop attachedToRef once the typed rel is established.
		if (!ref) {
			const json = element.bpmnAttributes;
			if (json && typeof json === 'string') {
				try {
					const obj = JSON.parse(json);
					if (obj && typeof obj.attachedToRef === 'string' && obj.attachedToRef) {
						ref = obj.attachedToRef;
					}
				} catch (_) { /* malformed: treat as no attachment */ }
			}
		}
		element._cachedAttachedToRef = ref;
		return ref;
	}

	/**
	 * BPMN element types that can host a boundary event. Per BPMN 2.0.2:
	 * any "activity" -- tasks of every flavour, sub-processes, and call
	 * activities. Events / gateways / data shapes / pools / lanes are
	 * not valid hosts.
	 */
	_isBoundaryHostType(elementType) {
		if (!elementType) return false;
		return elementType.endsWith('Task')
			|| elementType === 'task'
			|| elementType === 'subProcess'
			|| elementType === 'callActivity';
	}

	/** True for the boundary-event element type (with or without an event-definition suffix). */
	_isBoundaryElementType(type) {
		if (!type) return false;
		const baseType = type.includes(':') ? type.substring(0, type.indexOf(':')) : type;
		return baseType === 'boundaryEvent';
	}

	/**
	 * Hit-test the given screen coords for a valid boundary host shape.
	 * Returns `{ element, shape, elementId }` when the pointer sits over
	 * a task / subprocess / callActivity, else null. Pools, lanes,
	 * events, gateways, data shapes and the boundary itself are rejected.
	 *
	 * `selfElementId`, when set, lets callers exclude one element id
	 * (e.g. the boundary being dragged) from the hit-test even if its
	 * own DOM happens to win the elementFromPoint contest.
	 */
	_boundaryHostAtScreen(clientX, clientY, selfElementId = null) {
		const elementId = this._elementIdAtScreen(clientX, clientY);
		if (!elementId) return null;
		if (selfElementId && elementId === selfElementId) return null;
		const element = this._elements.get(elementId);
		if (!element) return null;
		if (!this._isBoundaryHostType(element.bpmnElementType)) return null;
		const shape = this._shapes.get(element.bpmnId);
		if (!shape) return null;
		return { element, shape, elementId };
	}

	/**
	 * Pick the cardinal slot of `hostShape` closest to `point`. Returns
	 * `{ x, y, side }` where side is one of "top" / "right" / "bottom" /
	 * "left". Order in `_cardinalsOf` is fixed (top, right, bottom, left)
	 * so we re-derive the side label by index.
	 */
	_nearestCardinalSlot(hostShape, point) {
		const cardinals = this._cardinalsOf(hostShape);
		const sides = ['top', 'right', 'bottom', 'left'];
		let best = { x: cardinals[0].x, y: cardinals[0].y, side: sides[0] };
		let bestD = Infinity;
		for (let i = 0; i < cardinals.length; i++) {
			const c = cardinals[i];
			const d = Math.hypot(c.x - point.x, c.y - point.y);
			if (d < bestD) { bestD = d; best = { x: c.x, y: c.y, side: sides[i] }; }
		}
		return best;
	}

	/**
	 * Classify which side of `hostShape` the point (px, py) is closest
	 * to: 'top' | 'right' | 'bottom' | 'left'. Used to figure out which
	 * side a boundary-event centre belongs to so siblings can be grouped
	 * for slot redistribution.
	 */
	_sideOfPointOnHost(hostShape, px, py) {
		const dTop    = Math.abs(py - hostShape.boundsY);
		const dBottom = Math.abs(py - (hostShape.boundsY + hostShape.boundsHeight));
		const dLeft   = Math.abs(px - hostShape.boundsX);
		const dRight  = Math.abs(px - (hostShape.boundsX + hostShape.boundsWidth));
		const m = Math.min(dTop, dBottom, dLeft, dRight);
		if (m === dTop)    return 'top';
		if (m === dBottom) return 'bottom';
		if (m === dLeft)   return 'left';
		return 'right';
	}

	/**
	 * Redistribute boundaries on `(hostBpmnId, side)` and commit the
	 * resulting bounds + edge-waypoint changes as a self-contained undo
	 * entry. Used after palette boundary creation so a freshly-dropped
	 * boundary doesn't pile on top of an existing sibling.
	 *
	 * Drag-end has its own redistribution path that rolls into the
	 * drag's compound undo entry; this helper is for callers that
	 * commit one step at a time (creation).
	 */
	_redistributeBoundariesOnSideWithUndo(hostBpmnId, side) {
		const hostShape = this._shapes.get(hostBpmnId);
		if (!hostShape) return;
		const onSide = this._attachedShapesFor(hostBpmnId)
			.filter(a => this._sideOfPointOnHost(hostShape,
				a.shape.boundsX + a.shape.boundsWidth  / 2,
				a.shape.boundsY + a.shape.boundsHeight / 2) === side);
		if (onSide.length < 2) return;  // single member: side midpoint already correct

		// Snapshot bounds + incident edges so we can diff after redistribution.
		const snaps = onSide.map(a => ({
			shape:    a.shape,
			boundsX0: a.shape.boundsX,
			boundsY0: a.shape.boundsY,
			edgesSnap: this._snapshotIncidentEdges(a.shape),
		}));
		this._redistributeBoundariesOnSide(hostShape, side, onSide.map(a => ({ shape: a.shape })));

		const changes = [];
		for (const s of snaps) {
			const dxs = s.shape.boundsX - s.boundsX0;
			const dys = s.shape.boundsY - s.boundsY0;
			if (dxs === 0 && dys === 0) continue;
			this._translateIncidentEdgeEndpoints(s.shape, s.edgesSnap, dxs, dys);
			changes.push({
				shape:       s.shape,
				before:      { boundsX: s.boundsX0,         boundsY: s.boundsY0 },
				after:       { boundsX: s.shape.boundsX,    boundsY: s.shape.boundsY },
				edgeChanges: s.edgesSnap.map(es => ({ edge: es.edge, before: es.waypointsBefore, after: es.edge.waypoints })),
			});
		}
		if (changes.length === 0) return;

		for (const c of changes) {
			if (c.shape.id) this._bufferUpdate(c.shape.id, c.after);
			for (const ec of c.edgeChanges) {
				if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
			}
		}
		this._pushUndo({
			apply: () => {
				for (const c of changes) {
					Object.assign(c.shape, c.after);
					if (c.shape.id) this._bufferUpdate(c.shape.id, c.after);
					for (const ec of c.edgeChanges) {
						ec.edge.waypoints = ec.after;
						if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.after });
					}
				}
				this.refresh();
			},
			revert: () => {
				for (const c of changes) {
					Object.assign(c.shape, c.before);
					if (c.shape.id) this._bufferUpdate(c.shape.id, c.before);
					for (const ec of c.edgeChanges) {
						ec.edge.waypoints = ec.before;
						if (ec.edge.id && ec.before !== ec.after) this._bufferUpdate(ec.edge.id, { waypoints: ec.before });
					}
				}
				this.refresh();
			},
		});
		this.refresh();
	}

	/**
	 * Spread `members` (array of { shape }) evenly along the given side
	 * of `hostShape`. Single member is left at the side midpoint (no
	 * redistribution needed). Existing parametric position along the
	 * side determines the sort order, so a boundary that was previously
	 * "left of" another stays left of it.
	 *
	 * Mutates each member's `shape.boundsX/Y` so its centre lands at the
	 * computed slot. Caller is responsible for snapshot/diff and edge
	 * translation -- this helper is geometry-only.
	 */
	_redistributeBoundariesOnSide(hostShape, side, members) {
		if (!hostShape || !members || members.length === 0) return;
		const horizontal = (side === 'top' || side === 'bottom');
		// Stable sort by current parametric position along the side axis.
		const sorted = members.slice().sort((a, b) => {
			if (horizontal) {
				return (a.shape.boundsX + a.shape.boundsWidth  / 2)
				     - (b.shape.boundsX + b.shape.boundsWidth  / 2);
			}
			return (a.shape.boundsY + a.shape.boundsHeight / 2)
			     - (b.shape.boundsY + b.shape.boundsHeight / 2);
		});
		const n = sorted.length;
		for (let i = 0; i < n; i++) {
			const sh = sorted[i].shape;
			// 1 boundary -> 0.5 (midpoint); 2 -> 1/3, 2/3; 3 -> 1/4, 2/4, 3/4.
			const t = (n === 1) ? 0.5 : (i + 1) / (n + 1);
			let cx, cy;
			if (side === 'top') {
				cx = hostShape.boundsX + t * hostShape.boundsWidth;
				cy = hostShape.boundsY;
			} else if (side === 'bottom') {
				cx = hostShape.boundsX + t * hostShape.boundsWidth;
				cy = hostShape.boundsY + hostShape.boundsHeight;
			} else if (side === 'left') {
				cx = hostShape.boundsX;
				cy = hostShape.boundsY + t * hostShape.boundsHeight;
			} else { // right
				cx = hostShape.boundsX + hostShape.boundsWidth;
				cy = hostShape.boundsY + t * hostShape.boundsHeight;
			}
			sh.boundsX = cx - sh.boundsWidth  / 2;
			sh.boundsY = cy - sh.boundsHeight / 2;
		}
	}

	/**
	 * Find every boundary-event shape attached to the given host activity
	 * (matched by host's bpmnId via `attachedToRef`). Each entry is
	 * `{ element, shape }` so callers can buffer-update both the shape
	 * (DI bounds) and element (e.g. lane reassignment) if needed.
	 */
	_attachedShapesFor(hostBpmnId) {
		if (!hostBpmnId) return [];
		const out = [];
		for (const [, el] of this._elements) {
			if (el.bpmnElementType !== 'boundaryEvent') continue;
			if (this._attachedToBpmnIdOf(el) !== hostBpmnId) continue;
			const sh = this._shapes.get(el.bpmnId);
			if (!sh) continue;
			out.push({ element: el, shape: sh });
		}
		return out;
	}
};
