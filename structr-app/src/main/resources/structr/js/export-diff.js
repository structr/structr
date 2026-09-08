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
	Structr.registerModule(_ExportDiff);
});

let _ExportDiff = {
	_moduleName: 'export-diff',

	report: null,
	byKind: {},
	selectedKind: null,

	// a kind can hold thousands of deltas; beyond this the rest is summarised rather than rendered
	rowLimit: 500,
	leftTabMinWidth: 260,
	resizerLeftKey: 'structrExportDiffResizerLeftKey_' + location.port,
	hideLowSignalKey: 'structrExportDiffHideLowSignal_' + location.port,

	init: () => {},

	unload: () => {
		_ExportDiff.report       = null;
		_ExportDiff.byKind       = {};
		_ExportDiff.selectedKind = null;
	},

	onload: () => {

		Structr.setMainContainerHTML(_ExportDiff.templates.main());
		Structr.setFunctionBarHTML(_ExportDiff.templates.functions());

		_ExportDiff.diffMain = $('#export-diff-main');

		Structr.initVerticalSlider($('.column-resizer-left', _ExportDiff.diffMain), _ExportDiff.resizerLeftKey, _ExportDiff.leftTabMinWidth, _ExportDiff.moveLeftResizer);
		_ExportDiff.updateResizers();

		document.getElementById('export-diff-compare').addEventListener('click', _ExportDiff.compare);
		document.getElementById('export-diff-hide-low').addEventListener('change', (e) => {
			LSWrapper.setItem(_ExportDiff.hideLowSignalKey, e.target.checked);
			_ExportDiff.groupDeltas();
			_ExportDiff.renderTree();
			_ExportDiff.renderContents();
		});

		document.getElementById('export-diff-hide-low').checked = (LSWrapper.getItem(_ExportDiff.hideLowSignalKey, true) === true);

		_ExportDiff.loadExports();
		_ExportDiff.renderContents();

		// activateEntry() blocked the menu; every module releases it once loaded
		Structr.mainMenu.unblock(100);
	},

	resize: () => {
		_ExportDiff.updateResizers();
	},

	prevAnimFrameReqId_moveLeftResizer: undefined,
	moveLeftResizer: (left) => {

		_Helpers.requestAnimationFrameWrapper(_ExportDiff.prevAnimFrameReqId_moveLeftResizer, () => {
			_ExportDiff.updateResizers(left);
		});
	},

	updateResizers: (left) => {

		if (!_ExportDiff.diffMain || _ExportDiff.diffMain.length === 0) {
			return;
		}

		left = left || LSWrapper.getItem(_ExportDiff.resizerLeftKey) || _ExportDiff.leftTabMinWidth;

		const availableWidth = _ExportDiff.diffMain.innerWidth();

		_ExportDiff.diffMain[0].querySelector('.column-resizer-left').style.left = `${left}px`;

		document.getElementById('export-diff-tree').style.width     = `calc(${left}px - 1rem)`;
		document.getElementById('export-diff-contents').style.width = `calc(${availableWidth}px - ${left}px - 5rem)`;
	},

	// only zips are offered: an export is read without unpacking, so a directory is not a candidate here
	loadExports: () => {

		Command.query('File', 1000, 1, 'name', 'asc', { contentType: 'application/zip' }, (files) => {

			const options = files.map(f => `<option value="${f.id}">${_Helpers.escapeForHtmlAttributes(f.name)}</option>`).join('');
			const left    = document.getElementById('export-diff-left');
			const right   = document.getElementById('export-diff-right');

			left.innerHTML  = options;
			right.innerHTML = options;

			// comparing a file with itself is never what was meant, so start on two different ones
			if (files.length > 1) {
				left.selectedIndex  = files.length - 2;
				right.selectedIndex = files.length - 1;
			} else {
				_ExportDiff.message('Fewer than two zip files in the filesystem: upload two deployment exports to compare them.');
			}
		}, true, 'ui');
	},

	compare: () => {

		const leftId  = document.getElementById('export-diff-left').value;
		const rightId = document.getElementById('export-diff-right').value;

		if (!leftId || !rightId) {
			_ExportDiff.message('Pick two exports.');
			return;
		}

		if (leftId === rightId) {
			_ExportDiff.message('Both sides are the same file.');
			return;
		}

		_ExportDiff.message('Comparing...');

		StructrWS.sendObj({ command: 'COMPARE_EXPORTS', data: { leftId: leftId, rightId: rightId } }, (result) => {

			// the command sends the report as JSON text, because the websocket serializer flattens
			// anything that is not a primitive into value.toString()
			_ExportDiff.report       = result?.json ? JSON.parse(result.json) : null;
			_ExportDiff.selectedKind = null;

			if (!_ExportDiff.report) {
				_ExportDiff.message('The server returned no report.');
				return;
			}

			_ExportDiff.groupDeltas();
			_ExportDiff.renderTree();
			_ExportDiff.renderContents();
		});
	},

	groupDeltas: () => {

		const all     = _ExportDiff.report?.deltas ?? [];
		const hideLow = document.getElementById('export-diff-hide-low')?.checked;

		// low signal is collapsed, never dropped: the classifier deliberately reports everything
		_ExportDiff.byKind = {};

		for (const d of (hideLow ? all.filter(d => d.signal !== 'LOW') : all)) {
			(_ExportDiff.byKind[d.kind] ??= []).push(d);
		}
	},

	renderTree: () => {

		const tree = document.getElementById('export-diff-tree');

		if (!tree) {
			return;
		}

		const kinds = Object.keys(_ExportDiff.byKind).sort();
		const total = kinds.reduce((n, k) => n + _ExportDiff.byKind[k].length, 0);

		tree.innerHTML = `
			<div class="export-diff-tree-entry ${_ExportDiff.selectedKind === null ? 'selected' : ''}" data-kind="">
				<b>Overview</b> <span class="text-gray-600">${total}</span>
			</div>
			${kinds.map(kind => `
				<div class="export-diff-tree-entry ${_ExportDiff.selectedKind === kind ? 'selected' : ''}" data-kind="${_Helpers.escapeForHtmlAttributes(kind)}">
					${_Helpers.escapeForHtmlAttributes(kind)} <span class="text-gray-600">${_ExportDiff.byKind[kind].length}</span>
				</div>
			`).join('')}
		`;

		for (const entry of tree.querySelectorAll('.export-diff-tree-entry')) {

			entry.addEventListener('click', () => {
				_ExportDiff.selectedKind = entry.dataset.kind || null;
				_ExportDiff.renderTree();
				_ExportDiff.renderContents();
			});
		}
	},

	renderContents: () => {

		const contents = document.getElementById('export-diff-contents');

		if (!contents) {
			return;
		}

		if (!_ExportDiff.report) {
			contents.innerHTML = _ExportDiff.templates.empty();
			return;
		}

		contents.innerHTML = _ExportDiff.selectedKind === null
			? _ExportDiff.templates.overview({ report: _ExportDiff.report, byKind: _ExportDiff.byKind })
			: _ExportDiff.templates.kind({ kind: _ExportDiff.selectedKind, deltas: _ExportDiff.byKind[_ExportDiff.selectedKind] ?? [] });
	},

	message: (text) => {

		const contents = document.getElementById('export-diff-contents');

		if (contents) {
			contents.innerHTML = _ExportDiff.templates.empty(text);
		}
	},

	// what each side IS, side by side, before what differs between them
	profilesHtml: (profiles, summary) => {

		if (!profiles) {
			return '';
		}

		const side = (name, p) => `
			<div class="export-diff-profile">
				<div class="export-diff-profile-name">${_Helpers.escapeForHtmlAttributes(name ?? '')}</div>
				<div>${_Helpers.escapeForHtmlAttributes(p?.description ?? '')}</div>
			</div>
		`;

		return `
			<div class="export-diff-profiles">
				${side(summary?.left, profiles.left)}
				${side(summary?.right, profiles.right)}
			</div>
		`;
	},

	// many entities carry no name, and a bare uuid reads as noise, so mark it as the identity it is
	label: (d) => d.name
		? _Helpers.escapeForHtmlAttributes(d.name)
		: `<span class="text-gray-600" title="no name, identified by uuid">${_Helpers.escapeForHtmlAttributes(d.key ?? '')}</span>`,

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/schema.css">
			<link rel="stylesheet" type="text/css" media="screen" href="css/code.css">
			<link rel="stylesheet" type="text/css" media="screen" href="css/export-diff.css">

			<div class="tree-main" id="export-diff-main">

				<div class="column-resizer-blocker"></div>
				<div class="column-resizer column-resizer-left"></div>

				<div class="tree-container" id="export-diff-tree-container">
					<div class="tree" id="export-diff-tree"></div>
				</div>

				<div class="tree-contents-container" id="export-diff-contents-container">
					<div class="flex flex-col tree-contents" id="export-diff-contents"></div>
				</div>
			</div>
		`,
		functions: config => `
			<div class="flex items-center gap-2">
				<select id="export-diff-left" class="select"></select>
				<span class="text-sm">compared with</span>
				<select id="export-diff-right" class="select"></select>
				<button id="export-diff-compare" class="action">Compare</button>
				<label class="flex items-center ml-4 text-sm"><input type="checkbox" id="export-diff-hide-low" class="mr-1">Hide low signal</label>
			</div>
		`,
		empty: text => `
			<div class="export-diff-empty">
				<p>${_Helpers.escapeForHtmlAttributes(text ?? 'Pick two deployment exports and press Compare.')}</p>
				<p class="export-diff-verdict-detail">The comparison reports whether the two are versions of one application or two different ones, and lists what changed between them, grouped by kind.</p>
			</div>
		`,
		overview: config => {

			const c = config.report.congruence;
			const s = config.report.summary ?? {};

			const byOperation = {};

			for (const [group, n] of Object.entries(s.byOperationAndKind ?? {})) {
				const space = group.indexOf(' ');
				(byOperation[group.substring(0, space)] ??= []).push({ kind: group.substring(space + 1), count: n });
			}

			const p      = c?.provenance ?? {};
			const timing = (p.daysApart !== undefined) ? `${p.daysApart} days apart, ${_Helpers.escapeForHtmlAttributes(p.newer ?? '')} is newer` : '';

			return `
				<h2>${_Helpers.escapeForHtmlAttributes(s.left ?? '')} &rarr; ${_Helpers.escapeForHtmlAttributes(s.right ?? '')}</h2>

				<div class="content-container">

					${c ? `
					<div class="export-diff-verdict export-diff-${_Helpers.escapeForHtmlAttributes((c.verdict ?? '').toLowerCase())}">
						<div class="export-diff-verdict-title">${_Helpers.escapeForHtmlAttributes((c.verdict ?? '').replace(/_/g, ' '))}</div>
						<div>${_Helpers.escapeForHtmlAttributes(c.explanation ?? '')}</div>
						<div class="export-diff-verdict-detail">
							identity ${Math.round((c.identityOverlap ?? 0) * 100)}%, similarity ${Math.round((c.similarity ?? 0) * 100)}%
							${Object.entries(c.signals ?? {}).map(([k, v]) => `&middot; ${_Helpers.escapeForHtmlAttributes(k)} ${Math.round(v * 100)}%`).join(' ')}
						</div>
						<div class="export-diff-verdict-detail">
							${_Helpers.escapeForHtmlAttributes(p.left?.structrVersion ?? 'unknown build')} &rarr;
							${_Helpers.escapeForHtmlAttributes(p.right?.structrVersion ?? 'unknown build')}${timing ? ' &middot; ' + timing : ''}
						</div>
					</div>` : ''}

					${_ExportDiff.profilesHtml(config.report.profiles, s)}

					<table class="props export-diff-counts">
						<tbody>
							<tr><td class="key">Entities</td><td>${s.leftEntities ?? 0} &rarr; ${s.rightEntities ?? 0}</td></tr>
							<tr><td class="key">Deltas</td><td>${s.deltas ?? 0} (${s.normal ?? 0} normal, ${s.lowSignal ?? 0} low signal)</td></tr>
							${Object.keys(byOperation).sort().map(operation => `
								<tr>
									<td class="key">${_Helpers.escapeForHtmlAttributes(operation)}</td>
									<td>${byOperation[operation].sort((a, b) => b.count - a.count).map(e =>
										`<span class="inline-block mr-4 whitespace-nowrap">${e.count} ${_Helpers.escapeForHtmlAttributes(e.kind)}</span>`).join('')}</td>
								</tr>
							`).join('')}
						</tbody>
					</table>

					<p class="text-sm mt-4">Pick a kind on the left to see its deltas.</p>
				</div>
			`;
		},
		kind: config => {

			const capped    = config.deltas.slice(0, _ExportDiff.rowLimit);
			const remaining = config.deltas.length - capped.length;

			return `
				<h2>${_Helpers.escapeForHtmlAttributes(config.kind)} <span class="text-gray-600">${config.deltas.length}</span></h2>

				<div class="content-container">
					<table class="props export-diff-table">
						<thead>
							<tr><th>Operation</th><th>Name</th><th>Detail</th><th>Matched by</th></tr>
						</thead>
						<tbody>
							${capped.map(d => `
								<tr class="${d.signal === 'LOW' ? 'export-diff-low' : ''}">
									<td class="whitespace-nowrap">${_Helpers.escapeForHtmlAttributes(d.operation ?? '')}</td>
									<td>${_ExportDiff.label(d)}</td>
									<td>${_Helpers.escapeForHtmlAttributes(d.detail ?? d.signalReason ?? '')}</td>
									<td class="whitespace-nowrap">${_Helpers.escapeForHtmlAttributes(d.matchedBy ?? '')}</td>
								</tr>
							`).join('')}
						</tbody>
					</table>
					${remaining > 0 ? `<p class="text-sm mt-2 italic">${remaining} more not shown.</p>` : ''}
				</div>
			`;
		}
	}
};
