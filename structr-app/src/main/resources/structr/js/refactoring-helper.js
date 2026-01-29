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
class RefactoringHelper {

	constructor(container) {
		this.container = container;
	}

	show() {

		this.container.insertAdjacentHTML('beforeend', `
			<p>
				This helper allows you to find and edit the attributes of HTML elements in Structr pages and Shared Components based on a type and some common attributes.
				Enter a type selector and one or more attributes to show all DOM nodes that contain a value in at least one of the fields.
			</p>
			<div id="select-container" class="flex flex-row items-center justify-between">
				<select class="refactoring-helper" id="page-input" placeholder="Page in which to search"></select>
				<input class="refactoring-helper" id="selector-input" placeholder="HTML tag, e.g. div, button">
				<input class="refactoring-helper" id="property-input" placeholder="Property keys to display, e.g. id, class, name, onclick">
				<input type="checkbox" id="empty-checkbox"><label for="empty-checkbox"> Show empty results</label>
			</div>
			<div id="result-container"></div>
			<div><pre id="error-container"></pre></div>
		`);

		let selectedPageId = LSWrapper.getItem(_Entities.selectedObjectIdKey);
		if (_Entities.selectedObject && _Entities.selectedObject.type) {

			if (_Entities.selectedObject.type !== 'Page') {
				selectedPageId  = _Entities.selectedObject.pageId;
			}
		}

		fetch(`${Structr.rootUrl}Page?hidden=false&${Structr.getRequestParameterName('sort')}=name`).then(async response => {

			let data = await response.json();

			let pageSelect = $('#page-input');

			for (let page of data.result) {
				let selected = (page.id === selectedPageId  ? 'selected' : '');
				pageSelect.append(`<option ${selected} value="${page.id}">${page.name}</option>`);
			}
		});

		window.setTimeout(() => {
			$('#selector-input').focus();
		}, 100);

		let loadFunction = _Helpers.debounce(this.loadResults, 300);

		$('#property-input').on('keyup', loadFunction);
		$('#empty-checkbox').on('click', loadFunction);
	}

	loadResults() {

		let pageSelector    = $('#page-input');
		let typeSelector    = $('#selector-input');
		let keysSelector    = $('#property-input');
		let emptyCheckbox   = $('#empty-checkbox');
		let resultContainer = $('#result-container');
		let errorContainer  = $('#error-container');

		let typeQuery     = typeSelector.val().trim();
		let properties    = keysSelector.val().trim();
		let showEmptyRows = emptyCheckbox.is(':checked');

		if (typeQuery && properties) {

			typeQuery = typeQuery[0].toUpperCase() + typeQuery.slice(1);

			resultContainer.empty();
			errorContainer.empty();

			fetch(Structr.rootUrl + typeQuery + '/all?pageId=' + pageSelector.val()).then(async response => {

				let data = await response.json();

				if (data && data.result && data.result.length) {

					resultContainer.append('<table class="refactoring-helper" id="result-table"><thead><tr id="header-row"></tr></thead></table>');
					let keyMap        = {};
					let keyCandidates = [];
					let table         = $('#result-table');
					let header        = $('#header-row');
					let list          = properties.split(',');
					let keys          = [];
					let i             = 0;

					for (let r of data.result) {
						for (let k in r) {
							keyMap[k] = true;
						}
					}

					keyCandidates = Object.keys(keyMap);

					for (let p of list) {

						let name = p.trim();

						if (keyCandidates.includes(name) && name !== 'id' && name !== 'type') {
							keys.push({ name: name, title: name });
						}

						if (keyCandidates.includes('_html_' + name)) {
							keys.push({ name: '_html_' + name, title: 'HTML ' + name });
						}

						if (keyCandidates.includes('_custom_html_' + name)) {
							keys.push({ name: '_custom_html_' + name, title: 'HTML ' + name });
						}

						if (keyCandidates.includes('_custom_html_data-' + name)) {
							keys.push({ name: '_custom_html_data-' + name, title: 'data-' + name });
						}
					}

					header.append(`<th>Source</th>${keys.map(k => `<th>${k.title}</th>`).join('')}`);

					_Helpers.sort(data.result, 'internalEntityContextPath');

					for (let v of data.result) {

						table.append(`<tr id="row${i}"></tr>`);

						let hasValue = false;
						let row      = $('#row' + i);
						let src      = v.type;

						if (v.internalEntityContextPath) {
							src = v.internalEntityContextPath;
						} else if (v.parent && v.parent.name) {
							src = v.parent.name;
						} else if (v.ownerDocument && v.ownerDocument.name) {
							src = v.ownerDocument.name;
						} else if (v.name) {
							src = v.name;
						}

						row.append('<td>' + src + '</td>');

						for (let k of keys) {
							let id    = 'edit-' + v.id + '-' + k.name;
							let value = '';

							if (v[k.name] || v[k.name] === 0) {
								value = v[k.name];
								hasValue = true;
							}

							row.append(`<td><input type="text" id="${id}" value="${value}"/></td>`);
							let input = $('#' + id);
							input.on('blur', e => {
								let val = $('#' + id).val();
								Command.setProperty(v.id, k.name, val, false, () => {
									_Helpers.blinkGreen(input);
								});
							});
						}

						i++;

						if (!hasValue && !showEmptyRows) {

							row.remove();
						}
					}
				}
			});
		}
	}
}