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
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_Processes);
});

let _Processes = {
	_moduleName: 'processes',
	activeTabKey: 'structrProcessesTab_' + location.port,

	// v2 suffix invalidates the localStorage-cached sort key that pre-cutover
	// sessions stored as 'processName' (now removed from BpmnDefinitions).
	definitionsPagerId: 'process-definitions-v2',
	instancesPagerId:   'process-instances',
	tokensPagerId:      'process-tokens',
	tasksPagerId:       'process-tasks',

	// Cache BpmnProcess id -> version, populated lazily by ensureProcessVersion().
	// The pager's related-node serialization is a stub (id/type/name only), so the
	// `version` field isn't included in the initial row payload. We fetch it on
	// demand, once per id, and patch the rendered cells when the answer arrives.
	//
	// Post multi-process refactor: ProcessInstance.process now points at a
	// BpmnProcess (not a BpmnDefinitions). The `version` field lives on every
	// BPMN node via BpmnBaseNodeTraitDefinition (set by the importer per import
	// generation), so a single fetch on the BpmnProcess id yields name + version
	// without the BpmnDefinitions hop.
	processVersionCache: new Map(),
	processVersionPending: new Set(),

	/**
	 * Normalize a related "process" field to its id. The field arrives in
	 * several shapes depending on the fetch: an object stub {id,...}, a
	 * single-element array of those, or a bare id string. Returns null when
	 * there is no process reference. Shared by the process-bound widget UIs
	 * that need the owning process of a BpmnElement / bound UserTask.
	 */
	processRefId: (node) => {
		const ref = node?.process;
		if (!ref) return null;
		if (Array.isArray(ref)) return ref[0]?.id ?? (typeof ref[0] === 'string' ? ref[0] : null);
		return ref.id ?? (typeof ref === 'string' ? ref : null);
	},

	/**
	 * The process-level subjectType of a BpmnProcess (or null). A process
	 * instance has exactly one subject, so its type is a process-level fact;
	 * the process-bound widget UIs resolve it here rather than off a step.
	 */
	getSubjectType: async (processId) => {
		if (!processId) return null;
		try { const p = await Command.getPromise(processId, 'id,subjectType', 'ui'); return p?.subjectType || null; }
		catch (e) { console.error('getSubjectType failed', e); return null; }
	},

	/**
	 * Fetch the version of a BpmnProcess and, when it lands, patch every
	 * Process Instance row whose process cell needs the version suffix.
	 * No-op if the version is already cached or the fetch is in flight.
	 */
	ensureProcessVersion: (procId) => {
		if (!procId) return;
		if (_Processes.processVersionCache.has(procId)) return;
		if (_Processes.processVersionPending.has(procId)) return;
		_Processes.processVersionPending.add(procId);
		Command.get(procId, 'id,name,processName,version', (proc) => {
			_Processes.processVersionPending.delete(procId);
			if (!proc) return;
			_Processes.processVersionCache.set(procId, proc.version ?? null);
			// Patch every visible row whose process matches.
			const rows = document.querySelectorAll('#processInstancesTable tbody tr.process-instance');
			for (const row of rows) {
				const procIdAttr = row.dataset.processId;
				if (procIdAttr !== procId) continue;
				const cell = row.querySelector('td.definition-cell');
				if (cell) {
					const name = (proc.processName ?? proc.name ?? procId);
					cell.textContent = proc.version ? `${name} (v${proc.version})` : name;
				}
			}
		});
	},

	init: () => {
		_Pager.initPager(_Processes.definitionsPagerId, 'BpmnDefinitions', 1, 25, 'name', 'asc');
		_Pager.initPager(_Processes.instancesPagerId,   'ProcessInstance', 1, 25, 'startTime',   'desc');
		_Pager.initPager(_Processes.tokensPagerId,      'ProcessToken',    1, 25, 'createdDate', 'desc');
		_Pager.initPager(_Processes.tasksPagerId,       'TaskInstance',    1, 25, 'createdTime', 'desc');
	},

	unload: () => {},

	onload: () => {

		_Processes.init();

		Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('processes'));

		Structr.setMainContainerHTML(_Processes.templates.main());
		Structr.setFunctionBarHTML(_Processes.templates.functions());

		UISettings.showSettingsForCurrentModule();

		let initialSubModule = LSWrapper.getItem(_Processes.activeTabKey) || 'definitions';
		if (location.hash.split(':').length > 1) {
			initialSubModule = location.hash.split(':')[1];
		}

		for (let tabLink of document.querySelectorAll('#function-bar .tabs-menu li a')) {

			tabLink.addEventListener('click', (e) => {
				e.preventDefault();

				let urlHash = e.target.closest('a').getAttribute('href');
				let subModule = urlHash.split(':')[1];
				window.location.hash = urlHash;

				_Processes.selectTab(subModule);
			});

			if (tabLink.closest('a').getAttribute('href') === '#processes:' + initialSubModule) {
				tabLink.click();
			}
		}

		Structr.mainMenu.unblock(100);
	},

	selectTab: (subModule) => {

		LSWrapper.setItem(_Processes.activeTabKey, subModule);

		for (let tab of document.querySelectorAll('#function-bar .tabs-menu li')) {
			tab.classList.remove('active');
		}

		let tabLink = document.querySelector('#function-bar .tabs-menu li a[href="#processes:' + subModule + '"]');
		if (tabLink) tabLink.closest('li').classList.add('active');

		for (let el of document.querySelectorAll('#processes .tab-content')) {
			el.classList.remove('active');
		}
		document.querySelector('#processes-' + subModule)?.classList.add('active');

		if (subModule === 'definitions') {
			_ProcessDefinitions.refresh();
		} else if (subModule === 'instances') {
			_ProcessInstances.refresh();
		} else if (subModule === 'tokens') {
			_ProcessTokens.refresh();
		} else if (subModule === 'tasks') {
			_ProcessTasks.refresh();
		}
	},

	rest: {
		callEntityMethod: async (type, id, method, params = {}, { showError = true } = {}) => {

			// Static methods are routed without an instance id: POST /{type}/{method}.
			let url = id
				? `${Structr.rootUrl}${type}/${id}/${method}`
				: `${Structr.rootUrl}${type}/${method}`;
			let response;
			try {
				response = await fetch(url, {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(params)
				});
			} catch (e) {
				if (showError) new ErrorMessage().text(`${method} failed: ${e.message}`).show();
				throw e;
			}

			let json = await response.json().catch(() => ({}));
			if (!response.ok) {
				// Expand the FULL Structr error envelope -- the top-level message
				// plus the per-field errors[] details -- via the shared formatter,
				// instead of json.message alone. Validation failures (e.g. a
				// duplicate user-defined function left over from an earlier import)
				// carry the actionable reason in errors[]; the bare message omits
				// it, so that reason used to be visible only in the browser console.
				let hasBody    = json && (json.errors?.length || json.message || json.code !== undefined);
				let detailHtml = hasBody
					? Structr.getErrorMessageFromResponse(json, true)
					: _Helpers.escapeTags(response.statusText || 'Request failed');
				if (showError) {
					new ErrorMessage().title(`${method} failed`).text(detailHtml).show();
				}
				// Plain-text detail for the thrown Error; attach the raw envelope so
				// callers can re-render it with their own context (e.g. a filename).
				let plain = (typeof detailHtml === 'string') ? detailHtml.replace(/<br\s*\/?>/gi, ' ').replace(/<[^>]*>/g, '') : String(detailHtml);
				let err = new Error(plain);
				err.responseData = json;
				throw err;
			}
			return json;
		},

		setProperty: async (type, id, key, value) => {

			let url = `${Structr.rootUrl}${type}/${id}`;
			let response = await fetch(url, {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ [key]: value })
			});

			if (!response.ok) {
				let json = await response.json().catch(() => ({}));
				let msg = json?.message ?? response.statusText;
				new ErrorMessage().text(`Update failed: ${msg}`).show();
				throw new Error(msg);
			}
		}
	},

	fmt: {
		shortId: (id) => id ? id.substring(0, 8) + '…' : '',
		dateTime: (v) => v ? new Date(v).toLocaleString() : '',
		relName: (rel, fallbackLabel = '—') => {
			if (!rel) return fallbackLabel;
			if (Array.isArray(rel)) {
				if (rel.length === 0) return fallbackLabel;
				rel = rel[0];
			}
			return rel.processName ?? rel.name ?? _Processes.fmt.shortId(rel.id) ?? fallbackLabel;
		},
		// Resolve the display name of a related BPMN node and append its version
		// stamp as a suffix (e.g. "Employee Leave Request (v4)"). The default
		// related-node serialization is a stub (id/type/name only) -- if the
		// version field isn't present, the suffix is omitted on this render and
		// applied later by enrichWithVersion() once the cache fills in.
		namedWithVersion: (rel, fallbackLabel = '—') => {
			if (!rel) return fallbackLabel;
			if (Array.isArray(rel)) {
				if (rel.length === 0) return fallbackLabel;
				rel = rel[0];
			}
			const name = rel.processName ?? rel.name ?? _Processes.fmt.shortId(rel.id) ?? fallbackLabel;
			const cachedVersion = _Processes.processVersionCache.get(rel.id);
			const v = rel.version ?? cachedVersion;
			return v ? `${name} (v${v})` : name;
		},
		statusBadge: (status) => {
			let s = status ?? '';
			return `<span class="status-badge status-${_Helpers.escapeTags(s.toLowerCase())}">${_Helpers.escapeTags(s)}</span>`;
		}
	},

	templates: {
		main: () => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/processes.css">

			<div class="main-app-box" id="processes">
				<div id="processesTabs" class="tabs-contents">
					<div id="processes-definitions" class="tab-content">
						<div id="processDefinitionsContainer"></div>
					</div>
					<div id="processes-instances" class="tab-content">
						<div id="processInstancesContainer"></div>
					</div>
					<div id="processes-tokens" class="tab-content">
						<div id="processTokensContainer"></div>
					</div>
					<div id="processes-tasks" class="tab-content">
						<div id="processTasksContainer"></div>
					</div>
				</div>
			</div>
		`,
		functions: () => `
			<ul id="processesTabsMenu" class="tabs-menu flex-grow">
				<li><a href="#processes:definitions"><span>Process Definitions</span></a></li>
				<li><a href="#processes:instances"><span>Process Instances</span></a></li>
				<li><a href="#processes:tokens"><span>Process Tokens</span></a></li>
				<li><a href="#processes:tasks"><span>Task Instances</span></a></li>
			</ul>
		`,
		definitionsTable: () => `
			<div class="processes-controls">
				<button class="action button btn flex items-center active:border-green btn-create-definition" title="Create Process Definition">
					${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'mr-2')} Create Process Definition
				</button>
				<div class="bpmn-dropzone" title="Drop a BPMN file or click to choose">
					${_Icons.getSvgIcon(_Icons.iconImportFromCSV, 16, 16, 'mr-2')} Drop .bpmn file here, or click to choose
					<input type="file" class="bpmn-dropzone-input" accept=".bpmn,.xml,application/xml,text/xml" multiple>
				</div>
				<div id="processDefinitionsPager"></div>
			</div>
			<table class="processes-table" id="processDefinitionsTable">
				<thead>
					<tr>
						<th>Name</th>
						<th>Version</th>
						<th>Processes</th>
						<th>Active Instances</th>
						<th>Suspended</th>
						<th>Completed</th>
						<th>Methods</th>
						<th>Security</th>
						<th class="actions-col">Actions</th>
					</tr>
				</thead>
				<tbody></tbody>
			</table>
		`,
		instancesTable: () => `
			<div class="processes-controls">
				<div id="processInstancesPager"></div>
			</div>
			<table class="processes-table" id="processInstancesTable">
				<thead>
					<tr>
						<th>ID</th>
						<th>Definition</th>
						<th>Subject</th>
						<th>Initiator</th>
						<th>Status</th>
						<th>Started</th>
						<th>Ended</th>
						<th class="actions-col">Actions</th>
					</tr>
				</thead>
				<tbody></tbody>
			</table>
		`,
		tokensTable: () => `
			<div class="processes-controls">
				<div id="processTokensPager"></div>
			</div>
			<table class="processes-table" id="processTokensTable">
				<thead>
					<tr>
						<th>ID</th>
						<th>Instance</th>
						<th>At Element</th>
						<th>Status</th>
						<th class="actions-col">Actions</th>
					</tr>
				</thead>
				<tbody></tbody>
			</table>
		`,
		tasksTable: () => `
			<div class="processes-controls">
				<div id="processTasksPager"></div>
			</div>
			<table class="processes-table" id="processTasksTable">
				<thead>
					<tr>
						<th>ID</th>
						<th>Name</th>
						<th>Process Instance</th>
						<th>Element</th>
						<th>Assignee</th>
						<th>Candidates</th>
						<th>Status</th>
						<th>Created</th>
						<th>Claimed</th>
						<th>Completed</th>
						<th class="actions-col">Actions</th>
					</tr>
				</thead>
				<tbody></tbody>
			</table>
		`
	}
};

let _ProcessDefinitions = {

	getContainer: () => document.querySelector('#processDefinitionsContainer'),

	refresh: () => {

		let el = _ProcessDefinitions.getContainer();
		_Helpers.fastRemoveAllChildren(el);
		el.insertAdjacentHTML('beforeend', _Processes.templates.definitionsTable());

		let pagerElement = el.querySelector('#processDefinitionsPager');
		let pager = _Pager.addPager(
			_Processes.definitionsPagerId,
			pagerElement,
			true,
			'BpmnDefinitions',
			'ui',
			(entities) => {
				for (let entity of entities) _ProcessDefinitions.appendRow(entity);
			},
			null,
			'id,name,type,securityLevel,version,processes,collaboration',
			true
		);

		pager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(document.querySelector('#processDefinitionsTable tbody'));
		};

		pagerElement.insertAdjacentHTML('beforeend', `
			<div class="processes-pager-filters">
				Filters: <input type="text" class="filter" data-attribute="name" placeholder="Name">
			</div>
		`);
		pager.activateFilterElements(pagerElement.querySelector('.processes-pager-filters'));
		pager.setIsPaused(false);
		pager.refresh();

		const createBtn = el.querySelector('.btn-create-definition');
		if (createBtn) {
			createBtn.addEventListener('click', () => _ProcessDefinitions.createNew());
		}

		const dropzone = el.querySelector('.bpmn-dropzone');
		if (dropzone) {
			const fileInput = dropzone.querySelector('.bpmn-dropzone-input');
			const importFiles = async (files) => {
				if (!files || files.length === 0) return;
				let imported = 0;
				for (const f of files) {
					try {
						const xml = await f.text();
						// showError:false -- suppress callEntityMethod's own popup so we
						// can surface a single, filename-scoped error below carrying the
						// full server-side detail (message + errors[]).
						await _Processes.rest.callEntityMethod('BpmnDefinitions', null, 'importBpmn', { xml, filename: f.name }, { showError: false });
						imported++;
						new SuccessMessage().text(`Imported "${f.name}".`).show();
					} catch (err) {
						if (err && err.responseData) {
							// Full Structr error envelope: expands the errors[] details
							// (e.g. a duplicate user-defined function name from a previous
							// import) that json.message alone leaves out.
							Structr.errorFromResponse(err.responseData, null, { title: `Import of "${f.name}" failed`, requiresConfirmation: true });
						} else {
							new ErrorMessage().title(`Import of "${f.name}" failed`).text(_Helpers.escapeTags(err.message ?? 'Unknown error')).requiresConfirmation().show();
						}
					}
				}
				if (imported > 0) _ProcessDefinitions.refresh();
			};

			const enter = (e) => { e.preventDefault(); dropzone.classList.add('drag-over'); };
			const leave = (e) => { e.preventDefault(); dropzone.classList.remove('drag-over'); };
			dropzone.addEventListener('dragenter', enter);
			dropzone.addEventListener('dragover',  enter);
			dropzone.addEventListener('dragleave', leave);
			dropzone.addEventListener('drop', async (e) => {
				e.preventDefault();
				dropzone.classList.remove('drag-over');
				await importFiles(Array.from(e.dataTransfer?.files ?? []));
			});

			// Click anywhere in the dropzone (except on the hidden input) to
			// open the native file picker. The hidden input's change event
			// then routes through the same importFiles path as drop.
			dropzone.addEventListener('click', (e) => {
				if (e.target === fileInput) return;
				fileInput.click();
			});
			if (fileInput) {
				fileInput.addEventListener('change', async () => {
					await importFiles(Array.from(fileInput.files ?? []));
					// Reset so the same file can be picked again later.
					fileInput.value = '';
				});
			}
		}
	},

	/**
	 * Create a new BpmnDefinitions plus an empty BpmnDiDiagram so the editor
	 * has somewhere to attach shapes. Refreshes the pager and opens the
	 * diagram editor on the new definition. Name follows the Structr-admin
	 * "New <type> NNN" convention; users rename later via the property dialog.
	 */
	createNew: () => {

		const defaultName = _Helpers.createRandomName('process definition');

		// processId must be unique-ish: it's used to chain versions of the
		// same process. A random suffix avoids collisions with other ad-hoc
		// processes; users can rename later via the property dialog.
		const procBpmnId = 'Process_' + Math.random().toString(36).slice(2, 10);

		Command.create({
			type:    'BpmnDefinitions',
			name:    defaultName,
			version: '1',
		}, (defNode) => {
			if (!defNode || !defNode.id) {
				new ErrorMessage().text('Failed to create process definition.').show();
				return;
			}
			// Create a child BpmnProcess: holds processId / processName and
			// will own the elements + sequence flows the user draws.
			Command.create({
				type:                 'BpmnProcess',
				name:                 defaultName,
				processName:          defaultName,
				processId:            procBpmnId,
				processIsExecutable:  true,
				definition:           defNode.id,
				version:              '1',
			}, (procNode) => {
				if (!procNode || !procNode.id) {
					new ErrorMessage().text('Failed to create process.').show();
					return;
				}
				// Default diagram so the editor has somewhere to place shapes.
				Command.create({
					type:         'BpmnDiDiagram',
					name:         'Diagram_1',
					diagramId:    'Diagram_1',
					planeId:      'Plane_1',
					planeElement: procBpmnId,
					definition:   defNode.id,
				}, () => {
					new SuccessMessage().text(`Created "${defaultName}".`).show();
					_ProcessDefinitions.refresh();
					_ProcessDiagram.show({ id: defNode.id, name: defaultName }, defaultName);
				});
			});
		});
	},

	appendRow: (def) => {

		let tbody = document.querySelector('#processDefinitionsTable tbody');
		// `processes` and `collaboration` are stub objects (id/type/name) on
		// the pager-loaded BpmnDefinitions; the file's display name is just
		// `def.name` (set on import to the first process's name).
		const processes = Array.isArray(def.processes) ? def.processes : [];
		const isCollab  = !!def.collaboration;
		let displayName = def.name ?? processes[0]?.name ?? '';
		let processesCell = processes.length === 0
			? '<span class="text-gray-500">0</span>'
			: (isCollab ? `${processes.length} (collab)` : String(processes.length));
		// Methods are now per-process; without an extra fetch we don't know
		// the aggregate count from the pager payload alone. Show a dash here
		// and let the side panel surface details once the editor is open.
		let methodsCell = '<span class="text-gray-500">—</span>';
		// Instance counts (running / suspended / completed) across this file's
		// processes. Not part of the pager payload, so render placeholders and
		// patch the cells asynchronously (see updateInstanceCounts below).
		let loadingCell = '<span class="text-gray-500">…</span>';
		let tr = _Helpers.createSingleDOMElementFromHTML(`
			<tr id="id_${def.id}" class="process-definition">
				<td><a href="#" class="show-diagram-link" title="Open BPMN diagram editor">${_Helpers.escapeTags(displayName)}</a></td>
				<td class="mono">${_Helpers.escapeTags(def.version ?? '')}</td>
				<td>${processesCell}</td>
				<td class="active-instances-cell mono">${loadingCell}</td>
				<td class="suspended-instances-cell mono">${loadingCell}</td>
				<td class="completed-instances-cell mono">${loadingCell}</td>
				<td>${methodsCell}</td>
				<td>${_Helpers.escapeTags(def.securityLevel ?? '')}</td>
				<td class="actions">
					<i>${_Icons.getSvgIcon(_Icons.iconPlayButton, 16, 16, ['mr-1', 'start-process'], 'Start the first process in this file')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconProcess, 16, 16, ['mr-1', 'show-diagram'], 'Show BPMN diagram')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconExportAsCSV, 16, 16, ['mr-1', 'export-bpmn'], 'Export BPMN XML')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, ['mr-1', 'edit-entity'], 'Edit properties')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'delete-entity']), 'Delete')}</i>
				</td>
			</tr>
		`);

		tr.querySelector('.start-process').addEventListener('click', async (e) => {
			e.stopPropagation();
			// startProcess lives on BpmnProcess now. Pick the first process in
			// the file. Multi-process files would need a process picker --
			// out of scope for v1.
			const firstProcess = processes[0];
			if (!firstProcess?.id) {
				new ErrorMessage().text('No process to start: this BpmnDefinitions has no BpmnProcess child.').show();
				return;
			}
			try {
				await _Processes.rest.callEntityMethod('BpmnProcess', firstProcess.id, 'startProcess');
				new SuccessMessage().text(`Started process "${displayName}"`).show();
			} catch (err) { /* error already shown */ }
		});

		const openDiagram = (e) => {
			e.preventDefault();
			e.stopPropagation();
			_ProcessDiagram.show(def, displayName);
		};
		tr.querySelector('.show-diagram').addEventListener('click', openDiagram);
		tr.querySelector('.show-diagram-link').addEventListener('click', openDiagram);

		tr.querySelector('.export-bpmn').addEventListener('click', async (e) => {
			e.stopPropagation();
			try {
				const xml = await _Processes.rest.callEntityMethod('BpmnDefinitions', def.id, 'exportBpmn');
				// callEntityMethod returns the parsed JSON body; the entity-method
				// result lives under `result` per Structr REST convention.
				const xmlString = (typeof xml === 'string') ? xml : (xml?.result ?? '');
				if (!xmlString) throw new Error('empty XML response');
				const safeName = (displayName || def.name || 'process').replace(/[^a-zA-Z0-9._-]+/g, '_');
				const blob = new Blob([xmlString], { type: 'application/xml' });
				const url  = URL.createObjectURL(blob);
				const a    = document.createElement('a');
				a.href     = url;
				a.download = `${safeName}.bpmn`;
				document.body.appendChild(a);
				a.click();
				document.body.removeChild(a);
				URL.revokeObjectURL(url);
			} catch (err) {
				new ErrorMessage().text(`Export failed: ${err.message}`).show();
			}
		});

		tr.querySelector('.edit-entity').addEventListener('click', (e) => {
			e.stopPropagation();
			_Entities.showProperties(def, 'ui');
		});

		tr.querySelector('.delete-entity').addEventListener('click', (e) => {
			e.stopPropagation();
			_Entities.deleteNode({ ...def, name: displayName || def.id }, false, () => tr.remove());
		});

		tbody.appendChild(tr);

		// Fill in the running / suspended / completed instance counts once the
		// queries come back.
		_ProcessDefinitions.updateInstanceCounts(processes.map(p => p?.id).filter(Boolean), tr);
	},

	/**
	 * Count ProcessInstances in the given status across all of a definition's
	 * BpmnProcess ids. Each process is queried with _pageSize=1 so only the
	 * envelope's result_count is used; a failed request counts as zero rather
	 * than breaking the total. Statuses match ProcessInstanceTraitDefinition
	 * (running / suspended / completed / terminated / error).
	 */
	countInstancesByStatus: async (processIds, status) => {

		const counts = await Promise.all(processIds.map(async (pid) => {
			const res = await fetch(`${Structr.rootUrl}ProcessInstance/ui?process=${pid}&status=${status}&_pageSize=1`, { credentials: 'same-origin', cache: 'no-store' });
			if (!res.ok) return 0;
			const json = await res.json();
			return json.result_count ?? (Array.isArray(json.result) ? json.result.length : 0);
		}));
		return counts.reduce((a, b) => a + b, 0);
	},

	/**
	 * Populate the running / suspended / completed cells of a definition row
	 * with plain counts. Each status is loaded independently so one failing
	 * query only degrades its own cell.
	 */
	updateInstanceCounts: async (processIds, tr) => {

		const activeCell    = tr.querySelector('td.active-instances-cell');
		const suspendedCell = tr.querySelector('td.suspended-instances-cell');
		const completedCell = tr.querySelector('td.completed-instances-cell');

		const renderCount = (cell, n) => { if (cell) cell.textContent = String(n); };
		const renderError = (cell)    => { if (cell) cell.innerHTML = '<span class="text-gray-500" title="Failed to load count">?</span>'; };

		if (!processIds || processIds.length === 0) {
			renderCount(activeCell, 0);
			renderCount(suspendedCell, 0);
			renderCount(completedCell, 0);
			return;
		}

		const load = async (status, cell) => {
			try {
				renderCount(cell, await _ProcessDefinitions.countInstancesByStatus(processIds, status));
			} catch (e) {
				renderError(cell);
			}
		};

		await Promise.all([
			load('running',   activeCell),
			load('suspended', suspendedCell),
			load('completed', completedCell),
		]);
	}
};

let _ProcessDiagram = {

	// Holds the active backend instance while the dialog is open.
	_api: null,

	// Palette entries: BPMN element types the user can place. The data-type
	// matches `bpmnElementType` so api.armElementType is called with the right
	// string. SVG glyph mirrors how the canvas renders the type (circle =
	// event, rounded rect = task, diamond = gateway).
	// Palette entries: BPMN element types the user can place. Grouped
	// by semantics so the toolbar lays them out in clusters with
	// section headers. Each glyph mirrors how the canvas renders the
	// corresponding shape, so the palette button previews the actual
	// placement (event circle with the right marker, task rect with
	// the right corner badge, gateway diamond with the right type
	// symbol, etc.).
	_paletteGroups: [
		{
			name: 'Events',
			entries: [
				{ type: 'startEvent',                                    label: 'Start',       glyph: 'evStart'      },
				{ type: 'endEvent',                                      label: 'End',         glyph: 'evEnd'        },
				{ type: 'startEvent:messageEventDefinition',             label: 'Start msg',   glyph: 'evStartMsg'   },
				{ type: 'endEvent:messageEventDefinition',               label: 'End msg',     glyph: 'evEndMsg'     },
				{ type: 'startEvent:timerEventDefinition',               label: 'Start timer', glyph: 'evStartTimer' },
				{ type: 'intermediateCatchEvent:timerEventDefinition',   label: 'Timer',       glyph: 'evCatchTimer' },
				{ type: 'intermediateCatchEvent:messageEventDefinition', label: 'Catch msg',   glyph: 'evCatchMsg'   },
				{ type: 'intermediateThrowEvent:messageEventDefinition', label: 'Throw msg',   glyph: 'evThrowMsg'   },
				{ type: 'endEvent:errorEventDefinition',                 label: 'End error',   glyph: 'evEndErr'     },
				{ type: 'endEvent:terminateEventDefinition',             label: 'Terminate',   glyph: 'evEndTerm'    },
			],
		},
		{
			name: 'Tasks',
			entries: [
				{ type: 'task',             label: 'Task',        glyph: 'task'        },
				{ type: 'subProcess',       label: 'Sub-process', glyph: 'subProcess'  },
				{ type: 'userTask',         label: 'User',        glyph: 'taskUser'    },
				{ type: 'serviceTask',      label: 'Service',     glyph: 'taskService' },
				{ type: 'scriptTask',       label: 'Script',      glyph: 'taskScript'  },
				{ type: 'manualTask',       label: 'Manual',      glyph: 'taskManual'  },
				{ type: 'businessRuleTask', label: 'Rule',        glyph: 'taskRule'    },
				{ type: 'sendTask',         label: 'Send',        glyph: 'taskSend'    },
				{ type: 'receiveTask',      label: 'Receive',     glyph: 'taskReceive' },
			],
		},
		{
			name: 'Gateways',
			entries: [
				{ type: 'exclusiveGateway',  label: 'XOR',     glyph: 'gwXor'     },
				{ type: 'parallelGateway',   label: 'AND',     glyph: 'gwAnd'     },
				{ type: 'inclusiveGateway',  label: 'OR',      glyph: 'gwOr'      },
				{ type: 'eventBasedGateway', label: 'Event',   glyph: 'gwEvent'   },
				{ type: 'complexGateway',    label: 'Complex', glyph: 'gwComplex' },
			],
		},
		{
			// Boundaries are armed exactly like other element types but
			// can only be dropped onto a valid host activity (task /
			// subProcess / callActivity). Empty-canvas clicks are
			// rejected by the editor while a boundary type is armed.
			name: 'Boundaries',
			entries: [
				{ type: 'boundaryEvent:timerEventDefinition',      label: 'Timer',       glyph: 'boundaryTimer'  },
				{ type: 'boundaryEvent:messageEventDefinition',    label: 'Message',     glyph: 'boundaryMsg'    },
				{ type: 'boundaryEvent:errorEventDefinition',      label: 'Error',       glyph: 'boundaryErr'    },
				{ type: 'boundaryEvent:escalationEventDefinition', label: 'Escalation',  glyph: 'boundaryEscal'  },
				{ type: 'boundaryEvent:signalEventDefinition',     label: 'Signal',      glyph: 'boundarySignal' },
			],
		},
		{
			name: 'Data',
			entries: [
				{ type: 'dataObjectReference', label: 'Data obj',   glyph: 'dataObject' },
				{ type: 'dataStoreReference',  label: 'Data store', glyph: 'dataStore'  },
			],
		},
	],

	_paletteGlyph: (kind) => {
		// 28x28 viewBox. Body builders, then optional inner markers.
		const ev    = (m = '') => `<svg viewBox="0 0 28 28" width="28" height="28"><circle cx="14" cy="14" r="11" fill="#fff" stroke="#333" stroke-width="2"/>${m}</svg>`;
		const evDb  = (m = '') => `<svg viewBox="0 0 28 28" width="28" height="28"><circle cx="14" cy="14" r="11" fill="#fff" stroke="#333" stroke-width="1"/><circle cx="14" cy="14" r="9" fill="#fff" stroke="#333" stroke-width="1"/>${m}</svg>`;
		const evEnd = (m = '') => `<svg viewBox="0 0 28 28" width="28" height="28"><circle cx="14" cy="14" r="11" fill="#fff" stroke="#333" stroke-width="3"/>${m}</svg>`;
		// Boundary events: dashed circle to match the on-canvas renderer
		// (community-svg.js applies stroke-dasharray for elementType
		// starting with "boundary"). Single circle for simplicity at
		// 28x28: a double dashed circle gets visually noisy at this size.
		const evBnd = (m = '') => `<svg viewBox="0 0 28 28" width="28" height="28"><circle cx="14" cy="14" r="11" fill="#fff" stroke="#333" stroke-width="1.5" stroke-dasharray="3 2"/>${m}</svg>`;
		const task  = (m = '') => `<svg viewBox="0 0 28 28" width="28" height="28"><rect x="3" y="6" width="22" height="16" rx="3" ry="3" fill="#fff" stroke="#333" stroke-width="2"/>${m}</svg>`;
		const gw    = (m = '') => `<svg viewBox="0 0 28 28" width="28" height="28"><polygon points="14,3 25,14 14,25 3,14" fill="#fff" stroke="#333" stroke-width="2"/>${m}</svg>`;
		// Inner markers (centre 14,14).
		const clock     = `<circle cx="14" cy="14" r="5" fill="none" stroke="#333" stroke-width="1.2"/><path d="M14 10 L14 14 L17 16" stroke="#333" stroke-width="1.2" fill="none" stroke-linecap="round"/>`;
		const envelope  = `<rect x="9" y="11" width="10" height="6" fill="#fff" stroke="#333" stroke-width="1.2"/><polyline points="9,11 14,15 19,11" fill="none" stroke="#333" stroke-width="1.2"/>`;
		const envFilled = `<rect x="9" y="11" width="10" height="6" fill="#333" stroke="#333" stroke-width="1.2"/><polyline points="9,11 14,15 19,11" fill="none" stroke="#fff" stroke-width="1.2"/>`;
		const bolt      = `<polygon points="11,8 17,8 14,13 18,13 12,20 14,15 10,15" fill="#333" stroke="#333" stroke-width="0.5" stroke-linejoin="round"/>`;
		const termDot   = `<circle cx="14" cy="14" r="6" fill="#333"/>`;
		// Up-pointing triangle for signal events.
		const triangle  = `<polygon points="14,8 19,18 9,18" fill="#fff" stroke="#333" stroke-width="1.2" stroke-linejoin="round"/>`;
		// Up-arrow chevron for escalation events.
		const escArrow  = `<polygon points="14,8 19,18 14,15.5 9,18" fill="#fff" stroke="#333" stroke-width="1.2" stroke-linejoin="round"/>`;
		// Task corner badges (top-left of body, leaving room for label).
		const userBadge   = `<circle cx="8" cy="11" r="2.5" fill="none" stroke="#333" stroke-width="1"/><path d="M5 17 Q8 13 11 17" fill="none" stroke="#333" stroke-width="1" stroke-linecap="round"/>`;
		const gearBadge   = `<circle cx="8" cy="14" r="2.5" fill="none" stroke="#333" stroke-width="1"/><path d="M8 10.5 L8 12 M8 16 L8 17.5 M5.5 14 L4 14 M12 14 L10.5 14" stroke="#333" stroke-width="1" stroke-linecap="round"/>`;
		const scriptBadge = `<path d="M5 10 L11 10 M5 12.5 L11 12.5 M5 15 L9 15 M5 17.5 L11 17.5" stroke="#333" stroke-width="1" stroke-linecap="round"/>`;
		const handBadge   = `<path d="M6 12 L6 17 M8 11 L8 17 M10 12 L10 17" stroke="#333" stroke-width="1" stroke-linecap="round" fill="none"/>`;
		const ruleBadge   = `<rect x="5" y="11" width="6" height="6" fill="none" stroke="#333" stroke-width="1"/><line x1="5" y1="13" x2="11" y2="13" stroke="#333" stroke-width="1"/><line x1="7.5" y1="11" x2="7.5" y2="17" stroke="#333" stroke-width="1"/>`;
		const sendBadge   = `<rect x="5" y="11" width="7" height="5" fill="#333" stroke="#333" stroke-width="1"/>`;
		const recvBadge   = `<rect x="5" y="11" width="7" height="5" fill="none" stroke="#333" stroke-width="1"/>`;
		switch (kind) {
			// Events
			case 'evStart':       return ev();
			case 'evStartMsg':    return ev(envelope);
			case 'evStartTimer':  return ev(clock);
			case 'evCatchMsg':    return evDb(envelope);
			case 'evCatchTimer':  return evDb(clock);
			case 'evThrowMsg':    return evDb(envFilled);
			case 'evEnd':         return evEnd();
			case 'evEndMsg':      return evEnd(envFilled);
			case 'evEndErr':      return evEnd(bolt);
			case 'evEndTerm':     return evEnd(termDot);
			// Boundaries
			case 'boundaryTimer':  return evBnd(clock);
			case 'boundaryMsg':    return evBnd(envelope);
			case 'boundaryErr':    return evBnd(bolt);
			case 'boundaryEscal':  return evBnd(escArrow);
			case 'boundarySignal': return evBnd(triangle);
			// Tasks
			case 'task':          return task();
			case 'taskUser':      return task(userBadge);
			case 'taskService':   return task(gearBadge);
			case 'taskScript':    return task(scriptBadge);
			case 'taskManual':    return task(handBadge);
			case 'taskRule':      return task(ruleBadge);
			case 'taskSend':      return task(sendBadge);
			case 'taskReceive':   return task(recvBadge);
			case 'subProcess':    return `<svg viewBox="0 0 28 28" width="28" height="28"><rect x="3" y="6" width="22" height="16" rx="3" ry="3" fill="#fff" stroke="#333" stroke-width="2"/><rect x="11.5" y="16.5" width="5" height="5" fill="#fff" stroke="#333" stroke-width="1"/><line x1="14" y1="17.5" x2="14" y2="20.5" stroke="#333" stroke-width="1"/><line x1="12.5" y1="19" x2="15.5" y2="19" stroke="#333" stroke-width="1"/></svg>`;
			// Gateways
			case 'gwXor':         return gw(`<path d="M9 9 L19 19 M19 9 L9 19" stroke="#333" stroke-width="2" stroke-linecap="round"/>`);
			case 'gwAnd':         return gw(`<path d="M9 14 L19 14 M14 9 L14 19" stroke="#333" stroke-width="2.5" stroke-linecap="round"/>`);
			case 'gwOr':          return gw(`<circle cx="14" cy="14" r="4.5" fill="none" stroke="#333" stroke-width="2"/>`);
			case 'gwEvent':       return gw(`<circle cx="14" cy="14" r="6" fill="none" stroke="#333" stroke-width="0.8"/><circle cx="14" cy="14" r="4.5" fill="none" stroke="#333" stroke-width="0.8"/><polygon points="14,11 16.5,12.8 15.5,15.8 12.5,15.8 11.5,12.8" fill="none" stroke="#333" stroke-width="1"/>`);
			case 'gwComplex':     return gw(`<path d="M9 14 L19 14 M14 9 L14 19 M10.5 10.5 L17.5 17.5 M17.5 10.5 L10.5 17.5" stroke="#333" stroke-width="1.5" stroke-linecap="round"/>`);
			// Data
			case 'dataObject':    return `<svg viewBox="0 0 28 28" width="28" height="28"><path d="M6 4 L18 4 L22 8 L22 24 L6 24 Z" fill="#fff" stroke="#333" stroke-width="1.5"/><path d="M18 4 L18 8 L22 8" fill="none" stroke="#333" stroke-width="1.5"/></svg>`;
			case 'dataStore':     return `<svg viewBox="0 0 28 28" width="28" height="28"><ellipse cx="14" cy="6" rx="9" ry="2.5" fill="#fff" stroke="#333" stroke-width="1.2"/><path d="M5 6 L5 22 A 9 2.5 0 0 0 23 22 L23 6" fill="#fff" stroke="#333" stroke-width="1.2"/><path d="M5 9 A 9 2.5 0 0 0 23 9 M5 12 A 9 2.5 0 0 0 23 12" fill="none" stroke="#333" stroke-width="0.8"/></svg>`;
		}
		return '';
	},

	show: (def, displayName) => {

		const title = `Process Diagram: ${displayName || def.name || ''}`;
		// Captured in show()'s closure so the Cancel callback can detach the
		// keydown listener installed below.
		let detachKeydown = null;
		const { dialogText } = _Dialogs.custom.openDialog(title, () => {
			// Cancel callback: tear down the editor.
			if (detachKeydown) detachKeydown();
			if (_ProcessDiagram._api) {
				try { _ProcessDiagram._api.unmount(); } catch (e) { console.error(e); }
				_ProcessDiagram._api = null;
			}
		}, ['process-diagram-dialog']);

		// Layout: toolbar at top, then a flex row of palette + canvas + side panel.
		// Side panel shows selected element's properties inline (no stacked
		// dialogs -- Structr's custom-dialog stack doesn't nest cleanly, and
		// inline display also keeps the diagram visible while inspecting).
		dialogText.style.display       = 'flex';
		dialogText.style.flexDirection = 'column';
		dialogText.style.height        = '100%';
		const paletteHtml = _ProcessDiagram._paletteGroups.map(group => `
			<div class="palette-group">
				<div class="palette-group-header">${_Helpers.escapeTags(group.name)}</div>
				<div class="palette-group-grid">
					${group.entries.map(p => `
						<button class="btn-palette" data-type="${p.type}" title="${_Helpers.escapeTags(p.label)}">
							${_ProcessDiagram._paletteGlyph(p.glyph)}
							<span class="palette-entry-label">${_Helpers.escapeTags(p.label)}</span>
						</button>
					`).join('')}
				</div>
			</div>
		`).join('');
		dialogText.innerHTML = `
			<div class="process-diagram-toolbar">
				<button class="action btn-fit">Fit</button>
				<button class="action btn-zoom-in">+</button>
				<button class="action btn-zoom-out">−</button>
				<span class="pd-toolbar-sep"></span>
				<button class="action btn-undo"    disabled>Undo</button>
				<button class="action btn-redo"    disabled>Redo</button>
				<span class="pd-toolbar-sep"></span>
				<button class="action btn-tidy" title="Snap shapes to grid and inset lanes inside their pools">Tidy</button>
				<span class="pd-toolbar-sep"></span>
				<button class="action btn-save"    disabled>Save</button>
				<button class="action btn-discard" disabled>Discard</button>
				<span class="pd-toolbar-sep"></span>
				<button class="action btn-toggle-xml">Show XML</button>
				<span class="diagram-dirty text-gray-500"></span>
				<span class="pd-spacer"></span>
				<span class="diagram-status text-gray-500"></span>
			</div>
			<div class="process-diagram-body">
				<div class="process-diagram-palette">
					${paletteHtml}
				</div>
				<div class="process-diagram-host"></div>
				<div class="process-diagram-sidepanel">
					<div class="text-gray-500">Select an element to see its properties.</div>
				</div>
			</div>
			<div class="process-diagram-xml is-hidden">
				<div class="process-diagram-xml-header">
					<span class="pd-bold">BPMN XML (live preview)</span>
					<button class="action btn-copy-xml">Copy</button>
					<span class="pd-spacer"></span>
					<span class="text-gray-500">read-only — reflects pending edits client-side</span>
				</div>
				<pre class="process-diagram-xml-pre"></pre>
			</div>
		`;
		const host       = dialogText.querySelector('.process-diagram-host');
		const status     = dialogText.querySelector('.diagram-status');
		const dirty      = dialogText.querySelector('.diagram-dirty');
		const saveBtn    = dialogText.querySelector('.btn-save');
		const undoBtn    = dialogText.querySelector('.btn-undo');
		const redoBtn    = dialogText.querySelector('.btn-redo');
		const discardBtn = dialogText.querySelector('.btn-discard');
		const palette    = dialogText.querySelector('.process-diagram-palette');
		const sidePanel  = dialogText.querySelector('.process-diagram-sidepanel');
		const xmlPane    = dialogText.querySelector('.process-diagram-xml');
		const xmlPre     = dialogText.querySelector('.process-diagram-xml-pre');
		const xmlBtn     = dialogText.querySelector('.btn-toggle-xml');
		const copyXmlBtn = dialogText.querySelector('.btn-copy-xml');

		// Instantiate the Community SVG backend. Swappable for Enterprise yFiles
		// at the constructor: the rest of this code binds only to the API surface.
		const api = new _BpmnDiagramCommunitySvg();
		_ProcessDiagram._api = api;

		api.mount(host, { editable: true });

		// Side-panel tab state. The Process tab is always reachable via
		// the tab strip; the Element tab shows the most-recently-selected
		// element (or a placeholder when nothing is selected). Selecting
		// an element on the canvas switches to the Element tab; deselecting
		// leaves the active tab alone so the user can stay in Process view
		// while clicking around to find an element.
		let activeTab = 'element';
		const tabs = (active) => _ProcessDiagram._renderSidePanelTabs(active);

		const renderSidePanelFor = (elementId) => {
			// Update status line regardless of which tab is active.
			if (!elementId) {
				status.textContent = 'No selection';
			}

			if (activeTab === 'process') {
				sidePanel.innerHTML = tabs('process') + _ProcessDiagram._renderProcessSettingsPanel(api);
				_ProcessDiagram._wireProcessSettingsPanel(sidePanel, api);
				return;
			}

			// activeTab === 'element'
			if (!elementId) {
				sidePanel.innerHTML = tabs('element') + `<div class="text-gray-500">Select an element on the canvas to edit its properties, or switch to the <b>Process</b> tab for process-wide settings.</div>`;
				return;
			}
			const elem = api.getElement && api.getElement(elementId);
			if (!elem) {
				status.textContent  = `Selected: ${elementId}`;
				sidePanel.innerHTML = tabs('element') + `<div class="text-gray-500">Element not found.</div>`;
				return;
			}
			const niceName = elem.bpmnName || elem.bpmnId || elem.id;
			status.textContent  = `Selected: ${niceName}`;
			sidePanel.innerHTML = tabs('element') + _ProcessDiagram._renderSidePanel(elem);

			// Inline name edit: commits on blur or Enter. Escape reverts.
			const nameInput = sidePanel.querySelector('.input-element-name');
			if (nameInput) {
				const original = nameInput.value;
				const commit = () => {
					const v = nameInput.value.trim();
					if (v === original) return;
					try { api.updateElement(elementId, { bpmnName: v, name: v || elem.bpmnId || elementId }); }
					catch (e) { console.error('rename failed', e); nameInput.value = original; }
				};
				nameInput.addEventListener('blur',    commit);
				nameInput.addEventListener('keydown', (e) => {
					if (e.key === 'Enter')      { e.preventDefault(); nameInput.blur(); }
					else if (e.key === 'Escape') { nameInput.value = original; nameInput.blur(); }
				});
			}

			const deleteBtn = sidePanel.querySelector('.btn-delete-element');
			if (deleteBtn) {
				deleteBtn.addEventListener('click', () => {
					try { api.deleteElement(elementId); }
					catch (e) { console.error('delete failed', e); }
				});
			}

			// True while the selected element only exists in the client-side
			// buffer (pending create, not yet saved). Server-side operations
			// (property dialog, setPerformers, handler methods) have nothing
			// to target until the element is persisted.
			const isBuffered = () => {
				if (!api.getPendingChanges) return false;
				const pending = api.getPendingChanges();
				return pending.creates.some(c => c.id === elementId);
			};

			const editBtn = sidePanel.querySelector('.btn-open-properties');
			if (editBtn) {
				editBtn.addEventListener('click', () => {
					// Buffered (not yet persisted) elements have no server-side
					// record yet, so the property dialog can't open them.
					if (isBuffered()) {
						new InfoMessage().title('Save first').text('Save your changes before opening the full property editor for this new element.').show();
						return;
					}
					_Dialogs.custom.clickDialogCancelButton();
					setTimeout(() => _Entities.showProperties(elem, 'ui'), 150);
				});
			}

			// Performer inputs (userTask only). Existing values are fetched
			// async (per-performer GET) and used to populate the inputs;
			// commit calls the setPerformers entity method which reconciles
			// (creates / updates / deletes BpmnPerformer nodes as needed).
			const assigneeInput     = sidePanel.querySelector('.input-performer-assignee');
			const candidatesInput   = sidePanel.querySelector('.input-performer-candidates');
			const candidatesPicker  = sidePanel.querySelector('.select-performer-candidate-principals');
			// Mark the picker's options that match `ids` as selected, idempotent
			// so that the population path (options arrive after the rel fetch)
			// and the rel-fetch path (rel ids arrive before the options) can
			// both call into this without ordering rules.
			const applyCandidatePickerSelection = (picker, ids) => {
				if (!picker || !Array.isArray(ids)) return;
				const set = new Set(ids);
				for (const opt of picker.options) opt.selected = set.has(opt.value);
			};
			// Set-equality for two id arrays. Used by commitPerformers to
			// decide whether the picker actually moved (multi-selects don't
			// fire change for "same set, different order").
			const sameStringSet = (a, b) => {
				if (a.length !== b.length) return false;
				const sa = new Set(a);
				for (const x of b) if (!sa.has(x)) return false;
				return true;
			};
			if (assigneeInput && candidatesInput && isBuffered()) {
				// Buffered (not yet persisted) elements have no server-side
				// record for setPerformers to target, so anything typed here
				// would be silently dropped on save. Disable the inputs with
				// a visible hint; the panel re-renders with enabled inputs
				// after the save.
				const msg = 'Save this new element before configuring performers.';
				for (const el of [assigneeInput, candidatesInput, candidatesPicker]) {
					if (!el) continue;
					el.disabled = true;
					el.title    = msg;
				}
				assigneeInput.closest('.pd-section')
					?.querySelector('.pd-section-title')
					?.insertAdjacentHTML('afterend', `<div class="pd-hint">${msg}</div>`);
			} else if (assigneeInput && candidatesInput) {
				const stubs = Array.isArray(elem.performers) ? elem.performers : [];
				let initialAssignee     = '';
				let initialCandidates   = '';
				let initialCandidateIds = [];
				const fetches = stubs.map(p => fetch(`${Structr.rootUrl}BpmnPerformer/${p.id}/ui`, { credentials: 'same-origin' }).then(r => r.ok ? r.json() : null).catch(() => null));
				Promise.all(fetches).then(bodies => {
					for (const body of bodies) {
						let perf = body?.result;
						if (Array.isArray(perf)) perf = perf[0];
						if (!perf) continue;
						if (perf.kind === 'humanPerformer') {
							initialAssignee = perf.expression || '';
						} else if (perf.kind === 'potentialOwner') {
							initialCandidates = perf.expression || '';
							// principals serialises as an array of {id, type, name} stubs.
							const ps = Array.isArray(perf.principals) ? perf.principals : [];
							initialCandidateIds = ps.map(p => (typeof p === 'string') ? p : (p?.id ?? null)).filter(Boolean);
						}
					}
					if (assigneeInput.value === '')   assigneeInput.value   = initialAssignee;
					if (candidatesInput.value === '') candidatesInput.value = initialCandidates;
					assigneeInput.dataset.original   = initialAssignee;
					candidatesInput.dataset.original = initialCandidates;
					if (candidatesPicker) {
						candidatesPicker.dataset.original = JSON.stringify(initialCandidateIds);
						applyCandidatePickerSelection(candidatesPicker, initialCandidateIds);
					}
				}).catch(() => { /* ignore */ });

				// Populate the principals picker from the graph (Users + Groups).
				// Single fetch reused across selections; the request is cheap and
				// the side panel re-renders from the same `elem` object on every
				// selection change.
				if (candidatesPicker) {
					Command.query('Principal', 1000, 1, 'name', 'asc', null, (principals) => {
						const opts = (principals ?? []).map(p => {
							const label = (p.type === 'Group' ? '[Group] ' : '') + (p.name ?? p.id);
							return `<option value="${_Helpers.escapeTags(p.id)}">${_Helpers.escapeTags(label)}</option>`;
						}).join('');
						candidatesPicker.insertAdjacentHTML('beforeend', opts);
						// Re-apply selection after options are populated.
						let preselect = [];
						try { preselect = JSON.parse(candidatesPicker.dataset.original || '[]'); } catch (_) {}
						applyCandidatePickerSelection(candidatesPicker, preselect);
					}, true, null, 'id,name,type');
				}

				const commitPerformers = async () => {
					// Backstop: the inputs are disabled while buffered, but the
					// buffered state can also arise between render and blur.
					if (isBuffered()) {
						new InfoMessage().title('Save first').text('Save this new element before configuring performers.').show();
						return;
					}
					const hp = assigneeInput.value.trim();
					const po = candidatesInput.value.trim();
					const pickerIds = candidatesPicker
						? Array.from(candidatesPicker.selectedOptions).map(o => o.value).filter(Boolean)
						: [];
					const pickerOriginal = candidatesPicker
						? JSON.parse(candidatesPicker.dataset.original || '[]')
						: [];
					const exprChanged    = hp !== (assigneeInput.dataset.original ?? '') || po !== (candidatesInput.dataset.original ?? '');
					const pickerChanged  = !sameStringSet(pickerIds, pickerOriginal);
					if (!exprChanged && !pickerChanged) return; // no change
					try {
						await _Processes.rest.callEntityMethod('BpmnElement', elementId, 'setPerformers', {
							humanPerformer:           hp,
							potentialOwner:           po,
							potentialOwnerPrincipals: pickerIds,
						});
						assigneeInput.dataset.original   = hp;
						candidatesInput.dataset.original = po;
						if (candidatesPicker) candidatesPicker.dataset.original = JSON.stringify(pickerIds);
						await api.refreshElement(elementId);
					} catch (err) {
						new ErrorMessage().text(`Setting performers failed: ${err.message}`).show();
					}
				};
				const onKey = (e) => {
					if (e.key === 'Enter') { e.preventDefault(); e.target.blur(); }
					else if (e.key === 'Escape') { e.target.value = e.target.dataset.original ?? ''; e.target.blur(); }
				};
				assigneeInput.addEventListener('blur', commitPerformers);
				candidatesInput.addEventListener('blur', commitPerformers);
				assigneeInput.addEventListener('keydown', onKey);
				candidatesInput.addEventListener('keydown', onKey);
				if (candidatesPicker) candidatesPicker.addEventListener('change', commitPerformers);
			}

			// Process / UI contract (userTask only): the subject type, the views that carve it up
			// per step, and the instructions text. Every control here persists its own property
			// through api.updateElement, so changes ride the editor's buffer and undo stack.

			// Views of the selected subject type. This is how a process designer says WHICH
			// properties a given step works on -- two steps on the same subject usually want
			// different subsets, and a view is the existing mechanism for naming a subset. The
			// picker is possible here (unlike render-template parameters) because the subject
			// type is a sibling field, so the type is known at design time.
			const formViewSelect     = sidePanel.querySelector('.input-subject-form-view');
			const writableViewSelect = sidePanel.querySelector('.input-subject-writable-view');

			// The subject type is process-level now: read it from the element's owning process so
			// the view pickers below list the right type's views. Only fall back to "the one
			// process" when there is exactly one -- in a multi-pool collaboration, borrowing
			// another pool's subject type would be wrong, so show none instead.
			const allProcesses       = (api.getProcesses?.() || []);
			const owningProcess       = allProcesses.find(p => p.id === _Processes.processRefId(elem))
			                          ?? (allProcesses.length === 1 ? allProcesses[0] : null);
			const processSubjectType = owningProcess?.subjectType || '';

			const loadSubjectViews = async (typeName) => {

				const selects = [formViewSelect, writableViewSelect].filter(Boolean);
				if (selects.length === 0) return;

				// reset to the placeholder plus whatever is currently stored, so a view that was
				// renamed or deleted stays visible instead of silently clearing the property
				for (const sel of selects) {
					const current     = sel.dataset.current || '';
					const placeholder = sel.querySelector('option[value=""]')?.outerHTML ?? '';
					sel.innerHTML     = placeholder + (current ? `<option value="${_Helpers.escapeTags(current)}" selected>${_Helpers.escapeTags(current)}</option>` : '');
				}

				if (!typeName) return;

				try {
					const nodes = await Command.queryPromise('SchemaNode', 1, 1, 'name', 'asc', { name: typeName }, true, null, 'id,name');
					if (!nodes || nodes.length === 0) return;

					const views = await Command.queryPromise('SchemaView', 1000, 1, 'name', 'asc', { schemaNode: nodes[0].id }, true, null, 'id,name');
					const names = (views ?? []).map(v => v.name).filter(Boolean);

					for (const sel of selects) {
						const current = sel.dataset.current || '';
						sel.insertAdjacentHTML('beforeend', names
							.filter(n => n !== current)
							.map(n => `<option value="${_Helpers.escapeTags(n)}">${_Helpers.escapeTags(n)}</option>`).join(''));
					}
				} catch (e) {
					console.error('fetching subject views failed', e);
				}
			};

			// The contract says the writable view is a subset of the form view. Nothing enforced
			// it, so a step could declare a writable field the form never shows. Checked on
			// commit rather than by filtering the dropdown: the views are authored elsewhere and
			// a warning explains the problem, whereas a silently shorter list would not.
			const warnIfNotSubset = async () => {

				const typeName = processSubjectType;
				const formName = formViewSelect?.value || '';
				const writName = writableViewSelect?.value || '';

				if (!typeName || !formName || !writName || formName === writName) return;

				const viewFields = async (viewName) => {
					const views = await Command.queryPromise('SchemaView', 1000, 1, 'name', 'asc', { name: viewName }, true, null, 'id,name,schemaProperties,nonGraphProperties');
					const view  = (views ?? []).find(v => v.name === viewName);
					if (!view) return null;
					const own   = (view.schemaProperties ?? []).map(p => p.name).filter(Boolean);
					const extra = (view.nonGraphProperties ?? '').split(',').map(v => v.trim()).filter(Boolean);
					return new Set([...own, ...extra]);
				};

				try {
					const formFields = await viewFields(formName);
					const writFields = await viewFields(writName);
					if (!formFields || !writFields) return;

					const outside = [...writFields].filter(f => !formFields.has(f));
					if (outside.length > 0) {
						new WarningMessage().text(
							`Writable view "${_Helpers.escapeTags(writName)}" contains ${outside.length} field(s) the form view "${_Helpers.escapeTags(formName)}" does not show: `
							+ `${_Helpers.escapeTags(outside.join(', '))}. Those fields can be written but never appear.`
						).show();
					}
				} catch (e) {
					console.error('checking writable view subset failed', e);
				}
			};

			for (const sel of [formViewSelect, writableViewSelect]) {
				if (sel) sel.addEventListener('change', warnIfNotSubset);
			}

			// The subject type is process-level (set on the Process tab); the per-step view
			// pickers just follow it. Populate them once from the owning process's type.
			loadSubjectViews(processSubjectType);

			// Commit wiring for the contract controls. An empty value clears the property.
			const contractInputs = [
				['.input-subject-form-view',      'subjectFormView'],
				['.input-subject-writable-view',  'subjectWritableView'],
				['.input-instructions',           'instructions'],
			];
			for (const [selector, propName] of contractInputs) {
				const el = sidePanel.querySelector(selector);
				if (!el) continue;
				const original = el.value;
				el.dataset.original = original;
				const commit = () => {
					const v = el.value.trim();
					if (v === el.dataset.original) return;
					try {
						api.updateElement(elementId, { [propName]: v.length === 0 ? null : v });
						el.dataset.original = v;
					} catch (e) {
						console.error(`updateElement ${propName} failed`, e);
						el.value = el.dataset.original;
					}
				};
				el.addEventListener('blur', commit);
				if (el.tagName === 'SELECT') {
					el.addEventListener('change', commit);
				}
				el.addEventListener('keydown', (e) => {
					// Enter commits only on single-line inputs; textareas keep Enter
					// for newline insertion.
					if (e.key === 'Enter' && el.tagName !== 'TEXTAREA') { e.preventDefault(); el.blur(); }
					else if (e.key === 'Escape') { el.value = el.dataset.original ?? ''; el.blur(); }
				});
			}

			// BPMN attributes key/value editor. Round-trips to the BPMN
			// element's XML attributes. Commits on blur of any row input
			// and on remove-button click; the new value is computed by
			// re-walking every row in the list (cheap; the list is short).
			// Commits go through `api.updateElement(...)` so the change
			// rides the editor's buffer / undo stack and is persisted on
			// the next overall Save.
			const attributesList = sidePanel.querySelector('.bpmn-attributes-list');
			const addAttributeBtn = sidePanel.querySelector('.btn-add-attribute');
			if (attributesList && addAttributeBtn) {
				const commitAttributes = () => {
					const attrs = {};
					for (const row of attributesList.querySelectorAll('.bpmn-attribute-row')) {
						const key = row.querySelector('.bpmn-attribute-key')?.value?.trim();
						if (!key) continue;          // empty key -> drop the entry
						const val = row.querySelector('.bpmn-attribute-value')?.value ?? '';
						attrs[key] = val;
					}
					const json = Object.keys(attrs).length === 0 ? null : JSON.stringify(attrs);
					try { api.updateElement(elementId, { bpmnAttributes: json }); }
					catch (e) { console.error('updateElement bpmnAttributes failed', e); }
				};
				const wireRow = (row) => {
					row.querySelectorAll('input').forEach(inp => inp.addEventListener('blur', commitAttributes));
					row.querySelector('.btn-remove-attribute')?.addEventListener('click', () => {
						row.remove();
						commitAttributes();
					});
				};
				// Wire any rows the initial render produced.
				attributesList.querySelectorAll('.bpmn-attribute-row').forEach(wireRow);

				addAttributeBtn.addEventListener('click', () => {
					const li = document.createElement('li');
					li.className = 'bpmn-attribute-row';
					li.style.cssText = 'display:flex; gap:4px; padding:3px 0; align-items:center;';
					li.innerHTML = `
						<input type="text" class="bpmn-attribute-key"   value="" placeholder="key"  >
						<input type="text" class="bpmn-attribute-value" value="" placeholder="value">
						<button class="action btn-remove-attribute" title="Remove this attribute">−</button>
					`;
					attributesList.appendChild(li);
					wireRow(li);
					li.querySelector('.bpmn-attribute-key').focus();
				});
			}

			// Task event handlers (userTask only). One handler per (event, phase):
			// a BpmnTaskListener pointing at a SchemaMethod that lives on the
			// element. Edits commit via Command.setProperties (the websocket
			// UPDATE path the rest of the editor uses), then refresh. Add creates
			// the method (auto-named) and the listener that calls it; remove
			// deletes both. Refused for buffered (not-yet-persisted) elements:
			// the rels can't land server-side until the element exists there.
			const TASK_EVENTS_W = ['created','assigned','claimed','available','declined','completed','cancelled'];

			// Build a valid SchemaMethod name from the task label + phase + event,
			// e.g. ("Review leave request","after","completed") -> reviewLeaveRequest_afterCompleted.
			const cap = (s) => s ? s.charAt(0).toUpperCase() + s.slice(1) : s;
			const buildMethodName = (phase, event) => {
				const label = (elem.bpmnName || elem.bpmnId || 'task');
				const parts = label.split(/[^a-zA-Z0-9]+/).filter(Boolean);
				let base = parts.length
					? parts[0].toLowerCase() + parts.slice(1).map(cap).join('')
					: 'task';
				if (!/^[a-z_]/.test(base)) base = 'task' + cap(base);
				return `${base}_${phase}${cap(event)}`;
			};

			// Open a SchemaMethod in the Code module. Shared by the handler-row
			// Edit buttons and the read-only methods list.
			const openMethodInCode = (methodId) => {
				if (!methodId) return;
				_Dialogs.custom.clickDialogCancelButton();
				setTimeout(async () => {
					try {
						const res = await fetch(`${Structr.rootUrl}SchemaMethod/${methodId}/ui`, { credentials: 'same-origin' });
						if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
						const body = await res.json();
						let m = body.result;
						if (Array.isArray(m)) m = m[0];
						if (m) _Code.helpers.navigateToSchemaObjectFromAnywhere(m);
					} catch (e) {
						console.error('open method failed', methodId, e);
						new ErrorMessage().text(`Failed to open method: ${e.message}`).show();
					}
				}, 150);
			};

			const taskListenersList  = sidePanel.querySelector('.bpmn-task-listeners-list');
			const addTaskListenerBtn = sidePanel.querySelector('.btn-add-task-listener');
			if (taskListenersList && addTaskListenerBtn) {

				const escTL    = _Helpers.escapeTags;
				const TASK_PHASES_W = ['after','on'];
				const countEl  = sidePanel.querySelector('.bpmn-task-listeners-count');

				// Persist via the websocket UPDATE command (Command.setProperties),
				// the path the VM editor uses successfully on process-module nodes.
				const setProps = (id, props) => new Promise((resolve, reject) => {
					try { Command.setProperties(id, props, () => resolve()); }
					catch (e) { reject(e); }
				});
				const methodOf = (tl) => {
					const m = Array.isArray(tl.method) ? tl.method[0] : tl.method;
					return (m && typeof m === 'object') ? m : null;
				};
				const rowHtml = (tl) => {
					const m = methodOf(tl);
					return `
					<li class="bpmn-listener-row bpmn-task-listener-row" data-listener-id="${escTL(tl.id)}" data-method-id="${escTL(m?.id ?? '')}">
						<select class="bpmn-listener-phase bpmn-task-listener-phase" title="on: pre-commit, can veto. after: post-commit, side-effects.">
							${TASK_PHASES_W.map(p => `<option value="${p}" ${p === (tl.phase ?? 'after') ? 'selected' : ''}>${p}</option>`).join('')}
						</select>
						<select class="bpmn-listener-event bpmn-task-listener-event">
							${TASK_EVENTS_W.map(e => `<option value="${e}" ${e === tl.event ? 'selected' : ''}>${e}</option>`).join('')}
						</select>
						<button class="border-0 btn-edit-listener-method" data-method-id="${escTL(m?.id ?? '')}" title="Edit the handler method">
							${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(), 'Edit')}
						</button>
						<button class="border-0 btn-remove-task-listener" title="Remove this handler (deletes its method)">
						    ${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesNonColorIcon(), 'Delete')}
						</button>
					</li>`;
				};

				let loadedListeners = [];

				const wireListenerRow = (row) => {
					const listenerId = row.dataset.listenerId;
					const methodId   = row.dataset.methodId;
					if (!listenerId) return;
					const eventSel = row.querySelector('.bpmn-task-listener-event');
					const phaseSel = row.querySelector('.bpmn-task-listener-phase');
					const editBt   = row.querySelector('.btn-edit-listener-method');
					const removeBt = row.querySelector('.btn-remove-task-listener');

					const commit = async () => {
						const event = eventSel.value;
						const phase = phaseSel.value;
						// Enforce one handler per (event, phase): reject a change that
						// collides with another existing handler.
						const collides = loadedListeners.some(tl =>
							tl.id !== listenerId && tl.event === event && (tl.phase ?? 'after') === phase);
						if (collides) {
							new ErrorMessage().text(`A handler for "${event}" (${phase}) already exists on this task.`).show();
							await loadRows(); // revert the select to the persisted value
							return;
						}
						eventSel.disabled = phaseSel.disabled = true;
						try {
							await setProps(listenerId, { event, phase });
							// Keep the auto-named method reflective of (event, phase). Best-effort.
							if (methodId) {
								try { await setProps(methodId, { name: buildMethodName(phase, event) }); }
								catch (_) {}
							}
							await loadRows();
							await api.refreshElement(elementId);
						} catch (err) {
							new ErrorMessage().text(`Update handler failed: ${err.message ?? err}`).show();
							await loadRows();
						}
					};
					eventSel?.addEventListener('change', commit);
					phaseSel?.addEventListener('change', commit);
					editBt?.addEventListener('click', () => openMethodInCode(methodId));
					removeBt?.addEventListener('click', async () => {
						removeBt.disabled = true;
						// Update the view immediately: the DELETE callback is unreliable
						// and a GET re-fetch can be cache-served, so don't depend on
						// either for the visual removal.
						loadedListeners = loadedListeners.filter(tl => tl.id !== listenerId);
						if (countEl) countEl.textContent = `(${loadedListeners.length})`;
						row.remove();

						// Awaitable delete with a fallback so an unreliable DELETE callback can't hang us.
						const del = (id) => new Promise((resolve) => {
							let settled = false;
							const done = () => { if (!settled) { settled = true; resolve(); } };
							Command.deleteNode(id, undefined, done);
							window.setTimeout(done, 1500);
						});

						// 1:1 listener <-> method. Always delete the listener. Delete its method
						// too, but ONLY when the body is empty: never destroy code the user wrote.
						await del(listenerId);
						if (methodId) {
							const source = await new Promise((resolve) => {
								let settled = false;
								const finish = (v) => { if (!settled) { settled = true; resolve(v); } };
								Command.get(methodId, 'id,source', (m) => finish(m ? (m.source || '') : null));
								window.setTimeout(() => finish(null), 1500);   // unknown -> treat as non-empty (keep)
							});
							if (source !== null && source.trim() === '') {
								await del(methodId);
							}
						}
						// Re-fetch the element so the method-count badge reflects the new state.
						await api.refreshElement(elementId);
					});
				};

				// Populate rows from a dedicated listener fetch (full event/phase/
				// method), NOT from the element's reduced nested serialization.
				const loadRows = async () => {
					try {
						const res  = await fetch(`${Structr.rootUrl}BpmnTaskListener/ui?element=${elementId}`, { credentials: 'same-origin', cache: 'no-store' });
						const body = res.ok ? await res.json() : { result: [] };
						loadedListeners = Array.isArray(body.result) ? body.result : (body.result ? [body.result] : []);
					} catch (_) {
						loadedListeners = [];
					}
					taskListenersList.innerHTML = loadedListeners.map(rowHtml).join('');
					if (countEl) countEl.textContent = `(${loadedListeners.length})`;
					taskListenersList.querySelectorAll('.bpmn-task-listener-row').forEach(wireListenerRow);
				};
				loadRows();

				addTaskListenerBtn.addEventListener('click', async () => {
					if (isBuffered()) {
						new InfoMessage().title('Save first').text('Save this new element before adding handlers to it.').show();
						return;
					}
					// Pick the first free (event, phase) pair: all 'after' slots
					// first (the common case), then 'on'. Refuse when every
					// combination already has a handler, so we never create a
					// duplicate (event, phase).
					const usedPairs = new Set(loadedListeners.map(tl => `${tl.event}|${tl.phase ?? 'after'}`));
					let event = null, phase = null;
					for (const ph of TASK_PHASES_W) {
						const free = TASK_EVENTS_W.find(e => !usedPairs.has(`${e}|${ph}`));
						if (free) { event = free; phase = ph; break; }
					}
					if (!event) {
						new InfoMessage().title('All handlers exist').text('Every event/phase combination already has a handler on this task.').show();
						return;
					}
					addTaskListenerBtn.disabled = true;
					try {
						const methodName = buildMethodName(phase, event);

						// 1. Create the handler method already linked to the element
						// via its `bpmnElement` inverse rel. We set the rel on the
						// method (not the element's `methods` collection): rewriting
						// the collection from the stale in-memory elem.methods would
						// detach every method added earlier in the session.
						const methodId = await new Promise((resolve, reject) => {
							Command.create({ type: 'SchemaMethod', name: methodName, source: '', bpmnElement: elementId },
								(m) => (m && m.id) ? resolve(m.id) : reject(new Error('SchemaMethod creation failed')));
						});

						// 2. Create the listener pointing at the method.
						await new Promise((resolve, reject) => {
							Command.create(
								{ type: 'BpmnTaskListener', element: elementId, event, phase, method: methodId },
								(n) => (n && n.id) ? resolve(n.id) : reject(new Error('BpmnTaskListener creation failed')));
						});

						await loadRows();
						await api.refreshElement(elementId);
					} catch (err) {
						new ErrorMessage().text(`Add handler failed: ${err.message ?? err}`).show();
					} finally {
						addTaskListenerBtn.disabled = false;
					}
				});
			}
		};

		// Tab strip clicks are handled via delegation so the listener
		// survives every side-panel re-render. Switching to the Element
		// tab shows whatever is currently selected on the canvas (or the
		// placeholder if nothing is selected); switching to Process never
		// changes canvas selection.
		sidePanel.addEventListener('click', (e) => {
			const btn = e.target.closest('.sidepanel-tab');
			if (!btn) return;
			const newTab = btn.dataset.tab;
			if (newTab === activeTab) return;
			activeTab = newTab;
			renderSidePanelFor(api.getSelected && api.getSelected());
		});

		api.onSelect((id) => {
			// Canvas selection forces the Element tab so the user sees the
			// element they just clicked. Deselecting (id == null) leaves
			// the active tab alone -- if they were on Process, stay there.
			if (id) activeTab = 'element';
			renderSidePanelFor(id);
		});
		// Re-render the side panel when the underlying data changes (e.g.
		// after rename, after undo/redo restores values, after a process
		// flag toggles). Two cases tracked by activeTab:
		//   * Element tab: re-render iff the changed entity is the selected
		//     one. Other entities' updates would otherwise rebuild a panel
		//     that already reflects their state via direct mutation.
		//   * Process tab: re-render whenever any BpmnProcess changes
		//     (typically undo/redo of a flag toggle), independent of
		//     canvas selection.
		api.onUpdate((id, type) => {
			const selected = api.getSelected && api.getSelected();
			if (activeTab === 'process') {
				if (type === 'BpmnProcess') renderSidePanelFor(selected);
				return;
			}
			if (selected && id === selected) renderSidePanelFor(selected);
		});

		// Render once on mount so the tab strip is visible immediately and
		// users see the Process tab without first having to interact with
		// the canvas. Replaces the static placeholder set in the dialog HTML.
		renderSidePanelFor(api.getSelected && api.getSelected());

		const updateDirty = () => {
			const has = api.hasPendingChanges && api.hasPendingChanges();
			saveBtn.disabled    = !has;
			discardBtn.disabled = !has;
			undoBtn.disabled    = !(api.canUndo && api.canUndo());
			redoBtn.disabled    = !(api.canRedo && api.canRedo());
			dirty.textContent = has ? '● Unsaved changes' : '';
			dirty.style.color = has ? '#c0392b' : '';
		};

		// Refresh the dirty / undo / redo indicators on every buffer change.
		api.onPendingChanged(updateDirty);

		// Palette: click to arm an element type. Re-clicking the active type
		// cancels. Visual highlighting follows api.onPlacementChanged so it
		// reflects programmatic cancels too (e.g. after a placement click).
		palette.querySelectorAll('.btn-palette').forEach(btn => {
			btn.addEventListener('click', () => {
				const t = btn.dataset.type;
				const currentArmed = palette.querySelector('.btn-palette.active')?.dataset?.type;
				if (currentArmed === t) {
					api.cancelPlacement();
				} else {
					api.armElementType(t);
				}
			});
		});
		api.onPlacementChanged((armedType) => {
			palette.querySelectorAll('.btn-palette').forEach(b => {
				b.classList.toggle('active', b.dataset.type === armedType);
			});
		});

		// Keyboard shortcuts. Attached to document so they work regardless of
		// focus inside the dialog, but we ignore events whose target is an
		// editable text field (input / textarea / contenteditable) so the
		// user can still type in inline editors.
		const onKeyDown = (e) => {
			const t = e.target;
			const tag = t?.tagName || '';
			const isEditable = (tag === 'INPUT' || tag === 'TEXTAREA' || (t && t.isContentEditable));
			if (isEditable) return;

			if (e.key === 'Escape') {
				api.cancelPlacement();
				return;
			}
			if (e.key === 'Delete' || e.key === 'Backspace') {
				const selectedFlowId = api.getSelectedFlow && api.getSelectedFlow();
				if (selectedFlowId) {
					e.preventDefault();
					api.deleteFlow(selectedFlowId);
					return;
				}
				const selectedId = api.getSelected && api.getSelected();
				if (selectedId) {
					e.preventDefault();
					api.deleteElement(selectedId);
				}
				return;
			}
			const meta = (e.metaKey || e.ctrlKey);
			if (meta && (e.key === 'z' || e.key === 'Z')) {
				e.preventDefault();
				if (e.shiftKey) api.redo(); else api.undo();
				return;
			}
			if (meta && (e.key === 'y' || e.key === 'Y')) {
				e.preventDefault();
				api.redo();
				return;
			}
		};
		document.addEventListener('keydown', onKeyDown);
		detachKeydown = () => document.removeEventListener('keydown', onKeyDown);

		undoBtn.addEventListener('click',    () => api.undo());
		redoBtn.addEventListener('click',    () => api.redo());

		// Tidy: snaps shapes to the grid and insets lanes inside their pools.
		// Recorded as a single undo entry so the user can ctrl-Z back to the
		// imported layout if they don't like the result.
		const tidyBtn = dialogText.querySelector('.btn-tidy');
		if (tidyBtn) {
			tidyBtn.addEventListener('click', () => {
				if (api.tidy) api.tidy();
			});
		}

		discardBtn.addEventListener('click', async () => {
			if (!api.hasPendingChanges()) return;
			discardBtn.disabled = true;
			try { await api.discardPendingChanges(); }
			finally { updateDirty(); }
		});

		// XML preview pane. Sourced from the server-side BpmnExporter --
		// the canonical exporter that walks the persisted graph state --
		// so what you see here is exactly what an Export download would
		// produce. The previous client-side serialiser was deliberately
		// lossy (sub-process containment, listeners, performers,
		// namespace declarations, bpmnAttributes passthrough -- the
		// drift list grew with every new BPMN feature), and "preview"
		// disagreeing with "export" was a recurring source of surprise.
		//
		// Refresh triggers:
		//   * Pane is opened          -> fetch (lazy; no fetch while hidden).
		//   * A save completes        -> re-fetch IF pane is visible.
		//   * Buffer dirty / clean    -> only the stale banner flips
		//     synchronously; no network. The banner tells the user the
		//     XML reflects the last saved state and they need to Save to
		//     refresh it -- live keystroke-by-keystroke updates would
		//     require round-tripping the buffer through the exporter,
		//     which only reads persisted state.
		let lastFetchedXml = null;
		const renderStaleBanner = () => {
			const has = api.hasPendingChanges && api.hasPendingChanges();
			let banner = xmlPane.querySelector('.process-diagram-xml-stale');
			if (has && lastFetchedXml !== null) {
				if (!banner) {
					banner = document.createElement('div');
					banner.className = 'process-diagram-xml-stale';
					banner.style.cssText = 'background:#fef3c7; color:#92400e; padding:6px 8px; font-size:11px; border-bottom:1px solid #fcd34d;';
					banner.textContent = 'Unsaved changes pending — XML reflects the last saved state. Save to refresh.';
					xmlPane.insertBefore(banner, xmlPre);
				}
			} else if (banner) {
				banner.remove();
			}
		};
		const refreshXml = async () => {
			if (xmlPane.classList.contains('is-hidden')) return;
			try {
				const resp = await _Processes.rest.callEntityMethod('BpmnDefinitions', def.id, 'exportBpmn');
				const xml  = (typeof resp === 'string') ? resp : (resp?.result ?? '');
				lastFetchedXml = xml || '';
				xmlPre.textContent = lastFetchedXml;
			} catch (e) {
				xmlPre.textContent = `(server export failed: ${e.message})`;
			}
			renderStaleBanner();
		};
		// Re-fetch after every save; show stale banner while edits are buffered.
		api.onPendingChanged(renderStaleBanner);
		xmlBtn.addEventListener('click', () => {
			const showing = !xmlPane.classList.contains('is-hidden');
			if (showing) {
				xmlPane.classList.add('is-hidden');
				xmlBtn.textContent    = 'Show XML';
			} else {
				xmlPane.classList.remove('is-hidden');
				xmlBtn.textContent    = 'Hide XML';
				refreshXml();
			}
		});
		copyXmlBtn.addEventListener('click', async () => {
			const text = xmlPre.textContent || '';
			try {
				await navigator.clipboard.writeText(text);
				new SuccessMessage().text('XML copied to clipboard.').show();
			} catch (e) {
				new ErrorMessage().text(`Copy failed: ${e.message}`).show();
			}
		});

		// Cross-tab notification: another tab of the same user just saved
		// changes to this definition. Surface a non-modal toast with a Reload
		// button -- mirrors the schema-recompile flow in schema.js so the user
		// stays in control of when their view refreshes (especially important
		// if they have unsaved local edits).
		api.onRemoteChange((args) => {
			const versionPart = args?.version ? ` (version ${_Helpers.escapeTags(String(args.version))})` : '';
			const text = (api.hasPendingChanges && api.hasPendingChanges())
				? `Another tab saved changes to this process${versionPart}. Reloading will discard your unsaved local changes.`
				: `Another tab saved changes to this process${versionPart}. Reload to see them.`;
			new InfoMessage()
				.title('Process diagram changed')
				.text(text)
				.specialInteractionButton('Reload', () => {
					api.discardPendingChanges()
						.then(() => updateDirty())
						.catch(e => console.error('remote-change reload failed', e));
				})
				.uniqueClass('process-diagram')
				.incrementsUniqueCount()
				.show();
		});

		dialogText.querySelector('.btn-fit').addEventListener('click', () => api.fit());
		dialogText.querySelector('.btn-zoom-in').addEventListener('click',  () => api.zoom(1.2));
		dialogText.querySelector('.btn-zoom-out').addEventListener('click', () => api.zoom(1 / 1.2));

		saveBtn.addEventListener('click', async () => {
			saveBtn.disabled = true;
			dirty.textContent = 'Saving…';
			dirty.style.color = '';
			try {
				const res = await api.savePendingChanges();
				if (res.ok) {
					new SuccessMessage().text('Saved.').show();
					// Refresh the XML preview from the server now that the
					// buffer has been flushed; the freshly-persisted state
					// is what the server-side exporter will read.
					refreshXml();
					// Re-render the side panel: buffered-only restrictions
					// (disabled performer inputs, "save first" guards) no
					// longer apply now that the elements are persisted.
					renderSidePanelFor(api.getSelected && api.getSelected());
				} else {
					new ErrorMessage().text(`Save failed: ${res.error || 'unknown error'}`).show();
				}
			} finally {
				updateDirty();
			}
		});

		api.load({ definitionId: def.id })
			.then(() => {
				// Auto-snap during load may have produced pending moves;
				// reflect that in the toolbar so the user can Save them.
				updateDirty();
			})
			.catch(e => {
				console.error('diagram load failed', e);
				new ErrorMessage().text('Failed to load diagram: ' + e.message).show();
			});
	},

	// Tab strip rendered at the top of the side panel so process-level
	// settings are reachable in one click regardless of canvas selection
	// state. Without this strip the Process settings view was only shown
	// when nothing was selected, which made discovering it depend on
	// knowing to click on empty canvas.
	_renderSidePanelTabs: (activeTab) => {
		const tab = (id, label) => {
			const isActive = (id === activeTab);
			return `<button type="button" class="sidepanel-tab${isActive ? ' active' : ''}" data-tab="${id}">${label}</button>`;
		};
		return `<div class="process-diagram-sidepanel-tabs">
			${tab('element', 'Element')}${tab('process', 'Process')}
		</div>`;
	},

	// Render the side-panel "Process settings" view that's shown when
	// the Process tab is active. Lists every BpmnProcess in the active
	// BpmnDefinitions and exposes Structr-only configuration that the
	// BPMN importer can't represent (the spec doesn't carry these), and
	// that would otherwise have to be set via the property editor.
	//
	// Why here and not on per-element panels: these settings govern
	// process-wide behaviour (assignment fallback, instance-page
	// binding). Selecting a single element to configure a process-wide
	// option is the wrong mental model.
	// Long-form help for the Process settings panel, rendered as info icons at wire
	// time via the admin UI's data-comment convention (_Helpers.activateCommentsInElement).
	// It used to sit inline as four permanently visible paragraphs, which in a ~330px
	// panel pushed the controls themselves below the fold. Inline text is reserved for
	// state that changes what a control does; explanations live here.
	_processSettingsHints: {
		panel:         'Structr-specific settings that are <strong>not part of the BPMN spec</strong>, so they are not preserved across re-imports. They control engine and routing behaviour at runtime.',
		subjectType:   'The schema type of the single domain object (the "subject") this process operates on — e.g. <code>Claim</code>, <code>LeaveRequest</code>.<br><br>A process instance has <strong>one</strong> subject, so its type is a process-level fact. Individual user tasks pick <em>which fields</em> of this type they show or write (via the form / writable view on each step), but the type is chosen here once. Process-bound widgets bind to the <code>current</code> process instance and use this type as their <em>expected type</em> (which fields to render).',
		autoAssign:    'When a userTask has no <code>humanPerformer</code> declared, the engine reserves the task for the user who started the instance.<br><br>Useful for editor-authored processes where assignment is not wired up explicitly. Off by default so imported BPMN keeps spec semantics.',
		instancePage:  'The page that renders an instance of this process. The Start-process EAM action navigates to <code>/&lt;page-name&gt;/&lt;instance-uuid&gt;</code>.<br><br>When unset, the URL falls back to the slugified process name.',
		pageTemplate:  'The page-template Widget the generated page is built from — the same templates the Pages area offers under "Create New Page". Page templates come from the widget set, which has to be imported once; without one, the page is a bare <code>html/head/body</code> shell.<br><br>The step divs are appended into the template\'s <strong>Main Content</strong> node — the content area page templates mark by that name. A template without one gets them in its <code>&lt;body&gt;</code>.',
		pageSkeleton:  'Generates a page with one empty <code>&lt;div&gt;</code> per step that needs a human — userTasks, the start event, manual tasks and message catch events — in flow order.<br><br>Each div is bound to its step by a VisibilityMapping, so it only renders when that step is actionable. Machine steps (service and script tasks, gateways) are skipped. The new page is bound above when no instance page is set yet; fill the divs in the Pages area.',
		eventHandlers: 'Runs a method on a process lifecycle event. <code>on</code> runs pre-commit and can veto; <code>after</code> runs post-commit.'
	},

	_renderProcessSettingsPanel: (api) => {
		const esc = (v) => v == null ? '' : _Helpers.escapeTags(String(v));
		const procs = (api.getProcesses && api.getProcesses()) || [];
		if (procs.length === 0) {
			return `<div class="text-gray-500">Select an element to see its properties.</div>`;
		}
		// instancePage is serialized as a {id, type, name} stub on the
		// process node when the rel is set; null/undefined when not.
		const extractPageId = (rel) => {
			if (!rel) return '';
			if (typeof rel === 'string') return rel;
			if (Array.isArray(rel)) return rel[0]?.id ?? (typeof rel[0] === 'string' ? rel[0] : '');
			return rel.id ?? '';
		};
		const hints = _ProcessDiagram._processSettingsHints;
		const comment = (key) => `data-comment="${_Helpers.escapeForHtmlAttributes(hints[key])}"`;
		const section = (proc) => {
			const name = proc.processName || proc.name || proc.processId || proc.id;
			const checked = proc.defaultAssigneeFromInitiator ? 'checked' : '';
			const boundPageId = extractPageId(proc.instancePage);
			const boundPageName = (proc.instancePage && proc.instancePage.name) || '';
			return `
				<div class="process-settings-section" data-process-id="${esc(proc.id)}" data-process-name="${esc(name)}">
					<label class="pd-field">
						<div class="pd-field-label">Process name</div>
						<input type="text" class="input-process-name" value="${esc(proc.processName ?? '')}" placeholder="${esc(proc.processId ?? 'Process')}">
					</label>

					<label class="pd-field">
						<div class="pd-label-row">
							<span>Subject type</span>
							<span class="pd-info" ${comment('subjectType')}></span>
						</div>
						<select class="input-process-subject-type" data-current="${esc(proc.subjectType ?? '')}">
							<option value="">— No subject type —</option>
							${proc.subjectType ? `<option value="${esc(proc.subjectType)}" selected>${esc(proc.subjectType)}</option>` : ''}
						</select>
					</label>

					<div class="pd-inline-row pd-mb-6">
						<label class="pd-checkbox-label pd-mb-0">
							<input type="checkbox" class="chk-default-assignee-from-initiator" ${checked}>
							<span class="pd-text-12">Auto-assign tasks to the initiator</span>
						</label>
						<span class="pd-info" ${comment('autoAssign')}></span>
					</div>

					<div class="pd-section">
						<div class="pd-label-row">
							<span>Instance page</span>
							<span class="pd-info" ${comment('instancePage')}></span>
						</div>
						<select class="select-instance-page" data-bound-page-id="${esc(boundPageId)}">
							<option value="">— No page bound —</option>
						</select>
						<div class="pd-label-row pd-field-mt6">
							<span>Page template</span>
							<span class="pd-info" ${comment('pageTemplate')}></span>
						</div>
						<select class="select-page-template">
							<option value="">— Loading templates… —</option>
						</select>
						<span class="pd-state-note no-page-templates" hidden></span>

						<div class="pd-button-row">
							<button class="action btn-create-page-skeleton" title="Generate a page with one empty div per step that requires user interaction">${boundPageName ? 'Create another page skeleton' : 'Create page skeleton'}</button>
							<span class="pd-info" ${comment('pageSkeleton')}></span>
						</div>
						${boundPageName ? `<span class="pd-state-note"><strong>${esc(boundPageName)}</strong> is already bound — this adds a second page.</span>` : ''}
					</div>

					<div class="bpmn-listener-block">
						<div class="bpmn-listener-header">
							<div class="bpmn-listener-title">
								Process event handlers <span class="bpmn-process-listeners-count"></span>
								<span class="pd-info" ${comment('eventHandlers')}></span>
							</div>
							<button class="action btn-add-process-listener" title="Add a handler that runs on a process lifecycle event">+ Add handler</button>
						</div>
						<ul class="bpmn-listener-list bpmn-process-listeners-list"></ul>
					</div>
				</div>
			`;
		};
		return `
			<div class="pd-heading-row">
				<h4 class="pd-heading">Process settings</h4>
				<span class="pd-info" ${comment('panel')}></span>
			</div>
			${procs.map(section).join('')}
		`;
	},

	// Wire the inputs inside the Process settings panel. Each commit
	// goes through the editor's `updateProcess` API so the change rides
	// the buffer / undo stack alongside element edits and is persisted
	// on Save (not immediately). The Page picker is populated lazily
	// once Pages have been fetched (Command.query is async); the saved
	// selection is restored from the section's data-bound-page-id.
	_wireProcessSettingsPanel: (sidePanel, api) => {
		// Turn every data-comment anchor in the panel into an info icon.
		_Helpers.activateCommentsInElement(sidePanel);

		const pageOption = (id, name) => `<option value="${_Helpers.escapeForHtmlAttributes(id)}">${_Helpers.escapeTags(name)}</option>`;
		// Populate every Page picker on this panel from a single fetch.
		// Hidden pages (preview / partial / template containers) are
		// excluded -- the binding is for user-facing instance pages.
		const pageSelects = sidePanel.querySelectorAll('.select-instance-page');
		if (pageSelects.length > 0) {
			Command.query('Page', 1000, 1, 'name', 'asc', { hidden: false }, (pages) => {
				const opts = (pages ?? []).map(p => pageOption(p.id, p.name ?? p.id)).join('');
				for (const sel of pageSelects) {
					sel.insertAdjacentHTML('beforeend', opts);
					if (sel.dataset.boundPageId) sel.value = sel.dataset.boundPageId;
				}
			}, true, null, 'id,name');
		}
		for (const section of sidePanel.querySelectorAll('.process-settings-section')) {
			const procId    = section.dataset.processId;
			const initBox   = section.querySelector('.chk-default-assignee-from-initiator');
			const pageSel   = section.querySelector('.select-instance-page');
			const nameInput = section.querySelector('.input-process-name');
			if (procId && nameInput) {
				// Commit on blur (change), buffered like the other process
				// settings. Sets BpmnProcess.processName, which is what the
				// Pages Events-tab process picker labels each process with.
				nameInput.addEventListener('change', () => {
					try {
						api.updateProcess(procId, { processName: nameInput.value });
					} catch (e) {
						console.error('updateProcess failed', e);
						nameInput.value = nameInput.defaultValue;
					}
				});
			}
			if (procId && initBox) {
				initBox.addEventListener('change', () => {
					try {
						api.updateProcess(procId, { defaultAssigneeFromInitiator: !!initBox.checked });
					} catch (e) {
						console.error('updateProcess failed', e);
						initBox.checked = !initBox.checked;  // revert on refusal
					}
				});
			}
			// Subject type: the process-level domain type this process operates on. Custom
			// schema types only -- service classes are excluded (they hold static methods and
			// are never instantiated, so they cannot be a subject). Commit clears the property
			// on empty. Changing it drives every step's form on the next render.
			const subjectTypeSel = section.querySelector('.input-process-subject-type');
			if (procId && subjectTypeSel) {

				const current = subjectTypeSel.dataset.current || '';
				fetch(`${Structr.rootUrl}_schema`).then(r => r.ok ? r.json() : null).then(json => {
					const types = (json?.result ?? [])
						.filter(t => !t.isBuiltin && !t.isRel && !t.isServiceClass && t.className !== current)
						.map(t => t.className)
						.sort((a, b) => a.localeCompare(b));
					subjectTypeSel.insertAdjacentHTML('beforeend',
						types.map(t => `<option value="${_Helpers.escapeTags(t)}">${_Helpers.escapeTags(t)}</option>`).join(''));
				}).catch(e => console.error('fetching schema types for subject type failed', e));

				subjectTypeSel.addEventListener('change', () => {
					try {
						api.updateProcess(procId, { subjectType: subjectTypeSel.value || null });
						subjectTypeSel.dataset.current = subjectTypeSel.value;
					} catch (e) {
						console.error('updateProcess subjectType failed', e);
						subjectTypeSel.value = subjectTypeSel.dataset.current || '';
					}
				});
			}
			// Generate an instance-page skeleton for this process: one empty div per
			// human-facing step, each with a VisibilityMapping bound to that step.
			// Server-side (BPMN_PAGE_SKELETON) so page, divs and mappings land in one
			// transaction instead of a chain of createDOMNode round trips.
			// Generate an instance-page skeleton for this process: one empty div per
			// human-facing step, each with a VisibilityMapping bound to that step.
			// Server-side (BPMN_PAGE_SKELETON) so page, divs and mappings land in one
			// transaction instead of a chain of createDOMNode round trips.
			const skeletonBtn = section.querySelector('.btn-create-page-skeleton');
			const templateSel = section.querySelector('.select-page-template');
			const noTemplates = section.querySelector('.no-page-templates');

			// Page templates are Widgets flagged isPageTemplate -- the same set the Pages
			// area's "Create New Page" dialog offers. They arrive with the widget set, which
			// has to be imported once, so an empty list is a normal first-run state and gets
			// an import offer rather than an empty dropdown.
			const SHELL_OPTION = '<option value="">— Bare html/head/body shell —</option>';

			const loadPageTemplates = async () => {
				if (!templateSel) return;
				let templates = [];
				try {
					templates = (await _Widgets.fetchAllPageTemplateWidgets()) ?? [];
				} catch (e) {
					console.error('fetching page templates failed', e);
				}
				templateSel.innerHTML = SHELL_OPTION + templates.map(w => pageOption(w.id, w.name ?? w.id)).join('');
				templateSel.hidden    = (templates.length === 0);
				noTemplates.hidden    = (templates.length > 0);
				if (templates.length === 0) {
					noTemplates.innerHTML = 'No page templates yet — they come with the widget set. '
						+ '<button class="btn-import-widget-set">Import widget set</button> '
						+ 'or generate a bare page shell now.';
					noTemplates.querySelector('.btn-import-widget-set').addEventListener('click', async (e) => {
						e.currentTarget.disabled = true;
						if (await _Widgets.importDefaultWidgetSet()) {
							new SuccessMessage().text('Widget set imported.').show();
							await loadPageTemplates();
						} else {
							e.currentTarget.disabled = false;
							new ErrorMessage().text('Could not import the widget set from structr.com.').show();
						}
					});
				}
			};
			loadPageTemplates();

			if (procId && skeletonBtn) {
				skeletonBtn.addEventListener('click', async () => {
					// The skeleton is built from persisted state, so unsaved diagram
					// edits (a new userTask, a renamed step) wouldn't be in it.
					if (api.hasPendingChanges && api.hasPendingChanges()) {
						new WarningMessage().text('Save your diagram changes first — the skeleton is generated from the saved process.').show();
						return;
					}
					// Pre-flight: forms are only inserted when the process declares a subject type
					// (process-level, one subject per instance). Without one, every user-task step
					// gets a bare div and no form -- surface that up front so "0 forms" is a choice,
					// not a surprise. The select reflects the saved value here (no pending changes).
					// Empty divs are still useful scaffolding, so warn-and-confirm rather than block.
					const subjectType = (sidePanel.querySelector('.input-process-subject-type')?.value || '').trim();
					if (!subjectType) {
						const proceed = await _Dialogs.confirmation.showPromise(
							`This process has <strong>no subject type</strong> set, so the generated user-task steps will get <strong>no forms</strong> — just empty step divs.<br><br>` +
							`Set the <strong>Subject type</strong> above (and save) first to get the forms, or create the skeleton without forms now?`
						);
						if (!proceed) return;
					}
					// Confirm rather than disable when a page is already bound: regenerating
					// after a model change is a real use case, silently creating a second
					// page is not.
					const boundName = pageSel.value ? (pageSel.selectedOptions[0]?.textContent ?? '').trim() : '';
					if (boundName) {
						const confirmed = await _Dialogs.confirmation.showPromise(
							`<strong>${_Helpers.escapeTags(boundName)}</strong> is already bound as the instance page of this process.<br><br>` +
							`This creates an additional page with a numbered name. Nothing on <strong>${_Helpers.escapeTags(boundName)}</strong> ` +
							`is changed or removed, and the binding stays as it is.<br><br>Create a second skeleton page?`
						);
						if (!confirmed) return;
					}
					skeletonBtn.disabled = true;
					StructrWS.sendObj({
						command: 'BPMN_PAGE_SKELETON',
						data:    {
							processId:        procId,
							templateWidgetId: templateSel?.value || null
						}
					}, (resp) => {
						skeletonBtn.disabled = false;
						// resp === null means the server replied STATUS 422 and the websocket
						// layer has already surfaced its message; anything else would double up.
						if (!resp) return;
						if (resp.ok !== true) {
							new ErrorMessage().text('Failed to create page skeleton: ' + (resp.message || 'unknown error')).show();
							return;
						}
						// This path doesn't go through api.updateProcess, so the panel is not
						// re-rendered: reflect the new page in the picker here.
						for (const sel of sidePanel.querySelectorAll('.select-instance-page')) {
							sel.insertAdjacentHTML('beforeend', pageOption(resp.pageId, resp.pageName));
						}
						if (resp.boundAsInstancePage) {
							pageSel.value                = resp.pageId;
							pageSel.dataset.boundPageId  = resp.pageId;
							skeletonBtn.textContent      = 'Create another page skeleton';
						}
						const bound = resp.boundAsInstancePage ? ' and bound it as the instance page' : '';
						// Forms are inserted automatically for user-task steps when the process
						// declares a subject type (it is process-level, one subject per instance).
						// Report how many forms landed, and -- when the process has no subject type
						// -- how many user tasks got none as a result.
						let forms = '';
						if (resp.formCount > 0) {
							forms = `, ${resp.formCount} form(s)`;
						}
						if (resp.stepsMissingSubject > 0) {
							forms += ` (the process declares no subject type, so its ${resp.stepsMissingSubject} user task(s) got no form — set it on the Process tab)`;
						}
						new SuccessMessage().text(`Created page "${_Helpers.escapeTags(resp.pageName)}" with ${resp.stepCount} step div(s)${forms}${bound}.`).show();
					});
				});
			}

			if (procId && pageSel) {
				pageSel.addEventListener('change', () => {
					try {
						// Empty value clears the relationship.
						api.updateProcess(procId, { instancePage: pageSel.value || null });
					} catch (e) {
						console.error('updateProcess failed', e);
						pageSel.value = pageSel.dataset.boundPageId || '';
					}
				});
			}

			// --- Process event handlers (phased BpmnProcessListener on the
			// process root). Mirrors the Task event handlers block: one handler
			// per (event, phase), method auto-named and bound directly. ---
			const plListEl = section.querySelector('.bpmn-process-listeners-list');
			const plAddBtn = section.querySelector('.btn-add-process-listener');
			if (procId && plListEl && plAddBtn) {

				const PROC_EVENTS  = ['created','started','subjectAttached','completed','terminated','suspended','resumed'];
				const PROC_PHASES  = ['after','on'];
				const plCountEl    = section.querySelector('.bpmn-process-listeners-count');
				const procName     = section.dataset.processName || 'process';
				const escPL        = _Helpers.escapeTags;
				const capPL        = (s) => s ? s.charAt(0).toUpperCase() + s.slice(1) : s;

				// <camelProcessName>_<phase><Event>, e.g. employeeLeaveRequest_afterCompleted
				const plMethodName = (phase, event) => {
					const parts = String(procName).split(/[^a-zA-Z0-9]+/).filter(Boolean);
					let base = parts.length ? parts[0].toLowerCase() + parts.slice(1).map(capPL).join('') : 'process';
					if (!/^[a-z_]/.test(base)) base = 'process' + capPL(base);
					return `${base}_${phase}${capPL(event)}`;
				};
				const plSetProps = (id, props) => new Promise((resolve, reject) => {
					try { Command.setProperties(id, props, () => resolve()); } catch (e) { reject(e); }
				});
				const plMethodOf = (pl) => {
					const m = Array.isArray(pl.method) ? pl.method[0] : pl.method;
					return (m && typeof m === 'object') ? m : null;
				};
				const plOpenMethod = (methodId) => {
					if (!methodId) return;
					_Dialogs.custom.clickDialogCancelButton();
					setTimeout(async () => {
						try {
							const res = await fetch(`${Structr.rootUrl}SchemaMethod/${methodId}/ui`, { credentials: 'same-origin' });
							if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
							const body = await res.json();
							let m = body.result; if (Array.isArray(m)) m = m[0];
							if (m) _Code.helpers.navigateToSchemaObjectFromAnywhere(m);
						} catch (e) {
							new ErrorMessage().text(`Failed to open method: ${e.message}`).show();
						}
					}, 150);
				};

				let plLoaded = [];

				const plRowHtml = (pl) => {
					const m = plMethodOf(pl);
					return `
					<li class="bpmn-listener-row bpmn-process-listener-row" data-listener-id="${escPL(pl.id)}" data-method-id="${escPL(m?.id ?? '')}">
						<select class="bpmn-listener-event bpmn-process-listener-event">
							${PROC_EVENTS.map(e => `<option value="${e}" ${e === pl.event ? 'selected' : ''}>${e}</option>`).join('')}
						</select>
						<select class="bpmn-listener-phase bpmn-process-listener-phase" title="on: pre-commit, can veto. after: post-commit, side-effects.">
							${PROC_PHASES.map(p => `<option value="${p}" ${p === (pl.phase ?? 'after') ? 'selected' : ''}>${p}</option>`).join('')}
						</select>
						<button class="action btn-edit-process-listener-method" title="Edit the handler method">Edit</button>
						<button class="action btn-remove-process-listener" title="Remove this handler (deletes its method)">&minus;</button>
					</li>`;
				};

				const plWireRow = (row) => {
					const listenerId = row.dataset.listenerId;
					const methodId   = row.dataset.methodId;
					if (!listenerId) return;
					const eventSel = row.querySelector('.bpmn-process-listener-event');
					const phaseSel = row.querySelector('.bpmn-process-listener-phase');
					const editBt   = row.querySelector('.btn-edit-process-listener-method');
					const removeBt = row.querySelector('.btn-remove-process-listener');

					const commit = async () => {
						const event = eventSel.value;
						const phase = phaseSel.value;
						const collides = plLoaded.some(pl =>
							pl.id !== listenerId && pl.event === event && (pl.phase ?? 'after') === phase);
						if (collides) {
							new ErrorMessage().text(`A handler for "${event}" (${phase}) already exists on this process.`).show();
							await plLoadRows();
							return;
						}
						eventSel.disabled = phaseSel.disabled = true;
						try {
							await plSetProps(listenerId, { event, phase });
							if (methodId) { try { await plSetProps(methodId, { name: plMethodName(phase, event) }); } catch (_) {} }
							await plLoadRows();
						} catch (err) {
							new ErrorMessage().text(`Update handler failed: ${err.message ?? err}`).show();
							await plLoadRows();
						}
					};
					eventSel?.addEventListener('change', commit);
					phaseSel?.addEventListener('change', commit);
					editBt?.addEventListener('click', () => plOpenMethod(methodId));
					removeBt?.addEventListener('click', async () => {
						removeBt.disabled = true;
						// Update the view immediately: the DELETE callback is unreliable
						// and a GET re-fetch can be cache-served, so don't depend on
						// either for the visual removal.
						plLoaded = plLoaded.filter(pl => pl.id !== listenerId);
						if (plCountEl) plCountEl.textContent = `(${plLoaded.length})`;
						row.remove();

						// 1:1 listener <-> method, same rule as the task-listener block: always
						// delete the listener (first: the CALLS rel cascades method -> listener,
						// not this direction); delete its method only when the body is empty, so
						// user-written code is never destroyed collaterally.
						const del = (id) => new Promise((resolve) => {
							let settled = false;
							const done = () => { if (!settled) { settled = true; resolve(); } };
							Command.deleteNode(id, undefined, done);
							window.setTimeout(done, 1500);
						});
						await del(listenerId);
						if (methodId) {
							const source = await new Promise((resolve) => {
								let settled = false;
								const finish = (v) => { if (!settled) { settled = true; resolve(v); } };
								Command.get(methodId, 'id,source', (m) => finish(m ? (m.source || '') : null));
								window.setTimeout(() => finish(null), 1500);   // unknown -> treat as non-empty (keep)
							});
							if (source !== null && source.trim() === '') {
								await del(methodId);
							}
						}
					});
				};

				const plLoadRows = async () => {
					try {
						const res  = await fetch(`${Structr.rootUrl}BpmnProcessListener/ui?process=${procId}`, { credentials: 'same-origin', cache: 'no-store' });
						const body = res.ok ? await res.json() : { result: [] };
						plLoaded = Array.isArray(body.result) ? body.result : (body.result ? [body.result] : []);
					} catch (_) {
						plLoaded = [];
					}
					plListEl.innerHTML = plLoaded.map(plRowHtml).join('');
					if (plCountEl) plCountEl.textContent = `(${plLoaded.length})`;
					plListEl.querySelectorAll('.bpmn-process-listener-row').forEach(plWireRow);
				};
				plLoadRows();

				plAddBtn.addEventListener('click', async () => {
					// First free (event, phase) pair, all 'after' then 'on'.
					const usedPairs = new Set(plLoaded.map(pl => `${pl.event}|${pl.phase ?? 'after'}`));
					let event = null, phase = null;
					for (const ph of PROC_PHASES) {
						const free = PROC_EVENTS.find(e => !usedPairs.has(`${e}|${ph}`));
						if (free) { event = free; phase = ph; break; }
					}
					if (!event) {
						new InfoMessage().title('All handlers exist').text('Every event/phase combination already has a handler on this process.').show();
						return;
					}
					plAddBtn.disabled = true;
					try {
						const methodName = plMethodName(phase, event);
						const methodId = await new Promise((resolve, reject) => {
							Command.create({ type: 'SchemaMethod', name: methodName, source: '', bpmnProcess: procId },
								(m) => (m && m.id) ? resolve(m.id) : reject(new Error('SchemaMethod creation failed')));
						});
						await new Promise((resolve, reject) => {
							Command.create({ type: 'BpmnProcessListener', process: procId, event, phase, method: methodId },
								(n) => (n && n.id) ? resolve(n.id) : reject(new Error('BpmnProcessListener creation failed')));
						});
						await plLoadRows();
					} catch (err) {
						new ErrorMessage().text(`Add handler failed: ${err.message ?? err}`).show();
					} finally {
						plAddBtn.disabled = false;
					}
				});
			}
		}
	},

	// Render the inline side-panel content for a selected BpmnElement. The
	// Name field is an inline editor; the Delete button removes the element
	// (cascading to incident flows). The "Open in editor…" route is the
	// escape hatch for fields not surfaced inline (documentation, event
	// definitions, attributes JSON, ...).
	_renderSidePanel: (elem) => {
		const esc = (v) => v == null ? '' : _Helpers.escapeTags(String(v));
		const row = (label, value) => `
			<div class="pd-mb-6">
				<div class="pd-field-label">${esc(label)}</div>
				<div>${esc(value) || '<span class="text-gray-500">—</span>'}</div>
			</div>
		`;
		// Performer inputs: only meaningful on userTasks. Inputs start empty;
		// a follow-up async fetch in the handler populates them with the
		// current assignee/candidate expressions for persisted performers.
		const isUserTask = (elem.bpmnElementType === 'userTask');
		const performersBlock = !isUserTask ? '' : `
			<div class="pd-section">
				<div class="pd-section-title">Performers</div>
				<label class="pd-field">
					<div class="pd-field-label">Assignee (humanPerformer)</div>
					<input type="text" class="input-performer-assignee" placeholder="\${initiator} or user(alice)">
				</label>
				<label class="pd-block">
					<div class="pd-field-label">Candidates expression (potentialOwner)</div>
					<input type="text" class="input-performer-candidates" placeholder="user(alice), group(managers)">
				</label>
				<label class="pd-field-mt6">
					<div class="pd-field-label">Candidate principals</div>
					<select class="select-performer-candidate-principals" multiple size="5">
					</select>
				</label>
				<div class="pd-hint">
					Expression syntax: <code>\${initiator}</code>, <code>user(<i>name</i>)</code>,
					<code>group(<i>name</i>)</code>, comma-separated. The picker below binds Users
					and Groups directly via a typed graph relationship -- preferred when
					non-empty, expression is the fallback. Both round-trip through BPMN as
					<code>user(name)</code> / <code>group(name)</code> in
					<code>resourceAssignmentExpression</code>.
				</div>
			</div>
		`;

		// Process / UI contract (userTask only): per-step properties the process
		// designer attaches to declare the contract the UI side consumes when
		// rendering a process-bound widget. The subject *type* is NOT here -- it
		// is process-level (a process has one subject), set on the Process tab.
		// A step only narrows WHICH fields of that one type it shows / writes,
		// via the two view pickers below (populated from the process's subject
		// type in the wiring). Empty values clear the property. See
		// project_process_ui_contract_pillar.md for the design rationale.
		const contractBlock = !isUserTask ? '' : `
			<div class="pd-section">
				<div class="pd-section-title">Process / UI contract</div>
				<div class="pd-hint pd-mb-6">
					The subject <b>type</b> is set once for the whole process on the
					<b>Process</b> tab. Here you choose which of its fields this step shows.
				</div>
				<label class="pd-field">
					<div class="pd-field-label">Form view — the fields this step shows</div>
					<select class="input-subject-form-view" data-current="${esc(elem.subjectFormView ?? '')}">
						<option value="">— All own properties (custom view) —</option>
						${elem.subjectFormView ? `<option value="${esc(elem.subjectFormView)}" selected>${esc(elem.subjectFormView)}</option>` : ''}
					</select>
				</label>
				<label class="pd-field">
					<div class="pd-field-label">Writable view (optional, subset of form view)</div>
					<select class="input-subject-writable-view" data-current="${esc(elem.subjectWritableView ?? '')}">
						<option value="">— Same as form view —</option>
						${elem.subjectWritableView ? `<option value="${esc(elem.subjectWritableView)}" selected>${esc(elem.subjectWritableView)}</option>` : ''}
					</select>
				</label>
				<label class="pd-block">
					<div class="pd-field-label">Instructions for the human user</div>
					<textarea class="input-instructions" rows="2" placeholder="Optional help text shown above the form">${esc(elem.instructions ?? '')}</textarea>
				</label>
				<div class="pd-hint">
					Process-bound widgets read these to derive their field set at
					render time. The data source comes from the process-level
					subject type; UI designers configure layout and styling on their side.
				</div>
			</div>
		`;

		// BPMN attributes editor: key/value pairs that round-trip to the
		// XML attributes on the element (default, cancelActivity,
		// scriptFormat, etc.). The importer collects every XML attribute
		// it doesn't have a typed property for into the bpmnAttributes
		// JSON; the exporter writes them straight back. Editing this here
		// avoids the previous workflow of "edit the XML directly to set
		// an attribute we don't otherwise surface in the UI."
		let attrEntries = [];
		try {
			const parsed = elem.bpmnAttributes ? JSON.parse(elem.bpmnAttributes) : {};
			if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
				attrEntries = Object.entries(parsed).map(([k, v]) => [k, v == null ? '' : String(v)]);
			}
		} catch (_) {
			// Malformed JSON -- surface raw so the user can fix it. Treat
			// as a single pseudo-row with key="_raw" for visibility.
			attrEntries = [['_raw', String(elem.bpmnAttributes ?? '')]];
		}
		const attrRow = (k, v) => `
			<li class="bpmn-attribute-row">
				<input type="text" class="bpmn-attribute-key"   value="${esc(k)}" placeholder="key"  >
				<input type="text" class="bpmn-attribute-value" value="${esc(v)}" placeholder="value">
				<button class="action btn-remove-attribute" title="Remove this attribute">−</button>
			</li>
		`;
		const attributesBlock = `
			<div class="pd-section">
				<div class="pd-block-header">
					<div class="pd-field-label">BPMN attributes (${attrEntries.length})</div>
					<span class="pd-spacer"></span>
					<button class="action btn-add-attribute" title="Add an XML-attribute key/value pair">+ Add</button>
				</div>
				<ul class="bpmn-attributes-list">
					${attrEntries.map(([k, v]) => attrRow(k, v)).join('')}
				</ul>
				<div class="pd-hint">
					Round-trips to BPMN as XML attributes on this element. Use for
					things like <code>default</code> (gateway default flow id),
					<code>cancelActivity</code>, <code>scriptFormat</code>. Empty key
					removes the entry on save.
				</div>
			</div>
		`;

		// Task listeners (userTask only). One handler per (event, phase). Each
		// BpmnTaskListener points directly at a SchemaMethod (the handler body);
		// the engine runs it when the task fires the matching event. Phase 'on'
		// runs pre-commit and can veto (roll back) the transition; 'after'
		// (default) runs post-commit for side-effects like notifications.
		// Rows are populated by an explicit BpmnTaskListener fetch in the wiring
		// step, NOT from elem.taskListeners: the element's nested serialization
		// reduces related nodes to id/type/name, dropping event/phase/method.
		const taskListenersBlock = !isUserTask ? '' : `
			<div class="bpmn-listener-block">
				<div class="bpmn-listener-header">
					<div class="bpmn-listener-title">Task event handlers <span class="bpmn-task-listeners-count"></span></div>
					<button class="action btn-add-task-listener" title="Add a handler that runs on a task lifecycle event">+ Add handler</button>
				</div>
				<ul class="bpmn-listener-list bpmn-task-listeners-list"></ul>
				<div class="bpmn-listener-hint">
					One handler per (event, phase). The method is created and named for you;
					edit its body in the Code module. <b>on</b> runs pre-commit and can roll
					back the transition; <b>after</b> runs post-commit for side-effects.
				</div>
			</div>
		`;

		const headerName = esc(elem.bpmnName || elem.bpmnId || elem.id);
		// Attached handler methods (the info the old canvas "method count" badge
		// showed). Listed here on selection instead of on the diagram.
		const methods = Array.isArray(elem.methods) ? elem.methods : [];
		const methodsBlock = methods.length === 0 ? '' : `
			<div class="pd-section">
				<div class="pd-section-title">Methods (${methods.length})</div>
				<ul class="pd-method-list">
					${methods.map(m => `<li><code>${esc((m && (m.name || m.id)) || m)}</code></li>`).join('')}
				</ul>
			</div>
		`;

		return `
			<h4 class="pd-heading">${headerName}</h4>
			<div class="pd-mb-6">
				<div class="pd-field-label">Name</div>
				<input type="text" class="input-element-name" value="${esc(elem.bpmnName ?? '')}" placeholder="${esc(elem.bpmnId ?? '')}">
			</div>
			${row('Type',          elem.bpmnElementType)}
			${row('BPMN id',       elem.bpmnId)}
			${row('Documentation', elem.documentation)}
			${row('Event def',     elem.eventDefinitionType)}
			${contractBlock}
			${attributesBlock}
			${performersBlock}
			${taskListenersBlock}
			${methodsBlock}
			<div class="pd-button-row">
				<button class="action btn-open-properties">Open in editor…</button>
				<button class="action btn-delete-element">Delete</button>
			</div>
		`;
	},

};

let _ProcessInstances = {

	getContainer: () => document.querySelector('#processInstancesContainer'),

	refresh: () => {

		let el = _ProcessInstances.getContainer();
		_Helpers.fastRemoveAllChildren(el);
		el.insertAdjacentHTML('beforeend', _Processes.templates.instancesTable());

		let pagerElement = el.querySelector('#processInstancesPager');
		let pager = _Pager.addPager(
			_Processes.instancesPagerId,
			pagerElement,
			true,
			'ProcessInstance',
			'ui',
			(entities) => {
				for (let entity of entities) _ProcessInstances.appendRow(entity);
			},
			null,
			'id,name,type,status,startTime,endTime,process,initiator,subject',
			true
		);

		pager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(document.querySelector('#processInstancesTable tbody'));
		};

		pagerElement.insertAdjacentHTML('beforeend', `
			<div class="processes-pager-filters">
				Filter: <input type="text" class="filter" data-attribute="status" placeholder="Status">
			</div>
		`);
		pager.activateFilterElements(pagerElement.querySelector('.processes-pager-filters'));
		pager.setIsPaused(false);
		pager.refresh();
	},

	appendRow: (inst) => {

		let tbody = document.querySelector('#processInstancesTable tbody');
		let status = inst.status ?? '';
		let canTerminate = (status === 'running' || status === 'suspended');
		let canSuspend   = (status === 'running');
		let canResume    = (status === 'suspended');

		let suspendResumeIcon = canResume
			? `<i>${_Icons.getSvgIcon(_Icons.iconPlayButton, 16, 16, ['mr-1', 'resume-instance'], 'Resume this instance')}</i>`
			: `<i>${_Icons.getSvgIcon(_Icons.iconPauseButton, 16, 16, ['mr-1', 'suspend-instance', ...(canSuspend ? [] : ['disabled'])], 'Suspend this instance')}</i>`;

		let subjectLabel = '—';
		let subjectTitle = '';
		if (inst.subject) {
			let s = inst.subject;
			subjectLabel = `${s.type ?? ''}: ${s.name ?? _Processes.fmt.shortId(s.id)}`;
			subjectTitle = `${s.type ?? ''}: ${s.name ?? s.id}`;
		}

		// ProcessInstance.process now points at a BpmnProcess (not a
		// BpmnDefinitions). The pager serializes it as a {id,type,name}
		// stub, so we need a follow-up fetch to surface the version.
		const procRel = inst.process;
		const procId = Array.isArray(procRel) ? procRel[0]?.id : procRel?.id;
		let tr = _Helpers.createSingleDOMElementFromHTML(`
			<tr id="id_${inst.id}" class="process-instance" data-process-id="${_Helpers.escapeTags(procId ?? '')}">
				<td class="mono" title="${_Helpers.escapeTags(inst.id)}">${_Helpers.escapeTags(_Processes.fmt.shortId(inst.id))}</td>
				<td class="definition-cell">${_Helpers.escapeTags(_Processes.fmt.namedWithVersion(inst.process))}</td>
				<td title="${_Helpers.escapeTags(subjectTitle)}">${_Helpers.escapeTags(subjectLabel)}</td>
				<td>${_Helpers.escapeTags(_Processes.fmt.relName(inst.initiator))}</td>
				<td>${_Processes.fmt.statusBadge(status)}</td>
				<td>${_Helpers.escapeTags(_Processes.fmt.dateTime(inst.startTime))}</td>
				<td>${_Helpers.escapeTags(_Processes.fmt.dateTime(inst.endTime))}</td>
				<td class="actions">
					${suspendResumeIcon}
					<i>${_Icons.getSvgIcon(_Icons.iconStopButton, 16, 16, ['mr-1', 'terminate-instance', ...(canTerminate ? [] : ['disabled'])], 'Terminate this instance')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, ['mr-1', 'edit-entity'], 'Edit properties')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'delete-entity']), 'Delete')}</i>
				</td>
			</tr>
		`);

		// Lazy-fetch the BpmnProcess's version (the related-node payload is a
		// stub). On arrival, ensureProcessVersion patches every row that matches.
		_Processes.ensureProcessVersion(procId);

		// Each transition goes through a dedicated engine method (instead of direct
		// status setProperty). The methods do auxiliary work (e.g. terminate marks
		// waiting tokens as completed) and ensure lifecycle events fire consistently.
		let callMethod = async (methodName, confirmText) => {
			if (confirmText) {
				let confirm = await _Dialogs.confirmation.showPromise(confirmText);
				if (!confirm) return;
			}
			try {
				await _Processes.rest.callEntityMethod('ProcessInstance', inst.id, methodName);
				_ProcessInstances.refresh();
			} catch (err) { /* handled */ }
		};

		tr.querySelector('.suspend-instance')?.addEventListener('click', (e) => {
			e.stopPropagation();
			if (e.currentTarget.classList.contains('disabled')) return;
			callMethod('suspend');
		});

		tr.querySelector('.resume-instance')?.addEventListener('click', (e) => {
			e.stopPropagation();
			callMethod('resume');
		});

		tr.querySelector('.terminate-instance').addEventListener('click', (e) => {
			e.stopPropagation();
			if (e.currentTarget.classList.contains('disabled')) return;
			callMethod('terminate', `Terminate ProcessInstance <strong>${_Helpers.escapeTags(_Processes.fmt.shortId(inst.id))}</strong>?`);
		});

		tr.querySelector('.edit-entity').addEventListener('click', (e) => {
			e.stopPropagation();
			_Entities.showProperties(inst, 'ui');
		});

		tr.querySelector('.delete-entity').addEventListener('click', (e) => {
			e.stopPropagation();
			_Entities.deleteNode({ ...inst, name: inst.id }, false, () => tr.remove());
		});

		tbody.appendChild(tr);
	}
};

let _ProcessTokens = {

	getContainer: () => document.querySelector('#processTokensContainer'),

	refresh: () => {

		let el = _ProcessTokens.getContainer();
		_Helpers.fastRemoveAllChildren(el);
		el.insertAdjacentHTML('beforeend', _Processes.templates.tokensTable());

		let pagerElement = el.querySelector('#processTokensPager');
		let pager = _Pager.addPager(
			_Processes.tokensPagerId,
			pagerElement,
			true,
			'ProcessToken',
			'ui',
			(entities) => {
				for (let entity of entities) _ProcessTokens.appendRow(entity);
			},
			null,
			'id,name,type,status,processInstance,atElement',
			true
		);

		pager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(document.querySelector('#processTokensTable tbody'));
		};

		pagerElement.insertAdjacentHTML('beforeend', `
			<div class="processes-pager-filters">
				Filters: <input type="text" class="filter" data-attribute="status" placeholder="Status">
			</div>
		`);
		pager.activateFilterElements(pagerElement.querySelector('.processes-pager-filters'));
		pager.setIsPaused(false);
		pager.refresh();
	},

	appendRow: (tok) => {

		let tbody = document.querySelector('#processTokensTable tbody');
		let instanceId = Array.isArray(tok.processInstance) ? tok.processInstance[0]?.id : tok.processInstance?.id;
		let elementLabel = _Processes.fmt.relName(tok.atElement);

		let tr = _Helpers.createSingleDOMElementFromHTML(`
			<tr id="id_${tok.id}" class="process-token">
				<td class="mono" title="${_Helpers.escapeTags(tok.id)}">${_Helpers.escapeTags(_Processes.fmt.shortId(tok.id))}</td>
				<td class="mono" title="${_Helpers.escapeTags(instanceId ?? '')}">${_Helpers.escapeTags(_Processes.fmt.shortId(instanceId))}</td>
				<td>${_Helpers.escapeTags(elementLabel)}</td>
				<td>${_Processes.fmt.statusBadge(tok.status ?? '')}</td>
				<td class="actions">
					<i>${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, ['mr-1', 'edit-entity'], 'Edit properties')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'delete-entity']), 'Delete')}</i>
				</td>
			</tr>
		`);

		tr.querySelector('.edit-entity').addEventListener('click', (e) => {
			e.stopPropagation();
			_Entities.showProperties(tok, 'ui');
		});

		tr.querySelector('.delete-entity').addEventListener('click', (e) => {
			e.stopPropagation();
			_Entities.deleteNode({ ...tok, name: tok.id }, false, () => tr.remove());
		});

		tbody.appendChild(tr);
	}
};

let _ProcessTasks = {

	getContainer: () => document.querySelector('#processTasksContainer'),

	refresh: () => {

		let el = _ProcessTasks.getContainer();
		_Helpers.fastRemoveAllChildren(el);
		el.insertAdjacentHTML('beforeend', _Processes.templates.tasksTable());

		let pagerElement = el.querySelector('#processTasksPager');
		let pager = _Pager.addPager(
			_Processes.tasksPagerId,
			pagerElement,
			true,
			'TaskInstance',
			'ui',
			(entities) => {
				for (let entity of entities) _ProcessTasks.appendRow(entity);
				// Re-apply the stalled filter (no-op when checkbox is off) so
				// pagination / refresh respects the current filter state.
				_ProcessTasks.applyStalledFilter();
			},
			null,
			'id,name,type,status,assignee,assigneeSetBy,candidateAssignees,declinedBy,createdTime,claimedTime,completedTime,cancelledTime,processInstance,definedBy',
			true
		);

		pager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(document.querySelector('#processTasksTable tbody'));
		};

		pagerElement.insertAdjacentHTML('beforeend', `
			<div class="processes-pager-filters">
				Filters: <input type="text" class="filter" data-attribute="status" placeholder="Status">
				<label class="ml-3"><input type="checkbox" id="filter-stalled-only"> Show stalled only</label>
			</div>
		`);
		pager.activateFilterElements(pagerElement.querySelector('.processes-pager-filters'));

		// "Stalled only" client-side filter: hides rows whose tasks aren't stalled
		// (status=available AND every effective candidate assignee has declined).
		pagerElement.querySelector('#filter-stalled-only').addEventListener('change', () => {
			_ProcessTasks.applyStalledFilter();
		});

		pager.setIsPaused(false);
		pager.refresh();
	},

	/**
	 * Filter the rendered task rows to show only stalled tasks. Stalled =
	 * status='available' AND every effective candidate assignee (with groups
	 * expanded to members) is in the task's declinedBy collection.
	 */
	applyStalledFilter: async () => {
		let onlyStalled = document.querySelector('#filter-stalled-only')?.checked ?? false;
		let rows = document.querySelectorAll('#processTasksTable tbody tr');

		if (!onlyStalled) {
			for (let row of rows) row.style.display = '';
			return;
		}

		// Cache group->members across the filter pass to avoid duplicate fetches.
		let groupMembersCache = new Map();
		let getGroupMembers = async (groupId) => {
			if (groupMembersCache.has(groupId)) return groupMembersCache.get(groupId);
			try {
				let resp = await fetch(`${Structr.rootUrl}Group/${groupId}/ui`);
				let data = await resp.json();
				let members = (data?.result?.members ?? []).map(m => m.id);
				groupMembersCache.set(groupId, members);
				return members;
			} catch (e) {
				groupMembersCache.set(groupId, []);
				return [];
			}
		};

		let stalledIds = new Set();
		try {
			let resp = await fetch(`${Structr.rootUrl}TaskInstance/ui?status=available&_pageSize=200`);
			let data = await resp.json();
			let tasks = data?.result ?? [];
			for (let task of tasks) {
				let declinedIds = new Set((task.declinedBy ?? []).map(p => p.id));
				let candidates = task.candidateAssignees ?? [];
				if (candidates.length === 0) continue;
				let allDeclined = true;
				for (let candidate of candidates) {
					if (candidate.type === 'Group') {
						let members = await getGroupMembers(candidate.id);
						if (members.length === 0) { allDeclined = false; break; }
						for (let m of members) {
							if (!declinedIds.has(m)) { allDeclined = false; break; }
						}
						if (!allDeclined) break;
					} else {
						if (!declinedIds.has(candidate.id)) { allDeclined = false; break; }
					}
				}
				if (allDeclined) stalledIds.add(task.id);
			}
		} catch (e) {
			new ErrorMessage().text(`Stalled filter failed: ${e.message}`).show();
			return;
		}

		for (let row of rows) {
			let id = row.id.replace(/^id_/, '');
			row.style.display = stalledIds.has(id) ? '' : 'none';
		}
	},

	appendRow: (task) => {

		let tbody = document.querySelector('#processTasksTable tbody');
		let instanceId = Array.isArray(task.processInstance) ? task.processInstance[0]?.id : task.processInstance?.id;
		let status = task.status ?? '';
		let canAssign        = (status !== 'completed' && status !== 'cancelled');
		let canCancel        = (status !== 'completed' && status !== 'cancelled');
		let canMakeAvailable = (status === 'reserved' || status === 'created');
		let canComplete      = (status === 'reserved');

		let elementLabel = _Processes.fmt.relName(task.definedBy);
		let assigneeLabel = _Processes.fmt.relName(task.assignee);
		let candidatesLabel = Array.isArray(task.candidateAssignees) && task.candidateAssignees.length
			? task.candidateAssignees.map(p => p.name ?? _Processes.fmt.shortId(p.id)).join(', ')
			: '—';

		let tr = _Helpers.createSingleDOMElementFromHTML(`
			<tr id="id_${task.id}" class="process-task">
				<td class="mono" title="${_Helpers.escapeTags(task.id)}">${_Helpers.escapeTags(_Processes.fmt.shortId(task.id))}</td>
				<td>${_Helpers.escapeTags(task.name ?? '')}</td>
				<td class="mono" title="${_Helpers.escapeTags(instanceId ?? '')}">${_Helpers.escapeTags(_Processes.fmt.shortId(instanceId))}</td>
				<td>${_Helpers.escapeTags(elementLabel)}</td>
				<td>${_Helpers.escapeTags(assigneeLabel)}</td>
				<td>${_Helpers.escapeTags(candidatesLabel)}</td>
				<td>${_Processes.fmt.statusBadge(status)}</td>
				<td>${_Helpers.escapeTags(_Processes.fmt.dateTime(task.createdTime))}</td>
				<td>${_Helpers.escapeTags(_Processes.fmt.dateTime(task.claimedTime))}</td>
				<td>${_Helpers.escapeTags(_Processes.fmt.dateTime(task.completedTime))}</td>
				<td class="actions">
					<i>${_Icons.getSvgIcon(_Icons.iconUserAdd, 16, 16, ['mr-1', 'assign-task', ...(canAssign ? [] : ['disabled'])], 'Assign task to a User or Group')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconResetArrow, 16, 16, ['mr-1', 'make-available-task', ...(canMakeAvailable ? [] : ['disabled'])], 'Return task to available pool')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green', 'mr-1', 'complete-task', ...(canComplete ? [] : ['disabled'])]), 'Complete task')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'mr-1', 'cancel-task', ...(canCancel ? [] : ['disabled'])]), 'Cancel task')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, ['mr-1', 'edit-entity'], 'Edit properties')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'delete-entity']), 'Delete')}</i>
				</td>
			</tr>
		`);

		tr.querySelector('.assign-task').addEventListener('click', (e) => {
			e.stopPropagation();
			if (e.currentTarget.classList.contains('disabled')) return;
			_ProcessTasks.openAssignDialog(task);
		});

		tr.querySelector('.complete-task').addEventListener('click', async (e) => {
			e.stopPropagation();
			if (e.currentTarget.classList.contains('disabled')) return;
			try {
				await _Processes.rest.callEntityMethod('TaskInstance', task.id, 'complete');
				new SuccessMessage().text('Task completed').show();
				_ProcessTasks.refresh();
			} catch (err) { /* handled */ }
		});

		tr.querySelector('.cancel-task').addEventListener('click', async (e) => {
			e.stopPropagation();
			if (e.currentTarget.classList.contains('disabled')) return;
			let confirm = await _Dialogs.confirmation.showPromise(
				`Cancel task <strong>${_Helpers.escapeTags(task.name ?? task.id)}</strong>?<br><br>` +
				`This marks the task as cancelled and consumes its waiting token. ` +
				`The process instance is not advanced or terminated automatically; ` +
				`you'll need to handle the instance separately if it should not continue.`
			);
			if (!confirm) return;
			try {
				await _Processes.rest.callEntityMethod('TaskInstance', task.id, 'cancel');
				new SuccessMessage().text('Task cancelled').show();
				_ProcessTasks.refresh();
			} catch (err) { /* handled */ }
		});

		tr.querySelector('.make-available-task').addEventListener('click', async (e) => {
			e.stopPropagation();
			if (e.currentTarget.classList.contains('disabled')) return;
			let confirm = await _Dialogs.confirmation.showPromise(
				`Return task <strong>${_Helpers.escapeTags(task.name ?? task.id)}</strong> to the available pool?<br><br>` +
				`The current assignee will be cleared. Read+write is re-granted to all current candidate assignees. ` +
				`The 'available' lifecycle event fires so notification handlers can re-notify candidates.`
			);
			if (!confirm) return;
			try {
				await _Processes.rest.callEntityMethod('TaskInstance', task.id, 'makeAvailable');
				new SuccessMessage().text('Task returned to available pool').show();
				_ProcessTasks.refresh();
			} catch (err) { /* handled */ }
		});

		tr.querySelector('.edit-entity').addEventListener('click', (e) => {
			e.stopPropagation();
			_Entities.showProperties(task, 'ui');
		});

		tr.querySelector('.delete-entity').addEventListener('click', (e) => {
			e.stopPropagation();
			_Entities.deleteNode({ ...task, name: task.id }, false, () => tr.remove());
		});

		tbody.appendChild(tr);
	},

	/**
	 * Open the admin "Assign task" dialog: pick a User or Group, call the
	 * engine's assignTask method. Different from claim — no potential-owner
	 * check; instead the engine requires accessControl on the task (admin level).
	 */
	openAssignDialog: async (task) => {

		let { dialogText } = _Dialogs.custom.openDialog(`Assign task: ${task.name ?? ''}`, undefined, ['process-assign-dialog']);

		let currentAssigneeName = _Processes.fmt.relName(task.assignee, '—');

		dialogText.insertAdjacentHTML('beforeend', `
			<div class="process-assign-content">
				<p class="mb-2">Assign <strong>${_Helpers.escapeTags(task.name ?? task.id)}</strong> to a User or Group.</p>
				<p class="mb-2 text-sm text-gray-666">Current assignee: <strong>${_Helpers.escapeTags(currentAssigneeName)}</strong></p>
				<div class="mb-2">
					<input type="text" id="assign-filter" placeholder="Filter by name…" class="w-full" autocomplete="off">
				</div>
				<select id="assign-picker" size="12" class="w-full">
					<option value="">Loading users and groups…</option>
				</select>
			</div>
		`);

		let select     = dialogText.querySelector('#assign-picker');
		let filter     = dialogText.querySelector('#assign-filter');
		let assignBtn  = _Dialogs.custom.appendCustomDialogButton(`<button id="assign-confirm" class="action" disabled>Assign</button>`);

		// Fetch all Principals (Users and Groups) in one shot. Sorted by name.
		let allPrincipals = [];
		try {
			let response = await fetch(`${Structr.rootUrl}Principal/ui?_pageSize=1000&_sort=name`);
			let payload  = await response.json();
			allPrincipals = (payload?.result ?? []).filter(p => !p.isAnonymous);
		} catch (err) {
			new ErrorMessage().text(`Could not load users/groups: ${err.message}`).show();
			return;
		}

		let renderOptions = (filterText) => {
			let needle = (filterText ?? '').toLowerCase();
			let filtered = needle
				? allPrincipals.filter(p => (p.name ?? '').toLowerCase().includes(needle))
				: allPrincipals;
			select.innerHTML = filtered.length === 0
				? '<option value="" disabled>No matches</option>'
				: filtered.map(p => `<option value="${p.id}">${_Helpers.escapeTags(p.name ?? '(unnamed)')} — ${_Helpers.escapeTags(p.type)}</option>`).join('');
			assignBtn.disabled = !select.value;
		};

		renderOptions('');

		select.addEventListener('change', () => {
			assignBtn.disabled = !select.value;
		});

		filter.addEventListener('input', () => {
			renderOptions(filter.value);
		});

		assignBtn.addEventListener('click', async () => {
			let assigneeId = select.value;
			if (!assigneeId) return;
			try {
				await _Processes.rest.callEntityMethod('TaskInstance', task.id, 'assignTask', { assignee: assigneeId });
				new SuccessMessage().text('Task assigned').show();
				_Dialogs.custom.clickDialogCancelButton();
				_ProcessTasks.refresh();
			} catch (err) { /* error already shown by callEntityMethod */ }
		});
	}
};
