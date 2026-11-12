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

		this.prepareMarkup();
		this.show().then(/*ignore*/);
	}

	async show() {

		let selectedPageId = LSWrapper.getItem(_Entities.selectedObjectIdKey);
		if (_Entities.selectedObject && _Entities.selectedObject.type) {

			if (_Entities.selectedObject.type !== 'Page') {
				selectedPageId = _Entities.selectedObject.pageId;
			}
		}

		let response = await fetch(`${Structr.rootUrl}Page?hidden=false&${Structr.getRequestParameterName('sort')}=name`);

		if (response.ok) {

			let data = await response.json();
			let html = data.result.map(page => `<option ${(page.id === selectedPageId  ? 'selected' : '')} value="${page.id}">${page.name}</option>`);

			this.pageSelect.insertAdjacentHTML('beforeend', html);

			this.tagOrTypeInput.focus();

			let loadFunction = _Helpers.debounce(this.loadResults, 300);

			this.pageSelect.addEventListener('change', loadFunction);
			this.tagOrTypeInput.addEventListener('input', loadFunction);
			this.attributeKeysInput.addEventListener('input', loadFunction);
			this.emptyCheckbox.addEventListener('input', loadFunction);

		} else {

			let responseText = await response.text();

			this.errorContainer.textContent = 'Loading pages failed - unable to populate bulk editing helper dialog.\n' + responseText;
		}
	}

	loadResults = async () => {

		let typeQuery     = this.tagOrTypeInput.value.trim().toLowerCase();
		let properties    = this.attributeKeysInput.value.trim();
		let showEmptyRows = this.container.querySelector('#empty-checkbox').checked;

		if (!typeQuery || !properties) {
			return;
		}

		if (typeQuery.contains(' ') || typeQuery.contains(',')) {

			this.errorContainer.textContent = 'Only one tag/type can be queried at a time. For all types, use "*" (can take a while, depending on number of page elements).';

			_Helpers.blinkRed(this.tagOrTypeInput);

			return;
		}

		if (typeQuery === '*') {

			typeQuery = 'DOMNode';

		} else {

			typeQuery = typeQuery[0].toUpperCase() + typeQuery.slice(1);
		}

		_Helpers.fastRemoveAllChildren(this.resultContainer);
		_Helpers.fastRemoveAllChildren(this.errorContainer);

		let response = await fetch(Structr.rootUrl + typeQuery + '/all?pageId=' + this.pageSelect.value);

		if (!response.ok) {

			this.errorContainer.textContent = await response.text();

			return;
		}

		let data = await response.json();

		if (data && data.result && data.result.length) {

			let keys = [];

			let keyCandidatesSet = new Set();
			for (let node of data.result) {
				keyCandidatesSet = keyCandidatesSet.union(new Set(Object.keys(node)));
			}

			for (let p of properties.split(',')) {

				let name = p.trim();

				if (keyCandidatesSet.has(name) && name !== 'id' && name !== 'type') {
					keys.push({ name: name, title: name });
				}

				if (keyCandidatesSet.has('_html_' + name)) {
					keys.push({ name: '_html_' + name, title: 'HTML ' + name });
				}

				if (keyCandidatesSet.has('_custom_html_' + name)) {
					keys.push({ name: '_custom_html_' + name, title: 'HTML ' + name });
				}

				if (keyCandidatesSet.has('_custom_html_data-' + name)) {
					keys.push({ name: '_custom_html_data-' + name, title: 'data-' + name });
				}
			}

			let table = _Helpers.createSingleDOMElementFromHTML(`
				<table class="w-full my-3" id="result-table">
					<thead>
						<tr id="header-row">
							<th style="width: 20%;" class="p-1 bg-gray-200">Source</th>
							${keys.map(k => `<th class="p-1 bg-gray-200">${k.title}</th>`).join('')}
						</tr>
					</thead>
				</table>
			`);
			this.resultContainer.appendChild(table);

			_Helpers.sort(data.result, 'internalEntityContextPath');

			for (let node of data.result) {

				let row      = _Helpers.createSingleDOMElementFromHTML(`<tr class="odd:bg-gray-100"></tr>`);
				let hasValue = false;
				let src      = node.type;

				if (node.internalEntityContextPath) {
					src = node.internalEntityContextPath;
				} else if (node.parent && node.parent.name) {
					src = node.parent.name;
				} else if (node.ownerDocument && node.ownerDocument.name) {
					src = node.ownerDocument.name;
				} else if (node.name) {
					src = node.name;
				}

				row.insertAdjacentHTML('beforeend', `<td>${src}</td>`);

				for (let k of keys) {

					let htmlId = `edit-${node.id}-${k.name}`;
					let value  = '';

					if (node.hasOwnProperty(k.name)) {

						if (node[k.name] || node[k.name] === 0) {
							value = node[k.name];
							hasValue = true;
						}

						if (value.includes('\n')) {
							row.insertAdjacentHTML('beforeend', `<td><textarea id="${htmlId}"></textarea></td>`);
						} else {
							row.insertAdjacentHTML('beforeend', `<td><input type="text" id="${htmlId}" class="w-full box-border"></td>`);
						}

						let element = row.querySelector('#' + htmlId);
						element.value = value;

						element.addEventListener('blur', e => {
							Command.setProperty(node.id, k.name, element.value, false, () => {
								_Helpers.blinkGreen(element);
							});
						});

					} else {

						row.insertAdjacentHTML('beforeend', `<td><span class="flex justify-around">-</span></td>`);
					}
				}

				if (hasValue || showEmptyRows) {

					table.appendChild(row);
				}
			}

		} else {

			this.errorContainer.textContent = 'No matching elements found';
		}
	}

	prepareMarkup() {

		this.container.insertAdjacentHTML('beforeend', `
			<div class="relative">

				<div class="inline-info">
					<div class="inline-info-icon top-1 right-1">
						${_Icons.getSvgIcon('info-icon', 20, 20)}
					</div>
					<div class="inline-info-text">
						<p>This helper allows you to find and edit the attributes of elements in Structr pages based on a tag or type and some common attributes.</p>
						<p>Enter a tag or type name and one or more attributes to show all DOM nodes that contain a value in at least one of the fields.</p>
						<div class="flex flex-col gap-2">
							<div>
								<span class="font-bold">Example</span>: div + id,class &rarr; All divs with their id and class attributes
							</div>
							<div>
								<span class="font-bold">Example</span>: Template + name,content &rarr; All Template elements with their name and content attributes
							</div>
							<div>
								<span class="font-bold">Example</span>: * + name,class &rarr; All elements with their name and class attributes
							</div>
						</div>
					</div>
				</div>

				<div id="select-container" class="flex flex-row items-center gap-x-4 justify-between pt-5">

					<div class="flex-grow flex flex-col gap-y-1">
						<label class="font-semibold ml-px">Page</label>
						<select class="w-full" id="page-select"></select>
					</div>

					<div class="flex-grow flex flex-col gap-y-1">
						<label class="font-semibold ml-px">HTML tag / type</label>
						<input class="w-full box-border" id="tag-or-type-input" placeholder="HTML Tag or Type">
					</div>

					<div class="flex-grow flex flex-col gap-y-1">
						<label class="font-semibold ml-px">Attribute key(s)</label>
						<input class="w-full box-border" id="attributes-input" placeholder="Comma-separated list of attribute keys">
					</div>

					<div class="flex flex-col gap-y-1">
						<div>&nbsp;</div>
						<div class="flex items-center gap-x-2">
							<input type="checkbox" id="empty-checkbox" class="mr-0">
							<label for="empty-checkbox">Show empty results</label>
						</div>
					</div>
				</div>

				<div id="result-container"></div>

				<div class="mt-5">
					<pre id="error-container"></pre>
				</div>

			</div>
		`);

		this.pageSelect         = this.container.querySelector('#page-select');
		this.tagOrTypeInput     = this.container.querySelector('#tag-or-type-input');
		this.attributeKeysInput = this.container.querySelector('#attributes-input');
		this.emptyCheckbox      = this.container.querySelector('#empty-checkbox');
		this.resultContainer    = this.container.querySelector('#result-container');
		this.errorContainer     = this.container.querySelector('#error-container');
	}
}