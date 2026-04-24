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

	definitionsPagerId: 'process-definitions',
	instancesPagerId:   'process-instances',
	tokensPagerId:      'process-tokens',
	tasksPagerId:       'process-tasks',

	init: () => {
		_Pager.initPager(_Processes.definitionsPagerId, 'BpmnDefinitions', 1, 25, 'processName', 'asc');
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
		callEntityMethod: async (type, id, method, params = {}) => {

			let url = `${Structr.rootUrl}${type}/${id}/${method}`;
			let response;
			try {
				response = await fetch(url, {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(params)
				});
			} catch (e) {
				new ErrorMessage().text(`${method} failed: ${e.message}`).show();
				throw e;
			}

			let json = await response.json().catch(() => ({}));
			if (!response.ok) {
				let msg = json?.message ?? response.statusText;
				new ErrorMessage().text(`${method} failed: ${msg}`).show();
				throw new Error(msg);
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
				<div id="processDefinitionsPager"></div>
			</div>
			<table class="processes-table" id="processDefinitionsTable">
				<thead>
					<tr>
						<th>Name</th>
						<th>Process ID</th>
						<th>Executable</th>
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
						<th>Assignee</th>
						<th>Status</th>
						<th>Created</th>
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
			'id,name,type,processName,processId,processIsExecutable,securityLevel',
			true
		);

		pager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(document.querySelector('#processDefinitionsTable tbody'));
		};

		pagerElement.insertAdjacentHTML('beforeend', `
			<div class="processes-pager-filters">
				Filters: <input type="text" class="filter" data-attribute="processName" placeholder="Name">
			</div>
		`);
		pager.activateFilterElements(pagerElement.querySelector('.processes-pager-filters'));
		pager.setIsPaused(false);
		pager.refresh();
	},

	appendRow: (def) => {

		let tbody = document.querySelector('#processDefinitionsTable tbody');
		let displayName = def.processName ?? def.name ?? '';
		let tr = _Helpers.createSingleDOMElementFromHTML(`
			<tr id="id_${def.id}" class="process-definition">
				<td>${_Helpers.escapeTags(displayName)}</td>
				<td class="mono">${_Helpers.escapeTags(def.processId ?? '')}</td>
				<td>${def.processIsExecutable ? 'Yes' : 'No'}</td>
				<td>${_Helpers.escapeTags(def.securityLevel ?? '')}</td>
				<td class="actions">
					<i>${_Icons.getSvgIcon(_Icons.iconPlayButton, 16, 16, ['mr-1', 'start-process'], 'Start new ProcessInstance')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, ['mr-1', 'edit-entity'], 'Edit properties')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'delete-entity']), 'Delete')}</i>
				</td>
			</tr>
		`);

		tr.querySelector('.start-process').addEventListener('click', async (e) => {
			e.stopPropagation();
			try {
				await _Processes.rest.callEntityMethod('BpmnDefinitions', def.id, 'startProcess');
				new SuccessMessage().text(`Started process "${displayName}"`).show();
			} catch (err) { /* error already shown */ }
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
	}
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
			'id,name,type,status,startTime,endTime,definition',
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

		let tr = _Helpers.createSingleDOMElementFromHTML(`
			<tr id="id_${inst.id}" class="process-instance">
				<td class="mono" title="${_Helpers.escapeTags(inst.id)}">${_Helpers.escapeTags(_Processes.fmt.shortId(inst.id))}</td>
				<td>${_Helpers.escapeTags(_Processes.fmt.relName(inst.definition))}</td>
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

		let setStatus = async (newStatus, confirmText) => {
			if (confirmText) {
				let confirm = await _Dialogs.confirmation.showPromise(confirmText);
				if (!confirm) return;
			}
			try {
				await _Processes.rest.setProperty('ProcessInstance', inst.id, 'status', newStatus);
				_ProcessInstances.refresh();
			} catch (err) { /* handled */ }
		};

		tr.querySelector('.suspend-instance')?.addEventListener('click', (e) => {
			e.stopPropagation();
			if (e.currentTarget.classList.contains('disabled')) return;
			setStatus('suspended');
		});

		tr.querySelector('.resume-instance')?.addEventListener('click', (e) => {
			e.stopPropagation();
			setStatus('running');
		});

		tr.querySelector('.terminate-instance').addEventListener('click', (e) => {
			e.stopPropagation();
			if (e.currentTarget.classList.contains('disabled')) return;
			setStatus('terminated', `Terminate ProcessInstance <strong>${_Helpers.escapeTags(_Processes.fmt.shortId(inst.id))}</strong>?`);
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
			},
			null,
			'id,name,type,status,assignee,createdTime,completedTime,processInstance',
			true
		);

		pager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(document.querySelector('#processTasksTable tbody'));
		};

		pagerElement.insertAdjacentHTML('beforeend', `
			<div class="processes-pager-filters">
				Filters: <input type="text" class="filter" data-attribute="status" placeholder="Status"> <input type="text" class="filter" data-attribute="assignee" placeholder="Assignee">
			</div>
		`);
		pager.activateFilterElements(pagerElement.querySelector('.processes-pager-filters'));
		pager.setIsPaused(false);
		pager.refresh();
	},

	appendRow: (task) => {

		let tbody = document.querySelector('#processTasksTable tbody');
		let instanceId = Array.isArray(task.processInstance) ? task.processInstance[0]?.id : task.processInstance?.id;
		let canComplete = (task.status !== 'completed' && task.status !== 'cancelled');

		let tr = _Helpers.createSingleDOMElementFromHTML(`
			<tr id="id_${task.id}" class="process-task">
				<td class="mono" title="${_Helpers.escapeTags(task.id)}">${_Helpers.escapeTags(_Processes.fmt.shortId(task.id))}</td>
				<td>${_Helpers.escapeTags(task.name ?? '')}</td>
				<td class="mono" title="${_Helpers.escapeTags(instanceId ?? '')}">${_Helpers.escapeTags(_Processes.fmt.shortId(instanceId))}</td>
				<td>${_Helpers.escapeTags(task.assignee ?? '')}</td>
				<td>${_Processes.fmt.statusBadge(task.status ?? '')}</td>
				<td>${_Helpers.escapeTags(_Processes.fmt.dateTime(task.createdTime))}</td>
				<td>${_Helpers.escapeTags(_Processes.fmt.dateTime(task.completedTime))}</td>
				<td class="actions">
					<i>${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green', 'mr-1', 'complete-task', ...(canComplete ? [] : ['disabled'])]), 'Complete task')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, ['mr-1', 'edit-entity'], 'Edit properties')}</i>
					<i>${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'delete-entity']), 'Delete')}</i>
				</td>
			</tr>
		`);

		tr.querySelector('.complete-task').addEventListener('click', async (e) => {
			e.stopPropagation();
			if (e.currentTarget.classList.contains('disabled')) return;
			try {
				await _Processes.rest.callEntityMethod('TaskInstance', task.id, 'complete');
				new SuccessMessage().text('Task completed').show();
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
	}
};
