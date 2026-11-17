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
 * The monitoring views of the instance (server log, event log, threads, HTTP statistics) in a panel at the foot
 * of the window. It is available in every area and pushes the area up while it is open. The views themselves
 * are the ones of _Dashboard.tabs; this panel only hosts them.
 */
let _MonitorPanel = {
	viewKey:       'monitorPanelView' + location.port,
	heightKey:     'monitorPanelHeight' + location.port,
	defaultHeight: 280,
	minHeight:     120,
	current:       null,
	views: {
		logs:     { label: 'Server Log',  tab: () => _Dashboard.tabs.logs, template: () => _Dashboard.tabs.logs.templates.tabContent({}), log: 'server' },
		// the same view as the server log, showing the other file
		requests: { label: 'Request Log', tab: () => _Dashboard.tabs.logs, template: () => _Dashboard.tabs.logs.templates.tabContent({}), log: 'request', module: 'logs' },
		events:  { label: 'Event Log',  tab: () => _Dashboard.tabs.events,  template: () => _Dashboard.templates.tabContentEventLog({}) },
		threads: { label: 'Threads',    tab: () => _Dashboard.tabs.threads, template: () => _Dashboard.templates.tabContentThreads({}) },
		stats:   { label: 'HTTP Stats', tab: () => _Dashboard.tabs.stats,   template: () => _Dashboard.tabs.stats.templates.tabContent({}) }
	},
	getPanel:    () => document.querySelector('#monitor-panel'),
	getSwitches: () => document.querySelector('#monitor-switches'),
	ensure: () => {

		if (_MonitorPanel.getSwitches()) {

			// after a logout and a new login the panel is there but closed
			if (!_MonitorPanel.current && LSWrapper.getItem(_MonitorPanel.viewKey)) {
				_MonitorPanel.restore();
			}

			return;
		}

		document.head.insertAdjacentHTML('beforeend', '<link rel="stylesheet" type="text/css" media="screen" href="css/dashboard.css">');
		document.body.insertAdjacentHTML('beforeend', _MonitorPanel.templates.switches() + _MonitorPanel.templates.panel());

		for (let button of _MonitorPanel.getSwitches().querySelectorAll('button')) {
			button.addEventListener('click', () => _MonitorPanel.toggle(button.dataset.view));
		}

		_MonitorPanel.getPanel().querySelector('#monitor-panel-close').addEventListener('click', () => _MonitorPanel.close());
		_MonitorPanel.getPanel().querySelector('#monitor-panel-resizer').addEventListener('mousedown', _MonitorPanel.startResize);

		_MonitorPanel.restore();
	},
	// the view that was open comes back, with the next page load or the next login
	restore: () => {

		_MonitorPanel.isRequestLoggingEnabled().then(enabled => {

			_MonitorPanel.getSwitches().querySelector('[data-view="requests"]').classList.toggle('hidden', !enabled);

			let storedView = LSWrapper.getItem(_MonitorPanel.viewKey);
			if (!_MonitorPanel.current && storedView && _MonitorPanel.views[storedView] && (storedView !== 'requests' || enabled)) {
				_MonitorPanel.open(storedView);
			}
		});
	},
	isRequestLog: (fileName) => /request/i.test(fileName.split(/[\\/]/).pop()),
	isRequestLoggingEnabled: async () => {

		let enabled = Structr.rawEnvInfo?.requestLoggingEnabled;

		// a server that does not say so yet: the request log counts as enabled if there is one
		if (enabled === undefined) {

			let logs = await Command.getAvailableServerLogs();
			enabled  = (logs?.result ?? []).some(_MonitorPanel.isRequestLog);
		}

		return (enabled === true);
	},
	selectLog: (kind) => {

		_Dashboard.tabs.logs.waitUntilInitialized().then(() => {

			let select      = _Dashboard.tabs.logs.getLogFileSelect();
			let wantRequest = (kind === 'request');
			let option      = [...select.options].find(o => _MonitorPanel.isRequestLog(o.value) === wantRequest);

			if (option && _MonitorPanel.isRequestLog(select.value) !== wantRequest) {

				select.value = option.value;
				select.dispatchEvent(new Event('change'));
			}
		});
	},
	toggle: (viewName) => {

		if (_MonitorPanel.current === viewName) {
			_MonitorPanel.close();
		} else {
			_MonitorPanel.open(viewName);
		}
	},
	open: (viewName) => {

		let view    = _MonitorPanel.views[viewName];
		let panel   = _MonitorPanel.getPanel();
		let content = panel.querySelector('#monitor-panel-content');

		_MonitorPanel.hideCurrentView();

		panel.classList.remove('hidden');
		_MonitorPanel.setHeight(LSWrapper.getItem(_MonitorPanel.heightKey, _MonitorPanel.defaultHeight));

		let moduleName = view.module ?? viewName;
		let element    = content.querySelector(`[data-module-name="${moduleName}"]`);
		if (!element) {

			content.insertAdjacentHTML('beforeend', view.template());
			element = content.querySelector(`[data-module-name="${moduleName}"]`);

			// a table scrolls by itself, below the controls of its view
			for (let table of element.querySelectorAll(':scope > table')) {

				let scroller = _Helpers.createSingleDOMElementFromHTML('<div class="monitor-panel-scroller"></div>');
				table.before(scroller);
				scroller.appendChild(table);
			}

			view.tab().init({});
		}

		element.classList.add('active');
		_MonitorPanel.getSwitches().querySelector(`[data-view="${viewName}"]`).classList.add('active');

		_MonitorPanel.current = viewName;
		LSWrapper.setItem(_MonitorPanel.viewKey, viewName);

		view.tab().onShow();

		if (view.log) {
			_MonitorPanel.selectLog(view.log);
		}

		_MonitorPanel.notifyResize();
	},
	close: (forget = true) => {

		let panel = _MonitorPanel.getPanel();
		if (!panel) {
			return;
		}

		_MonitorPanel.hideCurrentView();

		panel.classList.add('hidden');
		document.documentElement.style.removeProperty('--monitor-panel-height');

		if (forget) {
			LSWrapper.removeItem(_MonitorPanel.viewKey);
		}

		_MonitorPanel.notifyResize();
	},
	hideCurrentView: () => {

		if (_MonitorPanel.current) {

			_MonitorPanel.views[_MonitorPanel.current].tab().onHide();

			_MonitorPanel.getPanel().querySelector(`[data-module-name="${_MonitorPanel.views[_MonitorPanel.current].module ?? _MonitorPanel.current}"]`)?.classList.remove('active');
			_MonitorPanel.getSwitches().querySelector(`[data-view="${_MonitorPanel.current}"]`)?.classList.remove('active');

			_MonitorPanel.current = null;
		}
	},
	setHeight: (height) => {

		let clamped = Math.round(Math.max(_MonitorPanel.minHeight, Math.min(Number(height) || _MonitorPanel.defaultHeight, window.innerHeight * .7)));

		document.documentElement.style.setProperty('--monitor-panel-height', `${clamped}px`);

		return clamped;
	},
	startResize: (e) => {

		e.preventDefault();

		let height = null;

		let move = (moveEvent) => {
			height = _MonitorPanel.setHeight(window.innerHeight - moveEvent.clientY);
		};

		let stop = () => {

			document.removeEventListener('mousemove', move);
			document.removeEventListener('mouseup', stop);
			document.body.classList.remove('monitor-panel-resizing');

			if (height !== null) {
				LSWrapper.setItem(_MonitorPanel.heightKey, height);
			}

			_MonitorPanel.notifyResize();
		};

		document.body.classList.add('monitor-panel-resizing');
		document.addEventListener('mousemove', move);
		document.addEventListener('mouseup', stop);
	},
	notifyResize: () => {

		// the areas lay themselves out again for the room that is left
		window.dispatchEvent(new Event('resize'));
		Structr.resize();
	},
	templates: {
		switches: config => `
			<div id="monitor-switches">
				${Object.entries(_MonitorPanel.views).map(([name, view]) => `<button type="button" data-view="${name}">${view.label}</button>`).join('')}
			</div>
		`,
		panel: config => `
			<div id="monitor-panel" class="hidden">
				<div id="monitor-panel-resizer" title="Drag to resize"></div>
				<button type="button" id="monitor-panel-close" title="Close">${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12)}</button>
				<div id="monitor-panel-content"></div>
			</div>
		`
	}
};
