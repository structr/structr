/*
 * Copyright (C) 2010-2019 Structr GmbH
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
class RefactoringHelper {

	constructor(container) {
		this.container = container;
	}

	show() {

		this.container.append('<p>This helper allows you to find and edit HTML elements in Structr pages and Shared Components based on a type and some common attributes.</p>');
		this.container.append('<div id="select-container"></div>');

		var selectContainer = $('#select-container');

		selectContainer.append('<input class="refactoring-helper" id="selector-input" placeholder="Selector" />');
		selectContainer.append('<input class="refactoring-helper" id="query-input" placeholder="Query parameters" />');
		selectContainer.append('<input class="refactoring-helper" id="property-input" placeholder="Property keys to display" />');
		selectContainer.append('<input type="checkbox" id="empty-checkbox" /> Show empty results');
		selectContainer.append('<div id="result-container"></div>');
		selectContainer.append('<div><pre id="error-container"></pre></div>');

		window.setTimeout(() => { $('#selector-input').focus(); }, 100);

		$('#property-input').on('blur', () => { this.loadResults(); });
		$('#empty-checkbox').on('click', () => { this.loadResults(); });
	}

	loadResults() {

		var typeSelector    = $('#selector-input');
		var queryInput      = $('#query-input');
		var keysSelector    = $('#property-input');
		var emptyCheckbox   = $('#empty-checkbox');
		var resultContainer = $('#result-container');
		var errorContainer  = $('#error-container');

		var typeQuery     = typeSelector.val().trim();
		var searchQuery   = queryInput.val().trim();
		var properties    = keysSelector.val().trim();
		var showEmptyRows = emptyCheckbox.is(':checked');

		if (typeQuery && properties) {

			typeQuery = typeQuery[0].toUpperCase() + typeQuery.slice(1);

			resultContainer.empty();
			errorContainer.empty();

			$.ajax({
				url: '/structr/rest/' + typeQuery + '/all' + searchQuery,
				method: 'get',
				statusCode: {
					200: (response) => {

						if (response && response.result && response.result.length) {

							resultContainer.append('<table class="refactoring-helper" id="result-table"><thead><tr id="header-row"></tr></thead></table>');
							var keyMap        = {};
							var keyCandidates = [];
							var table         = $('#result-table');
							var header        = $('#header-row');
							var list          = properties.split(',');
							var keys          = [];
							var i             = 0;

							response.result.forEach(r => {
								Object.keys(r).forEach(k => {
									keyMap[k] = true;
								});
							});

							keyCandidates = Object.keys(keyMap);

							list.forEach(p => {

								var name = p.trim();

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
							});

							header.append('<th>Source</th>');

							keys.forEach(k => {
								header.append('<th>' + k.title + '</th>');
							});

							response.result.sort((a, b) => {
								if (a.internalEntityContextPath < b.internalEntityContextPath) { return -1; }
								if (a.internalEntityContextPath > b.internalEntityContextPath) { return 1; }
								return 0;
							});

							response.result.forEach(v => {

								table.append('<tr id="row' + i + '"></tr>');

								var hasValue = false;
								var row      = $('#row' + i);
								var src      = v.type;

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

								keys.forEach(k => {
									var id    = 'edit-' + v.id + '-' + k.name;
									var value = '';

									if (v[k.name] || v[k.name] === 0) {
										value = v[k.name];
										hasValue = true;
									}

									row.append('<td><input type="text" id="' + id + '" value="' + value + '"/></td>');
									var input = $('#' + id);
									input.on('blur', e => {
										var val = $('#' + id).val();
										Command.setProperty(v.id, k.name, val, false, function() {
											blinkGreen(input);
										});
									});
								});

								i++;

								if (!hasValue && !showEmptyRows) {

									row.remove();
								}
							});
						}
					},
					422:(response) =>{
						errorContainer.append(response.responseText);
					}
				}
			});
		}
	}
}